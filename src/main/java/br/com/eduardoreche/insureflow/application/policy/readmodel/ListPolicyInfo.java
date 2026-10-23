package br.com.eduardoreche.insureflow.application.policy.readmodel;

import br.com.eduardoreche.insureflow.application.policy.port.PolicyInfoViewRepository;
import br.com.eduardoreche.insureflow.application.policy.readmodel.dto.PolicyInfoPageDTO;
import org.springframework.transaction.annotation.Transactional;

public class ListPolicyInfo {

    private final PolicyInfoViewRepository views;

    public ListPolicyInfo(PolicyInfoViewRepository views) {
        this.views = views;
    }

    @Transactional(readOnly = true)
    public PolicyInfoPageDTO execute(int page, int pageSize) {
        return views.findPage(page, pageSize);
    }
}
