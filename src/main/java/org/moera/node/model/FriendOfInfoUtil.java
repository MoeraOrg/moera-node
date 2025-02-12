package org.moera.node.model;

import java.util.Collections;
import java.util.List;

import org.moera.lib.node.types.ContactInfo;
import org.moera.lib.node.types.FriendGroupDetails;
import org.moera.lib.node.types.FriendOfInfo;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.node.data.FriendOf;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

public class FriendOfInfoUtil {

    public static FriendOfInfo build(FriendOf friendOf, Options options) {
        FriendOfInfo friendOfInfo = new FriendOfInfo();
        friendOfInfo.setRemoteNodeName(friendOf.getRemoteNodeName());
        if (friendOf.getContact() != null) {
            friendOfInfo.setContact(ContactInfoUtil.build(friendOf.getContact(), options));
        }
        friendOfInfo.setGroups(Collections.singletonList(
            FriendGroupDetailsUtil.build(
                friendOf.getRemoteGroupId(),
                friendOf.getRemoteGroupTitle(),
                Util.toEpochSecond(friendOf.getRemoteAddedAt())
            )
        ));
        return friendOfInfo;
    }

    public static FriendOfInfo build(FriendOf friendOf, Options options, AccessChecker accessChecker) {
        FriendOfInfo friendOfInfo = build(friendOf, options);
        protect(friendOfInfo, accessChecker);
        return friendOfInfo;
    }

    public static FriendOfInfo build(String remoteNodeName, ContactInfo contact, List<FriendGroupDetails> groups) {
        FriendOfInfo friendOfInfo = new FriendOfInfo();
        friendOfInfo.setRemoteNodeName(remoteNodeName);
        friendOfInfo.setContact(contact);
        friendOfInfo.setGroups(groups);
        return friendOfInfo;
    }

    public static void protect(FriendOfInfo friendOf, AccessChecker accessChecker) {
        ContactInfoUtil.protect(friendOf.getContact(), accessChecker);
    }

}
