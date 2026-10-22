package br.com.eduardoreche.insureflow.adapter.out.persistence.policy;

import br.com.eduardoreche.insureflow.application.policy.PolicyQueryRepository;
import br.com.eduardoreche.insureflow.application.policy.dto.PolicyPageDTO;
import br.com.eduardoreche.insureflow.application.policy.dto.PolicySummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class PolicyQueryPersistenceAdapter implements PolicyQueryRepository {

    private final PolicyJpaRepository jpa;

    PolicyQueryPersistenceAdapter(PolicyJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public PolicyPageDTO findPage(int page, int pageSize) {
        Page<PolicySummaryDTO> result = jpa.findAllBy(PageRequest.of(page - 1, pageSize, Sort.by("policyId")));
        return new PolicyPageDTO(result.getContent(), result.getTotalElements(), result.getTotalPages());
    }
}
