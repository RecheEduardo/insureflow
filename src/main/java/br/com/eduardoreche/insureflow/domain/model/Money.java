package br.com.eduardoreche.insureflow.domain.model;

import br.com.eduardoreche.insureflow.domain.exception.BusinessRuleViolationException;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {

    private static final Currency BRL = Currency.getInstance("BRL");

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0) {
            throw new BusinessRuleViolationException("MONEY_NEGATIVE", "amount must not be negative");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new BusinessRuleViolationException("MONEY_SCALE", "amount must have at most 2 decimal places");
        }
        amount = amount.setScale(2);
    }

    public static Money brl(BigDecimal amount) {
        return new Money(amount, BRL);
    }

    public boolean isGreaterThan(Money other) {
        if (!currency.equals(other.currency)) {
            throw new BusinessRuleViolationException("MONEY_CURRENCY_MISMATCH",
                    "cannot compare " + currency + " with " + other.currency);
        }
        return amount.compareTo(other.amount) > 0;
    }
}
