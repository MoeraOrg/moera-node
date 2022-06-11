package org.moera.node.model;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Reaction;

public class ReactionOverride {

    private Map<String, Principal> operations;

    private Map<String, Principal> seniorOperations;

    private Map<String, Principal> majorOperations;

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

    public Principal getPrincipal(String operationName) {
        return operations != null ? operations.get(operationName) : null;
    }

    public Map<String, Principal> getSeniorOperations() {
        return seniorOperations;
    }

    public void setSeniorOperations(Map<String, Principal> seniorOperations) {
        this.seniorOperations = seniorOperations;
    }

    public Principal getSeniorPrincipal(String operationName) {
        return seniorOperations != null ? seniorOperations.get(operationName) : null;
    }

    public Map<String, Principal> getMajorOperations() {
        return majorOperations;
    }

    public void setMajorOperations(Map<String, Principal> majorOperations) {
        this.majorOperations = majorOperations;
    }

    public Principal getMajorPrincipal(String operationName) {
        return majorOperations != null ? majorOperations.get(operationName) : null;
    }

    public void toReaction(Reaction reaction) {
        toPrincipal(this::getPrincipal, "view", reaction::setViewPrincipal);
    }

    public void toPostingReaction(Reaction reaction) {
        toReaction(reaction);

        toPrincipal(this::getSeniorPrincipal, "view", reaction::setPostingViewPrincipal);
        toPrincipal(this::getSeniorPrincipal, "delete", reaction::setPostingDeletePrincipal);
    }

    public void toCommentReaction(Reaction reaction) {
        toReaction(reaction);

        toPrincipal(this::getSeniorPrincipal, "view", reaction::setCommentViewPrincipal);
        toPrincipal(this::getSeniorPrincipal, "delete", reaction::setCommentDeletePrincipal);

        toPrincipal(this::getMajorPrincipal, "view", reaction::setPostingViewPrincipal);
        toPrincipal(this::getMajorPrincipal, "delete", reaction::setPostingDeletePrincipal);
    }

    private void toPrincipal(Function<String, Principal> getPrincipal, String operationName,
                             Consumer<Principal> setPrincipal) {
        Principal value = getPrincipal.apply(operationName);
        if (value != null) {
            setPrincipal.accept(value);
        }
    }

}
