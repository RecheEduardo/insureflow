package br.com.eduardoreche.insureflow.application.policy.port;

import br.com.eduardoreche.insureflow.application.policy.command.IdempotencyRecord;

import java.util.Optional;

public interface IdempotencyRecordRepository {

    Optional<IdempotencyRecord> findByKey(String key);

    boolean saveIfAbsent(IdempotencyRecord idempotencyRecord);
}
