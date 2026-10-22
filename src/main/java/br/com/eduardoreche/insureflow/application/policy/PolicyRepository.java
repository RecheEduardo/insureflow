package br.com.eduardoreche.insureflow.application.policy;

import br.com.eduardoreche.insureflow.domain.model.Policy;

public interface PolicyRepository {

    void save(Policy policy);
}
