package br.com.eduardoreche.insureflow.application.policy;

import br.com.eduardoreche.insureflow.domain.exception.PolicyNotFoundException;
import br.com.eduardoreche.insureflow.domain.model.Address;
import br.com.eduardoreche.insureflow.domain.model.Coverage;
import br.com.eduardoreche.insureflow.domain.model.Insured;
import br.com.eduardoreche.insureflow.domain.model.InsuredObject;
import br.com.eduardoreche.insureflow.domain.model.Money;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import br.com.eduardoreche.insureflow.domain.model.Term;
import br.com.eduardoreche.insureflow.domain.model.enums.CancellationReason;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetPolicyTest {

    private static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");

    @Test
    void returnsPolicyFoundByPolicyId() {
        Policy stored = policy("AP-001");
        GetPolicy getPolicy = new GetPolicy(repositoryWith(stored));

        assertThat(getPolicy.execute("AP-001")).isSameAs(stored);
    }

    @Test
    void returnsCancelledPolicyWithItsStatus() {
        Policy stored = policy("AP-001");
        stored.cancel(CancellationReason.PEDIDO_DO_SEGURADO, NOW);
        GetPolicy getPolicy = new GetPolicy(repositoryWith(stored));

        assertThat(getPolicy.execute("AP-001").getStatus()).isEqualTo(PolicyStatus.CANCELLED);
    }

    @Test
    void failsWhenPolicyDoesNotExist() {
        GetPolicy getPolicy = new GetPolicy(repositoryWith(policy("AP-001")));

        assertThatThrownBy(() -> getPolicy.execute("AP-999"))
                .isInstanceOf(PolicyNotFoundException.class)
                .hasMessageContaining("AP-999");
    }

    private static PolicyRepository repositoryWith(Policy stored) {
        return new PolicyRepository() {
            @Override
            public void save(Policy policy) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<Policy> findByPolicyId(String policyId) {
                return stored.getPolicyId().equals(policyId) ? Optional.of(stored) : Optional.empty();
            }
        };
    }

    private static Policy policy(String policyId) {
        Term term = new Term(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30));
        Coverage coverage = new Coverage("0118", CoverageCode.INCENDIO, null, "15414.900000/2020-11",
                Money.brl(new BigDecimal("100000.00")), term, true, CoverageFeature.MASSIFICADOS,
                CoverageType.REGULAR_COMUM, PremiumPeriodicity.ANUAL);
        InsuredObject object = new InsuredObject("IMOVEL-1", PropertyType.CASA, "Casa residencial", "01310100",
                null, List.of(coverage));
        Insured insured = new Insured("12345678901", IdentificationType.CPF, "Maria Ficticia",
                LocalDate.of(1990, 5, 17), null,
                new Address(StreetType.RUA, "Rua Exemplo", "100", null, null, "Sao Paulo", Uf.SP, "01310100"));
        return Policy.create(policyId, DocumentType.APOLICE_INDIVIDUAL, null, IssuanceType.EMISSAO_PROPRIA,
                LocalDate.of(2026, 10, 1), term, Money.brl(new BigDecimal("200000.00")), "PROP-001",
                "Residencial Essencial", List.of(insured), List.of(object),
                LocalDate.of(2026, 10, 22), NOW);
    }
}
