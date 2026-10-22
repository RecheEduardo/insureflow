package br.com.eduardoreche.insureflow.domain.exception;

public class DuplicatePolicyException extends DomainException {

    public DuplicatePolicyException(String policyId) {
        super("POLICY_DUPLICATE", "Policy already exists: " + policyId);
    }
}
