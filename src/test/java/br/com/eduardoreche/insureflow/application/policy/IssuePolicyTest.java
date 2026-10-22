package br.com.eduardoreche.insureflow.application.policy;

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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssuePolicyTest {

    private static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final FakePolicyRepository policies = new FakePolicyRepository();
    private final IssuePolicy issuePolicy = new IssuePolicy(policies, CLOCK);

    @Test
    void issuesActivePolicyAndSavesIt() {
        Policy policy = issuePolicy.execute(command("AP-001", LocalDate.of(2026, 10, 22)));

        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(policy.getVersion()).isZero();
        assertThat(policy.getCreatedAt()).isEqualTo(NOW);
        assertThat(policies.saved).containsExactly(policy);
    }

    @Test
    void rejectsIssuanceInTheFutureAndSavesNothing() {
        assertThatThrownBy(() -> issuePolicy.execute(command("AP-001", LocalDate.of(2026, 10, 23))))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("issuanceDate");

        assertThat(policies.saved).isEmpty();
    }

    @Test
    void rejectsSecondPolicyWithSamePolicyId() {
        issuePolicy.execute(command("AP-001", LocalDate.of(2026, 10, 1)));

        assertThatThrownBy(() -> issuePolicy.execute(command("AP-001", LocalDate.of(2026, 10, 2))))
                .isInstanceOf(DuplicatePolicyException.class);
        assertThat(policies.saved).hasSize(1);
    }

    private static IssuePolicyCommand command(String policyId, LocalDate issuanceDate) {
        Term term = new Term(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30));
        Coverage coverage = new Coverage("0118", CoverageCode.INCENDIO, null, "15414.900000/2020-11",
                Money.brl(new BigDecimal("100000.00")), term, true, CoverageFeature.MASSIFICADOS,
                CoverageType.REGULAR_COMUM, PremiumPeriodicity.ANUAL);
        InsuredObject object = new InsuredObject("IMOVEL-1", PropertyType.CASA, "Casa residencial", "01310100",
                null, List.of(coverage));
        Insured insured = new Insured("12345678901", IdentificationType.CPF, "Maria Ficticia",
                LocalDate.of(1990, 5, 17), null,
                new Address(StreetType.RUA, "Rua Exemplo", "100", null, null, "Sao Paulo", Uf.SP, "01310100"));
        return new IssuePolicyCommand(policyId, DocumentType.APOLICE_INDIVIDUAL, null,
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
        public Optional<Policy> findByPolicyId(String policyId) {
            return saved.stream().filter(p -> p.getPolicyId().equals(policyId)).findFirst();
        }
    }
}
