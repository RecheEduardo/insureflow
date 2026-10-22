package br.com.eduardoreche.insureflow.adapter.in.web.admin.policy;

import br.com.eduardoreche.insureflow.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class PolicyIdempotencyIntegrationTest {

    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void repeatedRequestWithSameKeyAndBodyReturnsTheOriginalResponse() throws Exception {
        HttpResponse<String> first = issue("key-replay", body("AP-IDEM-1", "200000.00"));
        HttpResponse<String> second = issue("key-replay", body("AP-IDEM-1", "200000.00"));

        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(second.statusCode()).isEqualTo(201);
        assertThat(second.body()).isEqualTo(first.body());
        assertThat(second.headers().firstValue("ETag")).isEqualTo(first.headers().firstValue("ETag"));
        assertThat(second.headers().firstValue("Location")).contains("/api/v1/policies/AP-IDEM-1");
        assertThat(policiesWithId("AP-IDEM-1")).isOne();
    }

    @Test
    void replayKeepsTheOriginalResponseAfterAnEndorsement() throws Exception {
        issue("key-endorsed", body("AP-IDEM-2", "200000.00"));
        HttpResponse<String> endorsed = http.send(HttpRequest.newBuilder(uri("/api/v1/policies/AP-IDEM-2"))
                .header("Content-Type", "application/json")
                .header("If-Match", "\"0\"")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"productName\":\"Residencial Plus\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> replayed = issue("key-endorsed", body("AP-IDEM-2", "200000.00"));

        assertThat(endorsed.headers().firstValue("ETag")).contains("\"1\"");
        assertThat(replayed.statusCode()).isEqualTo(201);
        assertThat(replayed.headers().firstValue("ETag")).contains("\"0\"");
    }

    @Test
    void sameKeyWithDifferentBodyIsRejectedWith422() throws Exception {
        issue("key-reused", body("AP-IDEM-3", "200000.00"));

        HttpResponse<String> reused = issue("key-reused", body("AP-IDEM-3", "250000.00"));

        assertThat(reused.statusCode()).isEqualTo(422);
        assertThat(reused.body()).contains("IDEMPOTENCY_KEY_REUSED");
        assertThat(jdbc.queryForObject("SELECT max_lmg_amount::text FROM policy WHERE policy_id = 'AP-IDEM-3'",
                String.class)).isEqualTo("200000.00");
    }

    @Test
    void differentKeysForTheSamePolicyIdStillConflict() throws Exception {
        issue("key-a", body("AP-IDEM-4", "200000.00"));

        HttpResponse<String> duplicate = issue("key-b", body("AP-IDEM-4", "200000.00"));

        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM idempotency_record WHERE idempotency_key = 'key-b'",
                Integer.class)).isZero();
    }

    @Test
    void requestWithoutIdempotencyKeyIsRejectedWith400AndIssuesNothing() throws Exception {
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(uri("/api/v1/policies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body("AP-IDEM-6", "200000.00")))
                .build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("IDEMPOTENCY_KEY_REQUIRED");
        assertThat(policiesWithId("AP-IDEM-6")).isZero();
    }

    @Test
    void concurrentRequestsWithSameKeyIssueOnePolicyAndAllGetTheSameResponse() throws Exception {
        int requests = 8;
        CountDownLatch start = new CountDownLatch(1);
        Callable<HttpResponse<String>> request = () -> {
            start.await();
            return issue("key-concurrent", body("AP-IDEM-5", "200000.00"));
        };

        List<HttpResponse<String>> responses;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<HttpResponse<String>>> futures = IntStream.range(0, requests)
                    .mapToObj(i -> executor.submit(request))
                    .toList();
            start.countDown();
            responses = futures.stream().map(PolicyIdempotencyIntegrationTest::join).toList();
        }

        assertThat(responses).extracting(HttpResponse::statusCode).containsOnly(201);
        assertThat(responses).extracting(HttpResponse::body).containsOnly(responses.getFirst().body());
        assertThat(policiesWithId("AP-IDEM-5")).isOne();
    }

    private HttpResponse<String> issue(String idempotencyKey, String body) throws Exception {
        return http.send(HttpRequest.newBuilder(uri("/api/v1/policies"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private int policiesWithId(String policyId) {
        return jdbc.queryForObject("SELECT count(*) FROM policy WHERE policy_id = ?", Integer.class, policyId);
    }

    private static String body(String policyId, String maxLmg) {
        return PolicyControllerIssueTest.BODY
                .replace("\"AP-001\"", "\"" + policyId + "\"")
                .replace("\"200000.00\"", "\"" + maxLmg + "\"");
    }

    private static <T> T join(Future<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
