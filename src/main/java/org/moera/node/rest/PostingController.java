package org.moera.node.rest;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PostingText;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.api.naming.NamingCache;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.IncorrectSignatureException;
import org.moera.node.auth.UserBlockedException;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.OwnReactionRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.fingerprint.PostingFingerprintBuilder;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.PostingAddedLiberin;
import org.moera.node.liberin.model.PostingDeletedLiberin;
import org.moera.node.liberin.model.PostingReadLiberin;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.ClientReactionInfoUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingInfoUtil;
import org.moera.node.model.PostingTextUtil;
import org.moera.node.operations.BlockedByUserOperations;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.operations.EntryOperations;
import org.moera.node.operations.FeedOperations;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.moera.node.operations.OperationsValidator;
import org.moera.node.operations.PostingOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.operations.UserListOperations;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/postings")
@NoCache
public class PostingController {

    private static final Logger log = LoggerFactory.getLogger(PostingController.class);

    private static final Duration CREATED_AT_MARGIN = Duration.ofMinutes(10);

    @Inject
    private RequestContext requestContext;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private ReactionRepository reactionRepository;

    @Inject
    private OwnReactionRepository ownReactionRepository;

    @Inject
    private EntryAttachmentRepository entryAttachmentRepository;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    @PersistenceContext
    private EntityManager entityManager;

    @Inject
    private PostingOperations postingOperations;

    @Inject
    private EntryOperations entryOperations;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private BlockedUserOperations blockedUserOperations;

    @Inject
    private BlockedByUserOperations blockedByUserOperations;

    @Inject
    private FeedOperations feedOperations;

    @Inject
    private UserListOperations userListOperations;

    @Inject
    private TextConverter textConverter;

    @Inject
    private NamingCache namingCache;

    private int getMaxPostingSize() {
        return requestContext.getOptions().getInt("posting.max-size");
    }

    @PostMapping
    @Entitled
    @Transactional
    public ResponseEntity<PostingInfo> post(@RequestBody PostingText postingText) {
        log.info(
            "POST /postings (bodySrc = {}, bodySrcFormat = {})",
            LogUtil.format(postingText.getBodySrc().getEncoded(), 64),
            LogUtil.format(SourceFormat.toValue(postingText.getBodySrcFormat()))
        );

        postingText.validate();

        if (
            !requestContext.isAdmin(Scope.ADD_POST)
            && !requestContext.getOptions().getBool("posting.non-admin.allowed")
        ) {
            throw new AuthenticationException();
        }
        if (!ObjectUtils.isEmpty(postingText.getPublications()) && !requestContext.isAdmin(Scope.UPDATE_FEEDS)) {
            throw new AuthenticationException();
        }
        mediaOperations.validateAvatar(postingText.getOwnerAvatar());
        byte[] digest = validatePostingText(null, postingText, postingText.getOwnerName());
        if (postingText.getSignature() != null) {
            requestContext.authenticatedWithSignature(postingText.getOwnerName());
        }
        if (blockedUserOperations.isBlocked(BlockedOperation.POSTING)) {
            throw new UserBlockedException();
        }
        List<MediaFileOwner> media = mediaOperations.validateAttachments(
            postingText.getMedia(),
            true,
            requestContext.isAdmin(Scope.VIEW_MEDIA),
            requestContext.isAdmin(Scope.ADD_POST),
            requestContext.getClientName(Scope.VIEW_MEDIA)
        );

        Posting posting = postingOperations.newPosting(postingText);
        posting = postingOperations.createOrUpdatePosting(
            posting,
            null,
            media,
            postingText.getPublications(),
            null,
            revision -> PostingTextUtil.toEntryRevision(postingText, revision, digest, textConverter, media),
            entry -> PostingTextUtil.toEntry(postingText, entry)
        );
        List<Story> stories = storyRepository.findByEntryId(requestContext.nodeId(), posting.getId());

        requestContext.send(new PostingAddedLiberin(posting));

        return ResponseEntity
            .created(URI.create("/postings/" + posting.getId()))
            .body(withBlockings(PostingInfoUtil.build(
                posting,
                stories,
                MediaAttachmentsProvider.RELATIONS,
                requestContext,
                requestContext.getOptions()
            )));
    }

