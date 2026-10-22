package br.com.eduardoreche.insureflow.adapter.in.web.admin.policy;

import br.com.eduardoreche.insureflow.application.policy.CancelPolicy;
import br.com.eduardoreche.insureflow.application.policy.EndorsePolicy;
import br.com.eduardoreche.insureflow.application.policy.GetPolicy;
import br.com.eduardoreche.insureflow.application.policy.IssuePolicy;
import br.com.eduardoreche.insureflow.domain.exception.PolicyNotFoundException;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import br.com.eduardoreche.insureflow.domain.model.enums.CancellationReason;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static br.com.eduardoreche.insureflow.application.policy.PolicyFixtures.NOW;
import static br.com.eduardoreche.insureflow.application.policy.PolicyFixtures.activePolicy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyController.class)
@Import(PolicyControllerGetTest.FixedClock.class)
class PolicyControllerGetTest {

    static final String URL = "/api/v1/policies/AP-001";

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
    GetPolicy getPolicy;

    @MockitoBean
    IssuePolicy issuePolicy;

    @MockitoBean
    CancelPolicy cancelPolicy;

    @MockitoBean
    EndorsePolicy endorsePolicy;

    @Test
    void answersThePolicyWithItsVersionAsETag() throws Exception {
        when(getPolicy.execute("AP-001")).thenReturn(activePolicy("AP-001"));

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.policyId").value("AP-001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.maxLMG").value("200000.00"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.cancelledAt").isEmpty());
    }

    @Test
    void showsTheFullInsuredDocumentToTheOperator() throws Exception {
        when(getPolicy.execute("AP-001")).thenReturn(activePolicy("AP-001"));

        mvc.perform(get(URL))
                .andExpect(jsonPath("$.insureds[0].identification").value("12345678901"));
    }

    @Test
    void keepsAnsweringACancelledPolicyWithItsCancellationData() throws Exception {
        Policy policy = activePolicy("AP-001");
        policy.cancel(CancellationReason.PEDIDO_DO_SEGURADO, NOW);
        when(getPolicy.execute("AP-001")).thenReturn(policy);

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("PEDIDO_DO_SEGURADO"))
                .andExpect(jsonPath("$.cancelledAt").value("2026-10-22T12:00:00Z"));
    }

    @Test
    void answers404WhenThePolicyDoesNotExist() throws Exception {
        when(getPolicy.execute("AP-001")).thenThrow(new PolicyNotFoundException("AP-001"));

        mvc.perform(get(URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("POLICY_NOT_FOUND"));
    }
}
