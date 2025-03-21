package org.moera.node.rest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.PostingRevisionInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
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
import org.moera.node.liberin.Liberin;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingRevisionInfoUtil;
import org.moera.node.operations.EntryOperations;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.moera.node.operations.PostingOperations;
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
    private EntryOperations entryOperations;

    protected abstract Logger getLog();

    protected abstract String getDirectory();

    protected abstract Optional<Posting> findPosting(UUID postingId);

    protected abstract boolean isViewPermitted(Posting posting);

    private Posting getPosting(UUID postingId) {
        Posting posting = findPosting(postingId)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
        if (!isViewPermitted(posting)) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        return posting;
    }

    protected abstract Optional<EntryRevision> findRevision(UUID postingId, UUID id);

    protected abstract Optional<EntryRevision> findRevisionWithAttachments(UUID postingId, UUID id);

    private EntryRevision getRevision(UUID postingId, UUID id) {
        return findRevision(postingId, id)
            .orElseThrow(() -> new ObjectNotFoundFailure("posting-revision.not-found"));
    }

    private EntryRevision getRevisionWithAttachments(UUID postingId, UUID id) {
        return findRevisionWithAttachments(postingId, id)
            .orElseThrow(() -> new ObjectNotFoundFailure("posting-revision.not-found"));
    }

    protected abstract Liberin getRestorationLiberin(Posting posting, EntryRevision latest);

    @GetMapping
    @NoCache
    @Transactional
    public List<PostingRevisionInfo> getAll(
        @PathVariable UUID postingId, @RequestParam(required = false) Integer limit
    ) {
        getLog().info(
            "GET {}/{postingId}/revisions (postingId = {}, limit = {})",
            getDirectory(), LogUtil.format(postingId), LogUtil.format(limit)
        );

        Posting posting = getPosting(postingId);

        limit = limit != null && limit <= MAX_REVISIONS_PER_REQUEST ? limit : MAX_REVISIONS_PER_REQUEST;
        ValidationUtil.assertion(limit >= 0, "limit.invalid");

        return entryRevisionRepository
            .findAllByEntryId(
                requestContext.nodeId(),
                postingId,
                PageRequest.of(0, limit, Sort.Direction.DESC, "createdAt")
            )
            .get()
            .map(r ->
                PostingRevisionInfoUtil.build(posting, r, entryOperations, posting.getReceiverName(), requestContext)
            )
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Transactional
    public PostingRevisionInfo get(@PathVariable UUID postingId, @PathVariable UUID id) {
        getLog().info(
            "GET {}/{postingId}/revisions/{id} (postingId = {}, id = {})",
            getDirectory(), LogUtil.format(postingId), LogUtil.format(id)
        );

        Posting posting = getPosting(postingId);
        EntryRevision revision = getRevision(postingId, id);

        return PostingRevisionInfoUtil.build(
            posting, revision, entryOperations, posting.getReceiverName(), requestContext
        );
    }

    @PostMapping("/{id}/restore")
    @Admin(Scope.DELETE_OWN_CONTENT)
    @Entitled
    @Transactional
    public PostingRevisionInfo restore(@PathVariable UUID postingId, @PathVariable UUID id) {
        getLog().info(
            "POST {}/{postingId}/revisions/{id}/restore (postingId = {}, id = {})",
            getDirectory(), LogUtil.format(postingId), LogUtil.format(id)
        );

        Posting posting = getPosting(postingId);
        ValidationUtil.assertion(
            posting.getDeletedAt() != null || !posting.getCurrentRevision().getId().equals(id),
            "posting-revision.already-current"
        );
        EntryRevision latest = posting.getCurrentRevision();
        EntryRevision revision = getRevisionWithAttachments(postingId, id);

        posting.setDeletedAt(null);
        posting.setDeadline(null);
        List<MediaFileOwner> media = revision.getAttachments().stream()
                .map(EntryAttachment::getMediaFileOwner)
                .collect(Collectors.toList());
        posting = postingOperations.createOrUpdatePosting(posting, revision, media, null, null,
                null, null);

        requestContext.send(getRestorationLiberin(posting, latest));

        return PostingRevisionInfoUtil.build(
            posting, revision, MediaAttachmentsProvider.RELATIONS, posting.getReceiverName(), requestContext
        );
    }

}
