package org.moera.node.rest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.event.Event;
import org.moera.node.model.notification.PostingUpdatedNotification;
import org.moera.node.notification.send.Directions;
import org.moera.node.operations.PostingOperations;
import org.moera.node.operations.ReactionTotalOperations;
import org.slf4j.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

public abstract class PostingRevisionControllerBase {

    private static final int MAX_REVISIONS_PER_REQUEST = 10;

    @Inject
    protected RequestContext requestContext;

    @Inject
    protected PostingRepository postingRepository;

    @Inject
    protected EntryRevisionRepository entryRevisionRepository;

    @Inject
    private PostingOperations postingOperations;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    protected abstract Logger getLog();

    protected abstract String getDirectory();

    protected abstract Posting findPosting(UUID postingId);

    protected abstract EntryRevision findRevision(UUID postingId, UUID id);

    protected abstract Event getRestorationEvent(Posting posting);

    @GetMapping
    @NoCache
    @Transactional
    public List<PostingRevisionInfo> getAll(@PathVariable UUID postingId,
                                            @RequestParam(required = false) Integer limit) {
        getLog().info("GET {}/{postingId}/revisions (postingId = {}, limit = {})",
                getDirectory(), LogUtil.format(postingId), LogUtil.format(limit));

        Posting posting = findPosting(postingId);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }

        limit = limit != null && limit <= MAX_REVISIONS_PER_REQUEST ? limit : MAX_REVISIONS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }

        boolean countsVisible = reactionTotalOperations.isVisibleToClient(posting);
        return entryRevisionRepository.findAllByEntryId(requestContext.nodeId(), postingId,
                PageRequest.of(0, limit, Sort.Direction.DESC, "createdAt"))
                .get()
                .map(r -> new PostingRevisionInfo(r, posting.getReceiverName(), countsVisible))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Transactional
    public PostingRevisionInfo get(@PathVariable UUID postingId, @PathVariable UUID id) {
        getLog().info("GET {}/{postingId}/revisions/{id} (postingId = {}, id = {})",
                getDirectory(),
                LogUtil.format(postingId),
                LogUtil.format(id));

        Posting posting = findPosting(postingId);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        EntryRevision revision = findRevision(postingId, id);
        if (revision == null) {
            throw new ObjectNotFoundFailure("posting-revision.not-found");
        }

        return new PostingRevisionInfo(revision, posting.getReceiverName(),
                reactionTotalOperations.isVisibleToClient(posting));
    }

    @PostMapping("/{id}/restore")
    @Admin
    @Entitled
    @Transactional
    public PostingRevisionInfo restore(@PathVariable UUID postingId, @PathVariable UUID id) {
        getLog().info("POST {}/{postingId}/revisions/{id}/restore (postingId = {}, id = {})",
                getDirectory(),
                LogUtil.format(postingId),
                LogUtil.format(id));

        Posting posting = findPosting(postingId);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (posting.getDeletedAt() == null && posting.getCurrentRevision().getId().equals(id)) {
            throw new ValidationFailure("posting-revision.already-current");
        }
        EntryRevision revision = findRevision(postingId, id);
        if (revision == null) {
            throw new ObjectNotFoundFailure("posting-revision.not-found");
        }

        posting.setDeletedAt(null);
        posting.setDeadline(null);
        List<MediaFileOwner> media = revision.getAttachments().stream()
                .map(EntryAttachment::getMediaFileOwner)
                .collect(Collectors.toList());
        posting = postingOperations.createOrUpdatePosting(posting, revision, media, null, null,
                null, null);
        requestContext.send(getRestorationEvent(posting));
        requestContext.send(Directions.postingSubscribers(posting.getId()),
                new PostingUpdatedNotification(posting.getId()));

        return new PostingRevisionInfo(revision, posting.getReceiverName(), true);
    }

}
