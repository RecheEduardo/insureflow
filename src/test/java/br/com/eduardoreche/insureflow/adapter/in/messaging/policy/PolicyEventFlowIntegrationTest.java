package br.com.eduardoreche.insureflow.adapter.in.messaging.policy;

import br.com.eduardoreche.insureflow.TestcontainersConfiguration;
import br.com.eduardoreche.insureflow.application.policy.PolicyFixtures;
import br.com.eduardoreche.insureflow.application.policy.event.PolicyEventType;
import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.KafkaHeaders;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class PolicyEventFlowIntegrationTest {

    private static final Duration PROPAGATION = Duration.ofSeconds(30);

    private static final String BODY = """
            {"policyId":"%s","documentType":"APOLICE_INDIVIDUAL","issuanceType":"EMISSAO_PROPRIA",
             "issuanceDate":"2026-10-01","termStartDate":"2026-10-01","termEndDate":"2027-09-30",
             "maxLMG":"200000.00","proposalId":"PROP-001","productName":"Residencial Essencial",
             "insureds":[{"identification":"12345678901","identificationType":"CPF","name":"Maria Ficticia",
               "birthDate":"1990-05-17","address":{"streetType":"RUA","streetName":"Rua Exemplo","number":"100",
               "townName":"Sao Paulo","state":"SP","postCode":"01310100"}}],
             "insuredObjects":[{"identification":"IMOVEL-1","propertyType":"CASA","description":"Casa residencial",
               "postCode":"01310100","coverages":[{"branch":"0118","code":"INCENDIO",
               "susepProcessNumber":"15414.900000/2020-11","lmi":"100000.00","termStartDate":"2026-10-01",
               "termEndDate":"2027-09-30","mainCoverage":true,"feature":"MASSIFICADOS","type":"REGULAR_COMUM",
               "premiumPeriodicity":"ANUAL"}]}]}
            """;

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @Value("${local.server.port}")
    int port;

    @Value("${insureflow.kafka.policy-events.topic}")
    String topic;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    KafkaTemplate<String, String> kafka;

    @Autowired
    ConsumerFactory<String, String> consumers;

    @Autowired
    KafkaListenerEndpointRegistry listeners;

    @Test
    void issuanceEndorsementAndCancellationReachTheReadModelAndTheHistoryInOrder() throws Exception {
        assertThat(issue("AP-FLOW-1", "key-flow-1").statusCode()).isEqualTo(201);
        await().atMost(PROPAGATION).untilAsserted(() ->
                assertThat(get("/insurance-patrimonial/AP-FLOW-1/policy-info").statusCode()).isEqualTo(200));

        assertThat(endorse("AP-FLOW-1", "{\"termEndDate\":\"2028-09-30\",\"productName\":\"Residencial Plus\"}")
                .statusCode()).isEqualTo(200);
        await().atMost(PROPAGATION).untilAsserted(() ->
                assertThat(get("/insurance-patrimonial/AP-FLOW-1/policy-info").body()).contains("2028-09-30"));
        assertThat(get("/insurance-patrimonial?page-size=1000").body())
                .contains("{\"policyId\":\"AP-FLOW-1\",\"productName\":\"Residencial Plus\"}");

        assertThat(post("/api/v1/policies/AP-FLOW-1/cancellation").statusCode()).isEqualTo(200);
        await().atMost(PROPAGATION).untilAsserted(() -> assertThat(historyOf("AP-FLOW-1"))
                .containsExactly("POLICY_ISSUED", "POLICY_ENDORSED", "POLICY_CANCELLED"));

        assertThat(jdbc.queryForList("SELECT policy_version FROM policy_event_history WHERE policy_id = 'AP-FLOW-1' "
                + "ORDER BY policy_version", Long.class)).containsExactly(0L, 1L, 2L);
        assertThat(get("/insurance-patrimonial/AP-FLOW-1/policy-info").statusCode()).isEqualTo(200);
        assertThat(outboxRowsOf("AP-FLOW-1")).isZero();
    }

    @Test
    void eventsOfOnePolicyLandInTheSamePartitionInTheOrderTheyHappened() throws Exception {
        issue("AP-FLOW-2", "key-flow-2");
        endorse("AP-FLOW-2", "{\"productName\":\"Residencial Plus\"}");
        post("/api/v1/policies/AP-FLOW-2/cancellation");

        List<ConsumerRecord<String, String>> records = consume(topic, "AP-FLOW-2", 3);

        assertThat(records).extracting(ConsumerRecord::partition).containsOnly(records.getFirst().partition());
        assertThat(records).extracting(record -> header(record, "eventType"))
                .containsExactly("POLICY_ISSUED", "POLICY_ENDORSED", "POLICY_CANCELLED");
    }

    @Test
    void rejectedDuplicateIssuancePublishesNothing() throws Exception {
        assertThat(issue("AP-FLOW-3", "key-flow-3a").statusCode()).isEqualTo(201);
        assertThat(issue("AP-FLOW-3", "key-flow-3b").statusCode()).isEqualTo(409);

        await().atMost(PROPAGATION).untilAsserted(() -> assertThat(historyOf("AP-FLOW-3")).hasSize(1));
        assertThat(outboxRowsOf("AP-FLOW-3")).isZero();
        assertThat(historyOf("AP-FLOW-3")).containsExactly("POLICY_ISSUED");
    }

    @Test
    void unreadableMessageGoesToTheDeadLetterTopicWithoutBlockingThePartitionOrLeakingItsContent(
            CapturedOutput output) throws Exception {
        String poison = "{\"policyId\":\"AP-FLOW-4\",\"identification\":98765432100x";
        PolicyEventDTO valid =
                PolicyEventDTO.of(PolicyEventType.POLICY_ISSUED, PolicyFixtures.activePolicy("AP-FLOW-4"));

        kafka.send(topic, "AP-FLOW-4", poison).get();
        kafka.send(topic, "AP-FLOW-4", json.writeValueAsString(valid)).get();

        await().atMost(PROPAGATION).untilAsserted(() ->
                assertThat(get("/insurance-patrimonial/AP-FLOW-4/policy-info").statusCode()).isEqualTo(200));
        List<ConsumerRecord<String, String>> deadLetters = consume(topic + ".DLT", "AP-FLOW-4", 2);
        assertThat(deadLetters).extracting(ConsumerRecord::value).containsOnly(poison);
        assertThat(deadLetters).extracting(record -> header(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE))
                .allSatisfy(message -> assertThat(message).contains("could not be read"));
        assertThat(output.getAll()).doesNotContain("98765432100");
    }

    @Test
    void theReadModelGroupSplitsTheSixPartitionsAmongItsConsumers() {
        ConcurrentMessageListenerContainer<?, ?> container =
                (ConcurrentMessageListenerContainer<?, ?>) listeners.getListenerContainer("open-insurance-read-model");

        await().atMost(PROPAGATION).untilAsserted(() -> assertThat(container.getContainers())
                .hasSize(3)
                .allSatisfy(consumer -> assertThat(consumer.getAssignedPartitions()).hasSize(2)));
    }

    private List<ConsumerRecord<String, String>> consume(String fromTopic, String key, int expected) {
        List<ConsumerRecord<String, String>> records = new ArrayList<>();
        try (Consumer<String, String> consumer = consumers.createConsumer("test-" + UUID.randomUUID(), null)) {
            consumer.subscribe(List.of(fromTopic));
            await().atMost(PROPAGATION).pollInSameThread().until(() -> {
                consumer.poll(Duration.ofMillis(500)).forEach(record -> {
                    if (key.equals(record.key())) {
                        records.add(record);
                    }
                });
                return records.size() >= expected;
            });
        }
        return records;
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }

    private List<String> historyOf(String policyId) {
        return jdbc.queryForList("SELECT event_type FROM policy_event_history WHERE policy_id = ? "
                + "ORDER BY policy_version", String.class, policyId);
    }

    private int outboxRowsOf(String policyId) {
        return jdbc.queryForObject("SELECT count(*) FROM policy_outbox WHERE policy_id = ?", Integer.class, policyId);
    }

    private HttpResponse<String> issue(String policyId, String idempotencyKey) throws Exception {
        return http.send(HttpRequest.newBuilder(uri("/api/v1/policies"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .POST(HttpRequest.BodyPublishers.ofString(BODY.formatted(policyId)))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> endorse(String policyId, String body) throws Exception {
        return http.send(HttpRequest.newBuilder(uri("/api/v1/policies/" + policyId))
                .header("Content-Type", "application/json")
                .header("If-Match", "\"0\"")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
