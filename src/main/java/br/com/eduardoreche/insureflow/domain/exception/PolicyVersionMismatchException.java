package br.com.eduardoreche.insureflow.domain.exception;

public class PolicyVersionMismatchException extends DomainException {

    public PolicyVersionMismatchException(String policyId, long expectedVersion, long currentVersion) {
        super("POLICY_VERSION_MISMATCH", "Policy " + policyId + " is at version " + currentVersion
                + " but the request expected " + expectedVersion);
    }
}
