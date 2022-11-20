package org.moera.node.liberin.model;

import java.util.Map;
import java.util.UUID;

import org.moera.node.auth.principal.Principal;
import org.moera.node.liberin.Liberin;

public class FriendGroupDeletedLiberin extends Liberin {

    private UUID friendGroupId;
    private Principal latestViewPrincipal;

    public FriendGroupDeletedLiberin(UUID friendGroupId, Principal latestViewPrincipal) {
        this.friendGroupId = friendGroupId;
        this.latestViewPrincipal = latestViewPrincipal;
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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("friendGroupId", friendGroupId);
        model.put("latestViewPrincipal", latestViewPrincipal);
    }

}
