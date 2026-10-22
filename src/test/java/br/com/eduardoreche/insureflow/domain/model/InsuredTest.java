package br.com.eduardoreche.insureflow.domain.model;

import br.com.eduardoreche.insureflow.domain.exception.BusinessRuleViolationException;
import br.com.eduardoreche.insureflow.domain.model.enums.IdentificationType;
import br.com.eduardoreche.insureflow.domain.model.enums.StreetType;
import br.com.eduardoreche.insureflow.domain.model.enums.Uf;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsuredTest {

    private static final Address ADDRESS =
            new Address(StreetType.RUA, "Rua Exemplo", "100", null, null, "Sao Paulo", Uf.SP, "01310100");

    @Test
    void requiresBirthDateForAPerson() {
        assertThatThrownBy(() -> new Insured("12345678901", IdentificationType.CPF, "Maria Ficticia",
                null, null, ADDRESS))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("INSURED_BIRTH_DATE_REQUIRED");
    }

    @Test
    void doesNotRequireBirthDateForACompany() {
        Insured company = new Insured("12345678000199", IdentificationType.CNPJ, "Empresa Ficticia",
                null, null, ADDRESS);

        assertThat(company.birthDate()).isNull();
    }

    @Test
    void masksTheDocumentKeepingOnlyTheLastTwoCharacters() {
        Insured insured = new Insured("12345678901", IdentificationType.CPF, "Maria Ficticia",
                LocalDate.of(1990, 5, 17), null, ADDRESS);

        assertThat(insured.maskedIdentification()).isEqualTo("***01");
        assertThat(insured.toString()).doesNotContain("12345678901").doesNotContain("Maria");
    }
}
