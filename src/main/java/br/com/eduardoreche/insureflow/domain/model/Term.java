package br.com.eduardoreche.insureflow.domain.model;

import br.com.eduardoreche.insureflow.domain.exception.BusinessRuleViolationException;

import java.time.LocalDate;
import java.util.Objects;

// Início e fim inclusivos.
public record Term(LocalDate startDate, LocalDate endDate) {

    public Term {
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleViolationException("TERM_INVALID", "endDate must not be before startDate");
        }
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public boolean contains(Term other) {
        return contains(other.startDate) && contains(other.endDate);
    }

    public boolean hasEndedOn(LocalDate date) {
        return date.isAfter(endDate);
    }
}
