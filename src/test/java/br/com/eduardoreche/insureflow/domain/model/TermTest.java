package br.com.eduardoreche.insureflow.domain.model;

import br.com.eduardoreche.insureflow.domain.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TermTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final LocalDate END = LocalDate.of(2027, 9, 30);

    @Test
    void refusesEndBeforeStart() {
        assertThatThrownBy(() -> new Term(END, START))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("TERM_INVALID");
    }

    @Test
    void acceptsATermThatStartsAndEndsOnTheSameDay() {
        Term term = new Term(START, START);

        assertThat(term.contains(START)).isTrue();
    }

    @Test
    void containsBothEndsInclusively() {
        Term term = new Term(START, END);

        assertThat(term.contains(START)).isTrue();
        assertThat(term.contains(END)).isTrue();
        assertThat(term.contains(START.minusDays(1))).isFalse();
        assertThat(term.contains(END.plusDays(1))).isFalse();
    }

    @Test
    void containsAnotherTermOnlyWhenBothEndsAreInside() {
        Term term = new Term(START, END);

        assertThat(term.contains(new Term(START, END))).isTrue();
        assertThat(term.contains(new Term(START.plusDays(10), END.minusDays(10)))).isTrue();
        assertThat(term.contains(new Term(START.minusDays(1), END))).isFalse();
        assertThat(term.contains(new Term(START, END.plusDays(1)))).isFalse();
    }

    @Test
    void hasEndedOnlyAfterTheLastDay() {
        Term term = new Term(START, END);

        assertThat(term.hasEndedOn(END)).isFalse();
        assertThat(term.hasEndedOn(END.plusDays(1))).isTrue();
    }

    @Test
    void requiredDateMissingIsABug() {
        assertThatThrownBy(() -> new Term(null, END)).isInstanceOf(NullPointerException.class);
    }
}
