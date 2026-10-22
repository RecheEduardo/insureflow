package br.com.eduardoreche.insureflow.application.policy;

import br.com.eduardoreche.insureflow.application.policy.dto.PolicyPageDTO;

public interface PolicyQueryRepository {

    PolicyPageDTO findPage(int page, int pageSize);
}
