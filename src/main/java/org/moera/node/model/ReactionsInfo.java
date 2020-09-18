package org.moera.node.model;

import java.util.Map;

public interface ReactionsInfo {

    ReactionTotalsInfo getReactions();

    Map<String, String[]> getOperations();

}
