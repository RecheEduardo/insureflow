package br.com.eduardoreche.insureflow.domain.model;

import br.com.eduardoreche.insureflow.domain.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void refusesNegativeAmount() {
        assertThatThrownBy(() -> Money.brl(new BigDecimal("-0.01")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("MONEY_NEGATIVE");
    }

    @Test
    void refusesMoreThanTwoDecimalPlacesInsteadOfRounding() {
        assertThatThrownBy(() -> Money.brl(new BigDecimal("100.001")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("MONEY_SCALE");
    }

    @Test
    void acceptsTrailingZerosBeyondTwoPlacesBecauseNothingIsLost() {
        assertThat(Money.brl(new BigDecimal("100.500")).amount()).isEqualByComparingTo("100.50");
    }

    @Test
    void normalizesToTwoDecimalPlaces() {
        assertThat(Money.brl(new BigDecimal("100")).amount()).hasToString("100.00");
    }

    @Test
    void comparesAmountsOfTheSameCurrency() {
        Money lmi = Money.brl(new BigDecimal("100000.00"));

        assertThat(Money.brl(new BigDecimal("100000.01")).isGreaterThan(lmi)).isTrue();
        assertThat(Money.brl(new BigDecimal("100000.00")).isGreaterThan(lmi)).isFalse();
    }

    @Test
    void refusesToCompareDifferentCurrencies() {
        Money brl = Money.brl(new BigDecimal("10.00"));
        Money usd = new Money(new BigDecimal("10.00"), Currency.getInstance("USD"));

        assertThatThrownBy(() -> brl.isGreaterThan(usd))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("MONEY_CURRENCY_MISMATCH");
    }
}
