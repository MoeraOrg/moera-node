package org.moera.node.event.model;

public class RemotePostingVerifiedEvent extends RemotePostingEvent {

    private boolean correct;

    public RemotePostingVerifiedEvent() {
        super(EventType.REMOTE_POSTING_VERIFIED);
    }

    public RemotePostingVerifiedEvent(String nodeName, String id, String revisionId, boolean correct) {
        super(EventType.REMOTE_POSTING_VERIFIED, nodeName, id, revisionId);
        this.correct = correct;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

}
