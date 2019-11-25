package org.moera.node.event.model;

public class RemotePostingVerifyFailedEvent extends RemotePostingEvent {

    private String errorCode;
    private String errorMessage;

    public RemotePostingVerifyFailedEvent() {
        super(EventType.REMOTE_POSTING_VERIFY_FAILED);
    }

    public RemotePostingVerifyFailedEvent(String nodeName, String id, String revisionId,
                                          String errorCode, String errorMessage) {
        super(EventType.REMOTE_POSTING_VERIFY_FAILED, nodeName, id, revisionId);
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

}
