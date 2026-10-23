package br.com.eduardoreche.insureflow.application.policy.command.dto;

import br.com.eduardoreche.insureflow.domain.model.Insured;
import br.com.eduardoreche.insureflow.domain.model.InsuredObject;
import br.com.eduardoreche.insureflow.domain.model.Money;

import java.time.LocalDate;
import java.util.List;

public record EndorsePolicyCommandDTO(
        String policyId,
        long expectedVersion,
        LocalDate termStartDate,
        LocalDate termEndDate,
        Money maxLmg,
        String productName,
        List<Insured> insureds,
        List<InsuredObject> insuredObjects) {

    public boolean changesNothing() {
        return termStartDate == null && termEndDate == null && maxLmg == null && productName == null
                && insureds == null && insuredObjects == null;
    }
}
