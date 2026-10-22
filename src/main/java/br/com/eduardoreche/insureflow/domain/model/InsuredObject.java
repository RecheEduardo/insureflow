package br.com.eduardoreche.insureflow.domain.model;

import br.com.eduardoreche.insureflow.domain.exception.BusinessRuleViolationException;
import br.com.eduardoreche.insureflow.domain.model.enums.CoverageCode;
import br.com.eduardoreche.insureflow.domain.model.enums.PropertyType;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record InsuredObject(
        String identification,
        PropertyType propertyType,
        String description,
        String postCode,
        Money amount,
        List<Coverage> coverages) {

    public InsuredObject {
        Objects.requireNonNull(identification, "identification");
        Objects.requireNonNull(propertyType, "propertyType");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(postCode, "postCode");
        coverages = List.copyOf(Objects.requireNonNull(coverages, "coverages"));

        if (coverages.isEmpty()) {
            throw new BusinessRuleViolationException("OBJECT_WITHOUT_COVERAGE",
                    "insured object needs at least one coverage");
        }
        if (coverages.stream().noneMatch(Coverage::mainCoverage)) {
            throw new BusinessRuleViolationException("OBJECT_WITHOUT_MAIN_COVERAGE",
                    "insured object needs at least one main coverage");
        }
        Set<CoverageCode> codes = new HashSet<>();
        for (Coverage coverage : coverages) {
            if (!codes.add(coverage.code())) {
                throw new BusinessRuleViolationException("COVERAGE_DUPLICATED",
                        "coverage " + coverage.code() + " is repeated in the object");
            }
        }
    }
}
