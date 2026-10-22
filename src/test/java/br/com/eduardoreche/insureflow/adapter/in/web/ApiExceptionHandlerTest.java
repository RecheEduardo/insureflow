package br.com.eduardoreche.insureflow.adapter.in.web;

import br.com.eduardoreche.insureflow.application.policy.CancelPolicy;
import br.com.eduardoreche.insureflow.application.policy.EndorsePolicy;
import br.com.eduardoreche.insureflow.application.policy.GetPolicy;
import br.com.eduardoreche.insureflow.application.policy.IssuePolicy;
import br.com.eduardoreche.insureflow.application.policy.ListPolicies;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(ApiExceptionHandlerTest.FixedClock.class)
class ApiExceptionHandlerTest {

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-22T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    IssuePolicy issuePolicy;

    @MockitoBean
    GetPolicy getPolicy;

    @MockitoBean
    CancelPolicy cancelPolicy;

    @MockitoBean
    EndorsePolicy endorsePolicy;

    @MockitoBean
    ListPolicies listPolicies;

    @Test
    void answersAnUnknownRouteWith404InTheErrorEnvelope() throws Exception {
        mvc.perform(get("/api/v1/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.errors[0].requestDateTime").value("2026-10-22T12:00:00Z"));
    }

    @Test
    void answersAnUnsupportedMethodWith405AndTheAllowHeader() throws Exception {
        mvc.perform(delete("/api/v1/policies/AP-001"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", allOf(containsString("GET"), containsString("PATCH"))))
                .andExpect(jsonPath("$.errors[0].code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void answersANonJsonBodyWith415() throws Exception {
        mvc.perform(post("/api/v1/policies").header("Idempotency-Key", "key-1")
                        .contentType(MediaType.TEXT_PLAIN).content("policy"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errors[0].code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void answersAnUnexpectedFailureWith500WithoutLeakingTheCause() throws Exception {
        when(getPolicy.execute("AP-001")).thenThrow(new IllegalStateException("connection refused"));

        mvc.perform(get("/api/v1/policies/AP-001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errors[0].code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.errors[0].detail").value("Erro inesperado. Tente novamente mais tarde."));
    }
}
