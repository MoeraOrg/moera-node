package org.moera.node.model.event;

import org.moera.node.data.RemoteCommentVerification;
import org.moera.node.data.VerificationStatus;

public class RemoteCommentVerifiedEvent extends RemoteCommentVerificationEvent {

    private boolean correct;

    public RemoteCommentVerifiedEvent() {
        super(EventType.REMOTE_COMMENT_VERIFIED);
    }

    public RemoteCommentVerifiedEvent(RemoteCommentVerification data) {
        super(EventType.REMOTE_COMMENT_VERIFIED, data);
        correct = data.getStatus() == VerificationStatus.CORRECT;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

}
