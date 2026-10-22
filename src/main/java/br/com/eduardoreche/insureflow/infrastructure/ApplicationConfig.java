package br.com.eduardoreche.insureflow.infrastructure;

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
    IssuePolicy issuePolicy(PolicyRepository policies, Clock clock) {
        return new IssuePolicy(policies, clock);
    }
}
