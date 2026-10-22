package br.com.eduardoreche.insureflow.application.policy;

import br.com.eduardoreche.insureflow.application.policy.dto.CancelPolicyCommandDTO;
import br.com.eduardoreche.insureflow.domain.exception.PolicyNotFoundException;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import br.com.eduardoreche.insureflow.domain.model.enums.CancellationReason;
import br.com.eduardoreche.insureflow.domain.model.enums.PolicyStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static br.com.eduardoreche.insureflow.application.policy.PolicyFixtures.activePolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CancelPolicyTest {

    private static final Instant CANCELLATION_TIME = Instant.parse("2026-11-05T09:30:00Z");
    private static final Clock CLOCK = Clock.fixed(CANCELLATION_TIME, ZoneOffset.UTC);

    private final FakePolicyRepository policies = new FakePolicyRepository();
    private final CancelPolicy cancelPolicy = new CancelPolicy(policies, CLOCK);

    @Test
    void cancelsActivePolicyKeepingItInTheRepository() {
        policies.stored = activePolicy("AP-001");

        Policy cancelled = cancelPolicy.execute(
                new CancelPolicyCommandDTO("AP-001", CancellationReason.FALTA_DE_PAGAMENTO));

        assertThat(cancelled.getStatus()).isEqualTo(PolicyStatus.CANCELLED);
        assertThat(cancelled.getCancellationReason()).isEqualTo(CancellationReason.FALTA_DE_PAGAMENTO);
        assertThat(cancelled.getCancelledAt()).isEqualTo(CANCELLATION_TIME);
        assertThat(policies.updated).containsExactly(cancelled);
        assertThat(policies.findByPolicyId("AP-001")).isPresent();
    }

    @Test
    void cancellingAgainChangesNothingAndWritesNothing() {
        policies.stored = activePolicy("AP-001");
        cancelPolicy.execute(new CancelPolicyCommandDTO("AP-001", CancellationReason.FALTA_DE_PAGAMENTO));
        policies.updated.clear();

        Policy again = cancelPolicy.execute(new CancelPolicyCommandDTO("AP-001", CancellationReason.OUTROS));

        assertThat(again.getCancellationReason()).isEqualTo(CancellationReason.FALTA_DE_PAGAMENTO);
        assertThat(policies.updated).isEmpty();
    }

    @Test
    void failsWhenPolicyDoesNotExist() {
        policies.stored = activePolicy("AP-001");

        assertThatThrownBy(() -> cancelPolicy.execute(new CancelPolicyCommandDTO("AP-999", CancellationReason.OUTROS)))
                .isInstanceOf(PolicyNotFoundException.class);
        assertThat(policies.updated).isEmpty();
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
            return policy;
        }

        @Override
        public Optional<Policy> findByPolicyId(String policyId) {
            return stored != null && stored.getPolicyId().equals(policyId) ? Optional.of(stored) : Optional.empty();
        }
    }
}
