package org.moera.node.rest;

import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.event.model.PostingDraftRevisionDeletedEvent;
import org.moera.node.event.model.PostingDraftRevisionUpdatedEvent;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.BodyMappingException;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingText;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/postings/{postingId}/revisions/draft")
public class PostingDraftRevisionController {

    private static Logger log = LoggerFactory.getLogger(PostingDraftRevisionController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private PostingOperations postingOperations;

    @GetMapping
    @Admin
    public PostingInfo get(@PathVariable UUID postingId) {
        log.info("GET /moera/api/postings/{postingId}/revisions/draft (postingId = {})", LogUtil.format(postingId));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }

        EntryRevision revision = posting.getDraftRevision() != null
                ? posting.getDraftRevision() : posting.getCurrentRevision();
        return new PostingInfo(posting, revision, true, true);
    }

    @PutMapping
    @Admin
    @Transactional
    public PostingInfo put(@PathVariable UUID postingId, @RequestBody @Valid PostingText postingText) {
        log.info("PUT /moera/api/postings/{postingId}/revisions/draft"
                        + " (postingId = {}, bodySrc = {}, bodySrcFormat = {}, publishAt = {})",
                LogUtil.format(postingId),
                LogUtil.format(postingText.getBodySrc(), 64),
                LogUtil.format(postingText.getBodySrcFormat()),
                LogUtil.formatTimestamp(postingText.getPublishAt()));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        try {
            EntryRevision revision = posting.getDraftRevision() != null
                    ? posting.getDraftRevision() : posting.getCurrentRevision();
            posting = postingOperations.createOrUpdatePostingDraft(posting, revision, postingText::toEntryRevision);
        } catch (BodyMappingException e) {
            throw new ValidationFailure("postingText.bodySrc.wrong-encoding");
        }
        requestContext.send(new PostingDraftRevisionUpdatedEvent(posting));

        return new PostingInfo(posting, posting.getDraftRevision(), true, true);
    }

    @DeleteMapping
    @Admin
    @Transactional
    public Result delete(@PathVariable UUID postingId) {
        log.info("DELETE /moera/api/postings/{postingId}/revisions/draft (postingId = {})", LogUtil.format(postingId));

        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (posting.getDraftRevision() == null) {
            return Result.OK;
        }
        entryRevisionRepository.delete(posting.getDraftRevision());
        requestContext.send(new PostingDraftRevisionDeletedEvent(posting));

        return Result.OK;
    }

}
