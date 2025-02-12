package org.moera.node.liberin.model;

import java.util.Map;
import java.util.UUID;

import org.moera.lib.node.types.principal.Principal;
import org.moera.node.liberin.Liberin;

public class FriendGroupDeletedLiberin extends Liberin {

    private UUID friendGroupId;
    private Principal latestViewPrincipal;
    private String friendName; // null for admin

    public FriendGroupDeletedLiberin(UUID friendGroupId, Principal latestViewPrincipal, String friendName) {
        this.friendGroupId = friendGroupId;
        this.latestViewPrincipal = latestViewPrincipal;
        this.friendName = friendName;
    }

    public UUID getFriendGroupId() {
        return friendGroupId;
    }

    public void setFriendGroupId(UUID friendGroupId) {
        this.friendGroupId = friendGroupId;
    }

    public Principal getLatestViewPrincipal() {
        return latestViewPrincipal;
    }

    public void setLatestViewPrincipal(Principal latestViewPrincipal) {
        this.latestViewPrincipal = latestViewPrincipal;
    }

    public String getFriendName() {
        return friendName;
    }

    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("friendGroupId", friendGroupId);
        model.put("latestViewPrincipal", latestViewPrincipal);
        model.put("friendName", friendName);
    }

}
