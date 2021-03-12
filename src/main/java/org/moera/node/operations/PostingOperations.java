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
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Story;
import org.moera.node.domain.Domains;
import org.moera.node.event.EventManager;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.global.RequestContext;
import org.moera.node.model.AcceptedReactions;
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

    private static Logger log = LoggerFactory.getLogger(PostingOperations.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private Domains domains;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

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

    public Posting newPosting(PostingText postingText, Consumer<Posting> initializer) {
        if (postingText.getAcceptedReactions() == null) {
            postingText.setAcceptedReactions(new AcceptedReactions());
        }
        if (postingText.getAcceptedReactions().getPositive() == null) {
            postingText.getAcceptedReactions().setPositive("");
        }
        if (postingText.getAcceptedReactions().getNegative() == null) {
            postingText.getAcceptedReactions().setNegative("");
        }

        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(requestContext.nodeId());
        posting.setOwnerName(requestContext.nodeName());
        posting.setOwnerFullName(requestContext.fullName());
        if (initializer != null) {
            initializer.accept(posting);
        }
        postingText.toEntry(posting);

        return postingRepository.save(posting);
    }

    public Posting createOrUpdatePosting(Posting posting, EntryRevision revision, List<StoryAttributes> publications,
                                         Predicate<EntryRevision> isNothingChanged,
                                         Consumer<EntryRevision> revisionUpdater) {
        EntryRevision latest = posting.getCurrentRevision();
        if (latest != null && isNothingChanged != null && isNothingChanged.test(latest)) {
            return postingRepository.saveAndFlush(posting);
        }

        EntryRevision current = newPostingRevision(posting, revision);
        if (revisionUpdater != null) {
            revisionUpdater.accept(current);
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

    public Posting createOrUpdatePostingDraft(Posting posting, EntryRevision template,
                                              Predicate<EntryRevision> isNothingChanged,
                                              Consumer<EntryRevision> updater) {
        EntryRevision draft = posting.getDraftRevision();
        if (draft == null) {
            EntryRevision latest = posting.getCurrentRevision();
            if (latest != null && isNothingChanged != null && isNothingChanged.test(latest)) {
                return postingRepository.saveAndFlush(posting);
            }

            draft = newRevision(posting, template);
            posting.setDraftRevision(draft);
        }
        if (updater != null) {
            updater.accept(draft);
        }
        draft.setCreatedAt(Util.now());
        return postingRepository.saveAndFlush(posting);
    }

    private ECPrivateKey getSigningKey() {
        return (ECPrivateKey) requestContext.getOptions().getPrivateKey("profile.signing-key");
    }

    private EntryRevision newPostingRevision(Posting posting, EntryRevision template) {
        EntryRevision revision;

        if (template == null) {
            revision = newRevision(posting, null);
            posting.setTotalRevisions(1);
        } else {
            revision = newRevision(posting, template);
            if (posting.getCurrentRevision().getDeletedAt() == null) {
                posting.getCurrentRevision().setDeletedAt(Util.now());
            }
            posting.setTotalRevisions(posting.getTotalRevisions() + 1);
        }
        posting.setCurrentRevision(revision);

        return revision;
    }

    private EntryRevision newRevision(Posting posting, EntryRevision template) {
        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setEntry(posting);
        revision = entryRevisionRepository.save(revision);
        posting.addRevision(revision);

        if (template != null) {
            revision.setBodyPreview(template.getBodyPreview());
            revision.setSaneBodyPreview(template.getSaneBodyPreview());
            revision.setBodySrc(template.getBodySrc());
            revision.setBodySrcFormat(template.getBodySrcFormat());
            revision.setBody(template.getBody());
            revision.setSaneBody(template.getSaneBody());
            revision.setHeading(template.getHeading());
        }

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

    public void deletePosting(Posting posting) {
        deletePosting(posting, requestContext.getOptions(), requestContext::send, requestContext::send);
    }

    private void deletePosting(Posting posting, Options options, Consumer<Event> eventSender,
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

        eventSender.accept(new PostingDeletedEvent(posting));
        notificationSender.accept(Directions.postingSubscribers(posting.getId()),
                new PostingDeletedNotification(posting.getId()));
    }

    @Scheduled(fixedDelayString = "PT1M")
    public void deleteUnlinked() throws Throwable {
        for (String domainName : domains.getAllDomainNames()) {
            Options options = domains.getDomainOptions(domainName);
            List<Event> eventList = new ArrayList<>();
            List<Pair<Direction, Notification>> notificationList = new ArrayList<>();
            Transaction.execute(txManager, () -> {
                postingRepository.findUnlinked(options.nodeId()).forEach(posting -> {
                    log.info("Deleting unlinked posting {}", posting.getId());
                    deletePosting(posting, options, eventList::add,
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
