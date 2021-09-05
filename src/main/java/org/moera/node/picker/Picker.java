package org.moera.node.picker;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.api.NodeApiErrorStatusException;
import org.moera.node.api.NodeApiException;
import org.moera.node.api.NodeApiNotFoundException;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.EntrySource;
import org.moera.node.data.EntrySourceRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.Pick;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.ReactionTotalRepository;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.media.MediaManager;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StoryAttributes;
import org.moera.node.model.SubscriberDescriptionQ;
import org.moera.node.model.SubscriberInfo;
import org.moera.node.model.WhoAmI;
import org.moera.node.model.event.Event;
import org.moera.node.model.event.PostingAddedEvent;
import org.moera.node.model.event.PostingRestoredEvent;
import org.moera.node.model.event.PostingUpdatedEvent;
import org.moera.node.model.event.SubscriptionAddedEvent;
import org.moera.node.model.notification.FeedPostingAddedNotification;
import org.moera.node.model.notification.PostingUpdatedNotification;
import org.moera.node.notification.send.DirectedNotification;
import org.moera.node.notification.send.Directions;
import org.moera.node.notification.send.NotificationSenderPool;
import org.moera.node.operations.ReactionTotalOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.task.Task;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

public class Picker extends Task {

    private static Logger log = LoggerFactory.getLogger(Picker.class);

    private String remoteNodeName;
    private String remoteFullName;
    private MediaFile remoteAvatarMediaFile;
    private String remoteAvatarShape;
    private BlockingQueue<Pick> queue = new LinkedBlockingQueue<>();
    private boolean stopped = false;
    private PickerPool pool;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private ReactionTotalRepository reactionTotalRepository;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private EntrySourceRepository entrySourceRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    @Inject
    private MediaManager mediaManager;

    @Inject
    private NotificationSenderPool notificationSenderPool;

