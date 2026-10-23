package br.com.eduardoreche.insureflow.application.policy.event;

import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyEventOutbox;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyEventPublisher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static br.com.eduardoreche.insureflow.application.policy.PolicyFixtures.activePolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RelayPolicyEventsTest {

    private final FakePolicyEventOutbox outbox = new FakePolicyEventOutbox();
    private final FakePolicyEventPublisher publisher = new FakePolicyEventPublisher();
    private final RelayPolicyEvents relay = new RelayPolicyEvents(outbox, publisher, 2);

    @Test
    void publishesTheNextBatchInOrderAndRemovesItFromTheOutbox() {
        PolicyEventDTO first = issued("AP-001");
        PolicyEventDTO second = issued("AP-002");
        PolicyEventDTO third = issued("AP-003");
        outbox.pending.addAll(List.of(first, second, third));

        int published = relay.execute();

        assertThat(published).isEqualTo(2);
        assertThat(publisher.published).containsExactly(first, second);
        assertThat(outbox.pending).containsExactly(third);
    }

    @Test
    void keepsTheEventsInTheOutboxWhenPublishingFails() {
        PolicyEventDTO event = issued("AP-001");
        outbox.pending.add(event);
        publisher.failure = new IllegalStateException("broker unavailable");

        assertThatThrownBy(relay::execute).isInstanceOf(IllegalStateException.class);
        assertThat(outbox.pending).containsExactly(event);
    }

    @Test
    void doesNothingWhenTheOutboxIsEmpty() {
        assertThat(relay.execute()).isZero();
        assertThat(publisher.published).isEmpty();
    }

    private static PolicyEventDTO issued(String policyId) {
        return PolicyEventDTO.of(PolicyEventType.POLICY_ISSUED, activePolicy(policyId));
    }

    private static class FakePolicyEventOutbox implements PolicyEventOutbox {

        final List<PolicyEventDTO> pending = new ArrayList<>();

        @Override
        public void append(PolicyEventDTO event) {
            pending.add(event);
        }

        @Override
        public List<PolicyEventDTO> lockPending(int limit) {
            return List.copyOf(pending.subList(0, Math.min(limit, pending.size())));
        }

        @Override
        public void delete(List<UUID> eventIds) {
            pending.removeIf(event -> eventIds.contains(event.eventId()));
        }
    }

    private static class FakePolicyEventPublisher implements PolicyEventPublisher {

        final List<PolicyEventDTO> published = new ArrayList<>();
        RuntimeException failure;

        @Override
        public void publish(List<PolicyEventDTO> events) {
            if (failure != null) {
                throw failure;
            }
            published.addAll(events);
        }
    }
}
