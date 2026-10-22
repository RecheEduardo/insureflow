package br.com.eduardoreche.insureflow.domain.model;

import br.com.eduardoreche.insureflow.domain.exception.BusinessRuleViolationException;
import br.com.eduardoreche.insureflow.domain.model.enums.CoverageCode;
import br.com.eduardoreche.insureflow.domain.model.enums.CoverageFeature;
import br.com.eduardoreche.insureflow.domain.model.enums.CoverageType;
import br.com.eduardoreche.insureflow.domain.model.enums.PremiumPeriodicity;

import java.util.Objects;

public record Coverage(
        String branch,
        CoverageCode code,
        String description,
        String susepProcessNumber,
        Money lmi,
        Term term,
        boolean mainCoverage,
        CoverageFeature feature,
        CoverageType type,
        PremiumPeriodicity premiumPeriodicity) {

    public Coverage {
        Objects.requireNonNull(branch, "branch");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(susepProcessNumber, "susepProcessNumber");
        Objects.requireNonNull(lmi, "lmi");
        Objects.requireNonNull(term, "term");
        Objects.requireNonNull(feature, "feature");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(premiumPeriodicity, "premiumPeriodicity");
        if (code == CoverageCode.OUTRAS && (description == null || description.isBlank())) {
            throw new BusinessRuleViolationException("COVERAGE_DESCRIPTION_REQUIRED",
                    "description is required when code is OUTRAS");
        }
    }
}
