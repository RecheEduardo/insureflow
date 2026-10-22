package br.com.eduardoreche.insureflow.infrastructure;

import br.com.eduardoreche.insureflow.application.policy.CancelPolicy;
import br.com.eduardoreche.insureflow.application.policy.EndorsePolicy;
import br.com.eduardoreche.insureflow.application.policy.GetPolicy;
import br.com.eduardoreche.insureflow.application.policy.IssuePolicy;
import br.com.eduardoreche.insureflow.application.policy.PolicyRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
class ApplicationConfig {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }

    @Bean
    CancelPolicy cancelPolicy(PolicyRepository policies, Clock clock) {
        return new CancelPolicy(policies, clock);
    }

    @Bean
    EndorsePolicy endorsePolicy(PolicyRepository policies, Clock clock) {
        return new EndorsePolicy(policies, clock);
    }

    @Bean
    GetPolicy getPolicy(PolicyRepository policies) {
        return new GetPolicy(policies);
    }

    @Bean
    IssuePolicy issuePolicy(PolicyRepository policies, Clock clock) {
        return new IssuePolicy(policies, clock);
    }
}
