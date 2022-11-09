package org.moera.node.model;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.FriendGroup;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FriendGroupsFeatures {

    private FriendGroupInfo[] available;
    private FriendGroupInfo[] memberOf;

    public static FriendGroupsFeatures forAdmin(FriendGroup[] nodeGroups) {
        FriendGroupsFeatures features = new FriendGroupsFeatures();
        features.setAvailable(Arrays.stream(nodeGroups)
                .map(fg -> new FriendGroupInfo(fg, true))
                .toArray(FriendGroupInfo[]::new));
        return features;
    }

    public static FriendGroupsFeatures forRegular(FriendGroup[] nodeGroups, FriendGroup[] clientGroups) {
        FriendGroupsFeatures features = new FriendGroupsFeatures();
        features.setAvailable(Arrays.stream(nodeGroups)
                .filter(FriendGroup::isVisible)
                .map(fg -> new FriendGroupInfo(fg, false))
                .toArray(FriendGroupInfo[]::new));
        if (clientGroups != null) {
            features.setMemberOf(Arrays.stream(clientGroups)
                    .map(fg -> new FriendGroupInfo(fg, false))
                    .toArray(FriendGroupInfo[]::new));
        }
        return features;

    }

    public FriendGroupInfo[] getAvailable() {
        return available;
    }

    public void setAvailable(FriendGroupInfo[] available) {
        this.available = available;
    }

    public FriendGroupInfo[] getMemberOf() {
        return memberOf;
    }

    public void setMemberOf(FriendGroupInfo[] memberOf) {
        this.memberOf = memberOf;
    }

}
