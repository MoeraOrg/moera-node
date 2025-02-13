package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.ReactionInfo;
import org.moera.lib.util.LogUtil;
import org.springframework.data.util.Pair;

public class RemoteReactionAddedEvent extends RemoteReactionEvent {

    private boolean negative;
    private int emoji;
    private long createdAt;

    public RemoteReactionAddedEvent() {
        super(EventType.REMOTE_REACTION_ADDED);
    }

    public RemoteReactionAddedEvent(String remoteNodeName, String remotePostingId, ReactionInfo reactionInfo) {
        super(EventType.REMOTE_REACTION_ADDED, remoteNodeName, remotePostingId);
        this.negative = Boolean.TRUE.equals(reactionInfo.getNegative());
        this.emoji = reactionInfo.getEmoji();
        this.createdAt = reactionInfo.getCreatedAt();
    }

    public boolean isNegative() {
        return negative;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

    public int getEmoji() {
        return emoji;
    }

    public void setEmoji(int emoji) {
        this.emoji = emoji;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("negative", LogUtil.format(negative)));
        parameters.add(Pair.of("emoji", LogUtil.format(emoji)));
    }

}
