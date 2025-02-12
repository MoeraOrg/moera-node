package org.moera.node.model;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Entry;

public class CommentMassAttributes {

    private Map<String, Principal> seniorOperations;

    public Map<String, Principal> getSeniorOperations() {
        return seniorOperations;
    }

    public void setSeniorOperations(Map<String, Principal> seniorOperations) {
        this.seniorOperations = seniorOperations;
    }

    public Principal getSeniorPrincipal(String operationName) {
        return seniorOperations != null ? seniorOperations.get(operationName) : null;
    }

    public void toEntry(Entry entry) {
        toSeniorPrincipal("view", entry::setParentViewPrincipal);
        toSeniorPrincipal("viewReactions", entry::setParentViewReactionsPrincipal);
        toSeniorPrincipal("viewNegativeReactions", entry::setParentViewNegativeReactionsPrincipal);
        toSeniorPrincipal("viewReactionTotals", entry::setParentViewReactionTotalsPrincipal);
        toSeniorPrincipal("viewNegativeReactionTotals", entry::setParentViewNegativeReactionTotalsPrincipal);
        toSeniorPrincipal("viewReactionRatios", entry::setParentViewReactionRatiosPrincipal);
        toSeniorPrincipal("viewNegativeReactionRatios", entry::setParentViewNegativeReactionRatiosPrincipal);
        toSeniorPrincipal("addReaction", entry::setParentAddReactionPrincipal);
        toSeniorPrincipal("addNegativeReaction", entry::setParentAddNegativeReactionPrincipal);
    }

    private void toSeniorPrincipal(String operationName, Consumer<Principal> setPrincipal) {
        Principal value = getSeniorPrincipal(operationName);
        if (value != null) {
            setPrincipal.accept(value);
        }
    }

    public boolean sameAsEntry(Entry entry) {
        return sameSeniorPrincipalAs("view", entry.getParentViewPrincipal())
                && sameSeniorPrincipalAs("viewReactions", entry.getParentViewReactionsPrincipal())
                && sameSeniorPrincipalAs("viewNegativeReactions", entry.getParentViewNegativeReactionsPrincipal())
                && sameSeniorPrincipalAs("viewReactionTotals", entry.getParentViewReactionTotalsPrincipal())
                && sameSeniorPrincipalAs("viewNegativeReactionTotals",
                                         entry.getParentViewNegativeReactionTotalsPrincipal())
                && sameSeniorPrincipalAs("viewReactionRatios", entry.getParentViewReactionRatiosPrincipal())
                && sameSeniorPrincipalAs("viewNegativeReactionRatios",
                                         entry.getParentViewNegativeReactionRatiosPrincipal())
                && sameSeniorPrincipalAs("addReaction", entry.getParentAddReactionPrincipal())
                && sameSeniorPrincipalAs("addNegativeReaction", entry.getParentAddNegativeReactionPrincipal());
    }

    private boolean sameSeniorPrincipalAs(String operationName, Principal principal) {
        Principal value = getSeniorPrincipal(operationName);
        return value == null || principal == null && value.isUnset() || Objects.equals(value, principal);
    }

}
