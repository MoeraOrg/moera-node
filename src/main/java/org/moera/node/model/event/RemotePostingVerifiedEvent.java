package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.RemotePostingVerification;
import org.moera.node.data.VerificationStatus;
import org.springframework.data.util.Pair;

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

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("correct", LogUtil.format(correct)));
    }

}
