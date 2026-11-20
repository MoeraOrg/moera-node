package org.moera.node.rest;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.persistence.EntityManager;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SubscriptionDescription;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.ContactRepository;
import org.moera.node.data.Feed;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.SubscriptionAddedLiberin;
import org.moera.node.model.OperationFailure;
import org.moera.node.operations.ContactOperations;
import org.moera.node.operations.PostingOperations;
import org.moera.node.operations.SubscriptionOperations;
import org.moera.node.option.Options;
import org.moera.node.rest.task.RemoteFeedFetchJob;
import org.moera.node.task.Jobs;
import org.moera.node.util.Transaction;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.support.PersistenceExceptionTranslationInterceptor;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Runs the controller and its write transactions against a migrated PostgreSQL database. */
@EnabledIfSystemProperty(named = "moera.test.database", matches = ".+")
class SubscriptionControllerIT {

    private static LocalContainerEntityManagerFactoryBean factoryBean;
    private static JdbcTemplate jdbc;

    private final UUID nodeId = UUID.randomUUID();
    private final Options options = mock(Options.class);
    private final UniversalContext context = mock(UniversalContext.class);
    private final RequestContext request = mock(RequestContext.class);
    private final Jobs jobs = mock(Jobs.class);
    private final SubscriptionOperations operations = new SubscriptionOperations();
    private final SubscriptionController controller = new SubscriptionController();
    private final SubscriptionDescription description = new SubscriptionDescription();
    private TransactionTemplate controllerTransaction;

    @BeforeAll
    static void openDatabase() {
        var dataSource = new DriverManagerDataSource(
            System.getProperty("moera.test.database"), System.getProperty("user.name"), ""
        );
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");
        Flyway.configure().dataSource(dataSource).load().migrate();
        factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPackagesToScan("org.moera.node.data", "org.moera.node.model.principal");
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factoryBean.setJpaPropertyMap(Map.of(
            "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl"
        ));
        factoryBean.afterPropertiesSet();
    }

    @AfterAll
    static void closeDatabase() {
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        var factory = factoryBean.getObject();
        EntityManager entityManager = SharedEntityManagerCreator.createSharedEntityManager(factory);
        var repositories = new JpaRepositoryFactory(entityManager);
        repositories.addRepositoryProxyPostProcessor((proxy, info) ->
            proxy.addAdvice(new PersistenceExceptionTranslationInterceptor(factoryBean))
        );
        var manager = new JpaTransactionManager(factory);
        controllerTransaction = new TransactionTemplate(manager);
        var tx = new Transaction();
        when(options.getPrincipal(anyString())).thenReturn(Principal.PUBLIC);
        when(context.nodeId()).thenReturn(nodeId);
        when(context.getOptions()).thenReturn(options);
        ReflectionTestUtils.setField(tx, "txManager", manager);
        ReflectionTestUtils.setField(tx, "universalContext", context);
        var contacts = new ContactOperations();
        ReflectionTestUtils.setField(contacts, "tx", tx);
        ReflectionTestUtils.setField(contacts, "universalContext", context);
        ReflectionTestUtils.setField(contacts, "contactRepository",
            repositories.getRepository(ContactRepository.class));
        ReflectionTestUtils.setField(operations, "tx", tx);
        ReflectionTestUtils.setField(operations, "universalContext", context);
        ReflectionTestUtils.setField(operations, "subscriptionRepository",
            repositories.getRepository(SubscriptionRepository.class));
        var subscriptions = repositories.getRepository(UserSubscriptionRepository.class);
        ReflectionTestUtils.setField(operations, "userSubscriptionRepository", subscriptions);
        ReflectionTestUtils.setField(operations, "contactOperations", contacts);
        ReflectionTestUtils.setField(operations, "postingOperations", new PostingOperations());
        ReflectionTestUtils.setField(operations, "postingRepository",
            repositories.getRepository(PostingRepository.class));
        ReflectionTestUtils.setField(operations, "storyRepository", repositories.getRepository(StoryRepository.class));
        ReflectionTestUtils.setField(operations, "jobs", jobs);
        when(request.getOptions()).thenReturn(options);
        when(request.nodeId()).thenReturn(nodeId);
        when(request.isPrincipal(Principal.ADMIN, Scope.SUBSCRIBE)).thenReturn(true);
        ReflectionTestUtils.setField(controller, "userSubscriptionRepository", subscriptions);
        ReflectionTestUtils.setField(controller, "contactOperations", contacts);
        ReflectionTestUtils.setField(controller, "subscriptionOperations", operations);
        ReflectionTestUtils.setField(controller, "requestContext", request);

        description.setType(SubscriptionType.FEED);
        description.setFeedName(Feed.NEWS);
        description.setRemoteNodeName("remote");
        description.setRemoteFeedName(Feed.TIMELINE);
    }

