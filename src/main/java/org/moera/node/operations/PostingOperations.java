package org.moera.node.operations;

import java.security.interfaces.ECPrivateKey;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.BodyFormat;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.Feed;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.SourceFormat;
import org.moera.node.data.Story;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.domain.Domains;
import org.moera.node.event.EventManager;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.global.RequestContext;
import org.moera.node.model.Body;
import org.moera.node.model.PostingText;
import org.moera.node.model.StoryAttributes;
import org.moera.node.model.event.Event;
import org.moera.node.model.event.PostingDeletedEvent;
import org.moera.node.model.notification.MentionPostingAddedNotification;
import org.moera.node.model.notification.MentionPostingDeletedNotification;
import org.moera.node.model.notification.Notification;
import org.moera.node.model.notification.PostingDeletedNotification;
import org.moera.node.notification.send.Direction;
import org.moera.node.notification.send.Directions;
import org.moera.node.notification.send.NotificationSenderPool;
import org.moera.node.option.Options;
import org.moera.node.text.MediaExtractor;
import org.moera.node.text.MentionsExtractor;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class PostingOperations {

    public static final int MAX_POSTINGS_PER_REQUEST = 200;

    private static final Logger log = LoggerFactory.getLogger(PostingOperations.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private Domains domains;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private EntryAttachmentRepository entryAttachmentRepository;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private TimelinePublicPageOperations timelinePublicPageOperations;

    @Inject
    private EventManager eventManager;

    @Inject
    private NotificationSenderPool notificationSenderPool;

    @Inject
    private PlatformTransactionManager txManager;

    private Posting newPosting() {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(requestContext.nodeId());
        posting.setOwnerName(requestContext.nodeName());
        posting.setOwnerFullName(requestContext.fullName());
        if (requestContext.getAvatar() != null) {
            posting.setOwnerAvatarMediaFile(requestContext.getAvatar().getMediaFile());
            posting.setOwnerAvatarShape(requestContext.getAvatar().getShape());
        }
        return postingRepository.save(posting);
    }

    public Posting newPosting(PostingText postingText) {
        postingText.initAcceptedReactionsDefaults();

        Posting posting = newPosting();
        postingText.toEntry(posting);

        return posting;
    }

    public Posting newPosting(MediaFileOwner mediaFileOwner) {
        Posting posting = newPosting();
        posting.setParentMedia(mediaFileOwner);

        EntryRevision revision = newRevision(posting, null);
        revision.setBodySrc(Body.EMPTY);
        revision.setBodySrcFormat(SourceFormat.MARKDOWN);
        revision.setBody(Body.EMPTY);
        revision.setSaneBody(Body.EMPTY);
        revision.setBodyFormat(BodyFormat.MESSAGE.getValue());
        revision.setBodyPreview(Body.EMPTY);
        revision.setSaneBodyPreview(Body.EMPTY);

        return posting;
    }

    public Posting createOrUpdatePosting(Posting posting, EntryRevision revision, List<MediaFileOwner> media,
                                         List<StoryAttributes> publications,
                                         Predicate<EntryRevision> isNothingChanged,
                                         Consumer<EntryRevision> revisionUpdater,
                                         Consumer<Entry> mediaEntryUpdater) {
        EntryRevision latest = posting.getCurrentRevision();
        if (latest != null && isNothingChanged != null && isNothingChanged.test(latest)) {
            return postingRepository.saveAndFlush(posting);
        }

        EntryRevision current = newRevision(posting, revision);
        if (revisionUpdater != null) {
            revisionUpdater.accept(current);
        }

        if (media.size() > 0) {
            Set<String> embedded = MediaExtractor.extractMediaFileIds(new Body(current.getBody()).getText());
            int ordinal = 0;
            for (MediaFileOwner mfo : media) {
                EntryAttachment attachment = new EntryAttachment(current, mfo, ordinal++);
                attachment.setEmbedded(embedded.contains(mfo.getMediaFile().getId()));
                attachment = entryAttachmentRepository.save(attachment);
                current.addAttachment(attachment);

                if (mediaEntryUpdater != null) {
                    mediaEntryUpdater.accept(mfo.getPosting());
                }
            }
        }

        posting.setEditedAt(Util.now());
        posting = postingRepository.saveAndFlush(posting);
        storyOperations.publish(posting, publications);

        current = posting.getCurrentRevision();
        PostingFingerprint fingerprint = new PostingFingerprint(posting, current);
        current.setDigest(CryptoUtil.digest(fingerprint));
        current.setSignature(CryptoUtil.sign(fingerprint, getSigningKey()));
        current.setSignatureVersion(PostingFingerprint.VERSION);

        Story timelineStory = posting.getStory(Feed.TIMELINE);
        if (timelineStory != null) {
            timelinePublicPageOperations.updatePublicPages(timelineStory.getMoment());
        }

        notifyMentioned(posting.getId(), current, latest);

        return posting;
    }

    private ECPrivateKey getSigningKey() {
        return (ECPrivateKey) requestContext.getOptions().getPrivateKey("profile.signing-key");
    }

    private EntryRevision newRevision(Posting posting, EntryRevision template) {
        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setEntry(posting);
        revision = entryRevisionRepository.save(revision);

        posting.addRevision(revision);

        if (template == null) {
            posting.setTotalRevisions(1);
        } else {
            revision.setBodyPreview(template.getBodyPreview());
            revision.setSaneBodyPreview(template.getSaneBodyPreview());
            revision.setBodySrc(template.getBodySrc());
            revision.setBodySrcFormat(template.getBodySrcFormat());
            revision.setBody(template.getBody());
            revision.setSaneBody(template.getSaneBody());
            revision.setHeading(template.getHeading());
            revision.setDescription(template.getDescription());

            if (posting.getCurrentRevision().getDeletedAt() == null) {
                posting.getCurrentRevision().setDeletedAt(Util.now());
            }
            posting.setTotalRevisions(posting.getTotalRevisions() + 1);
        }

        posting.setCurrentRevision(revision);

        return revision;
    }

    private void notifyMentioned(UUID postingId, EntryRevision current, EntryRevision latest) {
        Set<String> currentMentions = MentionsExtractor.extract(new Body(current.getBody()));
        Set<String> latestMentions = latest != null
                ? MentionsExtractor.extract(new Body(latest.getBody()))
                : Collections.emptySet();
        notifyMentioned(postingId, current.getHeading(), currentMentions, latestMentions, requestContext.nodeName(),
                requestContext::send);
    }

    private void notifyMentioned(UUID postingId, String currentHeading, Set<String> currentMentions,
                                 Set<String> latestMentions, String nodeName,
                                 BiConsumer<Direction, Notification> notificationSender) {
        currentMentions.stream()
                .filter(m -> !m.equals(nodeName))
                .filter(m -> !m.equals(":"))
                .filter(m -> !latestMentions.contains(m))
                .map(Directions::single)
                .forEach(d -> notificationSender.accept(d,
                        new MentionPostingAddedNotification(postingId, currentHeading)));
        latestMentions.stream()
                .filter(m -> !m.equals(nodeName))
                .filter(m -> !m.equals(":"))
                .filter(m -> !currentMentions.contains(m))
                .map(Directions::single)
                .forEach(d -> notificationSender.accept(d, new MentionPostingDeletedNotification(postingId)));
    }

    public void deletePosting(Posting posting, boolean unsubscribe) {
        deletePosting(posting, unsubscribe, requestContext.getOptions(), requestContext::send, requestContext::send);
    }

    private void deletePosting(Posting posting, boolean unsubscribe, Options options, Consumer<Event> eventSender,
                               BiConsumer<Direction, Notification> notificationSender) {
        posting.setDeletedAt(Util.now());
        ExtendedDuration postingTtl = options.getDuration("posting.deleted.lifetime");
        if (!postingTtl.isNever()) {
            posting.setDeadline(Timestamp.from(Instant.now().plus(postingTtl.getDuration())));
        }

        if (posting.getCurrentRevision() != null) {
            posting.getCurrentRevision().setDeletedAt(Util.now());

            if (posting.isOriginal()) {
                Set<String> latestMentions = MentionsExtractor.extract(new Body(posting.getCurrentRevision().getBody()));
                notifyMentioned(posting.getId(), null, Collections.emptySet(), latestMentions,
                        options.nodeName(), notificationSender);
            }
        }
        if (!posting.isOriginal() && unsubscribe) {
            subscriptionRepository.deleteByTypeAndNodeAndEntryId(options.nodeId(), SubscriptionType.POSTING,
                    posting.getReceiverName(), posting.getReceiverEntryId());
        }

        eventSender.accept(new PostingDeletedEvent(posting));
        notificationSender.accept(Directions.postingSubscribers(posting.getId()),
                new PostingDeletedNotification(posting.getId()));
    }

    @Scheduled(fixedDelayString = "PT1H")
    public void deleteUnlinked() throws Throwable {
        for (String domainName : domains.getAllDomainNames()) {
            Options options = domains.getDomainOptions(domainName);
            List<Event> eventList = new ArrayList<>();
            List<Pair<Direction, Notification>> notificationList = new ArrayList<>();
            Transaction.execute(txManager, () -> {
                postingRepository.findUnlinked(options.nodeId()).forEach(posting -> {
                    log.info("Deleting unlinked posting {}", posting.getId());
                    deletePosting(posting, true, options, eventList::add,
                            (direction, notification) -> notificationList.add(Pair.of(direction, notification)));
                });
                return null;
            });
            eventList.forEach(event -> eventManager.send(options.nodeId(), event));
            notificationList.forEach(nt -> {
                nt.getFirst().setNodeId(options.nodeId());
                notificationSenderPool.send(nt.getFirst(), nt.getSecond());
            });
        }
    }

}
