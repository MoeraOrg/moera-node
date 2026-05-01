package org.moera.node.data;

import org.moera.lib.node.types.principal.Principal;

public class ChildOperationsUtil {

    public static void copyAll(Entry target, Entry source) {
        target.setChildOperations(copy(source.getChildOperations()));
        target.setReactionOperations(copy(source.getReactionOperations()));
        target.setChildReactionOperations(copy(source.getChildReactionOperations()));
    }

    public static void setRestrictive(Entry entry) {
        entry.setChildOperations(restrictiveChildOperations());
        entry.setReactionOperations(restrictiveReactionOperations());
        entry.setChildReactionOperations(restrictiveReactionOperations());
    }

    public static ChildOperations copy(ChildOperations operations) {
        if (operations == null) {
            return new ChildOperations();
        }
        try {
            return operations.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ChildOperations restrictiveChildOperations() {
        ChildOperations operations = new ChildOperations();
        operations.setView(Principal.ADMIN);
        operations.setEdit(Principal.NONE);
        operations.setDelete(Principal.ADMIN);
        operations.setViewReactions(Principal.ADMIN);
        operations.setViewNegativeReactions(Principal.ADMIN);
        operations.setViewReactionTotals(Principal.ADMIN);
        operations.setViewNegativeReactionTotals(Principal.ADMIN);
        operations.setViewReactionRatios(Principal.ADMIN);
        operations.setViewNegativeReactionRatios(Principal.ADMIN);
        operations.setAddReaction(Principal.NONE);
        operations.setAddNegativeReaction(Principal.NONE);
        operations.setOverrideReaction(Principal.NONE);
        return operations;
    }

    private static ChildOperations restrictiveReactionOperations() {
        ChildOperations operations = new ChildOperations();
        operations.setView(Principal.ADMIN);
        operations.setDelete(Principal.ADMIN);
        return operations;
    }

}
