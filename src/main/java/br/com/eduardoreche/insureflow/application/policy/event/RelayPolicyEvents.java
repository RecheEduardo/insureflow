package br.com.eduardoreche.insureflow.application.policy.event;

import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyEventOutbox;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class RelayPolicyEvents {

    private final PolicyEventOutbox outbox;
    private final PolicyEventPublisher publisher;
    private final int batchSize;

    public RelayPolicyEvents(PolicyEventOutbox outbox, PolicyEventPublisher publisher, int batchSize) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.batchSize = batchSize;
    }

    @Transactional
    public int execute() {
        List<PolicyEventDTO> pending = outbox.lockPending(batchSize);
        if (pending.isEmpty()) {
            return 0;
        }
        publisher.publish(pending);
        outbox.delete(pending.stream().map(PolicyEventDTO::eventId).toList());
        return pending.size();
    }
}
