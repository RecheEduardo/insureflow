package br.com.eduardoreche.insureflow.adapter.out.persistence;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PolicyPersistenceAdapter.class, PolicyMapper.class})
@Testcontainers
class PolicyPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine");

    @Autowired
    PolicyPersistenceAdapter repository;

    @Autowired
    PolicyJpaRepository jpa;

    @Autowired
    PolicyMapper mapper;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void persistsPolicyAndRestoresItWithNestedData() {
        Policy issued = policy("AP-001");

        repository.save(issued);

        Policy restored = mapper.toDomain(jpa.findByPolicyId("AP-001").orElseThrow());
        assertThat(restored).isEqualTo(issued);
        assertThat(restored.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(restored.getVersion()).isZero();
        assertThat(restored.getTerm()).isEqualTo(issued.getTerm());
        assertThat(restored.getMaxLmg()).isEqualTo(issued.getMaxLmg());
        assertThat(restored.getInsureds()).isEqualTo(issued.getInsureds());
        assertThat(restored.getInsuredObjects()).isEqualTo(issued.getInsuredObjects());
    }

    @Test
    void findsPolicyByPolicyIdThroughThePort() {
        Policy issued = policy("AP-001");
        repository.save(issued);

        assertThat(repository.findByPolicyId("AP-001")).contains(issued);
        assertThat(repository.findByPolicyId("AP-999")).isEmpty();
    }

    @Test
    void rejectsSecondPolicyWithSamePolicyId() {
        repository.save(policy("AP-001"));

        assertThatThrownBy(() -> repository.save(policy("AP-001")))
                .isInstanceOf(DuplicatePolicyException.class);
    }

    @Test
    void databaseRejectsTermEndingBeforeItStarts() {
        repository.save(policy("AP-001"));

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE policy SET term_end_date = term_start_date - 1 WHERE policy_id = 'AP-001'"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_policy_term");
    }

    private static Policy policy(String policyId) {
        Term term = new Term(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30));
        Coverage coverage = new Coverage("0118", CoverageCode.INCENDIO, null, "15414.900000/2020-11",
                Money.brl(new BigDecimal("100000.00")), term, true, CoverageFeature.MASSIFICADOS,
                CoverageType.REGULAR_COMUM, PremiumPeriodicity.ANUAL);
        InsuredObject object = new InsuredObject("IMOVEL-1", PropertyType.CASA, "Casa residencial", "01310100",
                Money.brl(new BigDecimal("350000.50")), List.of(coverage));
        Insured insured = new Insured("12345678901", IdentificationType.CPF, "Maria Ficticia",
                LocalDate.of(1990, 5, 17), "maria@exemplo.com",
                new Address(StreetType.RUA, "Rua Exemplo", "100", "Apto 12", "Centro", "Sao Paulo", Uf.SP,
                        "01310100"));
        return Policy.create(policyId, DocumentType.APOLICE_INDIVIDUAL, null, IssuanceType.EMISSAO_PROPRIA,
                LocalDate.of(2026, 10, 1), term, Money.brl(new BigDecimal("200000.00")), "PROP-001",
                "Residencial Essencial", List.of(insured), List.of(object),
                LocalDate.of(2026, 10, 22), Instant.parse("2026-10-22T12:00:00Z"));
    }
}
