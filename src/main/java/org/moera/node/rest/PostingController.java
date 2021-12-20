package org.moera.node.rest;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.MediaFileOwner;
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
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.BodyMappingException;
import org.moera.node.model.ClientReactionInfo;
import org.moera.node.model.FeedReference;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingFeatures;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingSubscriptionsInfo;
import org.moera.node.model.PostingText;
import org.moera.node.model.Result;
import org.moera.node.model.StoryAttributes;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.event.PostingAddedEvent;
import org.moera.node.model.event.PostingUpdatedEvent;
import org.moera.node.model.notification.FeedPostingAddedNotification;
import org.moera.node.model.notification.PostingImportantUpdateNotification;
import org.moera.node.model.notification.PostingUpdatedNotification;
import org.moera.node.notification.send.Directions;
import org.moera.node.operations.PostingOperations;
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
    private EntityManager entityManager;

    @Inject
    private PostingOperations postingOperations;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private TextConverter textConverter;

    private int getMaxPostingSize() {
        return requestContext.getOptions().getInt("posting.max-size");
    }

    @GetMapping("/features")
    public PostingFeatures getFeatures() {
        log.info("GET /postings/features");

        return new PostingFeatures(requestContext.getOptions());
    }

    @PostMapping
    @Admin
    @Entitled
    @Transactional
    public ResponseEntity<PostingInfo> post(@Valid @RequestBody PostingText postingText) {
        log.info("POST /postings (bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(postingText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(postingText.getBodySrcFormat())));

        mediaOperations.validateAvatar(
                postingText.getOwnerAvatar(),
                postingText::setOwnerAvatarMediaFile,
                () -> new ValidationFailure("postingText.ownerAvatar.mediaId.not-found"));
        if (ObjectUtils.isEmpty(postingText.getBodySrc()) && ObjectUtils.isEmpty(postingText.getMedia())) {
            throw new ValidationFailure("postingText.bodySrc.blank");
        }
        if (postingText.getBodySrc().length() > getMaxPostingSize()) {
            throw new ValidationFailure("postingText.bodySrc.wrong-size");
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
                    revision -> postingText.toEntryRevision(revision, textConverter, media));
        } catch (BodyMappingException e) {
            throw new ValidationFailure("postingText.bodySrc.wrong-encoding");
        }
        requestContext.send(new PostingAddedEvent(posting));

        if (postingText.getPublications() != null) {
            final UUID postingId = posting.getId();
            postingText.getPublications().stream()
                    .map(StoryAttributes::getFeedName)
                    .forEach(fn -> requestContext.send(Directions.feedSubscribers(fn),
                            new FeedPostingAddedNotification(fn, postingId)));
        }

        return ResponseEntity.created(URI.create("/postings/" + posting.getId()))
                .body(withStories(new PostingInfo(posting, true)));
    }

    @PutMapping("/{id}")
    @Admin
    @Entitled
    @Transactional
    public PostingInfo put(@PathVariable UUID id, @Valid @RequestBody PostingText postingText) {
        log.info("PUT /postings/{id}, (id = {}, bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(id),
                LogUtil.format(postingText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(postingText.getBodySrcFormat())));

        mediaOperations.validateAvatar(
                postingText.getOwnerAvatar(),
                postingText::setOwnerAvatarMediaFile,
                () -> new ValidationFailure("postingText.ownerAvatar.mediaId.not-found"));
        if (postingText.getBodySrc() != null && postingText.getBodySrc().length() > getMaxPostingSize()) {
            throw new ValidationFailure("postingText.bodySrc.wrong-size");
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

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!posting.isOriginal()) {
            throw new ValidationFailure("posting.not-original");
        }
        entityManager.lock(posting, LockModeType.PESSIMISTIC_WRITE);
        postingText.toEntry(posting);
        try {
            posting = postingOperations.createOrUpdatePosting(posting, posting.getCurrentRevision(), media,
                    null, postingText::sameAsRevision,
                    revision -> postingText.toEntryRevision(revision, textConverter, media));
        } catch (BodyMappingException e) {
            throw new ValidationFailure("postingText.bodySrc.wrong-encoding");
        }
        requestContext.send(new PostingUpdatedEvent(posting));
        requestContext.send(
                Directions.postingSubscribers(posting.getId()),
                new PostingUpdatedNotification(posting.getId()));
        if (posting.getCurrentRevision().isUpdateImportant()) {
            requestContext.send(
                    Directions.postingCommentsSubscribers(posting.getId()),
                    new PostingImportantUpdateNotification(posting.getId(), posting.getCurrentRevision().getHeading(),
                            posting.getCurrentRevision().getUpdateDescription()));
        }

        return withSubscribers(withStories(withClientReaction(new PostingInfo(posting, true))));
    }

    @GetMapping("/{id}")
    @Transactional
    public PostingInfo get(@PathVariable UUID id, @RequestParam(required = false) String include) {
        log.info("GET /postings/{id}, (id = {}, include = {})", LogUtil.format(id), LogUtil.format(include));

        Set<String> includeSet = Util.setParam(include);

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));

        return withSubscribers(withStories(withClientReaction(new PostingInfo(posting, includeSet.contains("source"),
                requestContext.isAdmin() || requestContext.isClient(posting.getOwnerName())))));
    }

    @DeleteMapping("/{id}")
    @Admin
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /postings/{id}, (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        entityManager.lock(posting, LockModeType.PESSIMISTIC_WRITE);
        postingOperations.deletePosting(posting, true);
        storyOperations.unpublish(posting.getId());

        return Result.OK;
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
