package br.com.eduardoreche.insureflow.adapter.out.persistence.policy.event;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
class PolicyOutboxMetrics implements MeterBinder {

    static final String PENDING = "insureflow.outbox.pending";

    private final JdbcClient jdbc;

    PolicyOutboxMetrics(JdbcTemplate jdbcTemplate) {
        this.jdbc = JdbcClient.create(jdbcTemplate);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder(PENDING, this, PolicyOutboxMetrics::pending)
                .description("Policy events waiting in the outbox to be published")
                .register(registry);
    }

    private double pending() {
        return jdbc.sql("SELECT count(*) FROM policy_outbox").query(Long.class).single();
    }
}
