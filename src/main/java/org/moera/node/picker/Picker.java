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
import org.moera.node.api.NodeApiException;
import org.moera.node.api.NodeApiNotFoundException;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.EntrySource;
import org.moera.node.data.EntrySourceRepository;
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
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.StoryAttributes;
import org.moera.node.model.SubscriberDescriptionQ;
import org.moera.node.model.SubscriberInfo;
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
import org.springframework.util.StringUtils;

public class Picker extends Task {

    private static Logger log = LoggerFactory.getLogger(Picker.class);

    private String remoteNodeName;
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
    public void run() {
        try {
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

    private void download(Pick pick) throws Throwable {
        initLoggingDomain();
        log.info("Downloading from node '{}', postingId = {}", remoteNodeName, pick.getRemotePostingId());

        nodeApi.setNodeId(nodeId);

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
        String receiverName = postingInfo.isOriginal() ? remoteNodeName : postingInfo.getReceiverName();
        String receiverPostingId = postingInfo.isOriginal() ? remotePostingId : postingInfo.getReceiverPostingId();
        Posting posting = postingRepository.findByReceiverId(nodeId, receiverName, receiverPostingId).orElse(null);
        if (posting == null) {
            posting = new Posting();
            posting.setId(UUID.randomUUID());
            posting.setNodeId(nodeId);
            posting.setReceiverName(receiverName);
            posting = postingRepository.save(posting);
            postingInfo.toPickedPosting(posting);
            downloadRevisions(posting);
            subscribe(receiverName, receiverPostingId, posting.getEditedAt(), events);
            events.add(new PostingAddedEvent(posting));
            notifications.add(new DirectedNotification(
                    Directions.feedSubscribers(feedName),
                    new FeedPostingAddedNotification(feedName, posting.getId())));
            publish(feedName, posting, events);
        } else if (!postingInfo.getEditedAt().equals(Util.toEpochSecond(posting.getEditedAt()))) {
            postingInfo.toPickedPosting(posting);
            downloadRevisions(posting);
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

    private void downloadRevisions(Posting posting) throws NodeApiException {
        PostingRevisionInfo[] revisionInfos = nodeApi.getPostingRevisions(remoteNodeName, posting.getReceiverEntryId());
        EntryRevision currentRevision = null;
        for (PostingRevisionInfo revisionInfo : revisionInfos) {
            if (revisionInfo.getId().equals(posting.getCurrentReceiverRevisionId())) {
                if (revisionInfo.getDeletedAt() == null) {
                    currentRevision = posting.getCurrentRevision();
                } else {
                    posting.getCurrentRevision().setDeletedAt(Util.now());
                    posting.getCurrentRevision().setReceiverDeletedAt(Util.toTimestamp(revisionInfo.getDeletedAt()));
                }
                break;
            }
            EntryRevision revision = new EntryRevision();
            revision.setId(UUID.randomUUID());
            revision.setEntry(posting);
            revision = entryRevisionRepository.save(revision);
            posting.addRevision(revision);
            revisionInfo.toPickedEntryRevision(revision);
            PostingFingerprint fingerprint = new PostingFingerprint(posting, revision);
            revision.setDigest(CryptoUtil.digest(fingerprint));
            if (revisionInfo.getDeletedAt() == null) {
                currentRevision = revision;
            }
            posting.setTotalRevisions(posting.getTotalRevisions() + 1);
        }
        posting.setCurrentRevision(currentRevision);
        if (currentRevision != null) {
            posting.setCurrentReceiverRevisionId(currentRevision.getReceiverRevisionId());
        }
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

    private void subscribe(String receiverName, String receiverPostingId, Timestamp lastUpdatedAt, List<Event> events)
            throws NodeApiException {

        SubscriberDescriptionQ description = new SubscriberDescriptionQ(SubscriptionType.POSTING, null,
                receiverPostingId, Util.toEpochSecond(lastUpdatedAt));
        SubscriberInfo subscriberInfo = nodeApi.postSubscriber(receiverName, generateCarte(), description);
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setNodeId(nodeId);
        subscription.setSubscriptionType(SubscriptionType.POSTING);
        subscription.setRemoteSubscriberId(subscriberInfo.getId());
        subscription.setRemoteNodeName(receiverName);
        subscription.setRemoteEntryId(receiverPostingId);
        subscription = subscriptionRepository.save(subscription);
        events.add(new SubscriptionAddedEvent(subscription));
    }

    private void saveSources(Posting posting, Pick pick) {
        if (StringUtils.isEmpty(pick.getRemoteFeedName())) {
            return;
        }
        List<EntrySource> sources = entrySourceRepository.findAllByEntryId(posting.getId());
        if (sources.stream().anyMatch(pick::isSame)) {
            return;
        }
        EntrySource entrySource = new EntrySource();
        entrySource.setId(UUID.randomUUID());
        entrySource.setEntry(posting);
        pick.toEntrySource(entrySource);
        entrySourceRepository.save(entrySource);
    }

    private void succeeded(Posting posting, Pick pick) {
        initLoggingDomain();
        log.info("Posting downloaded successfully, id = {}", posting.getId());
        pool.pickSucceeded(pick);
    }

    private void error(Throwable e) {
        initLoggingDomain();
        log.error(e.getMessage());
    }

    private void failed(Pick pick, Throwable e) {
        boolean fatal = e instanceof NodeApiNotFoundException;
        pool.pickFailed(pick, fatal);
    }

}
