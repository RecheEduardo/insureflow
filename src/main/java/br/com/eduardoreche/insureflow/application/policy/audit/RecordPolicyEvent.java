package br.com.eduardoreche.insureflow.application.policy.audit;

import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyEventHistoryRepository;
import org.springframework.transaction.annotation.Transactional;

public class RecordPolicyEvent {

    private final PolicyEventHistoryRepository history;

    public RecordPolicyEvent(PolicyEventHistoryRepository history) {
        this.history = history;
    }

    @Transactional
    public boolean execute(PolicyEventDTO event) {
        return history.saveIfAbsent(event);
    }
}
