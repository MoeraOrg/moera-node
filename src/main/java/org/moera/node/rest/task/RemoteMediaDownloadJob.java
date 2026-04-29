package org.moera.node.rest.task;

import java.util.Locale;
import java.util.Objects;
import jakarta.inject.Inject;

import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.Scope;
import org.moera.node.config.Config;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.RemoteMediaCache;
import org.moera.node.data.RemoteMediaCacheRepository;
import org.moera.node.data.RemoteMediaError;
import org.moera.node.liberin.model.RemoteMediaDownloadFailedLiberin;
import org.moera.node.liberin.model.RemoteMediaDownloadedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.media.RemoteMediaCacheOperations;
import org.moera.node.model.PrivateMediaFileInfoUtil;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import tools.jackson.databind.ObjectMapper;

public class RemoteMediaDownloadJob extends Job<RemoteMediaDownloadJob.Parameters, RemoteMediaDownloadJob.State> {

    public static class Parameters {

        private String nodeName;
        private String id;
        private String grant;

        public Parameters() {
        }

        public Parameters(String nodeName, String id, String grant) {
            this.nodeName = nodeName;
            this.id = id;
            this.grant = grant;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getGrant() {
            return grant;
        }

        public void setGrant(String grant) {
            this.grant = grant;
        }

    }

    public static class State {

        private PrivateMediaFileInfo mediaInfo;
        private PrivateMediaFileInfo downloadedMediaInfo;

        public State() {
        }

        public PrivateMediaFileInfo getMediaInfo() {
            return mediaInfo;
        }

        public void setMediaInfo(PrivateMediaFileInfo mediaInfo) {
            this.mediaInfo = mediaInfo;
        }

        public PrivateMediaFileInfo getDownloadedMediaInfo() {
            return downloadedMediaInfo;
        }

        public void setDownloadedMediaInfo(PrivateMediaFileInfo downloadedMediaInfo) {
            this.downloadedMediaInfo = downloadedMediaInfo;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(RemoteMediaDownloadJob.class);

    @Inject
    private Config config;

    @Inject
    private MediaManager mediaManager;

    @Inject
    private RemoteMediaCacheRepository remoteMediaCacheRepository;

    @Inject
    private RemoteMediaCacheOperations remoteMediaCacheOperations;

    @Inject
    private MessageSource messageSource;

    public RemoteMediaDownloadJob() {
        state = new State();
        retryCount(10, "PT3S");
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) {
        this.parameters = objectMapper.readValue(parameters, RemoteMediaDownloadJob.Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
        this.state = objectMapper.readValue(state, RemoteMediaDownloadJob.State.class);
    }

    @Override
    protected void started() {
        super.started();
        log.info("Downloading the private media file {} from node {}", parameters.id, parameters.nodeName);
    }

    @Override
    protected void execute() throws Exception {
        if (state.mediaInfo == null) {
            state.mediaInfo = nodeApi.at(parameters.nodeName).getPrivateMediaInfo(parameters.id, parameters.grant);
            checkpoint();
        }
        if (Boolean.TRUE.equals(state.mediaInfo.getMalware())) {
            tx.executeWrite(() ->
                remoteMediaCacheOperations.error(nodeId, parameters.nodeName, parameters.id, RemoteMediaError.MALWARE)
            );
            fail();
        }
        MediaFileOwner mediaFileOwner = tx.executeWriteWithExceptions(() ->
            mediaManager.downloadPrivateMediaNoLimits(
                parameters.nodeName,
                generateCarte(parameters.nodeName, Scope.VIEW_MEDIA),
                state.mediaInfo
            )
        );
        if (mediaFileOwner == null) {
            fail();
        }
        state.downloadedMediaInfo =
            PrivateMediaFileInfoUtil.build(mediaFileOwner, null, config.getMedia().getDirectServe(), null);
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        log.info("Succeeded to download the private media file {} from node {}", parameters.id, parameters.nodeName);
        send(new RemoteMediaDownloadedLiberin(parameters.nodeName, parameters.id, state.downloadedMediaInfo));
    }

    @Override
    protected void failed() {
        super.failed();
        var errorCode = "media.download-failed";
        RemoteMediaError error = remoteMediaCacheRepository.findByMediaWithoutNode(parameters.nodeName, parameters.id)
            .stream()
            .map(RemoteMediaCache::getError)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
        if (error != null && error.getErrorCode() != null) {
            errorCode = error.getErrorCode();
        } else {
            tx.executeWrite(() ->
                remoteMediaCacheOperations.error(
                    nodeId, parameters.nodeName, parameters.id, RemoteMediaError.DOWNLOAD_FAILED
                )
            );
        }
        var errorMessage = messageSource.getMessage(errorCode, null, Locale.getDefault());
        send(new RemoteMediaDownloadFailedLiberin(parameters.nodeName, parameters.id, errorCode, errorMessage));
    }

}
