package org.moera.node.model;

import org.moera.lib.node.types.ReactionTotalInfo;
import org.moera.node.data.ReactionTotal;

public class ReactionTotalInfoUtil {

    public static ReactionTotalInfo countsInfo(ReactionTotal reactionTotal) {
        ReactionTotalInfo info = new ReactionTotalInfo();
        info.setEmoji(reactionTotal.getEmoji());
        info.setTotal(reactionTotal.getTotal());
        return info;
    }

    public static ReactionTotalInfo shareInfo(ReactionTotal reactionTotal, int sum) {
        ReactionTotalInfo info = new ReactionTotalInfo();
        info.setEmoji(reactionTotal.getEmoji());
        info.setShare(((float) reactionTotal.getTotal()) / sum);
        return info;
    }

}
