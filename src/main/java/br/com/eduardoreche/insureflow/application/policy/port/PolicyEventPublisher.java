package br.com.eduardoreche.insureflow.application.policy.port;

import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;

import java.util.List;

public interface PolicyEventPublisher {

    void publish(List<PolicyEventDTO> events);
}
