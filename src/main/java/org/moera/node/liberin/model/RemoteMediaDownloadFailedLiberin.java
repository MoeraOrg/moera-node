package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;

public class RemoteMediaDownloadFailedLiberin extends Liberin {

    private String nodeName;
    private String mediaId;
    private String errorCode;
    private String errorMessage;

    public RemoteMediaDownloadFailedLiberin(
        String nodeName, String mediaId, String errorCode, String errorMessage
    ) {
        this.nodeName = nodeName;
        this.mediaId = mediaId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("mediaId", mediaId);
        model.put("errorCode", errorCode);
        model.put("errorMessage", errorMessage);
    }

}
