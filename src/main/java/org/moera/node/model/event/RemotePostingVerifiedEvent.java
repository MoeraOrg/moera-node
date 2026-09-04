package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.VerificationStatus;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.RemotePostingVerification;

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
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("correct", LogUtil.format(correct)));
    }

}
