package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.util.LogUtil;
import org.springframework.data.util.Pair;

public class RemoteMediaDownloadFailedEvent extends RemoteMediaDownloadEvent {

    private String errorCode;
    private String errorMessage;

    public RemoteMediaDownloadFailedEvent() {
        super(EventType.REMOTE_MEDIA_DOWNLOAD_FAILED);
    }

    public RemoteMediaDownloadFailedEvent(String nodeName, String mediaId, String errorCode, String errorMessage) {
        super(EventType.REMOTE_MEDIA_DOWNLOAD_FAILED, nodeName, mediaId);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
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
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("errorCode", LogUtil.format(errorCode)));
    }

}
