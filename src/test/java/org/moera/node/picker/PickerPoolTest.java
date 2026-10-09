package org.moera.node.picker;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.DomainInfo;
import org.moera.node.data.Pick;
import org.moera.node.data.PickRepository;
import org.moera.node.domain.Domains;
import org.moera.node.global.UniversalContext;
import org.moera.node.task.TaskAutowire;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.moera.node.util.MediaTransactionTestSupport.transaction;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PickerPoolTest {

    private final UUID nodeId = UUID.randomUUID();
    private final Map<UUID, Pick> stored = new HashMap<>();
    private final List<Runnable> tasks = new ArrayList<>();

    @Mock
    private Domains domains;

    @InjectMocks
    private final PickerPool pool = new PickerPool();

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        PickRepository pickRepository = mock(PickRepository.class);
        when(pickRepository.findAll()).thenAnswer(invocation -> new ArrayList<>(stored.values()));
        when(pickRepository.findById(any(UUID.class))).thenAnswer(
            invocation -> Optional.ofNullable(stored.get(invocation.getArgument(0)))
        );
        when(pickRepository.saveAndFlush(any(Pick.class))).thenAnswer(invocation -> {
            Pick pick = invocation.getArgument(0);
            stored.put(pick.getId(), pick);
            return pick;
        });
        doAnswer(invocation -> {
            Pick pick = invocation.getArgument(0);
            stored.remove(pick.getId());
            return null;
        }).when(pickRepository).delete(any(Pick.class));
        when(domains.getDomain(nodeId)).thenReturn(new DomainInfo());
        UniversalContext universalContext = mock(UniversalContext.class);
        when(universalContext.nodeId()).thenReturn(nodeId);
        ReflectionTestUtils.setField(pool, "pickRepository", pickRepository);
        ReflectionTestUtils.setField(pool, "universalContext", universalContext);
        ReflectionTestUtils.setField(pool, "tx", transaction());
        ReflectionTestUtils.setField(pool, "taskExecutor", (TaskExecutor) tasks::add);
        ReflectionTestUtils.setField(pool, "taskAutowire", mock(TaskAutowire.class));
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void startupDeletesExpiredPickEvenWhenRetryIsInTheFuture() {
        Pick pick = storedPick();
        pick.setCreatedAt(Timestamp.from(Instant.now().minus(13, ChronoUnit.HOURS)));
        pick.setRetryAt(Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)));

        pool.init();

        assertFalse(stored.containsKey(pick.getId()));
        pool.retry();
        assertTrue(tasks.isEmpty());
    }

    @Test
    void startupDeletesPickForUnknownLocalNode() {
        Pick pick = storedPick();
        pick.setNodeId(UUID.randomUUID());

        pool.init();

        assertFalse(stored.containsKey(pick.getId()));
        pool.retry();
        assertTrue(tasks.isEmpty());
    }

    @Test
    void retryDeletesExpiredRunningPick() {
        Pick pick = storedPick();
        pool.init();
        pick.setRunning(true);
        pick.setCreatedAt(Timestamp.from(Instant.now().minus(13, ChronoUnit.HOURS)));

        pool.retry();

        assertFalse(stored.containsKey(pick.getId()));
        assertTrue(tasks.isEmpty());
    }

    @Test
    void retryDeletesRunningPickAfterLocalNodeDisappears() {
        Pick pick = storedPick();
        pool.init();
        pick.setRunning(true);
        when(domains.getDomain(nodeId)).thenReturn(null);

        pool.retry();

        assertFalse(stored.containsKey(pick.getId()));
        assertTrue(tasks.isEmpty());
    }

    @Test
    void pickForUnknownLocalNodeIsNotScheduled() {
        Pick pick = pick();
        pick.setNodeId(UUID.randomUUID());

        pool.pick(pick);
        pool.retry();

        assertTrue(stored.isEmpty());
        assertTrue(tasks.isEmpty());
    }

    @Test
    void recentPickIsRetriedWhenDue() {
        Pick pick = storedPick();
        pick.setCreatedAt(Timestamp.from(Instant.now().minus(5, ChronoUnit.HOURS)));
        pick.setRetryAt(Timestamp.from(Instant.now().minus(1, ChronoUnit.SECONDS)));

        pool.init();
        pool.retry();

        assertTrue(stored.containsKey(pick.getId()));
        assertTrue(pick.isRunning());
        assertEquals(1, tasks.size());
    }

    @Test
    void recentPickIsRetainedUntilRetryIsDue() {
        Pick pick = storedPick();
        pick.setRetryAt(Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)));

        pool.init();
        pool.retry();

        assertTrue(stored.containsKey(pick.getId()));
        assertFalse(pick.isRunning());
        assertTrue(tasks.isEmpty());
    }

    @Test
    void newPickUsesCurrentLocalNode() {
        Pick pick = pick();
        pick.setNodeId(null);

        pool.pick(pick);

        assertEquals(nodeId, pick.getNodeId());
        assertTrue(stored.containsKey(pick.getId()));
        assertTrue(pick.isRunning());
        assertEquals(1, tasks.size());
    }

    @Test
    void lateFailureDoesNotRestoreExpiredPick() {
        Pick pick = pick();
        pool.pick(pick);
        pick.setCreatedAt(Timestamp.from(Instant.now().minus(13, ChronoUnit.HOURS)));
        pool.retry();
        assertFalse(stored.containsKey(pick.getId()));

        pool.pickFailed(pick, false);

        assertFalse(stored.containsKey(pick.getId()));
    }

    @Test
    void recoverableFailureIsPersistedForRetry() {
        Pick pick = pick();
        pool.pick(pick);
        Pick persistedPick = pick();
        persistedPick.setId(pick.getId());
        persistedPick.setCreatedAt(pick.getCreatedAt());
        stored.put(persistedPick.getId(), persistedPick);
        Instant failedAt = Instant.now();

        pool.pickFailed(pick, false);

        assertFalse(pick.isRunning());
        assertNotNull(stored.get(pick.getId()).getRetryAt());
        assertTrue(stored.get(pick.getId()).getRetryAt().toInstant().isAfter(failedAt));
    }

    private Pick storedPick() {
        Pick pick = pick();
        pick.setId(UUID.randomUUID());
        stored.put(pick.getId(), pick);
        return pick;
    }

    private Pick pick() {
        Pick pick = new Pick();
        pick.setNodeId(nodeId);
        pick.setRemoteNodeName("remote");
        pick.setRemotePostingId("posting");
        return pick;
    }

}
