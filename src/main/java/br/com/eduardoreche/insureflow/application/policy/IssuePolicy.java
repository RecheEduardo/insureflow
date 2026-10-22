package br.com.eduardoreche.insureflow.application.policy;

import br.com.eduardoreche.insureflow.application.policy.dto.IdempotencyKeyDTO;
import br.com.eduardoreche.insureflow.application.policy.dto.IssuePolicyCommandDTO;
import br.com.eduardoreche.insureflow.application.policy.dto.IssuePolicyResultDTO;
import br.com.eduardoreche.insureflow.domain.exception.BusinessRuleViolationException;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public class IssuePolicy {

    private final PolicyRepository policies;
    private final IdempotencyRecordRepository idempotencyRecords;
    private final Clock clock;

    public IssuePolicy(PolicyRepository policies, IdempotencyRecordRepository idempotencyRecords, Clock clock) {
        this.policies = policies;
        this.idempotencyRecords = idempotencyRecords;
        this.clock = clock;
    }

    @Transactional
    public IssuePolicyResultDTO execute(IssuePolicyCommandDTO command, IdempotencyKeyDTO idempotencyKey) {
        Optional<IdempotencyRecord> existingRecord = idempotencyRecords.findByKey(idempotencyKey.key());
        if (existingRecord.isPresent()) {
            return originalResultOf(existingRecord.get(), idempotencyKey);
        }

        Policy policy = newPolicy(command);

        IssuePolicyResultDTO result = IssuePolicyResultDTO.from(policy);
        IdempotencyRecord newRecord = new IdempotencyRecord(
                idempotencyKey.key(), idempotencyKey.requestHash(), result, Instant.now(clock));

        boolean wasIdempotencyRecordAlreadySaved = !idempotencyRecords.saveIfAbsent(newRecord);
        if (wasIdempotencyRecordAlreadySaved) {
            return originalResultOf(idempotencyRecords.findByKey(idempotencyKey.key()).orElseThrow(), idempotencyKey);
        }

        policies.save(policy);
        return result;
    }

    private Policy newPolicy(IssuePolicyCommandDTO command) {
        return Policy.create(
                command.policyId(),
                command.documentType(),
                command.groupCertificateId(),
                command.issuanceType(),
                command.issuanceDate(),
                command.term(),
                command.maxLmg(),
                command.proposalId(),
                command.productName(),
                command.insureds(),
                command.insuredObjects(),
                LocalDate.now(clock),
                Instant.now(clock));
    }

    private static IssuePolicyResultDTO originalResultOf(IdempotencyRecord existingRecord, IdempotencyKeyDTO idempotencyKey) {
        boolean isRequestHashDifferent = !existingRecord.requestHash().equals(idempotencyKey.requestHash());

        if (isRequestHashDifferent) {
            throw new BusinessRuleViolationException("IDEMPOTENCY_KEY_REUSED",
                    "idempotency key was already used with a different request");
        }
        return existingRecord.originalResult();
    }
}
