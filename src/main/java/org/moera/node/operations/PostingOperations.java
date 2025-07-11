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
import jakarta.inject.Inject;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.types.BodyFormat;
import org.moera.lib.node.types.PostingText;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.StoryAttributes;
import org.moera.lib.node.types.body.Body;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.Feed;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Story;
import org.moera.node.domain.Domains;
import org.moera.node.fingerprint.PostingFingerprintBuilder;
import org.moera.node.global.RequestContext;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinManager;
import org.moera.node.liberin.model.PostingDeletedLiberin;
import org.moera.node.liberin.model.PostingHeadingUpdatedLiberin;
import org.moera.node.liberin.model.PostingMediaTextUpdatedLiberin;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.PostingTextUtil;
import org.moera.node.option.Options;
import org.moera.node.text.MediaExtractor;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class PostingOperations {

    public static final int MAX_POSTINGS_PER_REQUEST = 200;
    private static final Duration UNSIGNED_TTL = Duration.of(15, ChronoUnit.MINUTES);

    private static final Logger log = LoggerFactory.getLogger(PostingOperations.class);

    @Inject
    private RequestCounter requestCounter;

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
    private MediaOperations mediaOperations;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private TimelinePublicPageOperations timelinePublicPageOperations;

    @Inject
    private LiberinManager liberinManager;

    @Inject
    private Transaction tx;

    private Posting newPosting(String ownerName) {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(universalContext.nodeId());
        if (ObjectUtils.isEmpty(ownerName)) {
            posting.setOwnerName(universalContext.nodeName());
            posting.setOwnerFullName(universalContext.fullName());
            posting.setOwnerGender(universalContext.gender());
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
        Posting posting = newPosting("");
        PostingTextUtil.toEntry(postingText, posting);

        return posting;
    }

    public Posting newPosting(MediaFileOwner mediaFileOwner) {
        Posting posting = newPosting(mediaFileOwner.getOwnerName());
        posting.setParentMedia(mediaFileOwner);
        posting.setViewPrincipal(mediaFileOwner.getViewPrincipal());
        posting.setViewCommentsPrincipal(mediaFileOwner.getViewPrincipal());
        posting.setAddCommentPrincipal(mediaFileOwner.getViewPrincipal());
        posting.setViewReactionsPrincipal(mediaFileOwner.getViewPrincipal());
        posting.setViewNegativeReactionsPrincipal(mediaFileOwner.getViewPrincipal());
        posting.setViewReactionTotalsPrincipal(mediaFileOwner.getViewPrincipal());
        posting.setViewNegativeReactionTotalsPrincipal(mediaFileOwner.getViewPrincipal());
        posting.setViewReactionRatiosPrincipal(mediaFileOwner.getViewPrincipal());
        posting.setViewNegativeReactionRatiosPrincipal(mediaFileOwner.getViewPrincipal());
        posting.setAddReactionPrincipal(mediaFileOwner.getViewPrincipal());
        posting.setAddNegativeReactionPrincipal(mediaFileOwner.getViewPrincipal());

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

    public Posting createOrUpdatePosting(
        Posting posting,
        EntryRevision revision,
        List<MediaFileOwner> media,
        List<StoryAttributes> publications,
        Predicate<EntryRevision> isNothingChanged,
        Consumer<EntryRevision> revisionUpdater,
        Consumer<Entry> mediaEntryUpdater
    ) {
        EntryRevision latest = posting.getCurrentRevision();
        if (latest != null) {
            if (isNothingChanged != null && isNothingChanged.test(latest)) {
                posting = postingRepository.saveAndFlush(posting);
                updateRelatedObjects(posting);
                return posting;
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

        if (!media.isEmpty()) {
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

        storyOperations.publish(posting, publications);

        updateRelatedObjects(posting);

        return posting;
    }

    private void signIfOwned(Posting posting) {
        EntryRevision current = posting.getCurrentRevision();

        if (current.getSignature() == null) {
            if (posting.getOwnerName().equals(universalContext.nodeName())) {
                byte[] fingerprint = PostingFingerprintBuilder.build(posting, current);
                current.setDigest(CryptoUtil.digest(fingerprint));
                current.setSignature(CryptoUtil.sign(fingerprint, getSigningKey()));
                current.setSignatureVersion(PostingFingerprintBuilder.LATEST_VERSION);
            } else {
                current.setDeadline(Timestamp.from(Instant.now().plus(UNSIGNED_TTL)));
            }
        }
    }

    private void updateRelatedObjects(Posting posting) {
        mediaOperations.updatePermissions(posting);

        if (posting.getViewCompound().isPublic()) {
            Story timelineStory = posting.getStory(Feed.TIMELINE);
            if (timelineStory != null) {
                timelinePublicPageOperations.updatePublicPages(timelineStory.getMoment());
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

    public void deletePosting(Posting posting) {
        deletePosting(posting, requestContext.getOptions());
    }

    private void deletePosting(Posting posting, Options options) {
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
    }

    public void deletePickedPosting(Posting posting) {
        EntryRevision latest = posting.getCurrentRevision();
        if (universalContext.getOptions().getBool("posting.picked.hide-on-delete")) {
            Principal latestView = posting.getViewE();
            posting.setViewPrincipal(Principal.ADMIN);
            posting.setEditedAt(Util.now());
            posting.setReceiverDeletedAt(Util.now());

            universalContext.send(new PostingUpdatedLiberin(posting, latest, latestView));
        } else {
            deletePosting(posting, universalContext.getOptions());
            storyOperations.unpublish(posting.getId());

            universalContext.send(new PostingDeletedLiberin(posting, latest));
        }
    }

    public void updatePickedMediaText(UUID postingId, String remoteMediaId, String text) {
        MediaFileOwner mediaFileOwner = postingRepository
            .findAttachedMediaByRemoteId(universalContext.nodeId(), postingId, remoteMediaId)
            .orElse(null);
        if (mediaFileOwner == null) {
            return;
        }
        mediaFileOwner.getMediaFile().setRecognizedText(text);
        mediaFileOwner.getMediaFile().setRecognizeAt(Util.now());
        universalContext.send(new PostingMediaTextUpdatedLiberin(postingId, mediaFileOwner.getId(), text));
    }

    public void updatePickedHeading(UUID postingId, String receiverRevisionId, String heading, String description) {
        EntryRevision revision = entryRevisionRepository
            .findByEntryIdAndReceiverId(universalContext.nodeId(), postingId, receiverRevisionId)
            .orElse(null);
        if (revision == null) {
            return;
        }
        revision.setHeading(heading);
        revision.setDescription(description);
        universalContext.send(
            new PostingHeadingUpdatedLiberin(postingId, revision.getId(), heading, description)
        );
    }

    @Scheduled(fixedDelayString = "PT1H")
    public void purgeUnlinked() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging unlinked postings");

            for (String domainName : domains.getAllDomainNames()) {
                Options options = domains.getDomainOptions(domainName);
                List<Liberin> liberinList = new ArrayList<>();
                tx.executeWrite(() ->
                    postingRepository.findUnlinked(options.nodeId()).forEach(posting -> {
                        log.info("Deleting unlinked posting {}", posting.getId());
                        EntryRevision latest = posting.getCurrentRevision();
                        deletePosting(posting, options);
                        liberinList.add(new PostingDeletedLiberin(posting, latest).withNodeId(options.nodeId()));
                    })
                );
                liberinList.forEach(liberinManager::send);
            }
        }
    }

    @Scheduled(fixedDelayString = "PT15M")
    public void purgeExpired() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging expired unsigned postings");

            List<Liberin> liberins = new ArrayList<>();

            tx.executeWrite(() -> {
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

                            liberins
                                .add(new PostingUpdatedLiberin(posting, latest, posting.getViewE())
                                .withNodeId(posting.getNodeId()));
                        }
                    }
                }
            });

            liberins.forEach(liberinManager::send);
        }
    }

}
