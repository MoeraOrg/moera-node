package org.moera.node.rest;

import java.net.URI;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.PublicPage;
import org.moera.node.data.PublicPageRepository;
import org.moera.node.global.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.PostingFeatures;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingText;
import org.moera.node.model.Result;
import org.moera.node.option.Options;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@ApiController
@RequestMapping("/moera/api/postings")
public class PostingController {

    private static final int PUBLIC_PAGE_MAX_SIZE = 30;
    private static final int PUBLIC_PAGE_AVG_SIZE = 20;

    private static Logger log = LoggerFactory.getLogger(PostingController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private PublicPageRepository publicPageRepository;

    private AtomicInteger nonce = new AtomicInteger(0);

    @GetMapping("/features")
    @ResponseBody
    public PostingFeatures getFeatures() {
        log.info("GET /postings/features");

        return PostingFeatures.INSTANCE;
    }

    @PostMapping
    @Admin
    @Transactional
    public ResponseEntity<PostingInfo> post(@Valid @RequestBody PostingText postingText) {
        log.info("POST /postings (bodySrc = {}, bodySrcFormat = {}, bodyHtml = {}, publishAt = {})",
                LogUtil.format(postingText.getBodySrc(), 64),
                LogUtil.format(postingText.getBodySrcFormat()),
                LogUtil.format(postingText.getBodyHtml(), 64),
                LogUtil.formatTimestamp(postingText.getPublishAt()));

        Options options = requestContext.getOptions();
        String name = options.getString("profile.registered-name");
        Integer generation = options.getInt("profile.registered-name.generation");
        if (name == null || generation == null) {
            throw new OperationFailure("posting.registered-name-not-set");
        }
        PrivateKey signingKey = options.getPrivateKey("profile.signing-key");
        if (signingKey == null) {
            throw new OperationFailure("posting.signing-key-not-set");
        }

        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setEntryId(UUID.randomUUID());
        posting.setNodeId(options.nodeId());
        posting.setOwnerName(name);
        posting.setOwnerGeneration(generation);
        postingText.toPosting(posting);
        posting.setMoment(buildMoment(posting.getPublishedAt()));
        posting.sign((ECPrivateKey) signingKey);
        postingRepository.saveAndFlush(posting);
        updatePublicPages(posting.getMoment());

        return ResponseEntity.created(URI.create("/postings/" + posting.getEntryId())).body(new PostingInfo(posting));
    }

    @PutMapping("/{id}")
    @Admin
    @Transactional
    public PostingInfo put(@PathVariable UUID id, @Valid @RequestBody PostingText postingText) {
        log.info("PUT /postings/{id}, (id = {}, bodySrc = {}, bodySrcFormat = {}, bodyHtml = {}, publishAt = {})",
                LogUtil.format(id),
                LogUtil.format(postingText.getBodySrc(), 64),
                LogUtil.format(postingText.getBodySrcFormat()),
                LogUtil.format(postingText.getBodyHtml(), 64),
                LogUtil.formatTimestamp(postingText.getPublishAt()));

        Posting latest = postingRepository.findByEntryId(requestContext.nodeId(), id).orElse(null);
        if (latest == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }

        Options options = requestContext.getOptions();
        PrivateKey signingKey = options.getPrivateKey("profile.signing-key");
        if (signingKey == null) {
            throw new OperationFailure("posting.signing-key-not-set");
        }

        latest.setDeletedAt(Util.now());

        Posting posting = latest.newRevision();
        postingText.toPosting(posting);
        if (posting.getPublishedAt().equals(latest.getPublishedAt())) {
            posting.setMoment(latest.getMoment());
        } else {
            posting.setMoment(buildMoment(posting.getPublishedAt()));
        }
        posting.sign((ECPrivateKey) signingKey);
        postingRepository.saveAndFlush(posting);
        updatePublicPages(posting.getMoment());

        return toPostingInfo(posting);
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
                        PageRequest.of(count - PUBLIC_PAGE_AVG_SIZE, 1, Sort.by(Sort.Direction.DESC, "moment")))
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
                        PageRequest.of(PUBLIC_PAGE_AVG_SIZE + 1, 1, Sort.by(Sort.Direction.DESC, "moment")))
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

    @GetMapping("/{id}")
    @ResponseBody
    public PostingInfo get(@PathVariable UUID id) {
        log.info("GET /postings/{id}, (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findByEntryId(requestContext.nodeId(), id).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }

        return toPostingInfo(posting);
    }

    @DeleteMapping("/{id}")
    @Admin
    @ResponseBody
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /postings/{id}, (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findByEntryId(requestContext.nodeId(), id).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        posting.setDeletedAt(Util.now());

        return Result.OK;
    }

    private PostingInfo toPostingInfo(Posting posting) {
        PostingInfo postingInfo = new PostingInfo(posting);
        int totalRevisions = postingRepository.countRevisions(requestContext.nodeId(), posting.getEntryId());
        postingInfo.setTotalRevisions(totalRevisions);
        if (totalRevisions > 1) {
            Timestamp createdAt = postingRepository.firstCreatedAt(requestContext.nodeId(), posting.getEntryId());
            if (createdAt != null) {
                postingInfo.setCreatedAt(Util.toEpochSecond(createdAt));
            }
        }

        return postingInfo;
    }

}
