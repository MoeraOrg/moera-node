package org.moera.node.data;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.principal.Principal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChildOperations implements Cloneable {

    private Principal view;
    private Principal edit;
    private Principal delete;
    private Principal viewReactions;
    private Principal viewNegativeReactions;
    private Principal viewReactionTotals;
    private Principal viewNegativeReactionTotals;
    private Principal viewReactionRatios;
    private Principal viewNegativeReactionRatios;
    private Principal addReaction;
    private Principal addNegativeReaction;
    private Principal overrideReaction;

    public static ChildOperations decode(String encoded) throws IOException {
        return encoded != null ? new ObjectMapper().readValue(encoded, ChildOperations.class) : null;
    }

    public static String encode(ChildOperations decoded) throws JsonProcessingException {
        return decoded != null ? new ObjectMapper().writeValueAsString(decoded) : null;
    }

    public Principal getView() {
        return view;
    }

    public void setView(Principal view) {
        this.view = view;
    }

    public Principal getEdit() {
        return edit;
    }

    public void setEdit(Principal edit) {
        this.edit = edit;
    }

    public Principal getDelete() {
        return delete;
    }

    public void setDelete(Principal delete) {
        this.delete = delete;
    }

    public Principal getViewReactions() {
        return viewReactions;
    }

    public void setViewReactions(Principal viewReactions) {
        this.viewReactions = viewReactions;
    }

    public Principal getViewNegativeReactions() {
        return viewNegativeReactions;
    }

    public void setViewNegativeReactions(Principal viewNegativeReactions) {
        this.viewNegativeReactions = viewNegativeReactions;
    }

    public Principal getViewReactionTotals() {
        return viewReactionTotals;
    }

    public void setViewReactionTotals(Principal viewReactionTotals) {
        this.viewReactionTotals = viewReactionTotals;
    }

    public Principal getViewNegativeReactionTotals() {
        return viewNegativeReactionTotals;
    }

    public void setViewNegativeReactionTotals(Principal viewNegativeReactionTotals) {
        this.viewNegativeReactionTotals = viewNegativeReactionTotals;
    }

    public Principal getViewReactionRatios() {
        return viewReactionRatios;
    }

    public void setViewReactionRatios(Principal viewReactionRatios) {
        this.viewReactionRatios = viewReactionRatios;
    }

    public Principal getViewNegativeReactionRatios() {
        return viewNegativeReactionRatios;
    }

    public void setViewNegativeReactionRatios(Principal viewNegativeReactionRatios) {
        this.viewNegativeReactionRatios = viewNegativeReactionRatios;
    }

    public Principal getAddReaction() {
        return addReaction;
    }

    public void setAddReaction(Principal addReaction) {
        this.addReaction = addReaction;
    }

    public Principal getAddNegativeReaction() {
        return addNegativeReaction;
    }

    public void setAddNegativeReaction(Principal addNegativeReaction) {
        this.addNegativeReaction = addNegativeReaction;
    }

    public Principal getOverrideReaction() {
        return overrideReaction;
    }

    public void setOverrideReaction(Principal overrideReaction) {
        this.overrideReaction = overrideReaction;
    }

    @Override
    public boolean equals(Object peer) {
        if (this == peer) {
            return true;
        }
        if (peer == null || getClass() != peer.getClass()) {
            return false;
        }
        ChildOperations that = (ChildOperations) peer;
        return Objects.equals(view, that.view)
                && Objects.equals(edit, that.edit)
                && Objects.equals(delete, that.delete)
                && Objects.equals(viewReactions, that.viewReactions)
                && Objects.equals(viewNegativeReactions, that.viewNegativeReactions)
                && Objects.equals(viewReactionTotals, that.viewReactionTotals)
                && Objects.equals(viewNegativeReactionTotals, that.viewNegativeReactionTotals)
                && Objects.equals(viewReactionRatios, that.viewReactionRatios)
                && Objects.equals(viewNegativeReactionRatios, that.viewNegativeReactionRatios)
                && Objects.equals(addReaction, that.addReaction)
                && Objects.equals(addNegativeReaction, that.addNegativeReaction)
                && Objects.equals(overrideReaction, that.overrideReaction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(view, edit, delete, viewReactions, viewNegativeReactions, viewReactionTotals,
                viewNegativeReactionTotals, viewReactionRatios, viewNegativeReactionRatios, addReaction,
                addNegativeReaction, overrideReaction);
    }

    @Override
    public ChildOperations clone() throws CloneNotSupportedException {
        try {
            return decode(encode(this));
        } catch (IOException e) {
            throw new CloneNotSupportedException("Cannot encode or decode object");
        }
    }

    @Override
    public String toString() {
        try {
            return encode(this);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

}
