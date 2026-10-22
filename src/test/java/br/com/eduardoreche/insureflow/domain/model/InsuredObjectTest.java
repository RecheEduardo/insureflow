package br.com.eduardoreche.insureflow.domain.model;

import br.com.eduardoreche.insureflow.domain.exception.BusinessRuleViolationException;
import br.com.eduardoreche.insureflow.domain.model.enums.CoverageCode;
import br.com.eduardoreche.insureflow.domain.model.enums.CoverageFeature;
import br.com.eduardoreche.insureflow.domain.model.enums.CoverageType;
import br.com.eduardoreche.insureflow.domain.model.enums.PremiumPeriodicity;
import br.com.eduardoreche.insureflow.domain.model.enums.PropertyType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsuredObjectTest {

    private static final Term TERM = new Term(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30));

    @Test
    void requiresAtLeastOneCoverage() {
        assertThatThrownBy(() -> object(List.of()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("OBJECT_WITHOUT_COVERAGE");
    }

    @Test
    void requiresAtLeastOneMainCoverage() {
        assertThatThrownBy(() -> object(List.of(coverage(CoverageCode.VENDAVAL, false))))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("OBJECT_WITHOUT_MAIN_COVERAGE");
    }

    @Test
    void refusesTheSameCoverageTwice() {
        assertThatThrownBy(() -> object(List.of(coverage(CoverageCode.INCENDIO, true),
                coverage(CoverageCode.INCENDIO, false))))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("COVERAGE_DUPLICATED");
    }

    @Test
    void acceptsAccessoryCoveragesAlongsideTheMainOne() {
        InsuredObject object = object(List.of(coverage(CoverageCode.INCENDIO, true),
                coverage(CoverageCode.VENDAVAL, false)));

        assertThat(object.coverages()).hasSize(2);
    }

    @Test
    void requiresDescriptionWhenCoverageIsOutras() {
        assertThatThrownBy(() -> new Coverage("0118", CoverageCode.OUTRAS, " ", "15414.900000/2020-11",
                Money.brl(new BigDecimal("1000.00")), TERM, true, CoverageFeature.MASSIFICADOS,
                CoverageType.REGULAR_COMUM, PremiumPeriodicity.ANUAL))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("COVERAGE_DESCRIPTION_REQUIRED");
    }

    private static InsuredObject object(List<Coverage> coverages) {
        return new InsuredObject("IMOVEL-1", PropertyType.CASA, "Casa residencial", "01310100", null, coverages);
    }

    private static Coverage coverage(CoverageCode code, boolean main) {
        return new Coverage("0118", code, null, "15414.900000/2020-11", Money.brl(new BigDecimal("1000.00")),
                TERM, main, CoverageFeature.MASSIFICADOS, CoverageType.REGULAR_COMUM, PremiumPeriodicity.ANUAL);
    }
}
