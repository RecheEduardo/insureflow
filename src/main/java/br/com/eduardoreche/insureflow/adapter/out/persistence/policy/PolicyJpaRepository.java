package br.com.eduardoreche.insureflow.adapter.out.persistence.policy;

import br.com.eduardoreche.insureflow.application.policy.dto.PolicySummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface PolicyJpaRepository extends JpaRepository<PolicyJpaEntity, UUID> {

    Optional<PolicyJpaEntity> findByPolicyId(String policyId);

    Page<PolicySummaryDTO> findAllBy(Pageable pageable);
}
