package br.com.eduardoreche.insureflow;

import com.github.dockerjava.api.DockerClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.kafka.KafkaContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class KafkaOutageIntegrationTest {

    private static final String BODY = """
            {"policyId":"AP-OUTAGE-1","documentType":"APOLICE_INDIVIDUAL","issuanceType":"EMISSAO_PROPRIA",
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

    @Value("${local.server.port}")
    int port;

    @Autowired
    KafkaContainer kafka;

    @Autowired
    MeterRegistry meters;

    @Test
    void aBrokerOutageDelaysEventsInTheOutboxWithoutTakingTheApiOutOfService() throws Exception {
        DockerClient docker = kafka.getDockerClient();
        docker.pauseContainerCmd(kafka.getContainerId()).exec();
        try {
            assertThat(issue().statusCode()).isEqualTo(201);

            HttpResponse<String> readiness = get("/actuator/health/readiness");
            assertThat(readiness.statusCode()).isEqualTo(200);
            assertThat(readiness.body()).contains("\"status\":\"UP\"");

            await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).until(() -> pending() == 1);
        } finally {
            docker.unpauseContainerCmd(kafka.getContainerId()).exec();
        }

        await().atMost(Duration.ofSeconds(60)).until(() -> pending() == 0);
    }

    private double pending() {
        return meters.get("insureflow.outbox.pending").gauge().value();
    }

    private HttpResponse<String> issue() throws Exception {
        return http.send(HttpRequest.newBuilder(uri("/api/v1/policies"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", "key-outage-1")
                .POST(HttpRequest.BodyPublishers.ofString(BODY))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
