package br.com.eduardoreche.insureflow.application.policy.port;

import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicySnapshotDTO;
import br.com.eduardoreche.insureflow.application.policy.readmodel.dto.PolicyInfoPageDTO;

import java.util.Optional;

public interface PolicyInfoViewRepository {

    boolean saveIfNewer(long policyVersion, PolicySnapshotDTO policy);

    Optional<PolicySnapshotDTO> findByPolicyId(String policyId);

    PolicyInfoPageDTO findPage(int page, int pageSize);
}