    @PutMapping("/{id}")
    @Entitled
    @Transactional
    public PostingInfo put(@PathVariable UUID id, @RequestBody PostingText postingText) {
        log.info(
            "PUT /postings/{id}, (id = {}, bodySrc = {}, bodySrcFormat = {})",
            LogUtil.format(id),
            LogUtil.format(postingText.getBodySrc(), 64),
            LogUtil.format(SourceFormat.toValue(postingText.getBodySrcFormat()))
        );

        postingText.validate();

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), id)
            .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        ValidationUtil.assertion(posting.isOriginal(), "posting.not-original");
        Principal latestView = posting.getViewE();
        EntryRevision latest = posting.getCurrentRevision();
        mediaOperations.validateAvatar(postingText.getOwnerAvatar());
        byte[] digest = validatePostingText(posting, postingText, posting.getOwnerName());
        if (postingText.getSignature() != null) {
            requestContext.authenticatedWithSignature(postingText.getOwnerName());
        }
        if (!requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (blockedUserOperations.isBlocked(BlockedOperation.POSTING)) {
            throw new UserBlockedException();
        }
        ValidationUtil.assertion(
            postingText.getPublications() == null || postingText.getPublications().isEmpty(),
            "posting.publications.cannot-modify"
        );
        List<MediaFileOwner> media = mediaOperations.validateAttachments(
            postingText.getMedia(),
            true,
            requestContext.isAdmin(Scope.VIEW_MEDIA),
            requestContext.isAdmin(Scope.UPDATE_POST),
            requestContext.getClientName(Scope.VIEW_MEDIA)
        );

        entityManager.lock(posting, LockModeType.PESSIMISTIC_WRITE);
        boolean sameViewComments = PostingTextUtil.sameViewComments(postingText, posting);
        PostingTextUtil.toEntry(postingText, posting);
        posting = postingOperations.createOrUpdatePosting(
            posting,
            posting.getCurrentRevision(),
            media,
            null,
            revision -> PostingTextUtil.sameAsRevision(postingText, revision),
            revision -> PostingTextUtil.toEntryRevision(postingText, revision, digest, textConverter, media),
            entry -> PostingTextUtil.toEntry(postingText, entry)
        );
        List<Story> stories = storyRepository.findByEntryId(requestContext.nodeId(), posting.getId());
        if (!sameViewComments) {
            mediaFileOwnerRepository.updateUsageOfCommentAttachments(
                requestContext.nodeId(), posting.getId(), Util.now()
            );
        }
        requestContext.send(new PostingUpdatedLiberin(posting, latest, latestView));

        return withBlockings(withClientReaction(
            PostingInfoUtil.build(
                posting,
                stories,
                MediaAttachmentsProvider.RELATIONS,
                requestContext,
                requestContext.getOptions()
            )
        ));
    }

    private byte[] validatePostingText(Posting posting, PostingText postingText, String ownerName) {
        byte[] digest = null;
        if (postingText.getSignature() == null) {
            Scope scope = posting == null ? Scope.ADD_POST : Scope.UPDATE_POST;
            String clientName = requestContext.getClientName(scope);
            boolean valid = false;
            if (!ObjectUtils.isEmpty(ownerName)) {
                valid =
                    ownerName.equals(clientName)
                    || ownerName.equals(requestContext.nodeName()) && requestContext.isAdmin(scope);
            } else {
                if (!ObjectUtils.isEmpty(clientName)) {
                    postingText.setOwnerName(clientName);
                    valid = true;
                } else if (requestContext.isAdmin(scope)) {
                    postingText.setOwnerName(requestContext.nodeName());
                    valid = true;
                }
            }
            valid = valid && (requestContext.isAdmin(scope) || postingText.getExternalSourceUri() == null);
            if (!valid) {
                throw new AuthenticationException();
            }

            ValidationUtil.assertion(
                posting != null || postingText.getBodySrc() != null || !ObjectUtils.isEmpty(postingText.getMedia()),
                "posting.body-src.blank"
            );
            ValidationUtil.maxSize(
                postingText.getBodySrc() != null ? postingText.getBodySrc().getEncoded() : null,
                getMaxPostingSize(),
                "posting.body-src.wrong-size"
            );
        } else {
            byte[] signingKey = namingCache.get(ownerName).getSigningKey();
            byte[] fingerprint = PostingFingerprintBuilder.build(
                postingText.getSignatureVersion(), postingText, parentMediaDigest(posting), this::mediaDigest
            );
            if (!CryptoUtil.verifySignature(fingerprint, postingText.getSignature(), signingKey)) {
                throw new IncorrectSignatureException();
            }
            digest = CryptoUtil.digest(fingerprint);

            ValidationUtil.assertion(
                !ObjectUtils.isEmpty(postingText.getBody()) || !ObjectUtils.isEmpty(postingText.getMedia()),
                "posting.body.blank"
            );
            ValidationUtil.maxSize(postingText.getBody().getEncoded(), getMaxPostingSize(), "posting.body.wrong-size");
            ValidationUtil.notNull(postingText.getBodyFormat(), "posting.body-format.missing");
            ValidationUtil.notNull(postingText.getCreatedAt(), "posting.created-at.missing");
            ValidationUtil.assertion(
                Duration
                    .between(Instant.ofEpochSecond(postingText.getCreatedAt()), Instant.now())
                    .abs()
                    .compareTo(CREATED_AT_MARGIN) <= 0,
                "posting.created-at.out-of-range"
            );
        }
        OperationsValidator.validateOperations(
            postingText.getOperations(),
            false,
            "posting.operations.wrong-principal"
        );
        OperationsValidator.validateOperations(
            postingText.getCommentOperations(),
            true,
            "posting.comment-operations.wrong-principal"
        );
        OperationsValidator.validateOperations(
            false,
            postingText.getReactionOperations(),
            true,
            "posting.reaction-operations.wrong-principal"
        );
        OperationsValidator.validateOperations(
            true,
            postingText.getCommentReactionOperations(),
            true,
            "posting.comment-reaction-operations.wrong-principal"
        );
        return digest;
    }

    private byte[] parentMediaDigest(Posting posting) {
        return posting != null && posting.getParentMedia() != null
            ? posting.getParentMedia().getMediaFile().getDigest()
            : null;
    }

    private byte[] mediaDigest(UUID id) {
        MediaFileOwner media = mediaFileOwnerRepository.findById(id).orElse(null);
        return media != null ? media.getMediaFile().getDigest() : null;
    }

    @GetMapping
    @Admin(Scope.VIEW_CONTENT)
    @Transactional
    public List<PostingInfo> getByExternalSourceUri(@RequestParam String external) {
        log.info("GET /postings (external = {})", LogUtil.format(external));

        return postingRepository.findByExternalSourceUri(requestContext.nodeId(), external)
            .stream()
            .map(posting -> {
                List<Story> stories = storyRepository.findByEntryId(requestContext.nodeId(), posting.getId());
                return withSheriffUserListMarks(withBlockings(withClientReaction(
                    PostingInfoUtil.build(
                        posting,
                        stories,
                        MediaAttachmentsProvider.RELATIONS,
                        requestContext,
                        requestContext.getOptions()
                    )
                )));
            })
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Transactional
    public PostingInfo get(@PathVariable UUID id, @RequestParam(required = false) String include) {
        log.info("GET /postings/{id}, (id = {}, include = {})", LogUtil.format(id), LogUtil.format(include));

        Set<String> includeSet = Util.setParam(include);

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), id)
            .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        List<Story> stories = storyRepository.findByEntryId(requestContext.nodeId(), id);
        if (
            !requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)
            && !feedOperations.isSheriffAllowed(stories, posting.getViewE())
        ) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }

        requestContext.send(new PostingReadLiberin(id));

        return withSheriffUserListMarks(withBlockings(withClientReaction(
            PostingInfoUtil.build(
                posting,
                stories,
                entryOperations,
                includeSet.contains("source"),
                requestContext,
                requestContext.getOptions()
            )
        )));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /postings/{id}, (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), id)
            .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        EntryRevision latest = posting.getCurrentRevision();
        if (
            requestContext.isClient(posting.getOwnerName(), Scope.IDENTIFY)
            && !requestContext.isPrincipal(posting.getDeleteE(), Scope.DELETE_OWN_CONTENT)
        ) {
            throw new AuthenticationException();
        }
        if (
            !requestContext.isClient(posting.getOwnerName(), Scope.IDENTIFY)
            && !requestContext.isPrincipal(posting.getDeleteE(), Scope.DELETE_OTHERS_CONTENT)
        ) {
            throw new AuthenticationException();
        }
        if (blockedUserOperations.isBlocked(BlockedOperation.POSTING)) {
            throw new UserBlockedException();
        }
        entityManager.lock(posting, LockModeType.PESSIMISTIC_WRITE);
        postingOperations.deletePosting(posting);
        storyOperations.unpublish(posting.getId());

        requestContext.send(new PostingDeletedLiberin(posting, latest));

        return Result.OK;
    }

    @GetMapping("/{id}/attached")
    @Transactional
    public List<PostingInfo> getAttached(@PathVariable UUID id) {
        log.info("GET /postings/{id}/attached, (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), id)
            .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        List<Posting> attached = posting.isOriginal()
            ? entryAttachmentRepository.findOwnAttachedPostings(
                requestContext.nodeId(), posting.getCurrentRevision().getId()
            )
            : entryAttachmentRepository.findReceivedAttachedPostings(
                requestContext.nodeId(), posting.getCurrentRevision().getId(), posting.getReceiverName()
            );
        return attached.stream()
            .map(p -> withBlockings(withClientReaction(PostingInfoUtil.build(p, false, requestContext))))
            .collect(Collectors.toList());
    }

    private PostingInfo withClientReaction(PostingInfo postingInfo) {
        String clientName = !requestContext.isOwner()
            ? requestContext.getClientName(Scope.IDENTIFY)
            : requestContext.nodeName();
        boolean viewContent = !requestContext.isOwner()
            ? requestContext.hasClientScope(Scope.VIEW_CONTENT)
            : requestContext.isAdmin(Scope.VIEW_CONTENT);
        if (ObjectUtils.isEmpty(clientName)) {
            return postingInfo;
        }
        if (PostingInfoUtil.isOriginal(postingInfo)) {
            Reaction reaction = reactionRepository.findByEntryIdAndOwner(
                UUID.fromString(postingInfo.getId()), clientName
            );
            if (reaction != null && (reaction.getViewE().isPublic() || viewContent)) {
                postingInfo.setClientReaction(ClientReactionInfoUtil.build(reaction));
            }
        } else if (requestContext.isAdmin(Scope.VIEW_CONTENT)) {
            // TODO to see public reactions, we need to store the reaction's view principal in OwnReaction
            OwnReaction ownReaction = ownReactionRepository.findByRemotePostingId(
                requestContext.nodeId(), postingInfo.getReceiverName(), postingInfo.getReceiverPostingId()
            ).orElse(null);
            postingInfo.setClientReaction(ownReaction != null ? ClientReactionInfoUtil.build(ownReaction) : null);
        }
        return postingInfo;
    }

    private PostingInfo withBlockings(PostingInfo postingInfo) {
        if (PostingInfoUtil.isOriginal(postingInfo)) {
            PostingInfoUtil.putBlockedOperations(
                postingInfo,
                blockedUserOperations.findBlockedOperations(UUID.fromString(postingInfo.getId()))
            );
        } else if (requestContext.isOwner() && requestContext.isAdmin(Scope.VIEW_PEOPLE)) {
            PostingInfoUtil.putBlockedOperations(
                postingInfo,
                blockedByUserOperations.findBlockedOperations(
                    postingInfo.getReceiverName(), postingInfo.getReceiverPostingId()
                )
            );
        }
        return postingInfo;
    }

    private PostingInfo withSheriffUserListMarks(PostingInfo postingInfo) {
        userListOperations.fillSheriffListMarks(postingInfo);
        return postingInfo;
    }

}
