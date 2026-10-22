package br.com.eduardoreche.insureflow.adapter.in.web.admin;

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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PolicyResponse(
        String policyId,
        DocumentType documentType,
        String groupCertificateId,
        IssuanceType issuanceType,
        LocalDate issuanceDate,
        LocalDate termStartDate,
        LocalDate termEndDate,
        String maxLMG,
        String proposalId,
        String productName,
        List<InsuredResponse> insureds,
        List<InsuredObjectResponse> insuredObjects,
        PolicyStatus status,
        Instant cancelledAt,
        CancellationReason cancellationReason,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    public static PolicyResponse from(Policy policy) {
        return new PolicyResponse(
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
                policy.getInsureds().stream().map(InsuredResponse::from).toList(),
                policy.getInsuredObjects().stream().map(InsuredObjectResponse::from).toList(),
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

    public record InsuredResponse(
            String identification,
            IdentificationType identificationType,
            String name,
            LocalDate birthDate,
            String email,
            AddressResponse address) {

        static InsuredResponse from(Insured insured) {
            return new InsuredResponse(insured.identification(), insured.identificationType(), insured.name(),
                    insured.birthDate(), insured.email(), AddressResponse.from(insured.address()));
        }
    }

    public record AddressResponse(
            StreetType streetType,
            String streetName,
            String number,
            String complement,
            String district,
            String townName,
            Uf state,
            String postCode) {

        static AddressResponse from(Address address) {
            return new AddressResponse(address.streetType(), address.streetName(), address.number(),
                    address.complement(), address.district(), address.townName(), address.state(),
                    address.postCode());
        }
    }

    public record InsuredObjectResponse(
            String identification,
            PropertyType propertyType,
            String description,
            String postCode,
            String amount,
            List<CoverageResponse> coverages) {

        static InsuredObjectResponse from(InsuredObject object) {
            return new InsuredObjectResponse(object.identification(), object.propertyType(),
                    object.description(), object.postCode(), money(object.amount()),
                    object.coverages().stream().map(CoverageResponse::from).toList());
        }
    }

    public record CoverageResponse(
            String branch,
            CoverageCode code,
            String description,
            String susepProcessNumber,
            String lmi,
            LocalDate termStartDate,
            LocalDate termEndDate,
            boolean mainCoverage,
            CoverageFeature feature,
            CoverageType type,
            PremiumPeriodicity premiumPeriodicity) {

        static CoverageResponse from(Coverage coverage) {
            return new CoverageResponse(coverage.branch(), coverage.code(), coverage.description(),
                    coverage.susepProcessNumber(), money(coverage.lmi()), coverage.term().startDate(),
                    coverage.term().endDate(), coverage.mainCoverage(), coverage.feature(), coverage.type(),
                    coverage.premiumPeriodicity());
        }
    }
}
