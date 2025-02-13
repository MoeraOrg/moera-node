package org.moera.node.model;

import org.moera.lib.node.types.AvatarOrdinal;

public class AvatarOrdinalUtil {

    public static AvatarOrdinal build(String id, int ordinal) {
        AvatarOrdinal avatarOrdinal = new AvatarOrdinal();
        avatarOrdinal.setId(id);
        avatarOrdinal.setOrdinal(ordinal);
        return avatarOrdinal;
    }

}
