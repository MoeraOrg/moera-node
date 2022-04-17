package org.moera.node.model;

import java.util.Map;

import org.moera.node.auth.principal.Principal;

public interface ReactionsInfo {

    ReactionTotalsInfo getReactions();

    Map<String, Principal> getOperations();

}
