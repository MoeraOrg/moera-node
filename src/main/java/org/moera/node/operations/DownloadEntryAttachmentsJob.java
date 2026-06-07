package org.moera.node.operations;

import java.util.HashSet;
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
import org.moera.node.media.MediaGrantGenerator;
import org.moera.node.media.MediaManager;
import org.moera.node.media.MediaUtil;
import org.moera.node.task.Job;
import org.moera.node.util.ParametrizedLock;
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

            int maxSize = getMediaMaxSize();

            while (true) {
                AttachmentLocation attachmentLocation = tx.executeRead(() -> findDownloadableAttachment(maxSize));
                if (attachmentLocation == null) {
                    return;
                }
                download(attachmentLocation, maxSize);
                releaseLeases();
            }
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
        Entry entry;
        if (parameters.commentId == null) {
            entry = postingRepository.findByNodeIdAndId(nodeId, parameters.postingId).orElse(null);
        } else {
            Comment comment = commentRepository.findByNodeIdAndId(nodeId, parameters.commentId).orElse(null);
            entry = comment != null
                    && comment.getPosting() != null
                    && Objects.equals(comment.getPosting().getId(), parameters.postingId)
                ? comment
                : null;
        }
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
        String carte = generateCarte(attachmentLocation.nodeName, Scope.VIEW_MEDIA);
        String grant = new MediaGrantGenerator(getOptions()).generateRemote(
            attachmentLocation.mediaId, MediaUtil.MEDIA_GRANT_TTL, false, null
        );
        PrivateMediaFileInfo mediaInfo = nodeApi.at(attachmentLocation.nodeName, carte)
            .getPrivateMediaInfo(attachmentLocation.mediaId, grant);
        if (mediaInfo == null) {
            log.error(
                "Failed to retrieve private media info for media {} at node {}",
                attachmentLocation.mediaId, attachmentLocation.nodeName
            );
            fail();
        }

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
            fail();
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
        state.leaseIds = new HashSet<>(leaseIds);
        checkpoint();
    }

    private void releaseLeases() throws Exception {
        if (state.remoteNodeName == null || ObjectUtils.isEmpty(state.leaseIds)) {
            state.remoteNodeName = null;
            state.leaseIds = new HashSet<>();
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
