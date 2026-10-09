package org.moera.node.auth;

import java.security.Security;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.persistence.EntityManagerFactory;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.TokenInfo;
import org.moera.lib.node.types.TokenUpdate;
import org.moera.node.data.Token;
import org.moera.node.data.TokenRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.model.TokenAddedLiberin;
import org.moera.node.liberin.model.TokenUpdatedLiberin;
import org.moera.node.liberin.receptor.TokenReceptor;
import org.moera.node.model.event.Event;
import org.moera.node.model.event.TokenAddedEvent;
import org.moera.node.model.event.TokenUpdatedEvent;
import org.moera.node.option.Options;
import org.moera.node.rest.TokenController;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.Util;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Run against an isolated, empty PostgreSQL database; upgrades an existing token through the migrations. */
@EnabledIfSystemProperty(named = "moera.test.database", matches = ".+")
class TokenHashingIT {

    private static final String SECRET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopq";
    private static final String HASH = "769e8d95aa246a02d94d48c42fb7183e531c85c41d5b3456ddb90011712d8bd7";
    private static final UUID NODE_ID = UUID.randomUUID();
    private static final UUID TOKEN_ID = UUID.randomUUID();

    private static JdbcTemplate jdbc;
    private static LocalContainerEntityManagerFactoryBean factoryBean;
    private static EntityManagerFactory factory;

    private AuthenticationManager authenticationManager;
    private TokenController tokenController;
    private TransactionTemplate transaction;

    @BeforeAll
    static void openDatabase() {
        Security.addProvider(new BouncyCastleProvider());
        var source = new DriverManagerDataSource(
            System.getProperty("moera.test.database"),
            System.getProperty("user.name"),
            ""
        );
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");
        Flyway.configure().dataSource(source).target("319").load().migrate();
        jdbc.update(
            "INSERT INTO tokens (id, token, node_id, name, auth_scope, created_at, deadline)"
                + " VALUES (?, ?, ?, 'existing', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 day')",
            TOKEN_ID,
            SECRET,
            NODE_ID,
            Scope.TOKENS.getMask()
        );
        Flyway.configure().dataSource(source).load().migrate();
        factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(source);
        factoryBean.setPackagesToScan("org.moera.node.data", "org.moera.node.model.principal");
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factoryBean.setJpaPropertyMap(Map.of(
            "hibernate.physical_naming_strategy",
            "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl"
        ));
        factoryBean.afterPropertiesSet();
        factory = factoryBean.getObject();
    }

    @AfterAll
    static void closeDatabase() {
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        var entityManager = SharedEntityManagerCreator.createSharedEntityManager(factory);
        var repositories = new JpaRepositoryFactory(entityManager);
        var tokenRepository = repositories.getRepository(TokenRepository.class);
        transaction = new TransactionTemplate(new JpaTransactionManager(factory));
        Options options = mock(Options.class);
        when(options.getDuration("token.lifetime")).thenReturn(ExtendedDuration.parse("1d"));
        UniversalContext universalContext = mock(UniversalContext.class);
        when(universalContext.getOptions()).thenReturn(options);
        when(universalContext.isBackground()).thenReturn(true);
        authenticationManager = new AuthenticationManager();
        ReflectionTestUtils.setField(authenticationManager, "requestContext", mock(RequestContext.class));
        ReflectionTestUtils.setField(authenticationManager, "universalContext", universalContext);
        ReflectionTestUtils.setField(authenticationManager, "tokenRepository", tokenRepository);
        RequestContext requestContext = mock(RequestContext.class);
        when(requestContext.nodeId()).thenReturn(NODE_ID);
        tokenController = new TokenController();
        ReflectionTestUtils.setField(tokenController, "requestContext", requestContext);
        ReflectionTestUtils.setField(tokenController, "tokenRepository", tokenRepository);
    }

    @Test
    void migrationReplacesTheSecretWithItsHashAndPreservesMetadata() {
        List<String> columns = jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'tokens'",
            String.class
        );
        assertFalse(columns.contains("token"));
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM tokens WHERE id = ?", TOKEN_ID);
        assertArrayEquals(HexFormat.of().parseHex(HASH), (byte[]) row.get("token_hash"));
        assertEquals("ABCD", row.get("token_prefix"));
        assertEquals(NODE_ID, row.get("node_id"));
        assertEquals(Scope.TOKENS.getMask(), row.get("auth_scope"));
        assertNotNull(row.get("deadline"));
    }

    @Test
    void theOriginalTokenStillAuthenticatesAfterMigration() throws InvalidTokenException {
        Token token = authenticationManager.getToken(SECRET, NODE_ID);

        assertNotNull(token);
        assertEquals(TOKEN_ID, token.getId());
        assertEquals(Scope.TOKENS.getMask(), token.getAuthScope());
    }

    @Test
    void theStoredHashCannotAuthenticate() {
        assertThrows(InvalidTokenException.class, () -> authenticationManager.getToken(HASH, NODE_ID));
        String encodedHash = Util.base64urlencode(HexFormat.of().parseHex(HASH));
        assertThrows(InvalidTokenException.class, () -> authenticationManager.getToken(encodedHash, NODE_ID));
    }

    @Test
    void aTokenCannotAuthenticateOnAnotherNode() {
        UUID otherNodeId = UUID.randomUUID();

        assertThrows(InvalidTokenException.class, () -> authenticationManager.getToken(SECRET, otherNodeId));
    }

    @Test
    void expiredTokenCannotAuthenticate() {
        jdbc.update("UPDATE tokens SET deadline = CURRENT_TIMESTAMP - INTERVAL '1 second' WHERE id = ?", TOKEN_ID);
        try {
            assertThrows(InvalidTokenException.class, () -> authenticationManager.getToken(SECRET, NODE_ID));
        } finally {
            jdbc.update("UPDATE tokens SET deadline = CURRENT_TIMESTAMP + INTERVAL '1 day' WHERE id = ?", TOKEN_ID);
        }
    }

    @Test
    void tokenEventsRevealOnlyTheOriginalPrefix() throws InvalidTokenException {
        Token token = authenticationManager.getToken(SECRET, NODE_ID);
        var receptor = new CapturingTokenReceptor();

        receptor.added(new TokenAddedLiberin(token));
        assertEquals("ABCD\u2026", ((TokenAddedEvent) receptor.event).getToken().getToken());
        receptor.updated(new TokenUpdatedLiberin(token));
        assertEquals("ABCD\u2026", ((TokenUpdatedEvent) receptor.event).getToken().getToken());
    }

    @Test
    void tokenReadAndUpdateResponsesKeepTheOriginalPrefix() {
        TokenInfo response = transaction.execute(status -> tokenController.get(TOKEN_ID.toString()));
        assertNotNull(response);
        assertEquals("ABCD\u2026", response.getToken());
        List<TokenInfo> list = transaction.execute(status -> tokenController.getAll());
        assertNotNull(list);
        assertEquals("ABCD\u2026", list.getFirst().getToken());
        TokenUpdate update = new TokenUpdate();
        update.setName("renamed");
        TokenInfo updated = transaction.execute(status -> tokenController.put(TOKEN_ID.toString(), update));
        assertNotNull(updated);
        assertEquals("ABCD\u2026", updated.getToken());
        assertEquals("renamed", updated.getName());
    }

    private static class CapturingTokenReceptor extends TokenReceptor {

        private Event event;

        @Override
        protected void send(Liberin liberin, Event event) {
            this.event = event;
        }

    }

}
