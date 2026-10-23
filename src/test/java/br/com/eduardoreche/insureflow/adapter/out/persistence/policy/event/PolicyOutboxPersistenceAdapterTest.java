package br.com.eduardoreche.insureflow.adapter.out.persistence.policy.event;

import br.com.eduardoreche.insureflow.application.policy.PolicyFixtures;
import br.com.eduardoreche.insureflow.application.policy.event.PolicyEventType;
import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PolicyOutboxPersistenceAdapter.class)
@Testcontainers
class PolicyOutboxPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine");

    @Autowired
    PolicyOutboxPersistenceAdapter outbox;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void returnsPendingEventsInTheOrderTheyWereAppended() {
        PolicyEventDTO first = issued("AP-002");
        PolicyEventDTO second = issued("AP-001");
        outbox.append(first);
        outbox.append(second);

        assertThat(outbox.lockPending(10)).containsExactly(first, second);
        assertThat(outbox.lockPending(1)).containsExactly(first);
    }

    @Test
    void deletesOnlyThePublishedEvents() {
        PolicyEventDTO published = issued("AP-001");
        PolicyEventDTO pending = issued("AP-002");
        outbox.append(published);
        outbox.append(pending);

        outbox.delete(List.of(published.eventId()));

        assertThat(outbox.lockPending(10)).containsExactly(pending);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Timeout(30)
    void concurrentRelaysTakeDifferentEventsWithoutWaitingForEachOther() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            outbox.append(issued("AP-001"));
            outbox.append(issued("AP-002"));
        });
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<List<PolicyEventDTO>> first = executor.submit(() -> tx.execute(status -> {
                List<PolicyEventDTO> locked = outbox.lockPending(1);
                firstLocked.countDown();
                awaitQuietly(releaseFirst);
                return locked;
            }));
            firstLocked.await();

            List<PolicyEventDTO> second = tx.execute(status -> outbox.lockPending(10));
            releaseFirst.countDown();

            assertThat(first.get()).extracting(PolicyEventDTO::policyId).containsExactly("AP-001");
            assertThat(second).extracting(PolicyEventDTO::policyId).containsExactly("AP-002");
        } finally {
            jdbc.update("DELETE FROM policy_outbox");
        }
    }

    private static PolicyEventDTO issued(String policyId) {
        return PolicyEventDTO.of(PolicyEventType.POLICY_ISSUED, PolicyFixtures.activePolicy(policyId));
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
