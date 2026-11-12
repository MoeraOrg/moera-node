package org.moera.node.rest;

import java.security.Security;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.crypto.KeyPair;
import org.moera.lib.node.carte.Carte;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.StoryType;
import org.moera.node.api.naming.NamingCache;
import org.moera.node.api.naming.RegisteredNameDetails;
import org.moera.node.auth.AuthenticationInterceptor;
import org.moera.node.auth.AuthenticationManager;
import org.moera.node.auth.TokenHash;
import org.moera.node.config.Config;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Feed;
import org.moera.node.data.GrantRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.Token;
import org.moera.node.data.TokenRepository;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.SubscribedCache;
import org.moera.node.global.RequestContextImpl;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.PostingDeletedLiberin;
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.operations.FeedOperations;
import org.moera.node.operations.GrantCache;
import org.moera.node.operations.PostingOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.option.Options;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.Util;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

class PostingControllerDeleteAuthorizationTest {

    private static final String ALICE = "alice_0";
    private static final String BOB = "bob_0";
    private static final String CAROL = "carol_0";
    private static final String TOKEN = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopq";

    private static Map<String, KeyPair> signingKeys;

    private final UUID nodeId = UUID.randomUUID();
    private final Map<String, Posting> postings = new HashMap<>();
    private final Map<UUID, Story> stories = new HashMap<>();
    private final EntityManager entityManager = mock(EntityManager.class);
    private final MediaOperations mediaOperations = mock(MediaOperations.class);
    private final TokenRepository tokenRepository = mock(TokenRepository.class);
    private final RequestContextImpl requestContext = new RequestContextImpl();
    private final GrantCache grantCache = new GrantCache();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mvc;

    @BeforeAll
    static void createSigningKeys() {
        Security.addProvider(new BouncyCastleProvider());
        signingKeys = Map.of(ALICE, CryptoUtil.generateKey(), BOB, CryptoUtil.generateKey());
    }

