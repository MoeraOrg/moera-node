package org.moera.node.operations;

import java.security.interfaces.ECPrivateKey;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.util.LogUtil;
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
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinManager;
import org.moera.node.liberin.model.PostingDeletedLiberin;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.PostingText;
import org.moera.node.model.StoryAttributes;
import org.moera.node.model.body.Body;
import org.moera.node.option.Options;
import org.moera.node.text.MediaExtractor;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ObjectUtils;

@Component
public class PostingOperations {

    public static final int MAX_POSTINGS_PER_REQUEST = 200;
    private static final Duration UNSIGNED_TTL = Duration.of(15, ChronoUnit.MINUTES);

    private static final Logger log = LoggerFactory.getLogger(PostingOperations.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private UniversalContext universalContext;

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
    private MediaOperations mediaOperations;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private TimelinePublicPageOperations timelinePublicPageOperations;

    @Inject
    private LiberinManager liberinManager;

    @Inject
    private PlatformTransactionManager txManager;

    private Posting newPosting(String ownerName) {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(universalContext.nodeId());
        if (ObjectUtils.isEmpty(ownerName)) {
            posting.setOwnerName(universalContext.nodeName());
            posting.setOwnerFullName(universalContext.fullName());
            if (universalContext.getAvatar() != null) {
                posting.setOwnerAvatarMediaFile(universalContext.getAvatar().getMediaFile());
                posting.setOwnerAvatarShape(universalContext.getAvatar().getShape());
            }
        } else {
            posting.setOwnerName(ownerName);
        }
        return postingRepository.save(posting);
    }

    public Posting newPosting(PostingText postingText) {
        postingText.initAcceptedReactionsDefaults();

        Posting posting = newPosting("");
        postingText.toEntry(posting);

        return posting;
    }

    public Posting newPosting(MediaFileOwner mediaFileOwner) {
        Posting posting = newPosting(mediaFileOwner.getOwnerName());
        posting.setParentMedia(mediaFileOwner);
        posting.setViewPrincipal(mediaFileOwner.getViewPrincipal());
        posting.setViewCommentsPrincipal(mediaFileOwner.getViewPrincipal());

        EntryRevision revision = newRevision(posting, null);
        revision.setBodySrc(Body.EMPTY);
        revision.setBodySrcFormat(SourceFormat.MARKDOWN);
        revision.setBody(Body.EMPTY);
        revision.setSaneBody(null);
        revision.setBodyFormat(BodyFormat.MESSAGE.getValue());
        revision.setBodyPreview(Body.EMPTY);
        revision.setSaneBodyPreview(null);

        signIfOwned(posting);

        return posting;
    }

    public Posting createOrUpdatePosting(Posting posting, EntryRevision revision, List<MediaFileOwner> media,
                                         List<StoryAttributes> publications,
                                         Predicate<EntryRevision> isNothingChanged,
                                         Consumer<EntryRevision> revisionUpdater,
                                         Consumer<Entry> mediaEntryUpdater) {
        EntryRevision latest = posting.getCurrentRevision();
        if (latest != null) {
            if (isNothingChanged != null && isNothingChanged.test(latest)) {
                return postingRepository.saveAndFlush(posting);
            }
            if (latest.getSignature() == null) {
                posting.removeRevision(latest);
                posting.setTotalRevisions(posting.getTotalRevisions() - 1);
                posting.setCurrentRevision(null);
                entryRevisionRepository.delete(latest);
            }
        }

        EntryRevision current = newRevision(posting, revision);
        if (revisionUpdater != null) {
            revisionUpdater.accept(current);
        }

        if (media.size() > 0) {
            Set<String> embedded = MediaExtractor.extractMediaFileIds(new Body(current.getBody()));
            int ordinal = 0;
            for (MediaFileOwner mfo : media) {
                EntryAttachment attachment = new EntryAttachment(current, mfo, ordinal++);
                attachment.setEmbedded(embedded.contains(mfo.getMediaFile().getId()));
                attachment = entryAttachmentRepository.save(attachment);
                current.addAttachment(attachment);

                if (mediaEntryUpdater != null) {
                    Posting mediaPosting = mfo.getPosting(posting.getReceiverName());
                    if (mediaPosting != null) {
                        mediaEntryUpdater.accept(mediaPosting);
                    }
                }
            }
        }

        posting.setEditedAt(Util.now());
        posting = postingRepository.saveAndFlush(posting);
        signIfOwned(posting);
        mediaOperations.updatePermissions(posting);

        storyOperations.publish(posting, publications);

        if (posting.getViewPrincipal().isPublic()) {
            Story timelineStory = posting.getStory(Feed.TIMELINE);
            if (timelineStory != null) {
                timelinePublicPageOperations.updatePublicPages(timelineStory.getMoment());
            }
        }

        return posting;
    }

    private void signIfOwned(Posting posting) {
        EntryRevision current = posting.getCurrentRevision();

        if (current.getSignature() == null) {
            if (posting.getOwnerName().equals(universalContext.nodeName())) {
                PostingFingerprint fingerprint = new PostingFingerprint(posting, current);
                current.setDigest(CryptoUtil.digest(fingerprint));
                current.setSignature(CryptoUtil.sign(fingerprint, getSigningKey()));
                current.setSignatureVersion(PostingFingerprint.VERSION);
            } else {
                current.setDeadline(Timestamp.from(Instant.now().plus(UNSIGNED_TTL)));
            }
        }
    }

    private ECPrivateKey getSigningKey() {
        return (ECPrivateKey) universalContext.getOptions().getPrivateKey("profile.signing-key");
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

            if (posting.getCurrentRevision() != null && posting.getCurrentRevision().getDeletedAt() == null) {
                posting.getCurrentRevision().setDeletedAt(Util.now());
            }
            posting.setTotalRevisions(posting.getTotalRevisions() + 1);
        }

        posting.setCurrentRevision(revision);

        return revision;
    }