    public Picker(PickerPool pool, String remoteNodeName) {
        this.pool = pool;
        this.remoteNodeName = remoteNodeName;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void put(@NotNull Pick pick) throws InterruptedException {
        queue.put(pick);
    }

    @Override
    protected void execute() {
        try {
            fetchNodeDetails();
            while (!stopped) {
                Pick pick;
                try {
                    pick = queue.poll(1, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    continue;
                }
                if (pick == null) {
                    stopped = true;
                    if (!queue.isEmpty()) { // queue may receive content before the previous statement
                        stopped = false;
                    }
                } else {
                    try {
                        download(pick);
                    } catch (Throwable e) {
                        failed(pick, e);
                        throw e;
                    }
                }
            }
        } catch (Throwable e) {
            error(e);
        } finally {
            pool.deletePicker(nodeId, remoteNodeName);
        }
    }

    private void fetchNodeDetails() throws NodeApiException {
        WhoAmI remote = nodeApi.whoAmI(remoteNodeName);
        remoteFullName = remote.getFullName();
        remoteAvatarMediaFile = mediaManager.downloadPublicMedia(remoteNodeName, remote.getAvatar());
        remoteAvatarShape = remote.getAvatar() != null ? remote.getAvatar().getShape() : null;
    }

    private void download(Pick pick) throws Throwable {
        log.info("Downloading from node '{}', postingId = {}", remoteNodeName, pick.getRemotePostingId());

        List<Event> events = new ArrayList<>();
        List<DirectedNotification> notifications = new ArrayList<>();
        Posting posting = inTransaction(() -> {
            Posting p = downloadPosting(pick.getRemotePostingId(), pick.getFeedName(), events, notifications);
            saveSources(p, pick);
            return p;
        });
        events.forEach(event -> eventManager.send(nodeId, event));
        notifications.forEach(
                dn -> notificationSenderPool.send(dn.getDirection().nodeId(nodeId), dn.getNotification()));

        succeeded(posting, pick);
    }

    private Posting downloadPosting(String remotePostingId, String feedName, List<Event> events,
                                    List<DirectedNotification> notifications) throws NodeApiException {
        PostingInfo postingInfo = nodeApi.getPosting(remoteNodeName, remotePostingId);
        MediaFile ownerAvatar = mediaManager.downloadPublicMedia(remoteNodeName, postingInfo.getOwnerAvatar());
        String receiverName = postingInfo.isOriginal() ? remoteNodeName : postingInfo.getReceiverName();
        String receiverFullName = postingInfo.isOriginal()
                ? postingInfo.getOwnerFullName() : postingInfo.getReceiverFullName();
        MediaFile receiverAvatar = postingInfo.isOriginal()
                ? ownerAvatar
                : mediaManager.downloadPublicMedia(remoteNodeName, postingInfo.getReceiverAvatar());
        String receiverAvatarShape;
        if (postingInfo.isOriginal()) {
            receiverAvatarShape = postingInfo.getOwnerAvatar() != null
                    ? postingInfo.getOwnerAvatar().getShape() : null;
        } else {
            receiverAvatarShape = postingInfo.getReceiverAvatar() != null
                    ? postingInfo.getReceiverAvatar().getShape() : null;
        }
        String receiverPostingId = postingInfo.isOriginal() ? remotePostingId : postingInfo.getReceiverPostingId();
        Posting posting = postingRepository.findByReceiverId(nodeId, receiverName, receiverPostingId).orElse(null);
        if (posting == null) {
            posting = new Posting();
            posting.setId(UUID.randomUUID());
            posting.setNodeId(nodeId);
            posting.setReceiverName(receiverName);
            posting.setReceiverFullName(receiverFullName);
            posting.setOwnerAvatarMediaFile(ownerAvatar);
            posting = postingRepository.save(posting);
            postingInfo.toPickedPosting(posting);
            updateRevision(posting, postingInfo);
            subscribe(receiverName, receiverFullName, receiverAvatar, receiverAvatarShape, receiverPostingId,
                    posting.getReceiverEditedAt(), events);
            events.add(new PostingAddedEvent(posting));
            notifications.add(new DirectedNotification(
                    Directions.feedSubscribers(feedName),
                    new FeedPostingAddedNotification(feedName, posting.getId())));
            publish(feedName, posting, events);
        } else if (!postingInfo.getEditedAt().equals(Util.toEpochSecond(posting.getEditedAt()))) {
            posting.setOwnerAvatarMediaFile(ownerAvatar);
            postingInfo.toPickedPosting(posting);
            updateRevision(posting, postingInfo);
            if (posting.getDeletedAt() == null) {
                events.add(new PostingUpdatedEvent(posting));
            } else {
                posting.setDeletedAt(null);
                events.add(new PostingRestoredEvent(posting));
            }
            notifications.add(new DirectedNotification(
                    Directions.postingSubscribers(posting.getId()),
                    new PostingUpdatedNotification(posting.getId())));
        }
        var reactionTotals = reactionTotalRepository.findAllByEntryId(posting.getId());
        if (!reactionTotalOperations.isSame(reactionTotals, postingInfo.getReactions())) {
            reactionTotalOperations.replaceAll(posting, postingInfo.getReactions());
        }
        return posting;
    }

    private void updateRevision(Posting posting, PostingInfo postingInfo) {
        if (postingInfo.getRevisionId().equals(posting.getCurrentReceiverRevisionId())) {
            return;
        }

        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setEntry(posting);
        revision = entryRevisionRepository.save(revision);
        posting.addRevision(revision);
        postingInfo.toPickedEntryRevision(revision);
        PostingFingerprint fingerprint = new PostingFingerprint(posting, revision);
        revision.setDigest(CryptoUtil.digest(fingerprint));
        posting.setTotalRevisions(posting.getTotalRevisions() + 1);

        if (posting.getCurrentRevision() != null) {
            posting.getCurrentRevision().setDeletedAt(Util.now());
            if (posting.getCurrentRevision().getReceiverDeletedAt() == null) {
                posting.getCurrentRevision().setReceiverDeletedAt(Util.toTimestamp(postingInfo.getRevisionCreatedAt()));
            }
        }
        posting.setCurrentRevision(revision);
        posting.setCurrentReceiverRevisionId(revision.getReceiverRevisionId());
    }

    private void publish(String feedName, Posting posting, List<Event> events) {
        int totalStories = storyRepository.countByFeedAndTypeAndEntryId(nodeId, feedName, StoryType.POSTING_ADDED,
                posting.getId());
        if (totalStories > 0) {
            return;
        }
        StoryAttributes publication = new StoryAttributes();
        publication.setFeedName(feedName);
        storyOperations.publish(posting, Collections.singletonList(publication), nodeId, events::add);
    }

    private void subscribe(String receiverName, String receiverFullName, MediaFile receiverAvatar,
                           String receiverAvatarShape, String receiverPostingId, Timestamp lastUpdatedAt,
                           List<Event> events) throws NodeApiException {
        SubscriberDescriptionQ description = new SubscriberDescriptionQ(SubscriptionType.POSTING, null,
                receiverPostingId, fullName(), getAvatar(), Util.toEpochSecond(lastUpdatedAt));
        try {
            SubscriberInfo subscriberInfo =
                    nodeApi.postSubscriber(receiverName, generateCarte(receiverName), description);
            Subscription subscription = new Subscription();
            subscription.setId(UUID.randomUUID());
            subscription.setNodeId(nodeId);
            subscription.setSubscriptionType(SubscriptionType.POSTING);
            subscription.setRemoteSubscriberId(subscriberInfo.getId());
            subscription.setRemoteNodeName(receiverName);
            subscription.setRemoteFullName(receiverFullName);
            if (receiverAvatar != null) {
                subscription.setRemoteAvatarMediaFile(receiverAvatar);
                subscription.setRemoteAvatarShape(receiverAvatarShape);
            }
            subscription.setRemoteEntryId(receiverPostingId);
            subscription = subscriptionRepository.save(subscription);
            events.add(new SubscriptionAddedEvent(subscription));
        } catch (NodeApiErrorStatusException e) {
            if (!e.getResult().getErrorCode().equals("subscriber.already-exists")) {
                throw e;
            }
        }
    }

    private void saveSources(Posting posting, Pick pick) {
        if (ObjectUtils.isEmpty(pick.getRemoteFeedName())) {
            return;
        }
        List<EntrySource> sources = entrySourceRepository.findAllByEntryId(posting.getId());
        if (sources.stream().anyMatch(pick::isSame)) {
            return;
        }
        EntrySource entrySource = new EntrySource();
        entrySource.setId(UUID.randomUUID());
        entrySource.setEntry(posting);
        entrySource.setRemoteFullName(remoteFullName);
        entrySource.setRemoteAvatarMediaFile(remoteAvatarMediaFile);
        entrySource.setRemoteAvatarShape(remoteAvatarShape);
        pick.toEntrySource(entrySource);
        entrySourceRepository.save(entrySource);
    }

    private void succeeded(Posting posting, Pick pick) {
        log.info("Posting downloaded successfully, id = {}", posting.getId());
        pool.pickSucceeded(pick);
    }

    private void error(Throwable e) {
        log.error(e.getMessage());
    }

    private void failed(Pick pick, Throwable e) {
        boolean fatal = e instanceof NodeApiNotFoundException;
        pool.pickFailed(pick, fatal);
    }

}
