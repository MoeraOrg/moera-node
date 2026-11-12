package org.moera.node.global;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.domain.Domains;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.SubscribedCache;
import org.moera.node.option.Options;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UniversalContextTest {

    private static final String FRIEND_GROUP = UUID.randomUUID().toString();

    private final RequestContextImpl requestContext = new RequestContextImpl();
    private final UniversalContext universalContext = new UniversalContext();

    @BeforeEach
    void setUp() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        ReflectionTestUtils.setField(universalContext, "requestContext", requestContext);
        requestContext.setClientName("bob_0");
        requestContext.setClientScope(Scope.DELETE_OWN_CONTENT.getMask());
        requestContext.setSubscribedToClient(true);
        requestContext.setFriendGroups(new String[]{FRIEND_GROUP});
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        MDC.remove("domain");
    }

    @ParameterizedTest
    @MethodSource("clientPrincipals")
    void identitySubscriptionAndFriendshipUseClientScope(Principal principal) {
        assertTrue(universalContext.isPrincipal(principal, Scope.DELETE_OWN_CONTENT, Scope.DELETE_OTHERS_CONTENT));
        assertFalse(universalContext.isPrincipal(principal, Scope.DELETE_OTHERS_CONTENT, Scope.DELETE_OWN_CONTENT));
        assertTrue(universalContext.isPrincipal(principal, Scope.DELETE_OWN_CONTENT));
        assertFalse(universalContext.isPrincipal(principal, Scope.DELETE_OTHERS_CONTENT));
    }

    private static Stream<Principal> clientPrincipals() {
        return Stream.of(
            Principal.SIGNED,
            Principal.ofNode("bob_0"),
            Principal.ofOnly("bob_0"),
            Principal.SUBSCRIBED,
            Principal.ofFriendGroup(FRIEND_GROUP)
        );
    }

    @Test
    void administrativeRightsUseAdminScopeAndRespectThePrincipal() {
        requestContext.setAdminScope(Scope.DELETE_OTHERS_CONTENT.getMask());

        assertTrue(universalContext.isPrincipal(Principal.ADMIN, Scope.DELETE_OWN_CONTENT, Scope.DELETE_OTHERS_CONTENT));
        assertFalse(universalContext.isPrincipal(Principal.ADMIN, Scope.DELETE_OTHERS_CONTENT, Scope.DELETE_OWN_CONTENT));
        assertFalse(universalContext.isPrincipal(Principal.NONE, Scope.DELETE_OWN_CONTENT, Scope.DELETE_OTHERS_CONTENT));
        assertFalse(universalContext.isPrincipal(
            Principal.ofOnly("alice_0"), Scope.DELETE_OWN_CONTENT, Scope.DELETE_OTHERS_CONTENT
        ));
        assertTrue(universalContext.isPrincipal(Principal.ADMIN, Scope.DELETE_OTHERS_CONTENT));
        assertFalse(universalContext.isPrincipal(Principal.ADMIN, Scope.DELETE_OWN_CONTENT));
    }

    @ParameterizedTest
    @CsvSource({"alice_0, true", "bob_0, false"})
    void backgroundAuthenticationPreservesIdentityAndAdministrativeRights(String client, boolean admin) {
        RequestContextHolder.resetRequestAttributes();
        UUID nodeId = UUID.randomUUID();
        Options options = mock(Options.class);
        when(options.nodeName()).thenReturn("alice_0");
        Domains domains = mock(Domains.class);
        when(domains.getDomainOptions(nodeId)).thenReturn(options);
        when(domains.getDomainEffectiveName(nodeId)).thenReturn("alice.example");
        FriendCache friendCache = mock(FriendCache.class);
        when(friendCache.getClientGroupIds(client)).thenReturn(new String[]{FRIEND_GROUP});
        SubscribedCache subscribedCache = mock(SubscribedCache.class);
        when(subscribedCache.isSubscribed(client)).thenReturn(true);
        ReflectionTestUtils.setField(universalContext, "domains", domains);
        ReflectionTestUtils.setField(universalContext, "friendCache", friendCache);
        ReflectionTestUtils.setField(universalContext, "subscribedCache", subscribedCache);
        universalContext.associate(nodeId);
        universalContext.authenticatedWithSignature(client);

        assertEquals(admin, universalContext.isPrincipal(
            Principal.ADMIN, Scope.DELETE_OWN_CONTENT, Scope.DELETE_OTHERS_CONTENT
        ));
        assertTrue(universalContext.isPrincipal(
            Principal.ofOnly(client), Scope.DELETE_OWN_CONTENT, Scope.DELETE_OTHERS_CONTENT
        ));
        assertTrue(universalContext.isPrincipal(
            Principal.SUBSCRIBED, Scope.DELETE_OWN_CONTENT, Scope.DELETE_OTHERS_CONTENT
        ));
        assertTrue(universalContext.isPrincipal(
            Principal.ofFriendGroup(FRIEND_GROUP), Scope.DELETE_OWN_CONTENT, Scope.DELETE_OTHERS_CONTENT
        ));
        assertEquals(admin, universalContext.isPrincipal(Principal.ADMIN, Scope.DELETE_OWN_CONTENT));
    }

}