    public void deletePosting(Posting posting, boolean unsubscribe) {
        deletePosting(posting, unsubscribe, requestContext.getOptions());
    }

    private void deletePosting(Posting posting, boolean unsubscribe, Options options) {
        posting.setDeletedAt(Util.now());
        ExtendedDuration postingTtl = options.getDuration("posting.deleted.lifetime");
        if (!postingTtl.isNever()) {
            posting.setDeadline(Timestamp.from(Instant.now().plus(postingTtl.getDuration())));
        }

        if (posting.getCurrentRevision() != null) {
            posting.getCurrentRevision().setDeletedAt(Util.now());
        }
        posting = postingRepository.saveAndFlush(posting);
        mediaOperations.updatePermissions(posting);

        if (!posting.isOriginal() && unsubscribe) {
            subscriptionRepository.deleteByTypeAndNodeAndEntryId(options.nodeId(), SubscriptionType.POSTING,
                    posting.getReceiverName(), posting.getReceiverEntryId());
        }
    }

    @Scheduled(fixedDelayString = "PT1H")
    public void purgeUnlinked() throws Throwable {
        for (String domainName : domains.getAllDomainNames()) {
            Options options = domains.getDomainOptions(domainName);
            List<Liberin> liberinList = new ArrayList<>();
            Transaction.execute(txManager, () -> {
                postingRepository.findUnlinked(options.nodeId()).forEach(posting -> {
                    log.info("Deleting unlinked posting {}", posting.getId());
                    EntryRevision latest = posting.getCurrentRevision();
                    deletePosting(posting, true, options);
                    liberinList.add(new PostingDeletedLiberin(posting, latest).withNodeId(options.nodeId()));
                });
                return null;
            });
            liberinList.forEach(liberinManager::send);
        }
    }

    @Scheduled(fixedDelayString = "PT15M")
    public void purgeExpired() throws Throwable {
        List<Liberin> liberins = new ArrayList<>();

        Transaction.execute(txManager, () -> {
            List<Posting> postings = postingRepository.findExpiredUnsigned(Util.now());
            for (Posting posting : postings) {
                universalContext.associate(posting.getNodeId());
                EntryRevision latest = posting.getCurrentRevision();
                if (posting.getDeletedAt() != null || posting.getTotalRevisions() <= 1) {
                    log.debug("Purging expired unsigned posting {}", LogUtil.format(posting.getId()));
                    storyOperations.unpublish(posting.getId(), posting.getNodeId(), liberins::add);
                    postingRepository.delete(posting);

                    liberins.add(new PostingDeletedLiberin(posting, latest).withNodeId(posting.getNodeId()));
                } else {
                    EntryRevision revision = posting.getRevisions().stream()
                            .min(Comparator.comparing(EntryRevision::getCreatedAt))
                            .orElse(null);
                    if (revision != null) { // always
                        revision.setDeletedAt(null);
                        entryRevisionRepository.delete(posting.getCurrentRevision());
                        posting.setCurrentRevision(revision);
                        posting.setTotalRevisions(posting.getTotalRevisions() - 1);

                        liberins.add(new PostingUpdatedLiberin(posting, latest, posting.getViewPrincipalAbsolute())
                                .withNodeId(posting.getNodeId()));
                    }
                }
            }

            return null;
        });

        liberins.forEach(liberinManager::send);
    }

}
