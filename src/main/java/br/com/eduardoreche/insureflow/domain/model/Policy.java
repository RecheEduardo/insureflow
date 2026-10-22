package br.com.eduardoreche.insureflow.domain.model;

import br.com.eduardoreche.insureflow.domain.exception.BusinessRuleViolationException;
import br.com.eduardoreche.insureflow.domain.exception.PolicyCancelledException;
import br.com.eduardoreche.insureflow.domain.model.enums.CancellationReason;
import br.com.eduardoreche.insureflow.domain.model.enums.DocumentType;
import br.com.eduardoreche.insureflow.domain.model.enums.IssuanceType;
import br.com.eduardoreche.insureflow.domain.model.enums.PolicyStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Policy {

    private final UUID id;
    private final String policyId;
    private final DocumentType documentType;
    private final String groupCertificateId;
    private final IssuanceType issuanceType;
    private final LocalDate issuanceDate;
    private final String proposalId;
    private final Instant createdAt;

    private Term term;
    private Money maxLmg;
    private String productName;
    private List<Insured> insureds;
    private List<InsuredObject> insuredObjects;
    private PolicyStatus status;
    private Instant cancelledAt;
    private CancellationReason cancellationReason;
    private long version;
    private Instant updatedAt;

    private Policy(UUID id, String policyId, DocumentType documentType, String groupCertificateId,
                   IssuanceType issuanceType, LocalDate issuanceDate, Term term, Money maxLmg,
                   String proposalId, String productName, List<Insured> insureds,
                   List<InsuredObject> insuredObjects, PolicyStatus status, Instant cancelledAt,
                   CancellationReason cancellationReason, long version, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.policyId = Objects.requireNonNull(policyId, "policyId");
        this.documentType = Objects.requireNonNull(documentType, "documentType");
        this.groupCertificateId = groupCertificateId;
        this.issuanceType = Objects.requireNonNull(issuanceType, "issuanceType");
        this.issuanceDate = Objects.requireNonNull(issuanceDate, "issuanceDate");
        this.proposalId = Objects.requireNonNull(proposalId, "proposalId");
        this.term = term;
        this.maxLmg = maxLmg;
        this.productName = Objects.requireNonNull(productName, "productName");
        this.insureds = List.copyOf(insureds);
        this.insuredObjects = List.copyOf(insuredObjects);
        this.status = Objects.requireNonNull(status, "status");
        this.cancelledAt = cancelledAt;
        this.cancellationReason = cancellationReason;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        checkInvariants(documentType, groupCertificateId, term, maxLmg, this.insureds, this.insuredObjects);
    }

    public static Policy create(String policyId, DocumentType documentType, String groupCertificateId,
                                IssuanceType issuanceType, LocalDate issuanceDate, Term term, Money maxLmg,
                                String proposalId, String productName, List<Insured> insureds,
                                List<InsuredObject> insuredObjects, LocalDate today, Instant now) {
        if (issuanceDate.isAfter(today)) {
            throw new BusinessRuleViolationException("ISSUANCE_IN_FUTURE", "issuanceDate must not be in the future");
        }
        return new Policy(UUID.randomUUID(), policyId, documentType, groupCertificateId, issuanceType,
                issuanceDate, term, maxLmg, proposalId, productName, insureds, insuredObjects,
                PolicyStatus.ACTIVE, null, null, 0L, now, now);
    }

    public static Policy restore(UUID id, String policyId, DocumentType documentType, String groupCertificateId,
                                 IssuanceType issuanceType, LocalDate issuanceDate, Term term, Money maxLmg,
                                 String proposalId, String productName, List<Insured> insureds,
                                 List<InsuredObject> insuredObjects, PolicyStatus status, Instant cancelledAt,
                                 CancellationReason cancellationReason, long version,
                                 Instant createdAt, Instant updatedAt) {
        return new Policy(id, policyId, documentType, groupCertificateId, issuanceType, issuanceDate, term,
                maxLmg, proposalId, productName, insureds, insuredObjects, status, cancelledAt,
                cancellationReason, version, createdAt, updatedAt);
    }

    public void endorse(Term newTerm, Money newMaxLmg, String newProductName,
                        List<Insured> newInsureds, List<InsuredObject> newInsuredObjects, Instant now) {
        if (isCancelled()) {
            throw new PolicyCancelledException(policyId);
        }
        List<Insured> insuredsCopy = List.copyOf(newInsureds);
        List<InsuredObject> insuredObjectsCopy = List.copyOf(newInsuredObjects);
        checkInvariants(documentType, groupCertificateId, newTerm, newMaxLmg, insuredsCopy, insuredObjectsCopy);

        this.term = newTerm;
        this.maxLmg = newMaxLmg;
        this.productName = Objects.requireNonNull(newProductName, "productName");
        this.insureds = insuredsCopy;
        this.insuredObjects = insuredObjectsCopy;
        this.updatedAt = now;
    }

    public void cancel(CancellationReason reason, Instant now) {
        if (isCancelled()) {
            return;
        }
        this.status = PolicyStatus.CANCELLED;
        this.cancellationReason = Objects.requireNonNull(reason, "reason");
        this.cancelledAt = Objects.requireNonNull(now, "now");
        this.updatedAt = now;
    }

    public boolean isCancelled() {
        return status == PolicyStatus.CANCELLED;
    }

    public boolean isInForceOn(LocalDate date) {
        return !isCancelled() && term.contains(date);
    }

    private static void checkInvariants(DocumentType documentType, String groupCertificateId, Term term,
                                        Money maxLmg, List<Insured> insureds, List<InsuredObject> insuredObjects) {
        Objects.requireNonNull(term, "term");
        Objects.requireNonNull(maxLmg, "maxLmg");

        if (insureds.isEmpty()) {
            throw new BusinessRuleViolationException("POLICY_WITHOUT_INSURED", "policy needs at least one insured");
        }
        if (insuredObjects.isEmpty()) {
            throw new BusinessRuleViolationException("POLICY_WITHOUT_OBJECT",
                    "policy needs at least one insured object");
        }

        boolean hasCertificateId = groupCertificateId != null && !groupCertificateId.isBlank();
        if (hasCertificateId != (documentType == DocumentType.CERTIFICADO)) {
            throw new BusinessRuleViolationException("GROUP_CERTIFICATE_ID_MISMATCH",
                    "groupCertificateId is required for CERTIFICADO and not allowed otherwise");
        }

        Set<String> objectIds = new HashSet<>();
        for (InsuredObject object : insuredObjects) {
            if (!objectIds.add(object.identification())) {
                throw new BusinessRuleViolationException("OBJECT_DUPLICATED",
                        "insured object " + object.identification() + " is repeated in the policy");
            }
            for (Coverage coverage : object.coverages()) {
                if (coverage.lmi().isGreaterThan(maxLmg)) {
                    throw new BusinessRuleViolationException("LMI_ABOVE_LMG",
                            "coverage " + coverage.code() + " LMI exceeds the policy LMG");
                }
                if (!term.contains(coverage.term())) {
                    throw new BusinessRuleViolationException("COVERAGE_OUTSIDE_TERM",
                            "coverage " + coverage.code() + " term must be inside the policy term");
                }
            }
        }
    }

    public UUID getId() { return id; }
    public String getPolicyId() { return policyId; }
    public DocumentType getDocumentType() { return documentType; }
    public String getGroupCertificateId() { return groupCertificateId; }
    public IssuanceType getIssuanceType() { return issuanceType; }
    public LocalDate getIssuanceDate() { return issuanceDate; }
    public Term getTerm() { return term; }
    public Money getMaxLmg() { return maxLmg; }
    public String getProposalId() { return proposalId; }
    public String getProductName() { return productName; }
    public List<Insured> getInsureds() { return insureds; }
    public List<InsuredObject> getInsuredObjects() { return insuredObjects; }
    public PolicyStatus getStatus() { return status; }
    public Instant getCancelledAt() { return cancelledAt; }
    public CancellationReason getCancellationReason() { return cancellationReason; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Policy other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
