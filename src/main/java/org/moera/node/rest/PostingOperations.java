package org.moera.node.rest;

import java.security.interfaces.ECPrivateKey;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.PublicPage;
import org.moera.node.data.PublicPageRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.event.model.StoryAddedEvent;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.global.RequestContext;
import org.moera.node.model.AcceptedReactions;
import org.moera.node.model.PostingText;
import org.moera.node.util.MomentFinder;
import org.moera.node.util.Util;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PostingOperations {

    public static final int MAX_POSTINGS_PER_REQUEST = 200;

    private static final int PUBLIC_PAGE_MAX_SIZE = 30;
    private static final int PUBLIC_PAGE_AVG_SIZE = 20;

    private static final Timestamp PINNED_TIME = Util.toTimestamp(90000000000000L); // 9E+13

    @Inject
    private RequestContext requestContext;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private PublicPageRepository publicPageRepository;

    private final MomentFinder momentFinder = new MomentFinder();

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
        posting.setReceiverName(requestContext.nodeName());
        posting.setOwnerName(requestContext.nodeName());
        if (initializer != null) {
            initializer.accept(posting);
        }
        postingText.toEntry(posting);

        return postingRepository.save(posting);
    }

    public Posting createOrUpdatePosting(Posting posting, EntryRevision revision,
                                         Function<EntryRevision, Boolean> isPreserveRevision,
                                         Consumer<EntryRevision> revisionUpdater,
                                         Consumer<Story> storyUpdater) {
        EntryRevision latest = posting.getCurrentRevision();
        if (latest != null && isPreserveRevision != null && isPreserveRevision.apply(latest)) {
            return postingRepository.saveAndFlush(posting);
        }
        EntryRevision current = newPostingRevision(posting, revision);
        if (revisionUpdater != null) {
            revisionUpdater.accept(current);
        }
        posting = postingRepository.saveAndFlush(posting);

        Story story = existingStory(posting);
        if (story == null) {
            story = newStory(posting, storyUpdater);
            updateMoment(story);
            story = storyRepository.saveAndFlush(story);
            requestContext.send(new StoryAddedEvent(story));
        }

        current = posting.getCurrentRevision();

        PostingFingerprint fingerprint = new PostingFingerprint(posting, current);
        current.setDigest(CryptoUtil.digest(fingerprint));
        current.setSignature(CryptoUtil.sign(fingerprint, getSigningKey()));
        current.setSignatureVersion(PostingFingerprint.VERSION);
        updatePublicPages(story.getMoment());

        return posting;
    }

    private void updateMoment(Story story) {
        story.setMoment(momentFinder.find(
                moment -> storyRepository.countMoments(requestContext.nodeId(), Feed.TIMELINE, moment) == 0,
                !story.isPinned() ? story.getPublishedAt() : PINNED_TIME));
    }

    public Posting createOrUpdatePostingDraft(Posting posting, EntryRevision template,
                                              Consumer<EntryRevision> updater) {
        EntryRevision draft = posting.getDraftRevision();
        if (draft == null) {
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
        posting.getRevisions().add(revision);
        posting.setCurrentRevision(revision);

        return revision;
    }

    private EntryRevision newRevision(Posting posting, EntryRevision template) {
        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setEntry(posting);
        revision = entryRevisionRepository.save(revision);

        if (template != null) {
            revision.setBodyPreview(template.getBodyPreview());
            revision.setBodySrc(template.getBodySrc());
            revision.setBodySrcFormat(template.getBodySrcFormat());
            revision.setBody(template.getBody());
            revision.setHeading(template.getHeading());
        }

        return revision;
    }

    private Story newStory(Posting posting, Consumer<Story> initializer) {
        Story story = new Story(
                UUID.randomUUID(), requestContext.nodeId(), Feed.TIMELINE, StoryType.POSTING_ADDED, posting);
        if (initializer != null) {
            initializer.accept(story);
        }
        return story;
    }

    private Story existingStory(Posting posting) {
        return storyRepository.findByFeedAndTypeAndEntryId(
                requestContext.nodeId(), Feed.TIMELINE, StoryType.POSTING_ADDED, posting.getId());
    }

    private void updatePublicPages(long moment) {
        UUID nodeId = requestContext.nodeId();
        PublicPage firstPage = publicPageRepository.findByBeforeMoment(nodeId, Long.MAX_VALUE);
        if (firstPage == null) {
            firstPage = new PublicPage();
            firstPage.setNodeId(requestContext.nodeId());
            firstPage.setAfterMoment(Long.MIN_VALUE);
            firstPage.setBeforeMoment(Long.MAX_VALUE);
            publicPageRepository.save(firstPage);
            return;
        }

        long after = firstPage.getAfterMoment();
        if (moment > after) {
            int count = storyRepository.countInRange(nodeId, Feed.TIMELINE, after, Long.MAX_VALUE);
            if (count >= PUBLIC_PAGE_MAX_SIZE) {
                long median = storyRepository.findMomentsInRange(nodeId, Feed.TIMELINE, after, Long.MAX_VALUE,
                        PageRequest.of(count - PUBLIC_PAGE_AVG_SIZE, 1,
                                Sort.by(Sort.Direction.DESC, "moment")))
                        .getContent().get(0);
                firstPage.setAfterMoment(median);
                PublicPage secondPage = new PublicPage();
                secondPage.setNodeId(requestContext.nodeId());
                secondPage.setAfterMoment(after);
                secondPage.setBeforeMoment(median);
                publicPageRepository.save(secondPage);
            }
            return;
        }

        PublicPage lastPage = publicPageRepository.findByAfterMoment(nodeId, Long.MIN_VALUE);
        long end = lastPage.getBeforeMoment();
        if (moment <= end) {
            int count = storyRepository.countInRange(nodeId, Feed.TIMELINE, Long.MIN_VALUE, end);
            if (count >= PUBLIC_PAGE_MAX_SIZE) {
                long median = storyRepository.findMomentsInRange(nodeId, Feed.TIMELINE, Long.MIN_VALUE, end,
                        PageRequest.of(PUBLIC_PAGE_AVG_SIZE + 1, 1,
                                Sort.by(Sort.Direction.DESC, "moment")))
                        .getContent().get(0);
                lastPage.setBeforeMoment(median);
                PublicPage prevPage = new PublicPage();
                prevPage.setNodeId(requestContext.nodeId());
                prevPage.setAfterMoment(median);
                prevPage.setBeforeMoment(end);
                publicPageRepository.save(prevPage);
            }
        }
    }

}
