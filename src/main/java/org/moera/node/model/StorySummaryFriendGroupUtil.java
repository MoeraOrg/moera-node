package org.moera.node.model;

import org.moera.lib.node.types.StorySummaryFriendGroup;

public class StorySummaryFriendGroupUtil {

    public static StorySummaryFriendGroup build(String id, String title) {
        StorySummaryFriendGroup group = new StorySummaryFriendGroup();
        group.setId(id);
        group.setTitle(title);
        return group;
    }

}
