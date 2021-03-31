package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.RemoteCommentVerification;
import org.moera.node.data.VerificationStatus;
import org.springframework.data.util.Pair;

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

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("correct", LogUtil.format(correct)));
    }

}
