package br.com.eduardoreche.insureflow.domain.model;

import br.com.eduardoreche.insureflow.domain.exception.BusinessRuleViolationException;
import br.com.eduardoreche.insureflow.domain.model.enums.IdentificationType;

import java.time.LocalDate;
import java.util.Objects;

public record Insured(
        String identification,
        IdentificationType identificationType,
        String name,
        LocalDate birthDate,
        String email,
        Address address) {

    public Insured {
        Objects.requireNonNull(identification, "identification");
        Objects.requireNonNull(identificationType, "identificationType");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(address, "address");
        if (identificationType != IdentificationType.CNPJ && birthDate == null) {
            throw new BusinessRuleViolationException("INSURED_BIRTH_DATE_REQUIRED",
                    "birthDate is required for " + identificationType);
        }
    }

    public String maskedIdentification() {
        String tail = identification.length() > 2 ? identification.substring(identification.length() - 2) : "";
        return "***" + tail;
    }

    // Mascarado para logs (LGPD).
    @Override
    public String toString() {
        return "Insured[identificationType=" + identificationType + ", identification=" + maskedIdentification() + "]";
    }
}
