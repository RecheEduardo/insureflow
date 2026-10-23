package br.com.eduardoreche.insureflow.application.policy.command;

import br.com.eduardoreche.insureflow.application.policy.command.dto.EndorsePolicyCommandDTO;
import br.com.eduardoreche.insureflow.application.policy.event.PolicyEventType;
import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyEventOutbox;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyRepository;
import br.com.eduardoreche.insureflow.domain.exception.BusinessRuleViolationException;
import br.com.eduardoreche.insureflow.domain.exception.PolicyCancelledException;
import br.com.eduardoreche.insureflow.domain.exception.PolicyNotFoundException;
import br.com.eduardoreche.insureflow.domain.exception.PolicyVersionMismatchException;
import br.com.eduardoreche.insureflow.domain.model.Money;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import br.com.eduardoreche.insureflow.domain.model.enums.CancellationReason;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static br.com.eduardoreche.insureflow.application.policy.PolicyFixtures.NOW;
import static br.com.eduardoreche.insureflow.application.policy.PolicyFixtures.activePolicy;
import static br.com.eduardoreche.insureflow.application.policy.PolicyFixtures.withNextVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EndorsePolicyTest {

    private static final Instant ENDORSEMENT_TIME = Instant.parse("2026-12-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(ENDORSEMENT_TIME, ZoneOffset.UTC);

    private final FakePolicyRepository policies = new FakePolicyRepository();
    private final FakePolicyEventOutbox events = new FakePolicyEventOutbox();
    private final EndorsePolicy endorsePolicy = new EndorsePolicy(policies, events, CLOCK);

    @Test
    void changesOnlyTheGivenFieldsAndKeepsTheRest() {
        policies.stored = activePolicy("AP-001");
        Policy before = policies.stored;

        Policy endorsed = endorsePolicy.execute(command(0, null, LocalDate.of(2028, 9, 30), null, "Residencial Plus"));

        assertThat(endorsed.getProductName()).isEqualTo("Residencial Plus");
        assertThat(endorsed.getTerm().endDate()).isEqualTo(LocalDate.of(2028, 9, 30));
        assertThat(endorsed.getTerm().startDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(endorsed.getMaxLmg()).isEqualTo(before.getMaxLmg());
        assertThat(endorsed.getInsureds()).isEqualTo(before.getInsureds());
        assertThat(endorsed.getUpdatedAt()).isEqualTo(ENDORSEMENT_TIME);
        assertThat(policies.updated).containsExactly(endorsed);
    }

    @Test
    void recordsAPolicyEndorsedEventWithTheVersionSavedByTheRepository() {
        policies.stored = activePolicy("AP-001");

        endorsePolicy.execute(command(0, null, null, null, "Residencial Plus"));

        assertThat(events.appended).singleElement().satisfies(event -> {
            assertThat(event.type()).isEqualTo(PolicyEventType.POLICY_ENDORSED);
            assertThat(event.policyVersion()).isEqualTo(1);
            assertThat(event.occurredAt()).isEqualTo(ENDORSEMENT_TIME);
            assertThat(event.policy().productName()).isEqualTo("Residencial Plus");
        });
    }

    @Test
    void refusesWhenExpectedVersionIsNotTheCurrentOne() {
        policies.stored = activePolicy("AP-001");

        assertThatThrownBy(() -> endorsePolicy.execute(command(7, null, null, null, "Residencial Plus")))
                .isInstanceOf(PolicyVersionMismatchException.class);
        assertThat(policies.updated).isEmpty();
        assertThat(events.appended).isEmpty();
        assertThat(policies.stored.getProductName()).isEqualTo("Residencial Essencial");
    }

    @Test
    void refusesEndorsementOfCancelledPolicy() {
        policies.stored = activePolicy("AP-001");
        policies.stored.cancel(CancellationReason.OUTROS, NOW);

        assertThatThrownBy(() -> endorsePolicy.execute(command(0, null, null, null, "Residencial Plus")))
                .isInstanceOf(PolicyCancelledException.class);
        assertThat(policies.updated).isEmpty();
        assertThat(events.appended).isEmpty();
    }

    @Test
    void refusesEndorsementThatChangesNothing() {
        policies.stored = activePolicy("AP-001");

        assertThatThrownBy(() -> endorsePolicy.execute(command(0, null, null, null, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("at least one field");
    }

    @Test
    void refusesLmgBelowAnExistingCoverageLmiWithoutSavingAnything() {
        policies.stored = activePolicy("AP-001");

        assertThatThrownBy(() -> endorsePolicy.execute(
                command(0, null, null, Money.brl(new BigDecimal("50000.00")), null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("LMI");
        assertThat(policies.updated).isEmpty();
        assertThat(policies.stored.getMaxLmg()).isEqualTo(Money.brl(new BigDecimal("200000.00")));
    }

    @Test
    void failsWhenPolicyDoesNotExist() {
        assertThatThrownBy(() -> endorsePolicy.execute(command(0, null, null, null, "Residencial Plus")))
                .isInstanceOf(PolicyNotFoundException.class);
    }

    private static EndorsePolicyCommandDTO command(long expectedVersion, LocalDate termStart, LocalDate termEnd,
                                                Money maxLmg, String productName) {
        return new EndorsePolicyCommandDTO("AP-001", expectedVersion, termStart, termEnd, maxLmg, productName,
                null, null);
    }

    private static class FakePolicyRepository implements PolicyRepository {

        Policy stored;
        final List<Policy> updated = new ArrayList<>();

        @Override
        public void save(Policy policy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Policy update(Policy policy) {
            updated.add(policy);
            return withNextVersion(policy);
        }

        @Override
        public Optional<Policy> findByPolicyId(String policyId) {
            return stored != null && stored.getPolicyId().equals(policyId) ? Optional.of(stored) : Optional.empty();
        }
    }

    private static class FakePolicyEventOutbox implements PolicyEventOutbox {

        final List<PolicyEventDTO> appended = new ArrayList<>();

        @Override
        public void append(PolicyEventDTO event) {
            appended.add(event);
        }

        @Override
        public List<PolicyEventDTO> lockPending(int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(List<UUID> eventIds) {
            throw new UnsupportedOperationException();
        }
    }
}
