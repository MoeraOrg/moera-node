package org.moera.node.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

import org.moera.lib.node.types.FriendGroupsFeatures;
import org.moera.node.data.Friend;
import org.moera.node.data.FriendGroup;

public class FriendGroupsFeaturesUtil {

    public static FriendGroupsFeatures forAdmin(FriendGroup[] nodeGroups) {
        FriendGroupsFeatures features = new FriendGroupsFeatures();
        features.setAvailable(
            Arrays.stream(nodeGroups)
                .map(fg -> FriendGroupInfoUtil.build(fg, true))
                .toList()
        );
        return features;
    }

    public static FriendGroupsFeatures forRegular(FriendGroup[] nodeGroups, Friend[] clientGroups) {
        FriendGroupsFeatures features = new FriendGroupsFeatures();
        features.setAvailable(
            Arrays.stream(nodeGroups)
                .filter(fg -> isFriendGroupVisible(fg, clientGroups))
                .map(fg -> FriendGroupInfoUtil.build(fg, false))
                .toList()
        );
        if (clientGroups != null) {
            features.setMemberOf(
                Arrays.stream(clientGroups)
                    .map(fr -> FriendGroupDetailsUtil.build(fr, false))
                    .collect(Collectors.toList())
            );
        } else {
            features.setMemberOf(Collections.emptyList());
        }
        return features;

    }

    private static boolean isFriendGroupVisible(FriendGroup friendGroup, Friend[] clientGroups) {
        return friendGroup.getViewPrincipal().isPublic()
            || friendGroup.getViewPrincipal().isPrivate() && isMemberOf(friendGroup.getId(), clientGroups);
    }

    private static boolean isMemberOf(UUID friendGroupId, Friend[] clientGroups) {
        if (clientGroups == null) {
            return false;
        }
        for (Friend clientGroup : clientGroups) {
            if (clientGroup.getFriendGroup().getId().equals(friendGroupId)) {
                return true;
            }
        }
        return false;
    }

}
