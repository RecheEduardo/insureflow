package br.com.eduardoreche.insureflow.adapter.in.web.admin.policy;

import br.com.eduardoreche.insureflow.application.policy.CancelPolicy;
import br.com.eduardoreche.insureflow.application.policy.EndorsePolicy;
import br.com.eduardoreche.insureflow.application.policy.GetPolicy;
import br.com.eduardoreche.insureflow.application.policy.IssuePolicy;
import br.com.eduardoreche.insureflow.application.policy.dto.IdempotencyKeyDTO;
import br.com.eduardoreche.insureflow.application.policy.dto.IssuePolicyResultDTO;
import br.com.eduardoreche.insureflow.domain.exception.BusinessRuleViolationException;
import br.com.eduardoreche.insureflow.domain.exception.DuplicatePolicyException;
import br.com.eduardoreche.insureflow.domain.model.enums.PolicyStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyController.class)
@Import(PolicyControllerIssueTest.FixedClock.class)
class PolicyControllerIssueTest {

    static final String BODY = """
            {"policyId":"AP-001","documentType":"APOLICE_INDIVIDUAL","issuanceType":"EMISSAO_PROPRIA",
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

    @Test
    void issuesAndAnswers201WithLocationAndETag() throws Exception {
        when(issuePolicy.execute(any(), any())).thenReturn(new IssuePolicyResultDTO("AP-001", PolicyStatus.ACTIVE, 0));

        mvc.perform(post("/api/v1/policies").header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/policies/AP-001"))
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void sameBodyWithDifferentLayoutProducesTheSameRequestHash() throws Exception {
        when(issuePolicy.execute(any(), any())).thenReturn(new IssuePolicyResultDTO("AP-001", PolicyStatus.ACTIVE, 0));
        String reformatted = BODY.replace(",\"", ", \"").replace("\n", "");

        mvc.perform(post("/api/v1/policies").header("Idempotency-Key", "key-1")
                .contentType(MediaType.APPLICATION_JSON).content(BODY));
        mvc.perform(post("/api/v1/policies").header("Idempotency-Key", "key-1")
                .contentType(MediaType.APPLICATION_JSON).content(reformatted));

        ArgumentCaptor<IdempotencyKeyDTO> keys = ArgumentCaptor.forClass(IdempotencyKeyDTO.class);
        verify(issuePolicy, times(2)).execute(any(), keys.capture());
        assertThat(keys.getAllValues()).extracting(IdempotencyKeyDTO::key).containsOnly("key-1");
        assertThat(keys.getAllValues().get(0).requestHash())
                .hasSize(64)
                .isEqualTo(keys.getAllValues().get(1).requestHash());
    }

    @Test
    void differentBodyProducesADifferentRequestHash() throws Exception {
        when(issuePolicy.execute(any(), any())).thenReturn(new IssuePolicyResultDTO("AP-001", PolicyStatus.ACTIVE, 0));

        mvc.perform(post("/api/v1/policies").header("Idempotency-Key", "key-1")
                .contentType(MediaType.APPLICATION_JSON).content(BODY));
        mvc.perform(post("/api/v1/policies").header("Idempotency-Key", "key-1")
                .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("200000.00", "250000.00")));

        ArgumentCaptor<IdempotencyKeyDTO> keys = ArgumentCaptor.forClass(IdempotencyKeyDTO.class);
        verify(issuePolicy, times(2)).execute(any(), keys.capture());
        assertThat(keys.getAllValues().get(0).requestHash())
                .isNotEqualTo(keys.getAllValues().get(1).requestHash());
    }

    @Test
    void answers422WhenTheKeyWasUsedWithAnotherBody() throws Exception {
        when(issuePolicy.execute(any(), any())).thenThrow(new BusinessRuleViolationException(
                "IDEMPOTENCY_KEY_REUSED", "idempotency key was already used with a different request"));

        mvc.perform(post("/api/v1/policies").header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].code").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void answers409WhenThePolicyIdAlreadyExists() throws Exception {
        when(issuePolicy.execute(any(), any())).thenThrow(new DuplicatePolicyException("AP-001"));

        mvc.perform(post("/api/v1/policies").header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].code").value("POLICY_DUPLICATE"));
    }

    @Test
    void rejectsMissingIdempotencyKey() throws Exception {
        mvc.perform(post("/api/v1/policies").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("IDEMPOTENCY_KEY_REQUIRED"));

        verifyNoInteractions(issuePolicy);
    }

    @Test
    void rejectsMalformedIdempotencyKey() throws Exception {
        mvc.perform(post("/api/v1/policies").header("Idempotency-Key", "chave com espaço")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_IDEMPOTENCY_KEY"));
        mvc.perform(post("/api/v1/policies").header("Idempotency-Key", "k".repeat(256))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(issuePolicy);
    }
}
