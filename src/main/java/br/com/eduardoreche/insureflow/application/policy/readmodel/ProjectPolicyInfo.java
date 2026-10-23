package br.com.eduardoreche.insureflow.application.policy.readmodel;

import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyInfoViewRepository;
import org.springframework.transaction.annotation.Transactional;

public class ProjectPolicyInfo {

    private final PolicyInfoViewRepository views;

    public ProjectPolicyInfo(PolicyInfoViewRepository views) {
        this.views = views;
    }

    @Transactional
    public boolean execute(PolicyEventDTO event) {
        return views.saveIfNewer(event.policyVersion(), event.policy());
    }
}
