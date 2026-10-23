package br.com.eduardoreche.insureflow.adapter.in.web;

import br.com.eduardoreche.insureflow.TestcontainersConfiguration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class OpenApiContractTest {

    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    int port;

    @Test
    void adminGroupDocumentsOnlyTheAdministrativeRoutes() throws Exception {
        DocumentContext admin = apiDocs("admin");

        Map<String, Object> paths = admin.read("$.paths");
        assertThat(paths).containsOnlyKeys("/api/v1/policies", "/api/v1/policies/{policyId}",
                "/api/v1/policies/{policyId}/cancellation");
        assertThat(admin.read("$.info.description", String.class)).contains("fictícia");
    }

    @Test
    void issuanceDeclaresCreatedWithLocationAndETagAndItsErrors() throws Exception {
        DocumentContext admin = apiDocs("admin");

        Map<String, Object> responses = admin.read("$.paths['/api/v1/policies'].post.responses");
        assertThat(responses).containsOnlyKeys("201", "400", "409", "422");
        Map<String, Object> headers = admin.read("$.paths['/api/v1/policies'].post.responses['201'].headers");
        assertThat(headers).containsOnlyKeys("Location", "ETag");
        assertThat(admin.read("$.paths['/api/v1/policies'].post.parameters[?(@.name == 'Idempotency-Key')].required",
                List.class)).containsExactly(true);
    }

    @Test
    void endorsementRequiresIfMatchAndDeclaresConcurrencyStatuses() throws Exception {
        DocumentContext admin = apiDocs("admin");

        assertThat(admin.read("$.paths['/api/v1/policies/{policyId}'].patch.parameters[?(@.name == 'If-Match')].required",
                List.class)).containsExactly(true);
        Map<String, Object> responses = admin.read("$.paths['/api/v1/policies/{policyId}'].patch.responses");
        assertThat(responses).containsOnlyKeys("200", "400", "404", "409", "412", "422", "428");
    }

    @Test
    void everyErrorResponseUsesTheSameEnvelope() throws Exception {
        for (String group : List.of("admin", "open-insurance")) {
            List<Map<String, Object>> responsesByOperation = apiDocs(group).read("$.paths.*.*.responses");
            List<Object> errorRefs = responsesByOperation.stream()
                    .flatMap(responses -> responses.entrySet().stream())
                    .filter(response -> response.getKey().matches("[45]\\d\\d"))
                    .map(response -> JsonPath.read(response.getValue(), "$.content['application/json'].schema.$ref"))
                    .toList();

            assertThat(errorRefs).isNotEmpty().containsOnly("#/components/schemas/ErrorResponseDTO");
        }
    }

    @Test
    void openInsuranceGroupDocumentsOnlyTheQueries() throws Exception {
        DocumentContext openInsurance = apiDocs("open-insurance");

        Map<String, Object> paths = openInsurance.read("$.paths");
        assertThat(paths).containsOnlyKeys("/insurance-patrimonial", "/insurance-patrimonial/{policyId}/policy-info");
        Map<String, Object> responses =
                openInsurance.read("$.paths['/insurance-patrimonial/{policyId}/policy-info'].get.responses");
        assertThat(responses).containsOnlyKeys("200", "404");
    }

    @Test
    void documentedIssuanceExampleIsAcceptedByTheApi() throws Exception {
        String example = JsonPath.parse((Object) apiDocs("admin").read(
                "$.paths['/api/v1/policies'].post.requestBody.content['application/json'].example")).jsonString();

        HttpResponse<String> response = http.send(HttpRequest.newBuilder(uri("/api/v1/policies"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", "openapi-example")
                .POST(HttpRequest.BodyPublishers.ofString(example))
                .build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(201);
    }

    @Test
    void swaggerUiIsServedLocally() throws Exception {
        HttpResponse<String> response = get("/swagger-ui/index.html");

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void scalarIsServedLocallyWithBothGroupsAndNoTelemetry() throws Exception {
        HttpResponse<String> page = get("/scalar");

        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body())
                .contains("/v3/api-docs/admin", "/v3/api-docs/open-insurance")
                .contains("\"telemetry\":false")
                .contains("src=\"/scalar/scalar.js\"");
        assertThat(get("/scalar/scalar.js").statusCode()).isEqualTo(200);
    }

    private DocumentContext apiDocs(String group) throws Exception {
        HttpResponse<String> response = get("/v3/api-docs/" + group);
        assertThat(response.statusCode()).isEqualTo(200);
        return JsonPath.parse(response.body());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
