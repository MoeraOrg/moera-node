package org.moera.node.model;

import org.moera.lib.node.types.SearchNodeInfo;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.Contact;

public class SearchNodeInfoUtil {

    public static SearchNodeInfo build(Contact contact, DirectServeConfig config) {
        SearchNodeInfo info = new SearchNodeInfo();
        info.setNodeName(contact.getRemoteNodeName());
        info.setFullName(contact.getRemoteFullName());
        info.setTitle(contact.getRemoteTitle());
        if (contact.getRemoteAvatarMediaFile() != null) {
            info.setAvatar(
                AvatarImageUtil.build(contact.getRemoteAvatarMediaFile(), contact.getRemoteAvatarShape(), config)
            );
        }
        info.setDistance(contact.getDistance());
        return info;
    }

}
