package org.moera.node.model;

import java.util.Map;

import org.moera.node.auth.principal.Principal;

public class ReactionAttributes {

    private boolean negative;
    private int emoji;
    private Map<String, Principal> operations;

    public boolean isNegative() {
        return negative;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

    public int getEmoji() {
        return emoji;
    }

    public void setEmoji(int emoji) {
        this.emoji = emoji;
    }

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

}
