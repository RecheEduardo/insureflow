package br.com.eduardoreche.insureflow.application.policy.port;

import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;

import java.util.List;
import java.util.UUID;

public interface PolicyEventOutbox {

    void append(PolicyEventDTO event);

    List<PolicyEventDTO> lockPending(int limit);

    void delete(List<UUID> eventIds);
}