    @Test
    void racingPostsAndRetryCreateOneFeedSubscription() throws Exception {
        var barrier = new CyclicBarrier(2);
        var requests = new AtomicInteger();
        when(context.nodeId()).thenAnswer(invocation -> {
            if (requests.incrementAndGet() <= 2) {
                barrier.await(10, TimeUnit.SECONDS);
            }
            return nodeId;
        });
        Callable<String> post = () -> {
            try {
                return controllerTransaction.execute(status -> controller.post(description).getId());
            } catch (OperationFailure e) {
                return e.getErrorCode();
            }
        };
        List<String> outcomes;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(post);
            var second = executor.submit(post);
            outcomes = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        }
        var failure = assertThrows(OperationFailure.class, () ->
            controllerTransaction.execute(status -> controller.post(description))
        );
        assertEquals("subscription.already-exists", failure.getErrorCode());
        var id = jdbc.queryForObject("SELECT id FROM user_subscriptions WHERE node_id = ?", UUID.class, nodeId);
        assertEquals(Set.of(id.toString(), "subscription.already-exists"), Set.copyOf(outcomes));
        assertEquals(1, jdbc.queryForObject("SELECT feed_subscription_count FROM contacts WHERE node_id = ?",
            Integer.class, nodeId));
        verify(context).subscriptionsUpdated();
        verify(context).invalidateSubscribedCache("remote_0");
        verify(context).send(any(SubscriptionAddedLiberin.class));
        verify(jobs).run(
            RemoteFeedFetchJob.class, new RemoteFeedFetchJob.Parameters(Feed.NEWS, "remote_0", Feed.TIMELINE), nodeId
        );
    }

    @Test
    void automaticSubscriptionHandlesAConcurrentWinnerAfterItsExistenceCheck() {
        when(options.getString("subscription.auto.node")).thenReturn("remote_0");
        controllerTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        controllerTransaction.executeWithoutResult(status -> {
            // Pin the snapshot used by autoSubscribe's initial count before the winner commits.
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM user_subscriptions WHERE node_id = ?",
                Integer.class, nodeId));
            controller.post(description);
            operations.autoSubscribe();
        });

        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM user_subscriptions WHERE node_id = ?",
            Integer.class, nodeId));
        verify(context).send(any(SubscriptionAddedLiberin.class));
    }

    @Test
    void distinctDestinationsShareOneFeedRegistrationDespiteAnExtraneousPostingId() {
        controllerTransaction.execute(status -> controller.post(description));
        description.setFeedName(Feed.EXPLORE);
        description.setRemotePostingId("unused");
        var other = controllerTransaction.execute(status -> controller.post(description));

        assertNull(other.getRemotePostingId());
        assertEquals(List.of(2), jdbc.queryForList(
            "SELECT usage_count FROM subscriptions WHERE node_id = ? AND subscription_type = 0", Integer.class, nodeId
        ));
    }

    @Test
    void abandoningAFeedRemovesExactlyItsDestinationsOnce() {
        controllerTransaction.execute(status -> controller.post(description));
        description.setFeedName(Feed.EXPLORE);
        controllerTransaction.execute(status -> controller.post(description));
        description.setRemoteFeedName("another-feed");
        var remaining = controllerTransaction.execute(status -> controller.post(description));
        var registration = new Subscription();
        registration.setId(jdbc.queryForObject(
            "SELECT id FROM subscriptions WHERE node_id = ? AND subscription_type = 0 AND remote_feed_name = ?",
            UUID.class, nodeId, Feed.TIMELINE
        ));
        registration.setNodeId(nodeId);
        registration.setRemoteNodeName("remote_0");
        registration.setSubscriptionType(SubscriptionType.FEED);
        registration.setRemoteFeedName(Feed.TIMELINE);

        controllerTransaction.executeWithoutResult(status -> operations.abandon(registration));
        assertEquals(1, jdbc.queryForObject("SELECT feed_subscription_count FROM contacts WHERE node_id = ?",
            Integer.class, nodeId));
        controllerTransaction.executeWithoutResult(status -> operations.abandon(registration));
        assertEquals(1, jdbc.queryForObject("SELECT feed_subscription_count FROM contacts WHERE node_id = ?",
            Integer.class, nodeId));
        controllerTransaction.execute(status -> controller.delete(remaining.getId()));
        assertEquals(0, jdbc.queryForObject("SELECT feed_subscription_count FROM contacts WHERE node_id = ?",
            Integer.class, nodeId));
    }

    @Test
    void concurrentDeletesOfDifferentDestinationsEachDecrementTheCurrentCount() throws Exception {
        String news = controllerTransaction.execute(status -> controller.post(description).getId());
        description.setFeedName(Feed.EXPLORE);
        String explore = controllerTransaction.execute(status -> controller.post(description).getId());
        var loaded = new CyclicBarrier(2);
        when(request.isPrincipal(Principal.ADMIN, Scope.SUBSCRIBE)).thenAnswer(invocation -> {
            loaded.await(10, TimeUnit.SECONDS);
            return true;
        });

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> controllerTransaction.execute(status -> controller.delete(news)));
            var second = executor.submit(() -> controllerTransaction.execute(status -> controller.delete(explore)));
            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);
        }
        assertEquals(0, jdbc.queryForObject("SELECT feed_subscription_count FROM contacts WHERE node_id = ?",
            Integer.class, nodeId));
    }

    @Test
    void invalidPostgresTextIsPropagatedAsAnIntegrityFailure() {
        description.setRemoteFeedName("invalid\0feed");
        assertThrows(DataIntegrityViolationException.class, () ->
            controllerTransaction.execute(status -> controller.post(description))
        );
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM contacts WHERE node_id = ?", Integer.class, nodeId));
    }

}
