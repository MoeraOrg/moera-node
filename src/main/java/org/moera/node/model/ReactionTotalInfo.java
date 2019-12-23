package org.moera.node.model;

import org.moera.node.data.ReactionTotal;

public class ReactionTotalInfo {

    private int emoji;
    private int total;

    public ReactionTotalInfo() {
    }

    public ReactionTotalInfo(ReactionTotal reactionTotal) {
        emoji = reactionTotal.getEmoji();
        total = reactionTotal.getTotal();
    }

    public int getEmoji() {
        return emoji;
    }

    public void setEmoji(int emoji) {
        this.emoji = emoji;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

}
