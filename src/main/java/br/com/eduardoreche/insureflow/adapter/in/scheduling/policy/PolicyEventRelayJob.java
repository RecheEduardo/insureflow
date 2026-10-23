package br.com.eduardoreche.insureflow.adapter.in.scheduling.policy;

import br.com.eduardoreche.insureflow.application.policy.event.RelayPolicyEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class PolicyEventRelayJob {

    private static final Logger log = LoggerFactory.getLogger(PolicyEventRelayJob.class);

    private final RelayPolicyEvents relayPolicyEvents;

    PolicyEventRelayJob(RelayPolicyEvents relayPolicyEvents) {
        this.relayPolicyEvents = relayPolicyEvents;
    }

    @Scheduled(fixedDelayString = "${insureflow.outbox.relay-interval-ms}")
    void relay() {
        try {
            int published;
            do {
                published = relayPolicyEvents.execute();
            } while (published > 0);
        } catch (RuntimeException e) {
            log.warn("Policy event relay failed; pending events stay in the outbox: {}", e.getMessage());
        }
    }
}
