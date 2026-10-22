package br.com.eduardoreche.insureflow.adapter.out.persistence.policy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "policy")
class PolicyJpaEntity {

    @Id
    UUID id;

    @Column(name = "policy_id", nullable = false, updatable = false, length = 60)
    String policyId;

    @Column(name = "document_type", nullable = false, updatable = false)
    String documentType;

    @Column(name = "group_certificate_id", updatable = false, length = 60)
    String groupCertificateId;

    @Column(name = "issuance_type", nullable = false, updatable = false)
    String issuanceType;

    @Column(name = "issuance_date", nullable = false, updatable = false)
    LocalDate issuanceDate;

    @Column(name = "term_start_date", nullable = false)
    LocalDate termStartDate;

    @Column(name = "term_end_date", nullable = false)
    LocalDate termEndDate;

    @Column(name = "max_lmg_amount", nullable = false, precision = 18, scale = 2)
    BigDecimal maxLmgAmount;

    @Column(name = "max_lmg_currency", nullable = false, length = 3)
    String maxLmgCurrency;

    @Column(name = "proposal_id", nullable = false, updatable = false, length = 60)
    String proposalId;

    @Column(name = "product_name", nullable = false, length = 70)
    String productName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    String insureds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "insured_objects", nullable = false)
    String insuredObjects;

    @Column(nullable = false)
    String status;

    @Column(name = "cancelled_at")
    Instant cancelledAt;

    @Column(name = "cancellation_reason")
    String cancellationReason;

    @Version
    Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;
}
