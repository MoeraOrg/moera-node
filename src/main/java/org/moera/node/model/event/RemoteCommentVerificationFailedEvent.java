package org.moera.node.model.event;

import org.moera.node.data.RemoteCommentVerification;

public class RemoteCommentVerificationFailedEvent extends RemoteCommentVerificationEvent {

    private String errorCode;
    private String errorMessage;

    public RemoteCommentVerificationFailedEvent() {
        super(EventType.REMOTE_COMMENT_VERIFICATION_FAILED);
    }

    public RemoteCommentVerificationFailedEvent(RemoteCommentVerification data) {
        super(EventType.REMOTE_COMMENT_VERIFICATION_FAILED, data);
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
