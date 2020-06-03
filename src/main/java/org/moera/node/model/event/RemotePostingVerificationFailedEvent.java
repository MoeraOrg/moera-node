package org.moera.node.model.event;

import org.moera.node.data.RemotePostingVerification;

public class RemotePostingVerificationFailedEvent extends RemotePostingVerificationEvent {

    private String errorCode;
    private String errorMessage;

    public RemotePostingVerificationFailedEvent() {
        super(EventType.REMOTE_POSTING_VERIFICATION_FAILED);
    }

    public RemotePostingVerificationFailedEvent(RemotePostingVerification data) {
        super(EventType.REMOTE_POSTING_VERIFICATION_FAILED, data);
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
