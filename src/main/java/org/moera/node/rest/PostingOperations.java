package org.moera.node.rest;

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.PublicPage;
import org.moera.node.data.PublicPageRepository;
import org.moera.node.fingerprint.PostingFingerprint;
import org.moera.node.global.RequestContext;
import org.moera.node.model.OperationFailure;
import org.moera.node.option.Options;
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

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private PublicPageRepository publicPageRepository;

    private MomentFinder momentFinder;

    @PostConstruct
    public void init() {
        momentFinder = new MomentFinder(
            moment -> entryRevisionRepository.countMoments(requestContext.nodeId(), moment) == 0
        );
    }

    public Posting createOrUpdatePosting(Posting posting, EntryRevision revision, Consumer<EntryRevision> updater) {
        ECPrivateKey signingKey = getSigningKey();
        EntryRevision latest = posting.getCurrentRevision();
        EntryRevision current = newPostingRevision(posting, revision);
        if (updater != null) {
            updater.accept(current);
        }
        if (latest == null || !current.getPublishedAt().equals(latest.getPublishedAt())) {
            current.setMoment(0);
        }
        posting = postingRepository.saveAndFlush(posting);
        current = posting.getCurrentRevision();
        if (current.getMoment() == 0) {
            current.setMoment(momentFinder.find(current.getPublishedAt()));
        }
        PostingFingerprint fingerprint = new PostingFingerprint(posting, current);
        current.setDigest(CryptoUtil.digest(fingerprint));
        current.setSignature(CryptoUtil.sign(fingerprint, signingKey));
        current.setSignatureVersion(PostingFingerprint.VERSION);
        updatePublicPages(current.getMoment());

        return posting;
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
        entryRevisionRepository.save(revision);

        if (template != null) {
            revision.setBodyPreview(template.getBodyPreview());
            revision.setBodySrc(template.getBodySrc());
            revision.setBodySrcFormat(template.getBodySrcFormat());
            revision.setBody(template.getBody());
            revision.setHeading(template.getHeading());
            revision.setPublishedAt(template.getPublishedAt());
            revision.setMoment(template.getMoment());
        }

        return revision;
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
