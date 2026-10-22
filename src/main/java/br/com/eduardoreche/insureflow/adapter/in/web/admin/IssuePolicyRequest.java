package br.com.eduardoreche.insureflow.adapter.in.web.admin;

import br.com.eduardoreche.insureflow.application.policy.IssuePolicyCommand;
import br.com.eduardoreche.insureflow.domain.model.Address;
import br.com.eduardoreche.insureflow.domain.model.Coverage;
import br.com.eduardoreche.insureflow.domain.model.Insured;
import br.com.eduardoreche.insureflow.domain.model.InsuredObject;
import br.com.eduardoreche.insureflow.domain.model.Money;
import br.com.eduardoreche.insureflow.domain.model.Term;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record IssuePolicyRequest(
        @NotBlank @Size(max = 60) String policyId,
        @NotNull DocumentType documentType,
        @Size(max = 60) String groupCertificateId,
        @NotNull IssuanceType issuanceType,
        @NotNull LocalDate issuanceDate,
        @NotNull LocalDate termStartDate,
        @NotNull LocalDate termEndDate,
        @NotNull @Pattern(regexp = "^\\d{1,16}\\.\\d{2}$") String maxLMG,
        @NotBlank @Size(max = 60) String proposalId,
        @NotBlank @Size(max = 70) String productName,
        @NotEmpty @Valid List<InsuredRequest> insureds,
        @NotEmpty @Valid List<InsuredObjectRequest> insuredObjects) {

    public IssuePolicyCommand toCommand() {
        return new IssuePolicyCommand(
                policyId, documentType, groupCertificateId, issuanceType, issuanceDate,
                new Term(termStartDate, termEndDate),
                money(maxLMG),
                proposalId, productName,
                insureds.stream().map(InsuredRequest::toDomain).toList(),
                insuredObjects.stream().map(InsuredObjectRequest::toDomain).toList());
    }

    private static Money money(String amount) {
        return amount == null ? null : Money.brl(new BigDecimal(amount));
    }

    public record InsuredRequest(
            @NotBlank String identification,
            @NotNull IdentificationType identificationType,
            @NotBlank String name,
            LocalDate birthDate,
            @Email String email,
            @NotNull @Valid AddressRequest address) {

        Insured toDomain() {
            return new Insured(identification, identificationType, name, birthDate, email, address.toDomain());
        }
    }

    public record AddressRequest(
            @NotNull StreetType streetType,
            @NotBlank String streetName,
            @NotBlank String number,
            String complement,
            String district,
            @NotBlank String townName,
            @NotNull Uf state,
            @NotNull @Pattern(regexp = "^\\d{8}$") String postCode) {

        Address toDomain() {
            return new Address(streetType, streetName, number, complement, district, townName, state, postCode);
        }
    }

    public record InsuredObjectRequest(
            @NotBlank String identification,
            @NotNull PropertyType propertyType,
            @NotBlank String description,
            @NotNull @Pattern(regexp = "^\\d{8}$") String postCode,
            @Pattern(regexp = "^\\d{1,16}\\.\\d{2}$") String amount,
            @NotEmpty @Valid List<CoverageRequest> coverages) {

        InsuredObject toDomain() {
            return new InsuredObject(identification, propertyType, description, postCode, money(amount),
                    coverages.stream().map(CoverageRequest::toDomain).toList());
        }
    }

    public record CoverageRequest(
            @NotNull @Pattern(regexp = "^\\d{4}$") String branch,
            @NotNull CoverageCode code,
            String description,
            @NotBlank String susepProcessNumber,
            @NotNull @Pattern(regexp = "^\\d{1,16}\\.\\d{2}$") String lmi,
            @NotNull LocalDate termStartDate,
            @NotNull LocalDate termEndDate,
            @NotNull Boolean mainCoverage,
            @NotNull CoverageFeature feature,
            @NotNull CoverageType type,
            @NotNull PremiumPeriodicity premiumPeriodicity) {

        Coverage toDomain() {
            return new Coverage(branch, code, description, susepProcessNumber, money(lmi),
                    new Term(termStartDate, termEndDate), mainCoverage, feature, type, premiumPeriodicity);
        }
    }
}
