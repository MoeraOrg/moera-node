package org.moera.node.rest;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.event.model.Event;
import org.moera.node.global.Entitled;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.ValidationFailure;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

public abstract class PostingRevisionControllerBase {

    @Inject
    protected RequestContext requestContext;

    @Inject
    protected PostingRepository postingRepository;

    @Inject
    protected EntryRevisionRepository entryRevisionRepository;

    @Inject
    private PostingOperations postingOperations;

    protected abstract Logger getLog();

    protected abstract String getDirectory();

    protected abstract Posting findPosting(UUID postingId);

    protected abstract EntryRevision findRevision(UUID postingId, UUID id);

    protected abstract Event getRestorationEvent(Posting posting);

    @GetMapping
    public List<PostingRevisionInfo> getAll(@PathVariable UUID postingId) {
        getLog().info("GET {}/{postingId}/revisions (postingId = {})", getDirectory(), LogUtil.format(postingId));

        Posting posting = findPosting(postingId);
        if (posting == null) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }

        boolean countsVisible = posting.isReactionTotalsVisible() || requestContext.isAdmin()
                || requestContext.isClient(posting.getOwnerName());
        return posting.getRevisions().stream()
                .map(r -> new PostingRevisionInfo(r, countsVisible))
                .sorted(Comparator.comparing(PostingRevisionInfo::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
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

        return new PostingRevisionInfo(revision, posting.isReactionTotalsVisible() || requestContext.isAdmin()
                || requestContext.isClient(posting.getOwnerName()));
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
        posting = postingOperations.createOrUpdatePosting(posting, revision, null, null);
        requestContext.send(getRestorationEvent(posting));

        return new PostingRevisionInfo(revision, true);
    }

}
