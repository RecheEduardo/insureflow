package br.com.eduardoreche.insureflow.domain.exception;

public class PolicyNotFoundException extends DomainException {

    public PolicyNotFoundException(String policyId) {
        super("POLICY_NOT_FOUND", "Policy not found: " + policyId);
    }
}
