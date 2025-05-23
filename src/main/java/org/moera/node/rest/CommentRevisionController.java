package org.moera.node.rest;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.CommentRevisionInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.CommentRevisionInfoUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/postings/{postingId}/comments/{commentId}/revisions")
public class CommentRevisionController {

    private static final Logger log = LoggerFactory.getLogger(CommentRevisionController.class);

    @Inject
    protected RequestContext requestContext;

    @Inject
    protected CommentRepository commentRepository;

    @Inject
    protected EntryRevisionRepository entryRevisionRepository;

    @GetMapping
    @NoCache
    @Transactional
    public List<CommentRevisionInfo> getAll(@PathVariable UUID postingId, @PathVariable UUID commentId) {
        log.info(
            "GET /postings/{postingId}/comments/{commentId}/revisions (postingId = {}, commentId = {})",
            LogUtil.format(postingId), LogUtil.format(commentId)
        );

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
            .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
        if (!requestContext.isPrincipal(comment.getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewCommentsE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }

        return comment.getRevisions().stream()
            .map(r -> CommentRevisionInfoUtil.build(comment, r, requestContext))
            .sorted(Comparator.comparing(CommentRevisionInfo::getCreatedAt).reversed())
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Transactional
    public CommentRevisionInfo get(@PathVariable UUID postingId, @PathVariable UUID commentId, @PathVariable UUID id) {
        log.info(
            "GET /postings/{postingId}/comments/{commentId}/revisions/{id} (postingId = {}, commentId = {}, id = {})",
            LogUtil.format(postingId), LogUtil.format(commentId), LogUtil.format(id)
        );

        Comment comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
            .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
        if (!requestContext.isPrincipal(comment.getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("posting.not-found");
        }
        if (!requestContext.isPrincipal(comment.getPosting().getViewCommentsE(), Scope.VIEW_CONTENT)) {
            throw new ObjectNotFoundFailure("comment.not-found");
        }
        if (!comment.getPosting().getId().equals(postingId)) {
            throw new ObjectNotFoundFailure("comment.wrong-posting");
        }
        EntryRevision revision = entryRevisionRepository.findByEntryIdAndId(requestContext.nodeId(), commentId, id)
            .orElseThrow(() -> new ObjectNotFoundFailure("comment-revision.not-found"));

        return CommentRevisionInfoUtil.build(comment, revision, requestContext);
    }

}
