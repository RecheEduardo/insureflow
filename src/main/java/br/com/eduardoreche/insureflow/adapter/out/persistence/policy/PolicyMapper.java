package br.com.eduardoreche.insureflow.adapter.out.persistence.policy;

import br.com.eduardoreche.insureflow.domain.model.Insured;
import br.com.eduardoreche.insureflow.domain.model.InsuredObject;
import br.com.eduardoreche.insureflow.domain.model.Money;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import br.com.eduardoreche.insureflow.domain.model.Term;
import br.com.eduardoreche.insureflow.domain.model.enums.CancellationReason;
import br.com.eduardoreche.insureflow.domain.model.enums.DocumentType;
import br.com.eduardoreche.insureflow.domain.model.enums.IssuanceType;
import br.com.eduardoreche.insureflow.domain.model.enums.PolicyStatus;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.Currency;
import java.util.List;

@Component
class PolicyMapper {

    private final JsonMapper json = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    PolicyJpaEntity toNewEntity(Policy policy) {
        PolicyJpaEntity entity = new PolicyJpaEntity();
        entity.id = policy.getId();
        entity.policyId = policy.getPolicyId();
        entity.documentType = policy.getDocumentType().name();
        entity.groupCertificateId = policy.getGroupCertificateId();
        entity.issuanceType = policy.getIssuanceType().name();
        entity.issuanceDate = policy.getIssuanceDate();
        entity.termStartDate = policy.getTerm().startDate();
        entity.termEndDate = policy.getTerm().endDate();
        entity.maxLmgAmount = policy.getMaxLmg().amount();
        entity.maxLmgCurrency = policy.getMaxLmg().currency().getCurrencyCode();
        entity.proposalId = policy.getProposalId();
        entity.productName = policy.getProductName();
        entity.insureds = json.writeValueAsString(policy.getInsureds());
        entity.insuredObjects = json.writeValueAsString(policy.getInsuredObjects());
        entity.status = policy.getStatus().name();
        entity.cancelledAt = policy.getCancelledAt();
        entity.cancellationReason = policy.getCancellationReason() == null
                ? null : policy.getCancellationReason().name();
        entity.createdAt = policy.getCreatedAt();
        entity.updatedAt = policy.getUpdatedAt();
        return entity;
    }

    void updateEntity(PolicyJpaEntity entity, Policy policy) {
        entity.termStartDate = policy.getTerm().startDate();
        entity.termEndDate = policy.getTerm().endDate();
        entity.maxLmgAmount = policy.getMaxLmg().amount();
        entity.maxLmgCurrency = policy.getMaxLmg().currency().getCurrencyCode();
        entity.productName = policy.getProductName();
        entity.insureds = json.writeValueAsString(policy.getInsureds());
        entity.insuredObjects = json.writeValueAsString(policy.getInsuredObjects());
        entity.status = policy.getStatus().name();
        entity.cancelledAt = policy.getCancelledAt();
        entity.cancellationReason = policy.getCancellationReason() == null
                ? null : policy.getCancellationReason().name();
        entity.updatedAt = policy.getUpdatedAt();
    }

    Policy toDomain(PolicyJpaEntity entity) {
        return Policy.restore(
                entity.id,
                entity.policyId,
                DocumentType.valueOf(entity.documentType),
                entity.groupCertificateId,
                IssuanceType.valueOf(entity.issuanceType),
                entity.issuanceDate,
                new Term(entity.termStartDate, entity.termEndDate),
                new Money(entity.maxLmgAmount, Currency.getInstance(entity.maxLmgCurrency)),
                entity.proposalId,
                entity.productName,
                json.readValue(entity.insureds, new TypeReference<List<Insured>>() { }),
                json.readValue(entity.insuredObjects, new TypeReference<List<InsuredObject>>() { }),
                PolicyStatus.valueOf(entity.status),
                entity.cancelledAt,
                entity.cancellationReason == null ? null : CancellationReason.valueOf(entity.cancellationReason),
                entity.version,
                entity.createdAt,
                entity.updatedAt);
    }
}
