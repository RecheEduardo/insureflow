package br.com.eduardoreche.insureflow.adapter.in.web.admin.policy;

import br.com.eduardoreche.insureflow.application.policy.command.CancelPolicy;
import br.com.eduardoreche.insureflow.application.policy.command.EndorsePolicy;
import br.com.eduardoreche.insureflow.application.policy.command.IssuePolicy;
import br.com.eduardoreche.insureflow.application.policy.command.dto.EndorsePolicyCommandDTO;
import br.com.eduardoreche.insureflow.application.policy.query.GetPolicy;
import br.com.eduardoreche.insureflow.domain.exception.PolicyCancelledException;
import br.com.eduardoreche.insureflow.domain.exception.PolicyNotFoundException;
import br.com.eduardoreche.insureflow.domain.exception.PolicyVersionMismatchException;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import static br.com.eduardoreche.insureflow.application.policy.PolicyFixtures.activePolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyController.class)
@Import(PolicyControllerEndorseTest.FixedClock.class)
class PolicyControllerEndorseTest {

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-12-01T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    EndorsePolicy endorsePolicy;

    @MockitoBean
    IssuePolicy issuePolicy;

    @MockitoBean
    GetPolicy getPolicy;

    @MockitoBean
    CancelPolicy cancelPolicy;

    @Test
    void refusesEndorsementWithoutIfMatchHeader() throws Exception {
        mvc.perform(patch("/api/v1/policies/AP-001").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Residencial Plus\"}"))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.errors[0].code").value("IF_MATCH_REQUIRED"))
                .andExpect(jsonPath("$.errors[0].requestDateTime").value("2026-12-01T10:00:00Z"));

        verifyNoInteractions(endorsePolicy);
    }

    @Test
    void refusesMalformedIfMatch() throws Exception {
        mvc.perform(patch("/api/v1/policies/AP-001").header("If-Match", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Residencial Plus\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_IF_MATCH"));

        verifyNoInteractions(endorsePolicy);
    }

    @Test
    void endorsesAndReturnsTheNewEtagWhenIfMatchIsTheCurrentVersion() throws Exception {
        Policy endorsed = activePolicy("AP-001");
        when(endorsePolicy.execute(any())).thenReturn(endorsed);

        mvc.perform(patch("/api/v1/policies/AP-001").header("If-Match", "\"3\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Residencial Plus\",\"maxLMG\":\"250000.00\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.policyId").value("AP-001"));

        ArgumentCaptor<EndorsePolicyCommandDTO> captor = ArgumentCaptor.forClass(EndorsePolicyCommandDTO.class);
        verify(endorsePolicy).execute(captor.capture());
        assertThat(captor.getValue().expectedVersion()).isEqualTo(3L);
        assertThat(captor.getValue().productName()).isEqualTo("Residencial Plus");
        assertThat(captor.getValue().maxLmg().amount()).hasToString("250000.00");
    }

    @Test
    void answers412WhenTheVersionIsStale() throws Exception {
        when(endorsePolicy.execute(any())).thenThrow(new PolicyVersionMismatchException("AP-001", 3, 4));

        mvc.perform(patch("/api/v1/policies/AP-001").header("If-Match", "\"3\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Residencial Plus\"}"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.errors[0].code").value("POLICY_VERSION_MISMATCH"));
    }

    @Test
    void answers409ForCancelledPolicyAndMissingPolicyAnswers404() throws Exception {
        when(endorsePolicy.execute(any())).thenThrow(new PolicyCancelledException("AP-001"));
        mvc.perform(patch("/api/v1/policies/AP-001").header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Residencial Plus\"}"))
                .andExpect(status().isConflict());

        doThrow(new PolicyNotFoundException("AP-404")).when(endorsePolicy).execute(any());
        mvc.perform(patch("/api/v1/policies/AP-404").header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Residencial Plus\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsImmutableOrUnknownFieldsInTheBody() throws Exception {
        mvc.perform(patch("/api/v1/policies/AP-001").header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyId\":\"OUTRO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

        verify(endorsePolicy, never()).execute(any());
    }
}
