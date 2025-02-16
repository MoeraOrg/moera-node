package org.moera.node.model;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.moera.lib.node.types.CommentMassAttributes;
import org.moera.lib.node.types.CommentOperations;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Entry;

public class CommentMassAttributesUtil {

    public static void toEntry(CommentMassAttributes attributes, Entry entry) {
        CommentOperations operations = attributes.getSeniorOperations();
        toPrincipal(operations::getView, entry::setParentViewPrincipal);
        toPrincipal(operations::getViewReactions, entry::setParentViewReactionsPrincipal);
        toPrincipal(operations::getViewNegativeReactions, entry::setParentViewNegativeReactionsPrincipal);
        toPrincipal(operations::getViewReactionTotals, entry::setParentViewReactionTotalsPrincipal);
        toPrincipal(operations::getViewNegativeReactionTotals, entry::setParentViewNegativeReactionTotalsPrincipal);
        toPrincipal(operations::getViewReactionRatios, entry::setParentViewReactionRatiosPrincipal);
        toPrincipal(operations::getViewNegativeReactionRatios, entry::setParentViewNegativeReactionRatiosPrincipal);
        toPrincipal(operations::getAddReaction, entry::setParentAddReactionPrincipal);
        toPrincipal(operations::getAddNegativeReaction, entry::setParentAddNegativeReactionPrincipal);
    }

    private static void toPrincipal(Supplier<Principal> getPrincipal, Consumer<Principal> setPrincipal) {
        Principal value = getPrincipal.get();
        if (value != null) {
            setPrincipal.accept(value);
        }
    }

    public static boolean sameAsEntry(CommentMassAttributes attributes, Entry entry) {
        CommentOperations operations = attributes.getSeniorOperations();
        return samePrincipalAs(operations::getView, entry.getParentViewPrincipal())
            && samePrincipalAs(operations::getViewReactions, entry.getParentViewReactionsPrincipal())
            && samePrincipalAs(operations::getViewNegativeReactions, entry.getParentViewNegativeReactionsPrincipal())
            && samePrincipalAs(operations::getViewReactionTotals, entry.getParentViewReactionTotalsPrincipal())
            && samePrincipalAs(
                operations::getViewNegativeReactionTotals,
                entry.getParentViewNegativeReactionTotalsPrincipal()
            )
            && samePrincipalAs(operations::getViewReactionRatios, entry.getParentViewReactionRatiosPrincipal())
            && samePrincipalAs(
                operations::getViewNegativeReactionRatios,
                entry.getParentViewNegativeReactionRatiosPrincipal()
            )
            && samePrincipalAs(operations::getAddReaction, entry.getParentAddReactionPrincipal())
            && samePrincipalAs(operations::getAddNegativeReaction, entry.getParentAddNegativeReactionPrincipal());
    }

    private static boolean samePrincipalAs(Supplier<Principal> getPrincipal, Principal principal) {
        Principal value = getPrincipal.get();
        return value == null || principal == null && value.isUnset() || Objects.equals(value, principal);
    }

}
