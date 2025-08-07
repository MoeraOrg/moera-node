package org.moera.node.model;

import java.util.List;

import org.moera.lib.node.types.ContactInfo;
import org.moera.lib.node.types.FriendGroupDetails;
import org.moera.lib.node.types.FriendInfo;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.node.data.Contact;
import org.moera.node.data.Friend;
import org.moera.node.option.Options;

public class FriendInfoUtil {

    public static FriendInfo build(Contact contact, Options options, AccessChecker accessChecker) {
        return build(
            contact.getRemoteNodeName(),
            ContactInfoUtil.build(contact, options, accessChecker),
            null
        );
    }

    public static FriendInfo build(Friend friend, Options options, AccessChecker accessChecker) {
        return build(
            friend.getRemoteNodeName(),
            ContactInfoUtil.build(friend.getContact(), options, accessChecker),
            null
        );
    }

    public static FriendInfo build(String nodeName, ContactInfo contact, List<FriendGroupDetails> groups) {
        FriendInfo friendInfo = new FriendInfo();
        friendInfo.setNodeName(nodeName);
        friendInfo.setContact(contact);
        friendInfo.setGroups(groups);
        return friendInfo;
    }

    public static void protect(FriendInfo friendInfo, AccessChecker accessChecker) {
        if (friendInfo.getContact() != null) {
            ContactInfoUtil.protect(friendInfo.getContact(), accessChecker);
        }
    }

}
