package org.moera.node.rest;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.IncorrectSignatureException;
import org.moera.node.auth.Scope;
import org.moera.node.auth.UserBlockedException;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.BlockedOperation;
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
import org.moera.node.data.SourceFormat;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.fingerprint.Fingerprints;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.PostingAddedLiberin;
import org.moera.node.liberin.model.PostingDeletedLiberin;
import org.moera.node.liberin.model.PostingReadLiberin;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.ClientReactionInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingText;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.body.BodyMappingException;
import org.moera.node.api.naming.NamingCache;
import org.moera.node.operations.BlockedByUserOperations;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.operations.EntryOperations;
import org.moera.node.operations.FeedOperations;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.moera.node.operations.OperationsValidator;
import org.moera.node.operations.PostingOperations;
import org.moera.node.operations.UserListOperations;
import org.moera.node.operations.StoryOperations;
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
    public ResponseEntity<PostingInfo> post(@Valid @RequestBody PostingText postingText) {
        log.info("POST /postings (bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(postingText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(postingText.getBodySrcFormat())));

        if (!requestContext.isAdmin(Scope.ADD_POST)
                && !requestContext.getOptions().getBool("posting.non-admin.allowed")) {
            throw new AuthenticationException();
        }
        if (!ObjectUtils.isEmpty(postingText.getPublications()) && !requestContext.isAdmin(Scope.UPDATE_FEEDS)) {
            throw new AuthenticationException();
        }
        mediaOperations.validateAvatar(
                postingText.getOwnerAvatar(),
                postingText::setOwnerAvatarMediaFile,
                () -> new ValidationFailure("postingText.ownerAvatar.mediaId.not-found"));
        byte[] digest = validatePostingText(null, postingText, postingText.getOwnerName(),
                requestContext.isAdmin(Scope.ADD_POST));
        if (postingText.getSignature() != null) {
            requestContext.authenticatedWithSignature(postingText.getOwnerName());
        }
        if (blockedUserOperations.isBlocked(BlockedOperation.POSTING)) {
            throw new UserBlockedException();
        }
        List<MediaFileOwner> media = mediaOperations.validateAttachments(
                postingText.getMedia(),
                () -> new ValidationFailure("postingText.media.not-found"),
                () -> new ValidationFailure("postingText.media.not-compressed"),
                requestContext.isAdmin(Scope.VIEW_MEDIA),
                requestContext.isAdmin(Scope.ADD_POST),
                requestContext.getClientName(Scope.VIEW_MEDIA));

        Posting posting = postingOperations.newPosting(postingText);
        try {
            posting = postingOperations.createOrUpdatePosting(posting, null, media,
                    postingText.getPublications(), null,
                    revision -> postingText.toEntryRevision(revision, digest, textConverter, media),
                    postingText::toEntry);
        } catch (BodyMappingException e) {
            throw new ValidationFailure("postingText.bodySrc.wrong-encoding");
        }
        List<Story> stories = storyRepository.findByEntryId(requestContext.nodeId(), posting.getId());

        requestContext.send(new PostingAddedLiberin(posting));

        return ResponseEntity.created(URI.create("/postings/" + posting.getId()))
                .body(withBlockings(new PostingInfo(
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
    public PostingInfo put(@PathVariable UUID id, @Valid @RequestBody PostingText postingText) {
        log.info("PUT /postings/{id}, (id = {}, bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(id),
                LogUtil.format(postingText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(postingText.getBodySrcFormat())));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!posting.isOriginal()) {
            throw new ValidationFailure("posting.not-original");
        }
        Principal latestView = posting.getViewE();
        EntryRevision latest = posting.getCurrentRevision();
        mediaOperations.validateAvatar(
                postingText.getOwnerAvatar(),
                postingText::setOwnerAvatarMediaFile,
                () -> new ValidationFailure("postingText.ownerAvatar.mediaId.not-found"));
        byte[] digest = validatePostingText(posting, postingText, posting.getOwnerName(),
                requestContext.isAdmin(Scope.UPDATE_POST));
        if (postingText.getSignature() != null) {
            requestContext.authenticatedWithSignature(postingText.getOwnerName());
        }
        if (!requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (blockedUserOperations.isBlocked(BlockedOperation.POSTING)) {
            throw new UserBlockedException();
        }
        if (postingText.getPublications() != null && !postingText.getPublications().isEmpty()) {
            throw new ValidationFailure("postingText.publications.cannot-modify");
        }
        List<MediaFileOwner> media = mediaOperations.validateAttachments(
                postingText.getMedia(),
                () -> new ValidationFailure("postingText.media.not-found"),
                () -> new ValidationFailure("postingText.media.not-compressed"),
                requestContext.isAdmin(Scope.VIEW_MEDIA),
                requestContext.isAdmin(Scope.UPDATE_POST),
                requestContext.getClientName(Scope.VIEW_MEDIA));

        entityManager.lock(posting, LockModeType.PESSIMISTIC_WRITE);
        boolean sameViewComments = postingText.sameViewComments(posting);
        postingText.toEntry(posting);
        try {
            posting = postingOperations.createOrUpdatePosting(posting, posting.getCurrentRevision(), media,
                    null, postingText::sameAsRevision,
                    revision -> postingText.toEntryRevision(revision, digest, textConverter, media),
                    postingText::toEntry);
        } catch (BodyMappingException e) {
            String field = e.getField() != null ? e.getField() : "bodySrc";
            throw new ValidationFailure(String.format("postingText.%s.wrong-encoding", field));
        }
        List<Story> stories = storyRepository.findByEntryId(requestContext.nodeId(), posting.getId());
        if (!sameViewComments) {
            mediaFileOwnerRepository.updateUsageOfCommentAttachments(
                    requestContext.nodeId(), posting.getId(), Util.now());
        }
        requestContext.send(new PostingUpdatedLiberin(posting, latest, latestView));

        return withBlockings(withClientReaction(
                new PostingInfo(
                        posting,
                        stories,
                        MediaAttachmentsProvider.RELATIONS,
                        requestContext,
                        requestContext.getOptions()
                )
        ));
    }

    private byte[] validatePostingText(Posting posting, PostingText postingText, String ownerName, boolean isAdmin) {
        byte[] digest = null;
        if (postingText.getSignature() == null) {
            if (!isAdmin) {
                Scope scope = posting == null ? Scope.ADD_POST : Scope.UPDATE_POST;
                String clientName = requestContext.getClientName(scope);
                if (ObjectUtils.isEmpty(clientName)) {
                    throw new AuthenticationException();
                }
                if (!ObjectUtils.isEmpty(ownerName) && !ownerName.equals(clientName)) {
                    throw new AuthenticationException();
                }
                postingText.setOwnerName(clientName);
            } else {
                if (!ObjectUtils.isEmpty(ownerName) && !ownerName.equals(requestContext.nodeName())) {
                    throw new AuthenticationException();
                }
                postingText.setOwnerName(requestContext.nodeName());
            }

            if (posting == null && ObjectUtils.isEmpty(postingText.getBodySrc())
                    && ObjectUtils.isEmpty(postingText.getMedia())) {
                throw new ValidationFailure("postingText.bodySrc.blank");
            }
            if (postingText.getBodySrc() != null && postingText.getBodySrc().length() > getMaxPostingSize()) {
                throw new ValidationFailure("postingText.bodySrc.wrong-size");
            }
        } else {
            byte[] signingKey = namingCache.get(ownerName).getSigningKey();
            Fingerprint fingerprint = Fingerprints.posting(postingText.getSignatureVersion())
                    .create(postingText, parentMediaDigest(posting), this::mediaDigest);
            if (!CryptoUtil.verify(fingerprint, postingText.getSignature(), signingKey)) {
                throw new IncorrectSignatureException();
            }
            digest = CryptoUtil.digest(fingerprint);

            if (ObjectUtils.isEmpty(postingText.getBody()) && ObjectUtils.isEmpty(postingText.getMedia())) {
                throw new ValidationFailure("postingText.body.blank");
            }
            if (postingText.getBody().length() > getMaxPostingSize()) {
                throw new ValidationFailure("postingText.body.wrong-size");
            }
            if (ObjectUtils.isEmpty(postingText.getBodyFormat())) {
                throw new ValidationFailure("postingText.bodyFormat.blank");
            }
            if (postingText.getCreatedAt() == null) {
                throw new ValidationFailure("postingText.createdAt.blank");
            }
            if (Duration.between(Instant.ofEpochSecond(postingText.getCreatedAt()), Instant.now()).abs()
                    .compareTo(CREATED_AT_MARGIN) > 0) {
                throw new ValidationFailure("postingText.createdAt.out-of-range");
            }
        }
        OperationsValidator.validateOperations(postingText::getPrincipal, OperationsValidator.POSTING_OPERATIONS,
                false, "postingText.operations.wrong-principal");
        OperationsValidator.validateOperations(postingText::getCommentPrincipal, OperationsValidator.COMMENT_OPERATIONS,
                true, "postingText.commentOperations.wrong-principal");
        OperationsValidator.validateOperations(postingText::getReactionPrincipal,
                OperationsValidator.POSTING_REACTION_OPERATIONS, true,
                "postingText.reactionOperations.wrong-principal");
        OperationsValidator.validateOperations(postingText::getCommentReactionPrincipal,
                OperationsValidator.COMMENT_REACTION_OPERATIONS, true,
                "postingText.commentReactionOperations.wrong-principal");
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

    @GetMapping("/{id}")
    @Transactional
    public PostingInfo get(@PathVariable UUID id, @RequestParam(required = false) String include) {
        log.info("GET /postings/{id}, (id = {}, include = {})", LogUtil.format(id), LogUtil.format(include));

        Set<String> includeSet = Util.setParam(include);

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        List<Story> stories = storyRepository.findByEntryId(requestContext.nodeId(), id);
        if (!requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)
                && !feedOperations.isSheriffAllowed(stories, posting.getViewE())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }

        requestContext.send(new PostingReadLiberin(id));

        return withSheriffUserListMarks(withBlockings(withClientReaction(
                new PostingInfo(posting, stories, entryOperations, includeSet.contains("source"), requestContext,
                        requestContext.getOptions()))));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /postings/{id}, (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        EntryRevision latest = posting.getCurrentRevision();
        if (requestContext.isClient(posting.getOwnerName(), Scope.IDENTIFY)
                && !requestContext.isPrincipal(posting.getDeleteE(), Scope.DELETE_OWN_CONTENT)) {
            throw new AuthenticationException();
        }
        if (!requestContext.isClient(posting.getOwnerName(), Scope.IDENTIFY)
                && !requestContext.isPrincipal(posting.getDeleteE(), Scope.DELETE_OTHERS_CONTENT)) {
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
                        requestContext.nodeId(), posting.getCurrentRevision().getId())
                : entryAttachmentRepository.findReceivedAttachedPostings(
                        requestContext.nodeId(), posting.getCurrentRevision().getId(), posting.getReceiverName());
        return attached.stream()
                .map(p -> withBlockings(withClientReaction(new PostingInfo(p, false, requestContext))))
                .collect(Collectors.toList());
    }

    private PostingInfo withClientReaction(PostingInfo postingInfo) {
        String clientName = requestContext.getClientName(Scope.IDENTIFY);
        if (ObjectUtils.isEmpty(clientName)) {
            return postingInfo;
        }
        if (postingInfo.isOriginal()) {
            Reaction reaction = reactionRepository.findByEntryIdAndOwner(
                    UUID.fromString(postingInfo.getId()), clientName);
            if (reaction != null
                    && (reaction.getViewE().isPublic() || requestContext.hasAuthScope(Scope.VIEW_CONTENT))) {
                postingInfo.setClientReaction(new ClientReactionInfo(reaction));
            }
        } else if (requestContext.isAdmin(Scope.VIEW_CONTENT)) {
            // TODO to see public reactions, we need to store the reaction's view principal in OwnReaction
            OwnReaction ownReaction = ownReactionRepository.findByRemotePostingId(
                    requestContext.nodeId(), postingInfo.getReceiverName(), postingInfo.getReceiverPostingId())
                    .orElse(null);
            postingInfo.setClientReaction(ownReaction != null ? new ClientReactionInfo(ownReaction) : null);
        }
        return postingInfo;
    }

    private PostingInfo withBlockings(PostingInfo postingInfo) {
        if (postingInfo.isOriginal()) {
            postingInfo.putBlockedOperations(
                    blockedUserOperations.findBlockedOperations(UUID.fromString(postingInfo.getId())));
        } else if (requestContext.isAdmin(Scope.IDENTIFY)) {
            postingInfo.putBlockedOperations(
                    blockedByUserOperations.findBlockedOperations(
                            postingInfo.getReceiverName(), postingInfo.getReceiverPostingId()));
        }
        return postingInfo;
    }

    private PostingInfo withSheriffUserListMarks(PostingInfo postingInfo) {
        userListOperations.fillSheriffListMarks(postingInfo);
        return postingInfo;
    }

}
