package br.com.eduardoreche.insureflow.domain.exception;

public class PolicyCancelledException extends DomainException {

    public PolicyCancelledException(String policyId) {
        super("POLICY_CANCELLED", "Policy is cancelled and no longer accepts changes: " + policyId);
    }
}
