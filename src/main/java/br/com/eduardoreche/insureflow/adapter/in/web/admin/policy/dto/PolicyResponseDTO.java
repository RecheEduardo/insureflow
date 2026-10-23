package br.com.eduardoreche.insureflow.adapter.in.web.admin.policy.dto;

import br.com.eduardoreche.insureflow.domain.model.Address;
import br.com.eduardoreche.insureflow.domain.model.Coverage;
import br.com.eduardoreche.insureflow.domain.model.Insured;
import br.com.eduardoreche.insureflow.domain.model.InsuredObject;
import br.com.eduardoreche.insureflow.domain.model.Money;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import br.com.eduardoreche.insureflow.domain.model.enums.CancellationReason;
import br.com.eduardoreche.insureflow.domain.model.enums.CoverageCode;
import br.com.eduardoreche.insureflow.domain.model.enums.CoverageFeature;
import br.com.eduardoreche.insureflow.domain.model.enums.CoverageType;
import br.com.eduardoreche.insureflow.domain.model.enums.DocumentType;
import br.com.eduardoreche.insureflow.domain.model.enums.IdentificationType;
import br.com.eduardoreche.insureflow.domain.model.enums.IssuanceType;
import br.com.eduardoreche.insureflow.domain.model.enums.PolicyStatus;
import br.com.eduardoreche.insureflow.domain.model.enums.PremiumPeriodicity;
import br.com.eduardoreche.insureflow.domain.model.enums.PropertyType;
import br.com.eduardoreche.insureflow.domain.model.enums.StreetType;
import br.com.eduardoreche.insureflow.domain.model.enums.Uf;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PolicyResponseDTO(
        String policyId,
        DocumentType documentType,
        String groupCertificateId,
        IssuanceType issuanceType,
        LocalDate issuanceDate,
        LocalDate termStartDate,
        LocalDate termEndDate,
        @Schema(example = "200000.00") String maxLMG,
        String proposalId,
        String productName,
        List<InsuredResponseDTO> insureds,
        List<InsuredObjectResponseDTO> insuredObjects,
        PolicyStatus status,
        Instant cancelledAt,
        CancellationReason cancellationReason,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    public static PolicyResponseDTO from(Policy policy) {
        return new PolicyResponseDTO(
                policy.getPolicyId(),
                policy.getDocumentType(),
                policy.getGroupCertificateId(),
                policy.getIssuanceType(),
                policy.getIssuanceDate(),
                policy.getTerm().startDate(),
                policy.getTerm().endDate(),
                money(policy.getMaxLmg()),
                policy.getProposalId(),
                policy.getProductName(),
                policy.getInsureds().stream().map(InsuredResponseDTO::from).toList(),
                policy.getInsuredObjects().stream().map(InsuredObjectResponseDTO::from).toList(),
                policy.getStatus(),
                policy.getCancelledAt(),
                policy.getCancellationReason(),
                policy.getVersion(),
                policy.getCreatedAt(),
                policy.getUpdatedAt());
    }

    private static String money(Money money) {
        return money == null ? null : money.amount().toPlainString();
    }

    public record InsuredResponseDTO(
            String identification,
            IdentificationType identificationType,
            String name,
            LocalDate birthDate,
            String email,
            AddressResponseDTO address) {

        static InsuredResponseDTO from(Insured insured) {
            return new InsuredResponseDTO(insured.identification(), insured.identificationType(), insured.name(),
                    insured.birthDate(), insured.email(), AddressResponseDTO.from(insured.address()));
        }
    }

    public record AddressResponseDTO(
            StreetType streetType,
            String streetName,
            String number,
            String complement,
            String district,
            String townName,
            Uf state,
            String postCode) {

        static AddressResponseDTO from(Address address) {
            return new AddressResponseDTO(address.streetType(), address.streetName(), address.number(),
                    address.complement(), address.district(), address.townName(), address.state(),
                    address.postCode());
        }
    }

    public record InsuredObjectResponseDTO(
            String identification,
            PropertyType propertyType,
            String description,
            String postCode,
            @Schema(example = "350000.00") String amount,
            List<CoverageResponseDTO> coverages) {

        static InsuredObjectResponseDTO from(InsuredObject object) {
            return new InsuredObjectResponseDTO(object.identification(), object.propertyType(),
                    object.description(), object.postCode(), money(object.amount()),
                    object.coverages().stream().map(CoverageResponseDTO::from).toList());
        }
    }

    public record CoverageResponseDTO(
            String branch,
            CoverageCode code,
            String description,
            String susepProcessNumber,
            @Schema(example = "100000.00") String lmi,
            LocalDate termStartDate,
            LocalDate termEndDate,
            boolean mainCoverage,
            CoverageFeature feature,
            CoverageType type,
            PremiumPeriodicity premiumPeriodicity) {

        static CoverageResponseDTO from(Coverage coverage) {
            return new CoverageResponseDTO(coverage.branch(), coverage.code(), coverage.description(),
                    coverage.susepProcessNumber(), money(coverage.lmi()), coverage.term().startDate(),
                    coverage.term().endDate(), coverage.mainCoverage(), coverage.feature(), coverage.type(),
                    coverage.premiumPeriodicity());
        }
    }
}
