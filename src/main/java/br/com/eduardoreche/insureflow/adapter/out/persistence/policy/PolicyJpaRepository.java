package br.com.eduardoreche.insureflow.adapter.out.persistence.policy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface PolicyJpaRepository extends JpaRepository<PolicyJpaEntity, UUID> {

    Optional<PolicyJpaEntity> findByPolicyId(String policyId);
}
