package br.com.eduardoreche.insureflow.application.policy.command.dto;

import br.com.eduardoreche.insureflow.domain.model.Insured;
import br.com.eduardoreche.insureflow.domain.model.InsuredObject;
import br.com.eduardoreche.insureflow.domain.model.Money;
import br.com.eduardoreche.insureflow.domain.model.Term;
import br.com.eduardoreche.insureflow.domain.model.enums.DocumentType;
import br.com.eduardoreche.insureflow.domain.model.enums.IssuanceType;

import java.time.LocalDate;
import java.util.List;

public record IssuePolicyCommandDTO(
        String policyId,
        DocumentType documentType,
        String groupCertificateId,
        IssuanceType issuanceType,
        LocalDate issuanceDate,
        Term term,
        Money maxLmg,
        String proposalId,
        String productName,
        List<Insured> insureds,
        List<InsuredObject> insuredObjects) {
}
