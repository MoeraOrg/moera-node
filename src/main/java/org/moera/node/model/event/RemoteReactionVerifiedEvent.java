package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.VerificationStatus;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.RemoteReactionVerification;
import org.springframework.data.util.Pair;

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

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("correct", LogUtil.format(correct)));
    }

}
