package org.moera.node.event.model;

import org.moera.node.data.RemotePostingVerification;

public class RemotePostingVerifyFailedEvent extends RemotePostingEvent {

    private String errorCode;
    private String errorMessage;

    public RemotePostingVerifyFailedEvent() {
        super(EventType.REMOTE_POSTING_VERIFY_FAILED);
    }

    public RemotePostingVerifyFailedEvent(RemotePostingVerification data) {
        super(EventType.REMOTE_POSTING_VERIFY_FAILED, data);
        errorCode = data.getErrorCode();
        errorMessage = data.getErrorMessage();
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

}
