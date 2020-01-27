package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.ReactionTotal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReactionTotalInfo {

    private int emoji;
    private Integer total;
    private Double share;

    public ReactionTotalInfo() {
    }

    public static ReactionTotalInfo countsInfo(ReactionTotal reactionTotal) {
        ReactionTotalInfo info = new ReactionTotalInfo();
        info.emoji = reactionTotal.getEmoji();
        info.total = reactionTotal.getTotal();
        return info;
    }

    public static ReactionTotalInfo shareInfo(ReactionTotal reactionTotal, int sum) {
        ReactionTotalInfo info = new ReactionTotalInfo();
        info.emoji = reactionTotal.getEmoji();
        info.share = ((double) reactionTotal.getTotal()) / sum;
        return info;
    }

    public int getEmoji() {
        return emoji;
    }

    public void setEmoji(int emoji) {
        this.emoji = emoji;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Double getShare() {
        return share;
    }

    public void setShare(Double share) {
        this.share = share;
    }

}
