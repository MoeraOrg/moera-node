package org.moera.node.rest;

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.inject.Inject;

import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.PublicPage;
import org.moera.node.data.PublicPageRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.model.OperationFailure;
import org.moera.node.option.Options;
import org.moera.node.util.Util;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PostingOperations {

    public static final int MAX_POSTINGS_PER_REQUEST = 200;

    private static final int PUBLIC_PAGE_MAX_SIZE = 30;
    private static final int PUBLIC_PAGE_AVG_SIZE = 20;

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private PublicPageRepository publicPageRepository;

    private AtomicInteger nonce = new AtomicInteger(0);

    public void createOrUpdatePosting(Posting posting, EntryRevision revision, Consumer<EntryRevision> updater) {
        ECPrivateKey signingKey = getSigningKey();
        EntryRevision latest = posting.getCurrentRevision();
        EntryRevision current = newPostingRevision(posting, revision);
        if (updater != null) {
            updater.accept(current);
        }
        if (latest == null || !current.getPublishedAt().equals(latest.getPublishedAt())) {
            current.setMoment(buildMoment(current.getPublishedAt()));
        }
        posting.sign(signingKey);
        postingRepository.saveAndFlush(posting);
        updatePublicPages(current.getMoment());
    }

    private ECPrivateKey getSigningKey() {
        Options options = requestContext.getOptions();
        PrivateKey signingKey = options.getPrivateKey("profile.signing-key");
        if (signingKey == null) {
            throw new OperationFailure("posting.signing-key-not-set");
        }
        return (ECPrivateKey) signingKey;
    }

    private EntryRevision newPostingRevision(Posting posting, EntryRevision template) {
        EntryRevision revision;

        if (template == null) {
            revision = newRevision(posting, null);
            posting.setTotalRevisions(1);
        } else {
            revision = newRevision(posting, template);
            posting.getCurrentRevision().setDeletedAt(Util.now());
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
        entryRevisionRepository.save(revision);

        if (template != null) {
            revision.setBodyPreviewHtml(template.getBodyPreviewHtml());
            revision.setBodySrc(template.getBodySrc());
            revision.setBodySrcFormat(template.getBodySrcFormat());
            revision.setBodyHtml(template.getBodyHtml());
            revision.setHeading(template.getHeading());
            revision.setPublishedAt(template.getPublishedAt());
            revision.setMoment(template.getMoment());
        }

        return revision;
    }

    private long buildMoment(Timestamp timestamp) {
        return Util.toEpochSecond(timestamp) * 100 + nonce.getAndIncrement() % 100;
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
            int count = postingRepository.countInRange(nodeId, after, Long.MAX_VALUE);
            if (count >= PUBLIC_PAGE_MAX_SIZE) {
                long median = postingRepository.findMomentsInRange(nodeId, after, Long.MAX_VALUE,
                        PageRequest.of(count - PUBLIC_PAGE_AVG_SIZE, 1,
                                Sort.by(Sort.Direction.DESC, "currentRevision.moment")))
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
            int count = postingRepository.countInRange(nodeId, Long.MIN_VALUE, end);
            if (count >= PUBLIC_PAGE_MAX_SIZE) {
                long median = postingRepository.findMomentsInRange(nodeId, Long.MIN_VALUE, end,
                        PageRequest.of(PUBLIC_PAGE_AVG_SIZE + 1, 1,
                                Sort.by(Sort.Direction.DESC, "currentRevision.moment")))
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