    @BeforeEach
    void setUp() {
        Options options = mock(Options.class);
        when(options.nodeId()).thenReturn(nodeId);
        when(options.nodeName()).thenReturn(ALICE);
        when(options.getDuration("posting.deleted.lifetime")).thenReturn(ExtendedDuration.NEVER);
        when(options.getDuration("token.lifetime")).thenReturn(new ExtendedDuration(Duration.ofDays(1)));
        requestContext.setOptions(options);
        UniversalContext universalContext = new UniversalContext();
        ReflectionTestUtils.setField(universalContext, "requestContext", requestContext);

        PostingRepository postingRepository = mock(PostingRepository.class);
        StoryRepository storyRepository = mock(StoryRepository.class);
        for (String owner : List.of(ALICE, BOB, CAROL)) {
            // All three are original postings stored on alice, not cached copies from other nodes.
            Posting posting = new Posting();
            posting.setId(UUID.randomUUID());
            posting.setNodeId(nodeId);
            posting.setOwnerName(owner);
            EntryRevision revision = new EntryRevision();
            revision.setId(UUID.randomUUID());
            revision.setEntry(posting);
            posting.setCurrentRevision(revision);
            postings.put(owner, posting);
            when(postingRepository.findFullByNodeIdAndId(nodeId, posting.getId())).thenReturn(Optional.of(posting));
            Story story = new Story(UUID.randomUUID(), nodeId, StoryType.POSTING_ADDED);
            story.setEntry(posting);
            story.setFeedName(Feed.TIMELINE);
            story.setMoment(1L);
            stories.put(posting.getId(), story);
        }
        when(postingRepository.saveAndFlush(any(Posting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storyRepository.findByEntryId(eq(nodeId), any(UUID.class))).thenAnswer(invocation -> {
            Story story = stories.get(invocation.<UUID>getArgument(1));
            return story != null ? List.of(story) : List.of();
        });
        doAnswer(invocation -> {
            Story story = invocation.getArgument(0);
            stories.remove(story.getEntry().getId());
            return null;
        }).when(storyRepository).delete(any(Story.class));
        StoryOperations storyOperations = new StoryOperations();
        ReflectionTestUtils.setField(storyOperations, "universalContext", universalContext);
        ReflectionTestUtils.setField(storyOperations, "storyRepository", storyRepository);
        PostingOperations postingOperations = new PostingOperations();
        ReflectionTestUtils.setField(postingOperations, "universalContext", universalContext);
        ReflectionTestUtils.setField(postingOperations, "postingRepository", postingRepository);
        ReflectionTestUtils.setField(postingOperations, "storyOperations", storyOperations);
        ReflectionTestUtils.setField(postingOperations, "mediaOperations", mediaOperations);
        PostingController controller = new PostingController();
        ReflectionTestUtils.setField(controller, "requestContext", requestContext);
        ReflectionTestUtils.setField(controller, "postingRepository", postingRepository);
        ReflectionTestUtils.setField(controller, "postingOperations", postingOperations);
        ReflectionTestUtils.setField(controller, "storyOperations", storyOperations);
        ReflectionTestUtils.setField(controller, "entityManager", entityManager);
        ReflectionTestUtils.setField(controller, "blockedUserOperations", mock(BlockedUserOperations.class));

        NamingCache namingCache = mock(NamingCache.class);
        signingKeys.forEach((name, keys) -> when(namingCache.get(name)).thenReturn(
            new RegisteredNameDetails(name, null, CryptoUtil.rawPublicKey(keys.getPublicKey()))
        ));
        AuthenticationManager authenticationManager = new AuthenticationManager();
        ReflectionTestUtils.setField(authenticationManager, "requestContext", requestContext);
        ReflectionTestUtils.setField(authenticationManager, "universalContext", universalContext);
        ReflectionTestUtils.setField(authenticationManager, "tokenRepository", tokenRepository);
        ReflectionTestUtils.setField(authenticationManager, "namingCache", namingCache);
        ReflectionTestUtils.setField(grantCache, "grantRepository", mock(GrantRepository.class));
        FriendCache friendCache = mock(FriendCache.class);
        when(friendCache.getClientGroupIds(anyString())).thenReturn(new String[0]);
        Config config = new Config();
        config.setRootSecret("unused-test-root-secret");
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);
        AuthenticationInterceptor interceptor = new AuthenticationInterceptor();
        ReflectionTestUtils.setField(interceptor, "config", config);
        ReflectionTestUtils.setField(interceptor, "authenticationManager", authenticationManager);
        ReflectionTestUtils.setField(interceptor, "requestContext", requestContext);
        ReflectionTestUtils.setField(interceptor, "grantCache", grantCache);
        ReflectionTestUtils.setField(interceptor, "friendCache", friendCache);
        ReflectionTestUtils.setField(interceptor, "subscribedCache", mock(SubscribedCache.class));
        ReflectionTestUtils.setField(interceptor, "feedOperations", mock(FeedOperations.class));
        ReflectionTestUtils.setField(interceptor, "messageSource", messageSource);
        ReflectionTestUtils.setField(interceptor, "objectMapper", objectMapper);
        ExceptionsControllerAdvice advice = new ExceptionsControllerAdvice();
        ReflectionTestUtils.setField(advice, "messageSource", messageSource);
        mvc = MockMvcBuilders.standaloneSetup(controller)
            .addInterceptors(interceptor)
            .setControllerAdvice(advice)
            .build();
    }

    @ParameterizedTest(name = "bob client delete-own-content / owner={0}: allowed={1}")
    @CsvSource({"alice_0, false", "bob_0, true", "carol_0, false"})
    void bobCarteWithOwnContentScopeCanDeleteOnlyBobsPosts(String owner, boolean allowed) throws Exception {
        assertDeletion(carte(BOB, Scope.DELETE_OWN_CONTENT, Scope.NONE), owner, allowed);
    }

    @ParameterizedTest(name = "bob client {0} / owner={1}: forbidden")
    @MethodSource("bobScopesWithoutOwnContentPermission")
    void bobCarteWithOtherScopesCannotDeletePosts(Scope scope, String owner) throws Exception {
        assertDeletion(carte(BOB, scope, Scope.NONE), owner, false);
    }

    private static Stream<Arguments> bobScopesWithoutOwnContentPermission() {
        return Stream.concat(
            Stream.of(Scope.IDENTIFY, Scope.VIEW_ALL),
            Arrays.stream(Scope.values()).filter(Scope::isBasic).filter(scope -> scope != Scope.DELETE_OWN_CONTENT)
        ).flatMap(scope -> Stream.of(ALICE, BOB, CAROL).map(owner -> Arguments.of(scope, owner)));
    }

    @ParameterizedTest(name = "alice {0} / {1} / owner={2}: allowed={3}")
    @CsvSource({
        "CARTE, DELETE_OWN_CONTENT, alice_0, true",
        "CARTE, DELETE_OWN_CONTENT, bob_0, false",
        "CARTE, DELETE_OWN_CONTENT, carol_0, false",
        "CARTE, DELETE_OTHERS_CONTENT, alice_0, false",
        "CARTE, DELETE_OTHERS_CONTENT, bob_0, true",
        "CARTE, DELETE_OTHERS_CONTENT, carol_0, true",
        "CARTE, OTHER, alice_0, false",
        "CARTE, OTHER, bob_0, false",
        "CARTE, OTHER, carol_0, false",
        "TOKEN, DELETE_OWN_CONTENT, alice_0, true",
        "TOKEN, DELETE_OWN_CONTENT, bob_0, false",
        "TOKEN, DELETE_OWN_CONTENT, carol_0, false",
        "TOKEN, DELETE_OTHERS_CONTENT, alice_0, false",
        "TOKEN, DELETE_OTHERS_CONTENT, bob_0, true",
        "TOKEN, DELETE_OTHERS_CONTENT, carol_0, true",
        "TOKEN, OTHER, alice_0, false",
        "TOKEN, OTHER, bob_0, false",
        "TOKEN, OTHER, carol_0, false"
    })
    void aliceDeletionPermissionsDependOnCredentialScope(
        Credential credential, Scope scope, String owner, boolean allowed
    ) throws Exception {
        String authorization = credential == Credential.CARTE ? carte(ALICE, Scope.IDENTIFY, scope) : token(scope);

        assertDeletion(authorization, owner, allowed);
    }

    @ParameterizedTest(name = "bob grant={0} / carte admin={1} / owner={2}: allowed={3}")
    @CsvSource({
        "DELETE_OWN_CONTENT, DELETE_OWN_CONTENT, alice_0, true",
        "DELETE_OWN_CONTENT, DELETE_OWN_CONTENT, carol_0, false",
        "DELETE_OTHERS_CONTENT, DELETE_OTHERS_CONTENT, alice_0, false",
        "DELETE_OTHERS_CONTENT, DELETE_OTHERS_CONTENT, carol_0, true",
        "DELETE_OWN_CONTENT, DELETE_OTHERS_CONTENT, alice_0, false",
        "DELETE_OWN_CONTENT, DELETE_OTHERS_CONTENT, carol_0, false",
        "DELETE_OTHERS_CONTENT, DELETE_OWN_CONTENT, alice_0, false",
        "DELETE_OTHERS_CONTENT, DELETE_OWN_CONTENT, carol_0, false",
        "NONE, DELETE_OWN_CONTENT, alice_0, false",
        "NONE, DELETE_OTHERS_CONTENT, carol_0, false",
        "DELETE_OWN_CONTENT, OTHER, alice_0, false",
        "DELETE_OTHERS_CONTENT, OTHER, carol_0, false"
    })
    void delegatedDeletionRequiresMatchingGrantAndCarteAdminScope(
        Scope grant, Scope adminScope, String owner, boolean allowed
    ) throws Exception {
        if (grant != Scope.NONE) {
            grantCache.grant(nodeId, BOB, grant.getMask());
        }

        assertDeletion(carte(BOB, Scope.IDENTIFY, adminScope), owner, allowed);
    }

    private String carte(String owner, Scope clientScope, Scope adminScope) {
        return "Bearer carte:" + Carte.generate(
            owner,
            null,
            Instant.now().minusSeconds(1),
            signingKeys.get(owner).getPrivateKey(),
            ALICE,
            clientScope.getMask(),
            adminScope.getMask()
        );
    }

    private String token(Scope scope) {
        Token token = new Token();
        token.setId(UUID.randomUUID());
        token.setNodeId(nodeId);
        token.setTokenHash(TokenHash.hash(TOKEN));
        token.setTokenPrefix(TOKEN.substring(0, 4));
        token.setAuthScope(scope.getMask());
        token.setDeadline(Timestamp.from(Instant.now().plus(Duration.ofDays(2))));
        token.setLastUsedAt(Util.now());
        when(tokenRepository.findByTokenHash(eq(nodeId), eq(token.getTokenHash()), any(Timestamp.class)))
            .thenReturn(Optional.of(token));
        return "Bearer token:" + TOKEN;
    }

    private void assertDeletion(String authorization, String owner, boolean allowed) throws Exception {
        Posting target = postings.get(owner);
        var response = mvc.perform(delete("/moera/api/postings/{id}", target.getId())
                .header(HttpHeaders.AUTHORIZATION, authorization))
            .andReturn().getResponse();

        assertAll(
            () -> assertEquals(allowed ? 200 : 403, response.getStatus(), response.getContentAsString()),
            () -> assertEquals(
                allowed ? "ok" : "authentication.required",
                objectMapper.readTree(response.getContentAsString()).get("errorCode").asString()
            ),
            () -> assertPostingState(target, allowed)
        );
    }

    private void assertPostingState(Posting target, boolean allowed) {
        for (Posting posting : postings.values()) {
            boolean deleted = allowed && posting == target;
            if (deleted) {
                assertNotNull(posting.getDeletedAt(), posting.getOwnerName());
                assertNotNull(posting.getCurrentRevision().getDeletedAt(), posting.getOwnerName());
            } else {
                assertNull(posting.getDeletedAt(), posting.getOwnerName());
                assertNull(posting.getCurrentRevision().getDeletedAt(), posting.getOwnerName());
            }
            assertEquals(!deleted, stories.containsKey(posting.getId()), posting.getOwnerName());
        }
        List<UUID> deletedIds = allowed ? List.of(target.getId()) : List.of();
        assertEquals(deletedIds, requestContext.getAfterCommitLiberins().stream()
            .filter(PostingDeletedLiberin.class::isInstance)
            .map(PostingDeletedLiberin.class::cast)
            .map(PostingDeletedLiberin::getPosting)
            .map(Posting::getId)
            .toList());
        assertEquals(deletedIds, requestContext.getAfterCommitLiberins().stream()
            .filter(StoryDeletedLiberin.class::isInstance)
            .map(StoryDeletedLiberin.class::cast)
            .map(StoryDeletedLiberin::getPostingId)
            .toList());
        if (!allowed) {
            verifyNoInteractions(entityManager, mediaOperations);
        }
    }

    private enum Credential {
        CARTE,
        TOKEN
    }

}
