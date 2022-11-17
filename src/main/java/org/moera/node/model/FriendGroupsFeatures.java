package org.moera.node.model;

import java.util.Arrays;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Friend;
import org.moera.node.data.FriendGroup;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FriendGroupsFeatures {

    private FriendGroupInfo[] available;
    private FriendGroupDetails[] memberOf;

    public static FriendGroupsFeatures forAdmin(FriendGroup[] nodeGroups) {
        FriendGroupsFeatures features = new FriendGroupsFeatures();
        features.setAvailable(Arrays.stream(nodeGroups)
                .map(FriendGroupInfo::new)
                .toArray(FriendGroupInfo[]::new));
        return features;
    }

    public static FriendGroupsFeatures forRegular(FriendGroup[] nodeGroups, Friend[] clientGroups) {
        FriendGroupsFeatures features = new FriendGroupsFeatures();
        features.setAvailable(Arrays.stream(nodeGroups)
                .filter(fg -> isFriendGroupVisible(fg, clientGroups))
                .map(FriendGroupInfo::new)
                .toArray(FriendGroupInfo[]::new));
        features.setMemberOf(
                Arrays.stream(clientGroups)
                        .map(fr -> new FriendGroupDetails(fr, false))
                        .toArray(FriendGroupDetails[]::new)
        );
        return features;

    }

    private static boolean isFriendGroupVisible(FriendGroup friendGroup, Friend[] clientGroups) {
        return friendGroup.getViewPrincipal().isPublic()
                || friendGroup.getViewPrincipal().isPrivate() && isMemberOf(friendGroup.getId(), clientGroups);
    }

    private static boolean isMemberOf(UUID friendGroupId, Friend[] clientGroups) {
        for (Friend clientGroup : clientGroups) {
            if (clientGroup.getFriendGroup().getId().equals(friendGroupId)) {
                return true;
            }
        }
        return false;
    }

    public FriendGroupInfo[] getAvailable() {
        return available;
    }

    public void setAvailable(FriendGroupInfo[] available) {
        this.available = available;
    }

    public FriendGroupDetails[] getMemberOf() {
        return memberOf;
    }

    public void setMemberOf(FriendGroupDetails[] memberOf) {
        this.memberOf = memberOf;
    }

}
