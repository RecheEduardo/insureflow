package br.com.eduardoreche.insureflow.adapter.in.web.openinsurance.policy;

import br.com.eduardoreche.insureflow.application.policy.readmodel.GetPolicyInfo;
import br.com.eduardoreche.insureflow.application.policy.readmodel.ListPolicyInfo;
import br.com.eduardoreche.insureflow.application.policy.readmodel.dto.PolicyInfoPageDTO;
import br.com.eduardoreche.insureflow.application.policy.readmodel.dto.PolicyInfoSummaryDTO;
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
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OpenInsurancePolicyController.class)
@Import(PolicyListControllerTest.FixedClock.class)
class PolicyListControllerTest {

    static final String URL = "/insurance-patrimonial";

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
    ListPolicyInfo listPolicyInfo;

    @MockitoBean
    GetPolicyInfo getPolicyInfo;

    @Test
    void listsPolicyIdentificationWithPaginationTotals() throws Exception {
        when(listPolicyInfo.execute(1, 25)).thenReturn(new PolicyInfoPageDTO(
                List.of(new PolicyInfoSummaryDTO("AP-001", "Residencial Essencial"),
                        new PolicyInfoSummaryDTO("AP-002", "Empresarial Plus")), 2, 1));

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].policyId").value("AP-001"))
                .andExpect(jsonPath("$.data[0].productName").value("Residencial Essencial"))
                .andExpect(jsonPath("$.data[1].policyId").value("AP-002"))
                .andExpect(jsonPath("$.meta.totalRecords").value(2))
                .andExpect(jsonPath("$.meta.totalPages").value(1));
    }

    @Test
    void answers200WithAnEmptyListWhenThereAreNoPolicies() throws Exception {
        when(listPolicyInfo.execute(1, 25)).thenReturn(new PolicyInfoPageDTO(List.of(), 0, 0));

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.totalRecords").value(0))
                .andExpect(jsonPath("$.meta.totalPages").value(0));
    }

    @Test
    void passesTheRequestedPageAndSizeToTheUseCase() throws Exception {
        when(listPolicyInfo.execute(3, 1000)).thenReturn(new PolicyInfoPageDTO(List.of(), 0, 0));

        mvc.perform(get(URL).param("page", "3").param("page-size", "1000")).andExpect(status().isOk());

        verify(listPolicyInfo).execute(3, 1000);
    }

    @Test
    void answers400ForPageBelowOne() throws Exception {
        mvc.perform(get(URL).param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

        verifyNoInteractions(listPolicyInfo);
    }

    @Test
    void answers400ForPageSizeAboveTheMaximum() throws Exception {
        mvc.perform(get(URL).param("page-size", "1001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

        verifyNoInteractions(listPolicyInfo);
    }

    @Test
    void answers400ForNonNumericPage() throws Exception {
        mvc.perform(get(URL).param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));
    }
}
