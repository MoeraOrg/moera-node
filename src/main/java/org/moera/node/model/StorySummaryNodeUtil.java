package org.moera.node.model;

import org.moera.lib.node.types.StorySummaryNode;
import org.moera.node.data.Contact;

public class StorySummaryNodeUtil {

    public static StorySummaryNode build(String ownerName, String ownerFullName, String ownerGender) {
        StorySummaryNode node = new StorySummaryNode();
        node.setOwnerName(ownerName);
        node.setOwnerFullName(ownerFullName);
        node.setOwnerGender(ownerGender);
        return node;
    }

    public static StorySummaryNode build(Contact contact) {
        return build(contact.getRemoteNodeName(), contact.getRemoteFullName(), contact.getRemoteGender());
    }

}
