package org.moera.node.rest;

import java.util.Objects;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.MediaLeaseAttributes;
import org.moera.lib.node.types.MediaLeaseInfo;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.config.Config;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.MediaLease;
import org.moera.node.data.MediaLeaseRepository;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.media.MediaGrantGenerator;
import org.moera.node.model.MediaLeaseInfoUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/media/leases")
public class MediaLeaseController {

    private static final Logger log = LoggerFactory.getLogger(MediaLeaseController.class);

    @Inject
    private Config config;

    @Inject
    private RequestContext requestContext;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private EntryAttachmentRepository entryAttachmentRepository;

    @Inject
    private MediaLeaseRepository mediaLeaseRepository;

    @PostMapping
    @Transactional
    public MediaLeaseInfo create(@RequestBody MediaLeaseAttributes attributes) {
        log.info(
            "POST /media/leases (nodeName = {}, mediaId = {}, postingId = {}, commentId = {})",
            LogUtil.format(attributes.getNodeName()),
            LogUtil.format(attributes.getMediaId()),
            LogUtil.format(attributes.getPostingId()),
            LogUtil.format(attributes.getCommentId())
        );

        attributes.validate();

        String clientName = requestContext.getClientName(Scope.LEASE_MEDIA);
        boolean admin = requestContext.isAdmin(Scope.LEASE_MEDIA) && requestContext.isAdmin(Scope.VIEW_MEDIA);
        if (clientName == null && !admin) {
            throw new AuthenticationException();
        }
        if (clientName != null && !requestContext.hasClientScope(Scope.VIEW_MEDIA) && !admin) {
            throw new AuthenticationException();
        }
        if (!admin && !Objects.equals(attributes.getNodeName(), clientName)) {
            throw new AuthenticationException();
        }

        Entry entry = null;
        if (attributes.getPostingId() != null) {
            UUID postingId = Util.uuid(attributes.getPostingId())
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
            Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
            if (!requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)) {
                throw new ObjectNotFoundFailure("posting.not-found");
            }

            Comment comment = null;
            if (attributes.getCommentId() != null) {
                UUID commentId = Util.uuid(attributes.getCommentId())
                    .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
                comment = commentRepository.findFullByNodeIdAndId(requestContext.nodeId(), commentId)
                    .orElseThrow(() -> new ObjectNotFoundFailure("comment.not-found"));
                if (!requestContext.isPrincipal(comment.getPosting().getViewE(), Scope.VIEW_CONTENT)) {
                    throw new ObjectNotFoundFailure("posting.not-found");
                }
                if (!requestContext.isPrincipal(comment.getPosting().getViewCommentsE(), Scope.VIEW_CONTENT)) {
                    throw new ObjectNotFoundFailure("comment.not-found");
                }
                if (!requestContext.isPrincipal(comment.getViewE(), Scope.VIEW_CONTENT)) {
                    throw new ObjectNotFoundFailure("comment.not-found");
                }
                if (!comment.getPosting().getId().equals(posting.getId())) {
                    throw new ObjectNotFoundFailure("comment.wrong-posting");
                }
            }

            entry = comment != null ? comment : posting;
        } else if (!admin) {
            throw new AuthenticationException();
        }

        UUID mediaFileOwnerId = Util.uuid(attributes.getMediaId())
            .orElseThrow(() -> new ObjectNotFoundFailure("media.not-found"));
        MediaFileOwner mediaFileOwner = mediaFileOwnerRepository.findFullById(requestContext.nodeId(), mediaFileOwnerId)
            .orElseThrow(() -> new ObjectNotFoundFailure("media.not-found"));
        if (
            entry != null
            && entryAttachmentRepository.countByEntryIdAndMedia(entry.getId(), mediaFileOwner.getId()) == 0
        ) {
            throw new ObjectNotFoundFailure("media.not-found");
        }

        MediaLease mediaLease = new MediaLease();
        mediaLease.setId(UUID.randomUUID());
        mediaLease.setNodeId(requestContext.nodeId());
        mediaLease.setOwnerName(attributes.getNodeName());
        mediaLease.setMediaFileOwner(mediaFileOwner);
        mediaLease.setEntry(entry);
        mediaLease = mediaLeaseRepository.save(mediaLease);

        var grantSupplier = entry != null ? new MediaGrantGenerator(null, requestContext.getOptions()) : null;
        return MediaLeaseInfoUtil.build(mediaLease, config.getMedia().getDirectServe(), grantSupplier);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result delete(@PathVariable String id) {
        log.info("DELETE /media/leases/{id} (id = {})", LogUtil.format(id));

        UUID mediaLeaseId = Util.uuid(id).orElseThrow(() -> new ObjectNotFoundFailure("media-lease.not-found"));
        MediaLease mediaLease = mediaLeaseRepository.findByNodeIdAndId(requestContext.nodeId(), mediaLeaseId)
            .orElseThrow(() -> new ObjectNotFoundFailure("media-lease.not-found"));
        if (
            !requestContext.isAdmin(Scope.LEASE_MEDIA)
            && !requestContext.isClient(mediaLease.getOwnerName(), Scope.LEASE_MEDIA)
        ) {
            throw new AuthenticationException();
        }

        mediaLeaseRepository.delete(mediaLease);

        return Result.OK;
    }

}
