package org.moera.node.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.exception.MoeraNodeApiNotFoundException;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.data.RemoteMediaFileRepository;
import org.moera.node.liberin.model.EntryMediaDownloadedLiberin;
import org.moera.node.media.MediaGrantGenerator;
import org.moera.node.media.MediaManager;
import org.moera.node.media.MediaUtil;
import org.moera.node.media.RemoteMediaOperations;
import org.moera.node.task.Job;
import org.moera.node.util.ParametrizedLock;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import tools.jackson.databind.ObjectMapper;

public class DownloadEntryAttachmentsJob
        extends Job<DownloadEntryAttachmentsJob.Parameters, DownloadEntryAttachmentsJob.State> {

    public static class Parameters {

        private UUID postingId;
        private UUID commentId;

        public Parameters() {
        }

        public Parameters(UUID postingId, UUID commentId) {
            this.postingId = postingId;
            this.commentId = commentId;
        }

        public UUID getPostingId() {
            return postingId;
        }

        public void setPostingId(UUID postingId) {
            this.postingId = postingId;
        }

        public UUID getCommentId() {
            return commentId;
        }

        public void setCommentId(UUID commentId) {
            this.commentId = commentId;
        }

    }

    public static class State {

        private String remoteNodeName;
        private Set<String> leaseIds = new HashSet<>();

        public State() {
        }

        public String getRemoteNodeName() {
            return remoteNodeName;
        }

        public void setRemoteNodeName(String remoteNodeName) {
            this.remoteNodeName = remoteNodeName;
        }

        public Set<String> getLeaseIds() {
            return leaseIds;
        }

        public void setLeaseIds(Set<String> leaseIds) {
            this.leaseIds = leaseIds;
        }

    }

    private record AttachmentLocation(UUID entryId, String nodeName, String mediaId) {
    }

    private static final Logger log = LoggerFactory.getLogger(DownloadEntryAttachmentsJob.class);

    private static final ParametrizedLock<UUID> ENTRY_LOCKS = new ParametrizedLock<>();

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private EntryAttachmentRepository entryAttachmentRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private RemoteMediaFileRepository remoteMediaFileRepository;

    @Inject
    private RemoteMediaOperations remoteMediaOperations;

    @Inject
    private MediaManager mediaManager;

    public DownloadEntryAttachmentsJob() {
        state = new State();
        retryCount(10, "PT10M");
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
        this.state = objectMapper.readValue(state, State.class);
    }

    @Override
    protected void started() {
        super.started();
        if (parameters.commentId != null) {
            log.info(
                "Downloading attachments for posting {}, comment {}",
                LogUtil.format(parameters.postingId),
                LogUtil.format(parameters.commentId)
            );
        } else {
            log.info(
                "Downloading attachments for posting {}",
                LogUtil.format(parameters.postingId)
            );
        }
    }

    @Override
    protected void execute() throws Exception {
        UUID entryId = parameters.commentId != null ? parameters.commentId : parameters.postingId;
        if (entryId == null) {
            fail();
        }

        if (getRetries() == 0) {
            log.info("Delay before starting download");
            retry();
        }

        var unlock = ENTRY_LOCKS.tryLock(entryId);
        if (unlock == null) {
            log.info("Failed to acquire lock for entry {}, try again later", LogUtil.format(entryId));
            retry();
        }

        try (unlock) {
            releaseLeases();

            validateRemoteMedia();

            int maxSize = getMediaMaxSize();
            while (true) {
                AttachmentLocation attachmentLocation = tx.executeRead(() -> findDownloadableAttachment(maxSize));
                if (attachmentLocation == null) {
                    return;
                }
                download(attachmentLocation, maxSize);
            }
        }
    }

    private Entry findEntry() {
        if (parameters.commentId == null) {
            return postingRepository.findByNodeIdAndId(nodeId, parameters.postingId).orElse(null);
        }

        Comment comment = commentRepository.findByNodeIdAndId(nodeId, parameters.commentId).orElse(null);
        return comment != null
                && comment.getPosting() != null
                && Objects.equals(comment.getPosting().getId(), parameters.postingId)
            ? comment
            : null;
    }

    private List<RemoteMediaFile> findRemoteMediaToValidate() {
        return tx.executeRead(() -> {
            Entry entry = findEntry();
            if (entry == null || entry.getCurrentRevision() == null) {
                return List.of();
            }

            List<EntryAttachment> list =
                entryAttachmentRepository.findRemoteMediaToValidate(entry.getCurrentRevision().getId());
            Set<UUID> seen = new HashSet<>();
            List<RemoteMediaFile> files = new ArrayList<>();
            for (EntryAttachment attachment : list) {
                RemoteMediaFile remoteMediaFile = attachment.getRemoteMediaFile();
                if (!seen.add(remoteMediaFile.getId())) {
                    continue;
                }
                files.add(remoteMediaFile);
            }
            return files;
        });
    }

    private void validateRemoteMedia() throws Exception {
        List<RemoteMediaFile> remoteMedia = findRemoteMediaToValidate();

        for (RemoteMediaFile remoteMediaFile : remoteMedia) {
            PrivateMediaFileInfo mediaInfo;
            try {
                mediaInfo = getPrivateMediaInfo(remoteMediaFile.getNodeName(), remoteMediaFile.getMediaId());
            } catch (MoeraNodeApiNotFoundException e) {
                invalidateRemoteMedia(remoteMediaFile, true);
                continue;
            }

            byte[] digest = Util.base64decode(mediaInfo.getDigest());
            if (!Arrays.equals(remoteMediaFile.getDigest(), digest)) {
                invalidateRemoteMedia(remoteMediaFile, false);
                continue;
            }

            if (remoteMediaInfoDiffers(remoteMediaFile, mediaInfo)) {
                tx.executeWrite(() -> remoteMediaOperations.update(remoteMediaFile.getId(), mediaInfo));
            }
        }
    }

    private PrivateMediaFileInfo getPrivateMediaInfo(String remoteNodeName, String remoteMediaId) throws Exception {
        String grant = new MediaGrantGenerator(getOptions()).generateRemote(
            remoteMediaId, MediaUtil.MEDIA_GRANT_TTL, false, null
        );
        var info = nodeApi.at(remoteNodeName).getPrivateMediaInfo(remoteMediaId, grant);
        if (info == null) {
            log.error("Failed to retrieve private media info for media {} at node {}", remoteNodeName, remoteMediaId);
            retry();
        }
        return info;
    }

    private static boolean remoteMediaInfoDiffers(
        RemoteMediaFile remoteMediaFile, PrivateMediaFileInfo mediaInfo
    ) {
        return !Objects.equals(remoteMediaFile.getHash(), mediaInfo.getHash())
            || !Objects.equals(remoteMediaFile.getMimeType(), mediaInfo.getMimeType())
            || remoteMediaFile.isAttachment() != Boolean.TRUE.equals(mediaInfo.getAttachment())
            || !Objects.equals(remoteMediaFile.getSizeX(), mediaInfo.getWidth())
            || !Objects.equals(remoteMediaFile.getSizeY(), mediaInfo.getHeight())
            || !Objects.equals(remoteMediaFile.getFileSize(), mediaInfo.getSize())
            || !Objects.equals(remoteMediaFile.getTitle(), mediaInfo.getTitle());
    }

    private void invalidateRemoteMedia(RemoteMediaFile remoteMediaFile, boolean notFound) throws Exception {
        String leaseId = tx.executeWrite(() -> remoteMediaOperations.invalidate(remoteMediaFile.getId()));

        if (notFound) {
            log.warn(
                "Remote media {} at node {} was not found, marking it invalid",
                LogUtil.format(remoteMediaFile.getMediaId()),
                LogUtil.format(remoteMediaFile.getNodeName())
            );
        } else {
            log.warn(
                "Remote media {} at node {} has a different digest, marking it invalid",
                LogUtil.format(remoteMediaFile.getMediaId()),
                LogUtil.format(remoteMediaFile.getNodeName())
            );
        }

        if (leaseId != null) {
            state.remoteNodeName = remoteMediaFile.getNodeName();
            state.leaseIds.add(leaseId);
            checkpoint();
            releaseLeases();
        }
    }

    private int getMediaMaxSize() {
        return Math.min(
            getOptions().getInt("media.max-size"),
            getOptions().getInt(
                parameters.commentId == null ? "posting.media.max-size" : "comment.media.max-size"
            )
        );
    }

    private AttachmentLocation findDownloadableAttachment(int maxSize) {
        Entry entry = findEntry();
        if (entry == null || entry.getCurrentRevision() == null) {
            return null;
        }

        EntryAttachment attachment = entryAttachmentRepository
            .findRemoteMediaToDownload(entry.getCurrentRevision().getId(), maxSize)
            .stream()
            .findFirst()
            .orElse(null);
        if (attachment == null) {
            return null;
        }

        RemoteMediaFile remoteMediaFile = attachment.getRemoteMediaFile();
        return new AttachmentLocation(entry.getId(), remoteMediaFile.getNodeName(), remoteMediaFile.getMediaId());
    }

    private void download(AttachmentLocation attachmentLocation, int maxSize) throws Exception {
        PrivateMediaFileInfo mediaInfo = getPrivateMediaInfo(attachmentLocation.nodeName, attachmentLocation.mediaId);

        String carte = generateCarte(attachmentLocation.nodeName, Scope.VIEW_CONTENT);
        MediaFileOwner mediaFileOwner = tx.executeWriteWithExceptions(() ->
            mediaManager.downloadPrivateMedia(
                attachmentLocation.nodeName, carte, mediaInfo, maxSize, attachmentLocation.entryId
            )
        );
        if (mediaFileOwner == null) {
            log.error(
                "Failed to download private media for media {} at node {}",
                attachmentLocation.mediaId, attachmentLocation.nodeName
            );
            retry();
        }

        Set<String> leaseIds = tx.executeWrite(() -> {
            entryAttachmentRepository.attachDownloadedRemoteMedia(
                nodeId, attachmentLocation.nodeName, attachmentLocation.mediaId, mediaFileOwner
            );
            entryRevisionRepository.clearAttachmentsCacheByRemoteMedia(
                nodeId, attachmentLocation.nodeName, attachmentLocation.mediaId
            );
            return remoteMediaFileRepository.findLeaseIdsByMedia(
                nodeId, attachmentLocation.nodeName, attachmentLocation.mediaId
            );
        });

        state.remoteNodeName = attachmentLocation.nodeName;
        state.leaseIds.addAll(leaseIds);
        checkpoint();

        send(new EntryMediaDownloadedLiberin(
            parameters.postingId,
            parameters.commentId,
            mediaFileOwner.getId(),
            attachmentLocation.nodeName,
            attachmentLocation.mediaId,
            mediaFileOwner.getTitle(),
            mediaFileOwner.getMediaFile().getRecognizedText()
        ));

        releaseLeases();
    }

    private void releaseLeases() throws Exception {
        if (state.remoteNodeName == null || ObjectUtils.isEmpty(state.leaseIds)) {
            state.remoteNodeName = null;
            state.leaseIds.clear();
            checkpoint();
            return;
        }

        for (String leaseId : Set.copyOf(state.leaseIds)) {
            try {
                nodeApi.at(
                    state.remoteNodeName,
                    generateCarte(state.remoteNodeName, Scope.LEASE_MEDIA)
                ).deleteMediaLease(leaseId);
            } catch (MoeraNodeApiNotFoundException e) {
                log.info(
                    "Remote media lease {} on node {} is already released",
                    LogUtil.format(leaseId),
                    LogUtil.format(state.remoteNodeName)
                );
            }

            tx.executeWrite(() ->
                remoteMediaFileRepository.clearLeaseId(nodeId, state.remoteNodeName, leaseId)
            );
            state.leaseIds.remove(leaseId);
            checkpoint();
        }

        state.remoteNodeName = null;
        checkpoint();
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        if (parameters.commentId != null) {
            log.info(
                "Successfully downloaded attachments for posting {}, comment {}",
                LogUtil.format(parameters.postingId),
                LogUtil.format(parameters.commentId)
            );
        } else {
            log.info(
                "Successfully downloaded attachments for posting {}",
                LogUtil.format(parameters.postingId)
            );
        }
    }

    @Override
    protected void failed() {
        super.failed();
        if (parameters.commentId != null) {
            log.error(
                "Failed to download attachments for posting {}, comment {}",
                LogUtil.format(parameters.postingId),
                LogUtil.format(parameters.commentId)
            );
        } else {
            log.error(
                "Failed to download attachments for posting {}",
                LogUtil.format(parameters.postingId)
            );
        }
    }

}
