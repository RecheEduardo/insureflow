package br.com.eduardoreche.insureflow.adapter.in.messaging.policy;

import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PolicyEventReader {

    // Ao contrário da API web, aceita campo desconhecido: o produtor pode acrescentar campos sem quebrar consumidores.
    private final JsonMapper json = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public PolicyEventDTO read(String payload) {
        try {
            return json.readValue(payload, PolicyEventDTO.class);
        } catch (JacksonException e) {
            throw new UnreadablePolicyEventException(e);
        }
    }

    public static class UnreadablePolicyEventException extends RuntimeException {

        // Sem a mensagem nem a causa do Jackson, que podem citar trechos do payload com dados do segurado.
        UnreadablePolicyEventException(JacksonException cause) {
            super("Policy event payload could not be read: " + cause.getClass().getSimpleName());
        }
    }
}
