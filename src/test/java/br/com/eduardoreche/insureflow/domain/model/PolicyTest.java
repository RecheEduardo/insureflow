package br.com.eduardoreche.insureflow.domain.model;

import br.com.eduardoreche.insureflow.domain.exception.BusinessRuleViolationException;
import br.com.eduardoreche.insureflow.domain.exception.PolicyCancelledException;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);
    private static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");
    private static final Instant LATER = Instant.parse("2026-12-01T10:00:00Z");
    private static final Term TERM = new Term(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30));
    private static final Money LMG = brl("200000.00");

    @Test
    void issuesAnActivePolicyAtVersionZero() {
        Policy policy = issue(objectWith(coverage(CoverageCode.INCENDIO, brl("100000.00"), TERM)));

        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(policy.getVersion()).isZero();
        assertThat(policy.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void refusesIssuanceDateInTheFuture() {
        assertThatThrownBy(() -> Policy.create("AP-001", DocumentType.APOLICE_INDIVIDUAL, null,
                IssuanceType.EMISSAO_PROPRIA, TODAY.plusDays(1), TERM, LMG, "PROP-001", "Residencial",
                List.of(insured()), List.of(defaultObject()), TODAY, NOW))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("ISSUANCE_IN_FUTURE");
    }

    @Test
    void acceptsIssuanceDateToday() {
        Policy policy = Policy.create("AP-001", DocumentType.APOLICE_INDIVIDUAL, null,
                IssuanceType.EMISSAO_PROPRIA, TODAY, TERM, LMG, "PROP-001", "Residencial",
                List.of(insured()), List.of(defaultObject()), TODAY, NOW);

        assertThat(policy.getIssuanceDate()).isEqualTo(TODAY);
    }

    @Test
    void requiresAtLeastOneInsured() {
        assertThatThrownBy(() -> issue(DocumentType.APOLICE_INDIVIDUAL, null, List.of(), List.of(defaultObject())))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("POLICY_WITHOUT_INSURED");
    }

    @Test
    void requiresAtLeastOneInsuredObject() {
        assertThatThrownBy(() -> issue(DocumentType.APOLICE_INDIVIDUAL, null, List.of(insured()), List.of()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("POLICY_WITHOUT_OBJECT");
    }

    @Test
    void requiresGroupCertificateIdForACertificate() {
        assertThatThrownBy(() -> issue(DocumentType.CERTIFICADO, null, List.of(insured()), List.of(defaultObject())))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("GROUP_CERTIFICATE_ID_MISMATCH");
    }

    @Test
    void refusesGroupCertificateIdOutsideACertificate() {
        assertThatThrownBy(() -> issue(DocumentType.APOLICE_INDIVIDUAL, "GRP-1", List.of(insured()),
                List.of(defaultObject())))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("GROUP_CERTIFICATE_ID_MISMATCH");
    }

    @Test
    void acceptsACertificateWithItsGroupId() {
        Policy policy = issue(DocumentType.CERTIFICADO, "GRP-1", List.of(insured()), List.of(defaultObject()));

        assertThat(policy.getGroupCertificateId()).isEqualTo("GRP-1");
    }

    @Test
    void refusesTheSameInsuredObjectTwice() {
        assertThatThrownBy(() -> issue(DocumentType.APOLICE_INDIVIDUAL, null, List.of(insured()),
                List.of(defaultObject(), defaultObject())))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("OBJECT_DUPLICATED");
    }

    @Test
    void refusesCoverageLmiAboveThePolicyLmg() {
        assertThatThrownBy(() -> issue(objectWith(coverage(CoverageCode.INCENDIO, brl("200000.01"), TERM))))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("LMI_ABOVE_LMG");
    }

    @Test
    void acceptsCoverageLmiEqualToThePolicyLmg() {
        Policy policy = issue(objectWith(coverage(CoverageCode.INCENDIO, LMG, TERM)));

        assertThat(policy.getInsuredObjects().getFirst().coverages().getFirst().lmi()).isEqualTo(LMG);
    }

    @Test
    void refusesCoverageTermOutsideThePolicyTerm() {
        Term longer = new Term(TERM.startDate(), TERM.endDate().plusDays(1));

        assertThatThrownBy(() -> issue(objectWith(coverage(CoverageCode.INCENDIO, brl("1000.00"), longer))))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("COVERAGE_OUTSIDE_TERM");
    }

    @Test
    void acceptsCoverageTermInsideThePolicyTerm() {
        Term shorter = new Term(TERM.startDate().plusMonths(1), TERM.endDate().minusMonths(1));

        Policy policy = issue(objectWith(coverage(CoverageCode.INCENDIO, brl("1000.00"), shorter)));

        assertThat(policy.getInsuredObjects().getFirst().coverages().getFirst().term()).isEqualTo(shorter);
    }

    @Test
    void appliesTheSameInvariantsWhenRestoringFromStorage() {
        assertThatThrownBy(() -> Policy.restore(UUID.randomUUID(), "AP-001", DocumentType.APOLICE_INDIVIDUAL, null,
                IssuanceType.EMISSAO_PROPRIA, TERM.startDate(), TERM, brl("1000.00"), "PROP-001", "Residencial",
                List.of(insured()), List.of(objectWith(coverage(CoverageCode.INCENDIO, brl("1000.01"), TERM))),
                PolicyStatus.ACTIVE, null, null, 3, NOW, NOW))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("LMI_ABOVE_LMG");
    }

    @Test
    void endorsementReplacesTheMutableFieldsAndKeepsTheIdentifiers() {
        Policy policy = issue(defaultObject());
        Term extended = new Term(TERM.startDate(), TERM.endDate().plusYears(1));

        policy.endorse(extended, brl("300000.00"), "Residencial Plus", policy.getInsureds(),
                policy.getInsuredObjects(), LATER);

        assertThat(policy.getTerm()).isEqualTo(extended);
        assertThat(policy.getMaxLmg()).isEqualTo(brl("300000.00"));
        assertThat(policy.getProductName()).isEqualTo("Residencial Plus");
        assertThat(policy.getUpdatedAt()).isEqualTo(LATER);
        assertThat(policy.getPolicyId()).isEqualTo("AP-001");
        assertThat(policy.getProposalId()).isEqualTo("PROP-001");
        assertThat(policy.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void endorsementThatBreaksAnInvariantLeavesThePolicyUntouched() {
        Policy policy = issue(objectWith(coverage(CoverageCode.INCENDIO, brl("100000.00"), TERM)));

        assertThatThrownBy(() -> policy.endorse(TERM, brl("50000.00"), "Residencial Plus", policy.getInsureds(),
                policy.getInsuredObjects(), LATER))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("LMI_ABOVE_LMG");

        assertThat(policy.getMaxLmg()).isEqualTo(LMG);
        assertThat(policy.getProductName()).isEqualTo("Residencial");
        assertThat(policy.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void cancelledPolicyRefusesEndorsement() {
        Policy policy = issue(defaultObject());
        policy.cancel(CancellationReason.FALTA_DE_PAGAMENTO, NOW);

        assertThatThrownBy(() -> policy.endorse(TERM, LMG, "Residencial Plus", policy.getInsureds(),
                policy.getInsuredObjects(), LATER))
                .isInstanceOf(PolicyCancelledException.class)
                .extracting("code").isEqualTo("POLICY_CANCELLED");
    }

    @Test
    void cancellationKeepsThePolicyAndRecordsReasonAndInstant() {
        Policy policy = issue(defaultObject());

        policy.cancel(CancellationReason.PEDIDO_DO_SEGURADO, LATER);

        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.CANCELLED);
        assertThat(policy.getCancellationReason()).isEqualTo(CancellationReason.PEDIDO_DO_SEGURADO);
        assertThat(policy.getCancelledAt()).isEqualTo(LATER);
        assertThat(policy.getPolicyId()).isEqualTo("AP-001");
    }

    @Test
    void cancellingAgainKeepsTheOriginalReasonAndInstant() {
        Policy policy = issue(defaultObject());
        policy.cancel(CancellationReason.PEDIDO_DO_SEGURADO, NOW);

        policy.cancel(CancellationReason.OUTROS, LATER);

        assertThat(policy.getCancellationReason()).isEqualTo(CancellationReason.PEDIDO_DO_SEGURADO);
        assertThat(policy.getCancelledAt()).isEqualTo(NOW);
    }

    @Test
    void isInForceOnlyInsideTheTermAndWhileNotCancelled() {
        Policy policy = issue(defaultObject());

        assertThat(policy.isInForceOn(TERM.startDate())).isTrue();
        assertThat(policy.isInForceOn(TERM.endDate())).isTrue();
        assertThat(policy.isInForceOn(TERM.endDate().plusDays(1))).isFalse();

        policy.cancel(CancellationReason.OUTROS, NOW);

        assertThat(policy.isInForceOn(TERM.startDate())).isFalse();
    }

    @Test
    void missingRequiredFieldIsABug() {
        assertThatThrownBy(() -> Policy.create("AP-001", DocumentType.APOLICE_INDIVIDUAL, null,
                IssuanceType.EMISSAO_PROPRIA, TERM.startDate(), null, LMG, "PROP-001", "Residencial",
                List.of(insured()), List.of(defaultObject()), TODAY, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    private static Policy issue(InsuredObject object) {
        return issue(DocumentType.APOLICE_INDIVIDUAL, null, List.of(insured()), List.of(object));
    }

    private static Policy issue(DocumentType documentType, String groupCertificateId, List<Insured> insureds,
                                List<InsuredObject> objects) {
        return Policy.create("AP-001", documentType, groupCertificateId, IssuanceType.EMISSAO_PROPRIA,
                TERM.startDate(), TERM, LMG, "PROP-001", "Residencial", insureds, objects, TODAY, NOW);
    }

    private static InsuredObject defaultObject() {
        return objectWith(coverage(CoverageCode.INCENDIO, brl("100000.00"), TERM));
    }

    private static InsuredObject objectWith(Coverage coverage) {
        return new InsuredObject("IMOVEL-1", PropertyType.CASA, "Casa residencial", "01310100", null,
                List.of(coverage));
    }

    private static Coverage coverage(CoverageCode code, Money lmi, Term term) {
        return new Coverage("0118", code, null, "15414.900000/2020-11", lmi, term, true,
                CoverageFeature.MASSIFICADOS, CoverageType.REGULAR_COMUM, PremiumPeriodicity.ANUAL);
    }

    private static Insured insured() {
        return new Insured("12345678901", IdentificationType.CPF, "Maria Ficticia", LocalDate.of(1990, 5, 17),
                null, new Address(StreetType.RUA, "Rua Exemplo", "100", null, null, "Sao Paulo", Uf.SP, "01310100"));
    }

    private static Money brl(String amount) {
        return Money.brl(new BigDecimal(amount));
    }
}
