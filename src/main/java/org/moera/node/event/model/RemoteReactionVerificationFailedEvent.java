package org.moera.node.event.model;

import org.moera.node.data.RemoteReactionVerification;

public class RemoteReactionVerificationFailedEvent extends RemoteReactionVerificationEvent {

    private String errorCode;
    private String errorMessage;

    public RemoteReactionVerificationFailedEvent() {
        super(EventType.REMOTE_REACTION_VERIFICATION_FAILED);
    }

    public RemoteReactionVerificationFailedEvent(RemoteReactionVerification data) {
        super(EventType.REMOTE_REACTION_VERIFICATION_FAILED, data);
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
