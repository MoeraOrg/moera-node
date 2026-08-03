package org.moera.node.rest.task;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.MediaAttachment;
import org.moera.lib.node.types.MediaCaptionText;
import org.moera.lib.node.types.MediaToAttach;
import org.moera.lib.node.types.PostingSourceText;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.body.Body;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaLease;
import org.moera.node.media.MediaLeaseOperations;
import org.moera.node.media.MediaManager;
import org.moera.node.model.MediaAttachmentUtil;
import org.moera.node.model.RemoteMediaUtil;
import org.moera.node.task.Job;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

public abstract class RemoteEntryPostJob<P, S> extends Job<P, S> {

    private static final Logger log = LoggerFactory.getLogger(RemoteEntryPostJob.class);

    @Inject
    protected MediaManager mediaManager;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private MediaLeaseOperations mediaLeaseOperations;

    protected void awaitMediaCompression(
        String targetNodeName,
        Body bodySrc,
        List<MediaToAttach> media
    ) throws MoeraNodeException {
        if (ObjectUtils.isEmpty(media)) {
            return;
        }
        for (var attach : media) {
            var mediaNodeName = attach.getRemoteMedia() != null
                ? attach.getRemoteMedia().getNodeName()
                : targetNodeName;
            var mediaId = attach.getRemoteMedia() != null
                ? attach.getRemoteMedia().getMediaId()
                : attach.getLocalMediaId();
            var info = nodeApi.at(mediaNodeName, generateCarte(mediaNodeName, Scope.VIEW_CONTENT))
                .getPrivateMediaInfo(mediaId, null);
            if (info == null) {
                log.error("Cannot get media info for media {} on node {}", mediaId, mediaNodeName);
                retry();
            }

            if (Boolean.TRUE.equals(info.getUncompressed())) {
                if (!Objects.equals(mediaNodeName, nodeName())) {
                    log.error("Media awaiting compression on node {} cannot be attached to our comment", mediaNodeName);
                    fail();
                }

                if (info.getCompressedMediaId() == null) {
                    log.info("Waiting for media {} on node {} to be compressed", mediaId, mediaNodeName);
                    mediaCompressionWaiting();
                    retry();
                }
                replaceAttachedMedia(targetNodeName, bodySrc, attach, info.getCompressedMediaId());
                checkpoint();
            }
        }
    }

    protected abstract void mediaCompressionWaiting();

    private void replaceAttachedMedia(String targetNodeName, Body bodySrc, MediaToAttach attach, String replacementId) {
        var mediaFileOwner = mediaFileOwnerRepository.findFullById(nodeId, UUID.fromString(replacementId)).orElse(null);
        if (mediaFileOwner == null) {
            log.error("Cannot find the compressed media {}", replacementId);
            fail();
        }
        String originalHash;
        if (attach.getRemoteMedia() != null) {
            originalHash = attach.getRemoteMedia().getHash();
            MediaLease mediaLease = mediaLeaseOperations.create(targetNodeName, mediaFileOwner, null, true);
            RemoteMediaUtil.toRemoteMedia(attach.getRemoteMedia(), mediaFileOwner, mediaLease.getId());
        } else {
            var original = mediaFileOwnerRepository.findFullById(nodeId, UUID.fromString(attach.getLocalMediaId()))
                .orElse(null);
            if (original == null) {
                log.error("Cannot find the uncompressed media {}", attach.getLocalMediaId());
                fail();
            }
            originalHash = original.getMediaFile().getId();
            attach.setLocalMediaId(replacementId);
        }
        if (bodySrc != null && StringUtils.hasText(bodySrc.getText())) {
            bodySrc.setText(bodySrc.getText().replace("hash:" + originalHash, mediaFileOwner.getMediaFile().getId()));
        }
        checkpoint();
    }

    protected void cacheUploadedRemoteMedia(List<MediaToAttach> media) {
        if (ObjectUtils.isEmpty(media)) {
            return;
        }
        media.stream()
            .map(MediaToAttach::getRemoteMedia)
            .filter(Objects::nonNull)
            .forEach(rm ->
                mediaManager.cacheUploadedRemoteMedia(
                    rm.getNodeName(), rm.getMediaId(), Util.base64decode(rm.getDigest())
                )
            );
    }

    protected void updateCaptions(
        String targetNodeName,
        List<MediaAttachment> media,
        List<MediaCaptionText> captions,
        Function<MediaCaptionText, PostingSourceText> captionTextBuilder
    ) {
        if (ObjectUtils.isEmpty(media)) {
            return;
        }
        var mediaPostings = media.stream()
            .filter(ma -> MediaAttachmentUtil.mediaId(ma) != null && ma.getPostingId() != null)
            .collect(Collectors.toMap(MediaAttachmentUtil::mediaId, MediaAttachment::getPostingId));

        for (var caption : captions) {
            var postingId = mediaPostings.get(caption.getMediaId());
            if (postingId == null) {
                continue;
            }

            jobs.run(
                RemotePostingPostJob.class,
                new RemotePostingPostJob.Parameters(
                    targetNodeName,
                    postingId,
                    captionTextBuilder.apply(caption)
                ),
                universalContext.nodeId()
            );
        }
    }

}
