package br.com.eduardoreche.insureflow.application.policy.port;

import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;

public interface PolicyEventHistoryRepository {

    boolean saveIfAbsent(PolicyEventDTO event);
}
