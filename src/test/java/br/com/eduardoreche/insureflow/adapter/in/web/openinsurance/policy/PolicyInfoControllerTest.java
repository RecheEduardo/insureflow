package br.com.eduardoreche.insureflow.adapter.in.web.openinsurance.policy;

import br.com.eduardoreche.insureflow.application.policy.GetPolicy;
import br.com.eduardoreche.insureflow.application.policy.ListPolicies;
import br.com.eduardoreche.insureflow.application.policy.PolicyFixtures;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OpenInsurancePolicyController.class)
@Import(PolicyInfoControllerTest.FixedClock.class)
class PolicyInfoControllerTest {

    static final String URL = "/insurance-patrimonial/AP-001/policy-info";

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
    ListPolicies listPolicies;

    @Test
    void answersWithTheMappedFieldsOfThePolicy() throws Exception {
        when(getPolicy.execute("AP-001")).thenReturn(PolicyFixtures.activePolicy("AP-001"));

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").value("APOLICE_INDIVIDUAL"))
                .andExpect(jsonPath("$.policyId").value("AP-001"))
                .andExpect(jsonPath("$.issuanceType").value("EMISSAO_PROPRIA"))
                .andExpect(jsonPath("$.issuanceDate").value("2026-10-01"))
                .andExpect(jsonPath("$.termStartDate").value("2026-10-01"))
                .andExpect(jsonPath("$.termEndDate").value("2027-09-30"))
                .andExpect(jsonPath("$.maxLMG.amount").value("200000.00"))
                .andExpect(jsonPath("$.maxLMG.currency").value("BRL"))
                .andExpect(jsonPath("$.proposalId").value("PROP-001"))
                .andExpect(jsonPath("$.insureds[0].identificationType").value("CPF"))
                .andExpect(jsonPath("$.insureds[0].address.countrySubDivision").value("SP"))
                .andExpect(jsonPath("$.insuredObjects[0].identification").value("IMOVEL-1"))
                .andExpect(jsonPath("$.insuredObjects[0].propertyType").value("CASA"))
                .andExpect(jsonPath("$.insuredObjects[0].postCode").value("01310100"))
                .andExpect(jsonPath("$.insuredObjects[0].coverages[0].LMI.amount").value("100000.00"))
                .andExpect(jsonPath("$.insuredObjects[0].coverages[0].isMainCoverage").value(true));
    }

    @Test
    void masksTheInsuredDocumentKeepingOnlyTheLastTwoDigits() throws Exception {
        when(getPolicy.execute("AP-001")).thenReturn(PolicyFixtures.activePolicy("AP-001"));

        mvc.perform(get(URL))
                .andExpect(jsonPath("$.insureds[0].identification").value("***01"));
    }

    @Test
    void omitsOptionalFieldsThatAreAbsent() throws Exception {
        when(getPolicy.execute("AP-001")).thenReturn(PolicyFixtures.activePolicy("AP-001"));

        mvc.perform(get(URL))
                .andExpect(jsonPath("$.groupCertificateId").doesNotExist())
                .andExpect(jsonPath("$.insureds[0].email").doesNotExist())
                .andExpect(jsonPath("$.insuredObjects[0].amount").doesNotExist());
    }

    @Test
    void keepsShowingACancelledPolicyWithoutAStatusField() throws Exception {
        Policy policy = PolicyFixtures.activePolicy("AP-001");
        policy.cancel(CancellationReason.OUTROS, PolicyFixtures.NOW);
        when(getPolicy.execute("AP-001")).thenReturn(policy);

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").value("AP-001"))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.cancelledAt").doesNotExist());
    }

    @Test
    void answers404WhenThePolicyDoesNotExist() throws Exception {
        when(getPolicy.execute("AP-001")).thenThrow(new PolicyNotFoundException("AP-001"));

        mvc.perform(get(URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("POLICY_NOT_FOUND"));
    }
}
