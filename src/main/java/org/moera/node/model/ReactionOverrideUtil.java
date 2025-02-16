package org.moera.node.model;

import java.util.function.Consumer;

import org.moera.lib.node.types.ReactionOverride;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Reaction;

public class ReactionOverrideUtil {

    public static void toReaction(ReactionOverride override, Reaction reaction) {
        if (override.getOperations() != null) {
            toPrincipal(override.getOperations().getView(), reaction::setViewPrincipal);
        }
    }

    public static void toPostingReaction(ReactionOverride override, Reaction reaction) {
        toReaction(override, reaction);

        if (override.getSeniorOperations() != null) {
            toPrincipal(override.getSeniorOperations().getView(), reaction::setPostingViewPrincipal);
            toPrincipal(override.getSeniorOperations().getView(), reaction::setPostingDeletePrincipal);
        }
    }

    public static void toCommentReaction(ReactionOverride override, Reaction reaction) {
        toReaction(override, reaction);

        if (override.getSeniorOperations() != null) {
            toPrincipal(override.getSeniorOperations().getView(), reaction::setCommentViewPrincipal);
            toPrincipal(override.getSeniorOperations().getView(), reaction::setCommentDeletePrincipal);
        }

        if (override.getMajorOperations() != null) {
            toPrincipal(override.getMajorOperations().getView(), reaction::setPostingViewPrincipal);
            toPrincipal(override.getMajorOperations().getDelete(), reaction::setPostingDeletePrincipal);
        }
    }

    private static void toPrincipal(Principal value, Consumer<Principal> setPrincipal) {
        if (value != null) {
            setPrincipal.accept(value);
        }
    }

}
