package org.moera.node.model;

import org.moera.lib.node.types.RejectedReactions;
import org.springframework.util.ObjectUtils;

public class RejectedReactionsUtil {

    public static RejectedReactions build(String positive, String negative) {
        if (ObjectUtils.isEmpty(positive) && ObjectUtils.isEmpty(negative)) {
            return null;
        }
        RejectedReactions rejectedReactions = new RejectedReactions();
        if (!ObjectUtils.isEmpty(positive)) {
            rejectedReactions.setPositive(positive);
        }
        if (!ObjectUtils.isEmpty(negative)) {
            rejectedReactions.setNegative(negative);
        }
        return rejectedReactions;
    }

}
