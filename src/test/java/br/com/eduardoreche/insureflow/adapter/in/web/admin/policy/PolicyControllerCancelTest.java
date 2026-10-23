package br.com.eduardoreche.insureflow.adapter.in.web.admin.policy;

import br.com.eduardoreche.insureflow.application.policy.command.CancelPolicy;
import br.com.eduardoreche.insureflow.application.policy.command.EndorsePolicy;
import br.com.eduardoreche.insureflow.application.policy.command.IssuePolicy;
import br.com.eduardoreche.insureflow.application.policy.command.dto.CancelPolicyCommandDTO;
import br.com.eduardoreche.insureflow.application.policy.query.GetPolicy;
import br.com.eduardoreche.insureflow.domain.exception.PolicyNotFoundException;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import br.com.eduardoreche.insureflow.domain.model.enums.CancellationReason;
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

import static br.com.eduardoreche.insureflow.application.policy.PolicyFixtures.NOW;
import static br.com.eduardoreche.insureflow.application.policy.PolicyFixtures.activePolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyController.class)
@Import(PolicyControllerCancelTest.FixedClock.class)
class PolicyControllerCancelTest {

    static final String URL = "/api/v1/policies/AP-001/cancellation";

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
    CancelPolicy cancelPolicy;

    @MockitoBean
    IssuePolicy issuePolicy;

    @MockitoBean
    GetPolicy getPolicy;

    @MockitoBean
    EndorsePolicy endorsePolicy;

    @Test
    void cancelsWithTheGivenReasonAndAnswersTheCancelledPolicy() throws Exception {
        when(cancelPolicy.execute(any())).thenReturn(cancelled(CancellationReason.FALTA_DE_PAGAMENTO));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"FALTA_DE_PAGAMENTO\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("FALTA_DE_PAGAMENTO"))
                .andExpect(jsonPath("$.cancelledAt").value("2026-10-22T12:00:00Z"));

        assertThat(captureCommand()).isEqualTo(new CancelPolicyCommandDTO("AP-001", CancellationReason.FALTA_DE_PAGAMENTO));
    }

    @Test
    void cancelsWithReasonOutrosWhenTheBodyIsAbsent() throws Exception {
        when(cancelPolicy.execute(any())).thenReturn(cancelled(CancellationReason.OUTROS));

        mvc.perform(post(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancellationReason").value("OUTROS"));

        assertThat(captureCommand().reason()).isEqualTo(CancellationReason.OUTROS);
    }

    @Test
    void rejectsAnUnknownReason() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"ARREPENDIMENTO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

        verifyNoInteractions(cancelPolicy);
    }

    @Test
    void answers404WhenThePolicyDoesNotExist() throws Exception {
        when(cancelPolicy.execute(any())).thenThrow(new PolicyNotFoundException("AP-001"));

        mvc.perform(post(URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("POLICY_NOT_FOUND"));
    }

    private static Policy cancelled(CancellationReason reason) {
        Policy policy = activePolicy("AP-001");
        policy.cancel(reason, NOW);
        return policy;
    }

    private CancelPolicyCommandDTO captureCommand() {
        ArgumentCaptor<CancelPolicyCommandDTO> captor = ArgumentCaptor.forClass(CancelPolicyCommandDTO.class);
        verify(cancelPolicy).execute(captor.capture());
        return captor.getValue();
    }
}
