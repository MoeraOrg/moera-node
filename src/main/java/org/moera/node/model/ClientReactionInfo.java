package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Reaction;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientReactionInfo {

    @JsonIgnore
    private String postingId;

    private boolean negative;
    private int emoji;
    private long createdAt;
    private Long deadline;

    public ClientReactionInfo() {
    }

    public ClientReactionInfo(Reaction reaction) {
        postingId = reaction.getEntryRevision().getEntry().getId().toString();
        negative = reaction.isNegative();
        emoji = reaction.getEmoji();
        createdAt = Util.toEpochSecond(reaction.getCreatedAt());
        deadline = Util.toEpochSecond(reaction.getDeadline());
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
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

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

}
