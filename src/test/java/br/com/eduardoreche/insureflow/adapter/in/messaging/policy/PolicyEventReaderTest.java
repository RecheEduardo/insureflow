package br.com.eduardoreche.insureflow.adapter.in.messaging.policy;

import br.com.eduardoreche.insureflow.application.policy.event.PolicyEventType;
import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static br.com.eduardoreche.insureflow.application.policy.PolicyFixtures.activePolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyEventReaderTest {

    private final PolicyEventReader reader = new PolicyEventReader();
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void readsTheEventWrittenByThePublisher() {
        PolicyEventDTO event = PolicyEventDTO.of(PolicyEventType.POLICY_ISSUED, activePolicy("AP-001"));

        assertThat(reader.read(json.writeValueAsString(event))).isEqualTo(event);
    }

    @Test
    void ignoresFieldsAddedByANewerProducer() {
        PolicyEventDTO event = PolicyEventDTO.of(PolicyEventType.POLICY_ISSUED, activePolicy("AP-001"));
        String withNewField = json.writeValueAsString(event).replaceFirst("\\{", "{\"schemaVersion\":2,");

        assertThat(reader.read(withNewField)).isEqualTo(event);
    }

    @Test
    void rejectsAnUnreadablePayloadWithoutRepeatingItsContent() {
        assertThatThrownBy(() -> reader.read("{\"policyId\":\"AP-001\",\"identification\":98765432100x"))
                .isInstanceOf(PolicyEventReader.UnreadablePolicyEventException.class)
                .hasNoCause()
                .message().doesNotContain("98765432100");
    }

    @Test
    void rejectsAnEventThatBreaksADomainRule() {
        PolicyEventDTO event = PolicyEventDTO.of(PolicyEventType.POLICY_ISSUED, activePolicy("AP-001"));
        String withoutBirthDate = json.writeValueAsString(event).replace("\"birthDate\":\"1990-05-17\",", "");

        assertThatThrownBy(() -> reader.read(withoutBirthDate))
                .isInstanceOf(PolicyEventReader.UnreadablePolicyEventException.class);
    }
}
