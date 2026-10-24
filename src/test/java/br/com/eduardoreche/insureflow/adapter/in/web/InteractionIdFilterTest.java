package br.com.eduardoreche.insureflow.adapter.in.web;

import br.com.eduardoreche.insureflow.application.policy.command.CancelPolicy;
import br.com.eduardoreche.insureflow.application.policy.command.EndorsePolicy;
import br.com.eduardoreche.insureflow.application.policy.command.IssuePolicy;
import br.com.eduardoreche.insureflow.application.policy.query.GetPolicy;
import br.com.eduardoreche.insureflow.application.policy.readmodel.GetPolicyInfo;
import br.com.eduardoreche.insureflow.application.policy.readmodel.ListPolicyInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(InteractionIdFilterTest.FixedClock.class)
@ExtendWith(OutputCaptureExtension.class)
class InteractionIdFilterTest {

    static final String POLICY_INFO = "/insurance-patrimonial/AP-001/policy-info";
    static final String ADMIN_POLICY = "/api/v1/policies/AP-001";
    static final String UUID_FORMAT = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
    static final String SENT = "0d8f3c3e-5b8a-4f43-9d3e-6a1f2b7c9e10";

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
    ListPolicyInfo listPolicyInfo;

    @MockitoBean
    GetPolicyInfo getPolicyInfo;

    @Test
    void echoesTheInteractionIdSentByTheCaller() throws Exception {
        mvc.perform(get(POLICY_INFO).header(InteractionIdFilter.HEADER, SENT))
                .andExpect(header().string(InteractionIdFilter.HEADER, SENT));
    }

    @Test
    void generatesAnInteractionIdWhenTheCallerSendsNone() throws Exception {
        mvc.perform(get(POLICY_INFO))
                .andExpect(header().string(InteractionIdFilter.HEADER, matchesPattern(UUID_FORMAT)));
    }

    @Test
    void rejectsAMalformedInteractionIdWith400InTheCommonEnvelopeAndAnswersWithAFreshOne() throws Exception {
        mvc.perform(get(POLICY_INFO).header(InteractionIdFilter.HEADER, "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_INTERACTION_ID"))
                .andExpect(jsonPath("$.errors[0].requestDateTime").value("2026-10-22T12:00:00Z"))
                .andExpect(header().string(InteractionIdFilter.HEADER, matchesPattern(UUID_FORMAT)))
                .andExpect(header().string(InteractionIdFilter.HEADER, not("not-a-uuid")));

        verifyNoInteractions(getPolicyInfo);
    }

    @Test
    void appliesTheSameRuleToTheAdministrativeApi() throws Exception {
        mvc.perform(get(ADMIN_POLICY).header(InteractionIdFilter.HEADER, "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_INTERACTION_ID"));

        verifyNoInteractions(getPolicy);
    }

    @Test
    void tagsTheLogsOfTheRequestWithItsInteractionId(CapturedOutput output) throws Exception {
        when(getPolicyInfo.execute(any())).thenThrow(new IllegalStateException("boom"));

        mvc.perform(get(POLICY_INFO).header(InteractionIdFilter.HEADER, SENT))
                .andExpect(status().isInternalServerError());

        assertThat(output.getAll()).contains("\"interactionId\":\"" + SENT + "\"");
    }
}
