package org.moera.node.rest;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
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
import org.moera.node.auth.principal.Principal;
import org.moera.node.auth.principal.PrincipalFlag;
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
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.fingerprint.Fingerprints;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.PostingAddedLiberin;
import org.moera.node.liberin.model.PostingDeletedLiberin;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.ClientReactionInfo;
import org.moera.node.model.FeedReference;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingFeatures;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingSubscriptionsInfo;
import org.moera.node.model.PostingText;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.body.BodyMappingException;
import org.moera.node.naming.NamingCache;
import org.moera.node.operations.PostingOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
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

    private static final List<Pair<String, Integer>> OPERATION_PRINCIPALS = List.of(
        Pair.of("view",
                PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.PRIVATE),
        Pair.of("viewComments",
                PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
        Pair.of("addComment",
                PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
        Pair.of("viewReactions",
                PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
        Pair.of("viewNegativeReactions",
                PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
        Pair.of("viewReactionTotals",
                PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
        Pair.of("viewNegativeReactionTotals",
                PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
        Pair.of("viewReactionRatios",
                PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
        Pair.of("viewNegativeReactionRatios",
                PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
        Pair.of("addReaction",
                PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.NONE),
        Pair.of("addNegativeReaction",
                PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.NONE)
    );

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
    private SubscriberRepository subscriberRepository;

    @Inject
    private SubscriptionRepository subscriptionRepository;

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
    private MediaOperations mediaOperations;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private TextConverter textConverter;

    @Inject
    private NamingCache namingCache;

    private int getMaxPostingSize() {
        return requestContext.getOptions().getInt("posting.max-size");
    }

    @GetMapping("/features")
    public PostingFeatures getFeatures() {
        log.info("GET /postings/features");

        return new PostingFeatures(requestContext.getOptions());
    }

    @PostMapping
    @Entitled
    @Transactional
    public ResponseEntity<PostingInfo> post(@Valid @RequestBody PostingText postingText) {
        log.info("POST /postings (bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(postingText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(postingText.getBodySrcFormat())));

        if (!requestContext.isAdmin() && !requestContext.getOptions().getBool("posting.non-admin.allowed")) {
            throw new AuthenticationException();
        }
        mediaOperations.validateAvatar(
                postingText.getOwnerAvatar(),
                postingText::setOwnerAvatarMediaFile,
                () -> new ValidationFailure("postingText.ownerAvatar.mediaId.not-found"));
        byte[] digest = validatePostingText(null, postingText, postingText.getOwnerName());
        if (postingText.getSignature() != null) {
            requestContext.authenticatedWithSignature(postingText.getOwnerName());
        }
        List<MediaFileOwner> media = mediaOperations.validateAttachments(
                postingText.getMedia(),
                () -> new ValidationFailure("postingText.media.not-found"),
                () -> new ValidationFailure("postingText.media.not-compressed"),
                requestContext.isAdmin(),
                requestContext.getClientName());

        Posting posting = postingOperations.newPosting(postingText);
        try {
            posting = postingOperations.createOrUpdatePosting(posting, null, media,
                    postingText.getPublications(), null,
                    revision -> postingText.toEntryRevision(revision, digest, textConverter, media),
                    postingText::toEntry);
        } catch (BodyMappingException e) {
            throw new ValidationFailure("postingText.bodySrc.wrong-encoding");
        }

        requestContext.send(new PostingAddedLiberin(posting));

        return ResponseEntity.created(URI.create("/postings/" + posting.getId()))
                .body(withStories(new PostingInfo(posting, requestContext)));
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
        Principal latestView = posting.getViewPrincipalAbsolute();
        EntryRevision latest = posting.getCurrentRevision();
        mediaOperations.validateAvatar(
                postingText.getOwnerAvatar(),
                postingText::setOwnerAvatarMediaFile,
                () -> new ValidationFailure("postingText.ownerAvatar.mediaId.not-found"));
        byte[] digest = validatePostingText(posting, postingText, posting.getOwnerName());
        if (postingText.getSignature() != null) {
            requestContext.authenticatedWithSignature(postingText.getOwnerName());
        }
        if (!requestContext.isPrincipal(posting.getViewPrincipalAbsolute())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (postingText.getPublications() != null && !postingText.getPublications().isEmpty()) {
            throw new ValidationFailure("postingText.publications.cannot-modify");
        }
        List<MediaFileOwner> media = mediaOperations.validateAttachments(
                postingText.getMedia(),
                () -> new ValidationFailure("postingText.media.not-found"),
                () -> new ValidationFailure("postingText.media.not-compressed"),
                requestContext.isAdmin(),
                requestContext.getClientName());

        entityManager.lock(posting, LockModeType.PESSIMISTIC_WRITE);
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

        requestContext.send(new PostingUpdatedLiberin(posting, latest, latestView));

        return withSubscribers(withStories(withClientReaction(new PostingInfo(posting, requestContext))));
    }

    private byte[] validatePostingText(Posting posting, PostingText postingText, String ownerName) {
        byte[] digest = null;
        if (postingText.getSignature() == null) {
            if (!requestContext.isAdmin()) {
                String clientName = requestContext.getClientName();
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

            if (ObjectUtils.isEmpty(postingText.getBodySrc()) && ObjectUtils.isEmpty(postingText.getMedia())) {
                throw new ValidationFailure("postingText.bodySrc.blank");
            }
            if (postingText.getBodySrc().length() > getMaxPostingSize()) {
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
        validateOperations(postingText::getPrincipal, "postingText.operations.wrong-principal");
        return digest;
    }

    private void validateOperations(Function<String, Principal> getPrincipal, String errorCode) {
        for (var desc : OPERATION_PRINCIPALS) {
            Principal principal = getPrincipal.apply(desc.getFirst());
            if (principal != null && !principal.isOneOf(desc.getSecond())) {
                throw new ValidationFailure(errorCode);
            }
        }
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
        if (!requestContext.isPrincipal(posting.getViewPrincipalAbsolute())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }

        return withSubscribers(withStories(withClientReaction(new PostingInfo(posting, includeSet.contains("source"),
                requestContext))));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /postings/{id}, (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        EntryRevision latest = posting.getCurrentRevision();
        if (!requestContext.isPrincipal(posting.getDeletePrincipalAbsolute())) {
            throw new AuthenticationException();
        }
        entityManager.lock(posting, LockModeType.PESSIMISTIC_WRITE);
        postingOperations.deletePosting(posting, true);
        storyOperations.unpublish(posting.getId());

        requestContext.send(new PostingDeletedLiberin(posting, latest));

        return Result.OK;
    }

    @GetMapping("/{id}/operations")
    @Transactional
    public Map<String, Principal> getOperations(@PathVariable UUID id) {
        log.info("GET /postings/{id}/operations, (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!requestContext.isPrincipal(posting.getViewPrincipalAbsolute())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }

        return new PostingInfo(posting, requestContext).getOperations();
    }

    @PutMapping("/{id}/operations")
    @Transactional
    public Map<String, Principal> putOperations(@PathVariable UUID id,
                                                @Valid @RequestBody Map<String, Principal> operations) {
        log.info("PUT /postings/{id}/operations, (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        Principal latestView = posting.getViewPrincipalAbsolute();
        if (!requestContext.isPrincipal(posting.getEditPrincipalAbsolute())) {
            throw new AuthenticationException();
        }

        if (posting.getParentMedia() != null) {
            throw new ValidationFailure("posting-operations.attached-to-media");
        }
        validateOperations(operations::get, "posting-operations.wrong-principal");

        posting.setEditedAt(Util.now());

        requestContext.send(new PostingUpdatedLiberin(posting, posting.getCurrentRevision(), latestView));

        return new PostingInfo(posting, requestContext).getOperations();
    }

    @GetMapping("/{id}/attached")
    @Transactional
    public List<PostingInfo> getAttached(@PathVariable UUID id) {
        log.info("GET /postings/{id}/attached, (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!requestContext.isPrincipal(posting.getViewPrincipalAbsolute())) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        List<Posting> attached = posting.isOriginal()
                ? entryAttachmentRepository.findOwnAttachedPostings(
                        requestContext.nodeId(), posting.getCurrentRevision().getId())
                : entryAttachmentRepository.findReceivedAttachedPostings(
                        requestContext.nodeId(), posting.getCurrentRevision().getId(), posting.getReceiverName());
        return attached.stream()
                .map(p -> withClientReaction(new PostingInfo(p, false, requestContext)))
                .collect(Collectors.toList());
    }

    private PostingInfo withClientReaction(PostingInfo postingInfo) {
        String clientName = requestContext.getClientName();
        if (ObjectUtils.isEmpty(clientName)) {
            return postingInfo;
        }
        if (postingInfo.isOriginal()) {
            Reaction reaction = reactionRepository.findByEntryIdAndOwner(
                    UUID.fromString(postingInfo.getId()), clientName);
            postingInfo.setClientReaction(reaction != null ? new ClientReactionInfo(reaction) : null);
        } else if (requestContext.isAdmin()) {
            OwnReaction ownReaction = ownReactionRepository.findByRemotePostingId(
                    requestContext.nodeId(), postingInfo.getReceiverName(), postingInfo.getReceiverPostingId())
                    .orElse(null);
            postingInfo.setClientReaction(ownReaction != null ? new ClientReactionInfo(ownReaction) : null);
        }
        return postingInfo;
    }

    private PostingInfo withStories(PostingInfo postingInfo) {
        List<Story> stories = storyRepository.findByEntryId(requestContext.nodeId(),
                UUID.fromString(postingInfo.getId()));
        if (stories != null && !stories.isEmpty()) {
            postingInfo.setFeedReferences(stories.stream().map(FeedReference::new).collect(Collectors.toList()));
        }
        return postingInfo;
    }

    private PostingInfo withSubscribers(PostingInfo postingInfo) {
        String clientName = requestContext.getClientName();
        if (ObjectUtils.isEmpty(clientName)) {
            return postingInfo;
        }
        if (postingInfo.isOriginal()) {
            Set<Subscriber> subscribers = subscriberRepository.findByEntryId(requestContext.nodeId(), clientName,
                    UUID.fromString(postingInfo.getId()));
            postingInfo.setSubscriptions(PostingSubscriptionsInfo.fromSubscribers(subscribers));
        } else if (requestContext.isAdmin()) {
            List<Subscription> subscriptions = subscriptionRepository.findAllByNodeAndEntryId(
                    requestContext.nodeId(), postingInfo.getReceiverName(), postingInfo.getReceiverPostingId());
            postingInfo.setSubscriptions(PostingSubscriptionsInfo.fromSubscriptions(subscriptions));
        }
        return postingInfo;
    }

}
