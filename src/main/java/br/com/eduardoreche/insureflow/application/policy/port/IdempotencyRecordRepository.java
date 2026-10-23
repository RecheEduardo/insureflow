package br.com.eduardoreche.insureflow.application.policy;

import java.util.Optional;

public interface IdempotencyRecordRepository {

    Optional<IdempotencyRecord> findByKey(String key);

    boolean saveIfAbsent(IdempotencyRecord idempotencyRecord);
}
