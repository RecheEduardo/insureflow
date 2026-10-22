package br.com.eduardoreche.insureflow.application.policy;

import br.com.eduardoreche.insureflow.domain.model.Policy;

import java.util.Optional;

public interface PolicyRepository {

    void save(Policy policy);

    Optional<Policy> findByPolicyId(String policyId);
}
