package org.moera.node.model.event;

import org.moera.node.model.ReactionInfo;

public class RemoteReactionAddedEvent extends RemoteReactionEvent {

    private boolean negative;
    private int emoji;
    private long createdAt;

    public RemoteReactionAddedEvent() {
        super(EventType.REMOTE_REACTION_ADDED);
    }

    public RemoteReactionAddedEvent(String remoteNodeName, String remotePostingId, ReactionInfo reactionInfo) {
        super(EventType.REMOTE_REACTION_ADDED, remoteNodeName, remotePostingId);
        this.negative = reactionInfo.isNegative();
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

}
