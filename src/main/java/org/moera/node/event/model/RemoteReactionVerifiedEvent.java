package org.moera.node.event.model;

import org.moera.node.data.RemoteReactionVerification;
import org.moera.node.data.VerificationStatus;

public class RemoteReactionVerifiedEvent extends RemoteReactionVerificationEvent {

    private boolean correct;

    public RemoteReactionVerifiedEvent() {
        super(EventType.REMOTE_REACTION_VERIFIED);
    }

    public RemoteReactionVerifiedEvent(RemoteReactionVerification data) {
        super(EventType.REMOTE_REACTION_VERIFIED, data);
        correct = data.getStatus() == VerificationStatus.CORRECT;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

}
