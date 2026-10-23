package br.com.eduardoreche.insureflow.adapter.out.persistence.policy;

import br.com.eduardoreche.insureflow.application.policy.port.PolicyRepository;
import br.com.eduardoreche.insureflow.domain.exception.DuplicatePolicyException;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class PolicyPersistenceAdapter implements PolicyRepository {

    private static final String UNIQUE_POLICY_ID = "uk_policy_policy_id";

    private final PolicyJpaRepository jpa;
    private final PolicyMapper mapper;

    PolicyPersistenceAdapter(PolicyJpaRepository jpa, PolicyMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public void save(Policy policy) {
        try {
            jpa.saveAndFlush(mapper.toNewEntity(policy));
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException violation
                    && UNIQUE_POLICY_ID.equals(violation.getConstraintName())) {
                throw new DuplicatePolicyException(policy.getPolicyId());
            }
            throw e;
        }
    }

    @Override
    public Policy update(Policy policy) {
        PolicyJpaEntity entity = jpa.findById(policy.getId()).orElseThrow();
        mapper.updateEntity(entity, policy);
        jpa.saveAndFlush(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Policy> findByPolicyId(String policyId) {
        return jpa.findByPolicyId(policyId).map(mapper::toDomain);
    }
}
