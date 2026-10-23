package br.com.eduardoreche.insureflow.application.policy.command;

import br.com.eduardoreche.insureflow.application.policy.command.dto.IdempotencyKeyDTO;
import br.com.eduardoreche.insureflow.application.policy.command.dto.IssuePolicyCommandDTO;
import br.com.eduardoreche.insureflow.application.policy.command.dto.IssuePolicyResultDTO;
import br.com.eduardoreche.insureflow.application.policy.event.PolicyEventType;
import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;
import br.com.eduardoreche.insureflow.application.policy.port.IdempotencyRecordRepository;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyEventOutbox;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyRepository;
import br.com.eduardoreche.insureflow.domain.exception.BusinessRuleViolationException;
import br.com.eduardoreche.insureflow.domain.exception.DuplicatePolicyException;
import br.com.eduardoreche.insureflow.domain.model.Address;
import br.com.eduardoreche.insureflow.domain.model.Coverage;
import br.com.eduardoreche.insureflow.domain.model.Insured;
import br.com.eduardoreche.insureflow.domain.model.InsuredObject;
import br.com.eduardoreche.insureflow.domain.model.Money;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import br.com.eduardoreche.insureflow.domain.model.Term;
import br.com.eduardoreche.insureflow.domain.model.enums.CoverageCode;
import br.com.eduardoreche.insureflow.domain.model.enums.CoverageFeature;
import br.com.eduardoreche.insureflow.domain.model.enums.CoverageType;
import br.com.eduardoreche.insureflow.domain.model.enums.DocumentType;
import br.com.eduardoreche.insureflow.domain.model.enums.IdentificationType;
import br.com.eduardoreche.insureflow.domain.model.enums.IssuanceType;
import br.com.eduardoreche.insureflow.domain.model.enums.PolicyStatus;
import br.com.eduardoreche.insureflow.domain.model.enums.PremiumPeriodicity;
import br.com.eduardoreche.insureflow.domain.model.enums.PropertyType;
import br.com.eduardoreche.insureflow.domain.model.enums.StreetType;
import br.com.eduardoreche.insureflow.domain.model.enums.Uf;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssuePolicyTest {

    private static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final FakePolicyRepository policies = new FakePolicyRepository();
    private final FakeIdempotencyRecordRepository idempotencyRecords = new FakeIdempotencyRecordRepository();
    private final FakePolicyEventOutbox events = new FakePolicyEventOutbox();
    private final IssuePolicy issuePolicy = new IssuePolicy(policies, idempotencyRecords, events, CLOCK);

    @Test
    void issuesActivePolicyAndRecordsTheIdempotencyKey() {
        IssuePolicyResultDTO result = issuePolicy.execute(command("AP-001", LocalDate.of(2026, 10, 22)),
                new IdempotencyKeyDTO("key-1", "hash-a"));

        assertThat(result).isEqualTo(new IssuePolicyResultDTO("AP-001", PolicyStatus.ACTIVE, 0));
        assertThat(policies.saved).singleElement().satisfies(policy -> {
            assertThat(policy.getPolicyId()).isEqualTo("AP-001");
            assertThat(policy.getCreatedAt()).isEqualTo(NOW);
        });
        assertThat(idempotencyRecords.byKey.get("key-1")).isEqualTo(new IdempotencyRecord("key-1", "hash-a", result, NOW));
    }

    @Test
    void recordsAPolicyIssuedEventWithTheStateOfTheNewPolicy() {
        issuePolicy.execute(command("AP-001", LocalDate.of(2026, 10, 1)), new IdempotencyKeyDTO("key-1", "hash-a"));

        assertThat(events.appended).singleElement().satisfies(event -> {
            assertThat(event.type()).isEqualTo(PolicyEventType.POLICY_ISSUED);
            assertThat(event.policyId()).isEqualTo("AP-001");
            assertThat(event.policyVersion()).isZero();
            assertThat(event.occurredAt()).isEqualTo(NOW);
            assertThat(event.policy().productName()).isEqualTo("Residencial Essencial");
            assertThat(event.policy().status()).isEqualTo(PolicyStatus.ACTIVE);
        });
    }

    @Test
    void replaysTheOriginalResultForSameKeyAndSameRequest() {
        IdempotencyKeyDTO key = new IdempotencyKeyDTO("key-1", "hash-a");
        IssuePolicyResultDTO original = issuePolicy.execute(command("AP-001", LocalDate.of(2026, 10, 1)), key);

        IssuePolicyResultDTO replayed = issuePolicy.execute(command("AP-001", LocalDate.of(2026, 10, 1)), key);

        assertThat(replayed).isEqualTo(original);
        assertThat(policies.saved).hasSize(1);
        assertThat(events.appended).hasSize(1);
    }

    @Test
    void rejectsSameKeyWithDifferentRequest() {
        issuePolicy.execute(command("AP-001", LocalDate.of(2026, 10, 1)), new IdempotencyKeyDTO("key-1", "hash-a"));

        assertThatThrownBy(() -> issuePolicy.execute(command("AP-002", LocalDate.of(2026, 10, 1)),
                new IdempotencyKeyDTO("key-1", "hash-b")))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class,
                        e -> assertThat(e.getCode()).isEqualTo("IDEMPOTENCY_KEY_REUSED"));
        assertThat(policies.saved).extracting(Policy::getPolicyId).containsExactly("AP-001");
    }

    @Test
    void replaysTheConcurrentWinnerWhenTheKeyIsTakenBetweenLookupAndSave() {
        IssuePolicyResultDTO winner = new IssuePolicyResultDTO("AP-001", PolicyStatus.ACTIVE, 0);
        idempotencyRecords.concurrentWinner = new IdempotencyRecord("key-1", "hash-a", winner, NOW);

        IssuePolicyResultDTO result = issuePolicy.execute(command("AP-001", LocalDate.of(2026, 10, 1)),
                new IdempotencyKeyDTO("key-1", "hash-a"));

        assertThat(result).isEqualTo(winner);
        assertThat(policies.saved).isEmpty();
        assertThat(events.appended).isEmpty();
    }

    @Test
    void doesNotRecordTheKeyWhenIssuanceIsRejected() {
        assertThatThrownBy(() -> issuePolicy.execute(command("AP-001", LocalDate.of(2026, 10, 23)),
                new IdempotencyKeyDTO("key-1", "hash-a")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("issuanceDate");

        assertThat(policies.saved).isEmpty();
        assertThat(idempotencyRecords.byKey).isEmpty();
        assertThat(events.appended).isEmpty();
    }

    @Test
    void rejectsSecondPolicyWithSamePolicyIdAndDifferentKey() {
        issuePolicy.execute(command("AP-001", LocalDate.of(2026, 10, 1)), new IdempotencyKeyDTO("key-1", "hash-a"));

        assertThatThrownBy(() -> issuePolicy.execute(command("AP-001", LocalDate.of(2026, 10, 2)),
                new IdempotencyKeyDTO("key-2", "hash-b")))
                .isInstanceOf(DuplicatePolicyException.class);
        assertThat(policies.saved).hasSize(1);
        assertThat(events.appended).extracting(PolicyEventDTO::policyId).containsExactly("AP-001");
    }

    private static IssuePolicyCommandDTO command(String policyId, LocalDate issuanceDate) {
        Term term = new Term(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30));
        Coverage coverage = new Coverage("0118", CoverageCode.INCENDIO, null, "15414.900000/2020-11",
                Money.brl(new BigDecimal("100000.00")), term, true, CoverageFeature.MASSIFICADOS,
                CoverageType.REGULAR_COMUM, PremiumPeriodicity.ANUAL);
        InsuredObject object = new InsuredObject("IMOVEL-1", PropertyType.CASA, "Casa residencial", "01310100",
                null, List.of(coverage));
        Insured insured = new Insured("12345678901", IdentificationType.CPF, "Maria Ficticia",
                LocalDate.of(1990, 5, 17), null,
                new Address(StreetType.RUA, "Rua Exemplo", "100", null, null, "Sao Paulo", Uf.SP, "01310100"));
        return new IssuePolicyCommandDTO(policyId, DocumentType.APOLICE_INDIVIDUAL, null,
                IssuanceType.EMISSAO_PROPRIA, issuanceDate, term, Money.brl(new BigDecimal("200000.00")),
                "PROP-001", "Residencial Essencial", List.of(insured), List.of(object));
    }

    private static class FakePolicyRepository implements PolicyRepository {

        final List<Policy> saved = new ArrayList<>();

        @Override
        public void save(Policy policy) {
            if (saved.stream().anyMatch(p -> p.getPolicyId().equals(policy.getPolicyId()))) {
                throw new DuplicatePolicyException(policy.getPolicyId());
            }
            saved.add(policy);
        }

        @Override
        public Policy update(Policy policy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Policy> findByPolicyId(String policyId) {
            return saved.stream().filter(p -> p.getPolicyId().equals(policyId)).findFirst();
        }
    }

    private static class FakeIdempotencyRecordRepository implements IdempotencyRecordRepository {

        final Map<String, IdempotencyRecord> byKey = new HashMap<>();
        IdempotencyRecord concurrentWinner;

        @Override
        public Optional<IdempotencyRecord> findByKey(String key) {
            return Optional.ofNullable(byKey.get(key));
        }

        @Override
        public boolean saveIfAbsent(IdempotencyRecord idempotencyRecord) {
            if (concurrentWinner != null) {
                byKey.put(concurrentWinner.key(), concurrentWinner);
            }
            return byKey.putIfAbsent(idempotencyRecord.key(), idempotencyRecord) == null;
        }
    }

    private static class FakePolicyEventOutbox implements PolicyEventOutbox {

        final List<PolicyEventDTO> appended = new ArrayList<>();

        @Override
        public void append(PolicyEventDTO event) {
            appended.add(event);
        }

        @Override
        public List<PolicyEventDTO> lockPending(int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(List<UUID> eventIds) {
            throw new UnsupportedOperationException();
        }
    }
}
