package br.com.eduardoreche.insureflow.adapter.in.web.openinsurance.policy.dto;

import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicySnapshotDTO;
import br.com.eduardoreche.insureflow.domain.model.Address;
import br.com.eduardoreche.insureflow.domain.model.Coverage;
import br.com.eduardoreche.insureflow.domain.model.Insured;
import br.com.eduardoreche.insureflow.domain.model.InsuredObject;
import br.com.eduardoreche.insureflow.domain.model.Money;
import br.com.eduardoreche.insureflow.domain.model.enums.CoverageCode;
import br.com.eduardoreche.insureflow.domain.model.enums.CoverageFeature;
import br.com.eduardoreche.insureflow.domain.model.enums.CoverageType;
import br.com.eduardoreche.insureflow.domain.model.enums.DocumentType;
import br.com.eduardoreche.insureflow.domain.model.enums.IdentificationType;
import br.com.eduardoreche.insureflow.domain.model.enums.IssuanceType;
import br.com.eduardoreche.insureflow.domain.model.enums.PremiumPeriodicity;
import br.com.eduardoreche.insureflow.domain.model.enums.PropertyType;
import br.com.eduardoreche.insureflow.domain.model.enums.StreetType;
import br.com.eduardoreche.insureflow.domain.model.enums.Uf;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PolicyInfoResponseDTO(
        DocumentType documentType,
        String policyId,
        String groupCertificateId,
        IssuanceType issuanceType,
        LocalDate issuanceDate,
        LocalDate termStartDate,
        LocalDate termEndDate,
        AmountDetailsDTO maxLMG,
        String proposalId,
        List<InsuredDTO> insureds,
        List<InsuredObjectDTO> insuredObjects) {

    public static PolicyInfoResponseDTO from(PolicySnapshotDTO policy) {
        return new PolicyInfoResponseDTO(
                policy.documentType(),
                policy.policyId(),
                policy.groupCertificateId(),
                policy.issuanceType(),
                policy.issuanceDate(),
                policy.term().startDate(),
                policy.term().endDate(),
                AmountDetailsDTO.from(policy.maxLmg()),
                policy.proposalId(),
                policy.insureds().stream().map(InsuredDTO::from).toList(),
                policy.insuredObjects().stream().map(InsuredObjectDTO::from).toList());
    }

    public record AmountDetailsDTO(@Schema(example = "200000.00") String amount,
                                   @Schema(example = "BRL") String currency) {

        static AmountDetailsDTO from(Money money) {
            return money == null ? null
                    : new AmountDetailsDTO(money.amount().toPlainString(), money.currency().getCurrencyCode());
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InsuredDTO(
            @Schema(description = "Documento mascarado: só os dois últimos caracteres ficam visíveis.",
                    example = "***01")
            String identification,
            IdentificationType identificationType,
            String name,
            LocalDate birthDate,
            String email,
            AddressDTO address) {

        static InsuredDTO from(Insured insured) {
            return new InsuredDTO(insured.maskedIdentification(), insured.identificationType(), insured.name(),
                    insured.birthDate(), insured.email(), AddressDTO.from(insured.address()));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AddressDTO(
            StreetType type,
            String name,
            String number,
            String addressComplementaryInfo,
            String districtName,
            String townName,
            Uf countrySubDivision,
            String postCode) {

        static AddressDTO from(Address address) {
            return new AddressDTO(address.streetType(), address.streetName(), address.number(),
                    address.complement(), address.district(), address.townName(), address.state(),
                    address.postCode());
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InsuredObjectDTO(
            String identification,
            PropertyType propertyType,
            String description,
            String postCode,
            AmountDetailsDTO amount,
            List<CoverageDTO> coverages) {

        static InsuredObjectDTO from(InsuredObject object) {
            return new InsuredObjectDTO(object.identification(), object.propertyType(), object.description(),
                    object.postCode(), AmountDetailsDTO.from(object.amount()),
                    object.coverages().stream().map(CoverageDTO::from).toList());
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CoverageDTO(
            String branch,
            CoverageCode code,
            String description,
            String susepProcessNumber,
            AmountDetailsDTO LMI,
            LocalDate termStartDate,
            LocalDate termEndDate,
            boolean isMainCoverage,
            CoverageFeature feature,
            CoverageType type,
            PremiumPeriodicity premiumPeriodicity) {

        static CoverageDTO from(Coverage coverage) {
            return new CoverageDTO(coverage.branch(), coverage.code(), coverage.description(),
                    coverage.susepProcessNumber(), AmountDetailsDTO.from(coverage.lmi()),
                    coverage.term().startDate(), coverage.term().endDate(), coverage.mainCoverage(),
                    coverage.feature(), coverage.type(), coverage.premiumPeriodicity());
        }
    }
}
