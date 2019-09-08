package org.moera.node.rest;

import java.net.URI;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
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

    private static Logger log = LoggerFactory.getLogger(PostingController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private PostingOperations postingOperations;

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

        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(options.nodeId());
        posting.setOwnerName(name);
        posting.setOwnerGeneration(generation);
        postingRepository.save(posting);

        postingOperations.createOrUpdatePosting(posting, null, postingText::toEntryRevision);

        return ResponseEntity.created(URI.create("/postings/" + posting.getId())).body(new PostingInfo(posting));
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

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), id).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        postingOperations.createOrUpdatePosting(posting, posting.getCurrentRevision(), postingText::toEntryRevision);

        return new PostingInfo(posting);
    }

    @GetMapping("/{id}")
    @ResponseBody
    public PostingInfo get(@PathVariable UUID id) {
        log.info("GET /postings/{id}, (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), id).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }

        return new PostingInfo(posting);
    }

    @DeleteMapping("/{id}")
    @Admin
    @ResponseBody
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /postings/{id}, (id = {})", LogUtil.format(id));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), id).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        posting.setDeletedAt(Util.now());
        posting.getCurrentRevision().setDeletedAt(Util.now());
        entryRevisionRepository.save(posting.getCurrentRevision());

        return Result.OK;
    }

}
