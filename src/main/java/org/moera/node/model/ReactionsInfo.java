package org.moera.node.model;

import java.util.Map;

import org.moera.lib.node.types.ReactionTotalsInfo;
import org.moera.lib.node.types.principal.Principal;

public interface ReactionsInfo {

    ReactionTotalsInfo getReactions();

    Map<String, Principal> getOperations();

    default Principal getPrincipal(String operationName, Principal defaultValue) {
        return getOperations() != null ? getOperations().getOrDefault(operationName, defaultValue) : defaultValue;
    }

}
