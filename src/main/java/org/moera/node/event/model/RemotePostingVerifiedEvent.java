package org.moera.node.event.model;

import org.moera.node.data.RemotePostingVerification;
import org.moera.node.data.VerificationStatus;

public class RemotePostingVerifiedEvent extends RemotePostingVerificationEvent {

    private boolean correct;

    public RemotePostingVerifiedEvent() {
        super(EventType.REMOTE_POSTING_VERIFIED);
    }

    public RemotePostingVerifiedEvent(RemotePostingVerification data) {
        super(EventType.REMOTE_POSTING_VERIFIED, data);
        correct = data.getStatus() == VerificationStatus.CORRECT;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

}
