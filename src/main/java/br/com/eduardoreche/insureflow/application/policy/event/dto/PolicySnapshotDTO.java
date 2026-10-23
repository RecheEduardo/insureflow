package br.com.eduardoreche.insureflow.application.policy.event.dto;

import br.com.eduardoreche.insureflow.domain.model.Insured;
import br.com.eduardoreche.insureflow.domain.model.InsuredObject;
import br.com.eduardoreche.insureflow.domain.model.Money;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import br.com.eduardoreche.insureflow.domain.model.Term;
import br.com.eduardoreche.insureflow.domain.model.enums.CancellationReason;
import br.com.eduardoreche.insureflow.domain.model.enums.DocumentType;
import br.com.eduardoreche.insureflow.domain.model.enums.IssuanceType;
import br.com.eduardoreche.insureflow.domain.model.enums.PolicyStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PolicySnapshotDTO(
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
        List<InsuredObject> insuredObjects,
        PolicyStatus status,
        Instant cancelledAt,
        CancellationReason cancellationReason) {

    public static PolicySnapshotDTO from(Policy policy) {
        return new PolicySnapshotDTO(
                policy.getPolicyId(),
                policy.getDocumentType(),
                policy.getGroupCertificateId(),
                policy.getIssuanceType(),
                policy.getIssuanceDate(),
                policy.getTerm(),
                policy.getMaxLmg(),
                policy.getProposalId(),
                policy.getProductName(),
                policy.getInsureds(),
                policy.getInsuredObjects(),
                policy.getStatus(),
                policy.getCancelledAt(),
                policy.getCancellationReason());
    }
}
