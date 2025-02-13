package org.moera.node.model;

import org.moera.lib.node.types.ReactionCreated;
import org.moera.lib.node.types.ReactionTotalsInfo;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.node.data.Reaction;

public class ReactionCreatedUtil {
    
    public static ReactionCreated build(Reaction reaction, ReactionTotalsInfo totals, AccessChecker accessChecker) {
        ReactionCreated reactionCreated = new ReactionCreated();
        reactionCreated.setReaction(reaction != null ? ReactionInfoUtil.build(reaction, accessChecker) : null);
        reactionCreated.setTotals(totals);
        return reactionCreated;
    }

}
