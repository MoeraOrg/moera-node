package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import org.moera.node.data.RemoteMediaFileRepository;
import org.moera.node.operations.OcrOperations;
import org.moera.node.task.Job;
import org.springframework.util.ObjectUtils;
import tools.jackson.databind.ObjectMapper;

public class MediaTextUpdatedJob extends Job<MediaTextUpdatedJob.Parameters, Object> {

    public static class Parameters {

        private String senderNodeName;
        private String mediaId;
        private String leaseId;
        private String textContent;

        public Parameters() {
        }

        public Parameters(String senderNodeName, String mediaId, String leaseId, String textContent) {
            this.senderNodeName = senderNodeName;
            this.mediaId = mediaId;
            this.leaseId = leaseId;
            this.textContent = textContent;
        }

        public String getSenderNodeName() {
            return senderNodeName;
        }

        public void setSenderNodeName(String senderNodeName) {
            this.senderNodeName = senderNodeName;
        }

        public String getMediaId() {
            return mediaId;
        }

        public void setMediaId(String mediaId) {
            this.mediaId = mediaId;
        }

        public String getLeaseId() {
            return leaseId;
        }

        public void setLeaseId(String leaseId) {
            this.leaseId = leaseId;
        }

        public String getTextContent() {
            return textContent;
        }

        public void setTextContent(String textContent) {
            this.textContent = textContent;
        }

    }

    @Inject
    private RemoteMediaFileRepository remoteMediaFileRepository;

    @Inject
    private OcrOperations ocrOperations;

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
        this.state = null;
    }

    @Override
    protected void execute() {
        if (ObjectUtils.isEmpty(parameters.textContent)) {
            return;
        }

        tx.executeWrite(() -> {
            var remoteMediaFiles = remoteMediaFileRepository.findByMediaAndLease(
                nodeId, parameters.senderNodeName, parameters.mediaId, parameters.leaseId
            );
            if (remoteMediaFiles.isEmpty()) {
                return;
            }
            remoteMediaFiles.forEach(remoteMediaFile -> remoteMediaFile.setRecognizedText(parameters.textContent));
            ocrOperations.updateRemote(remoteMediaFiles, parameters.textContent);
        });
    }

}
