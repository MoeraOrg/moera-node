package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.FriendGroup;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.FriendGroupInfoUtil;

public class FriendGroupUpdatedLiberin extends Liberin {

    private FriendGroup friendGroup;
    private Principal latestViewPrincipal;

    public FriendGroupUpdatedLiberin(FriendGroup friendGroup, Principal latestViewPrincipal) {
        this.friendGroup = friendGroup;
        this.latestViewPrincipal = latestViewPrincipal;
    }

    public FriendGroup getFriendGroup() {
        return friendGroup;
    }

    public void setFriendGroup(FriendGroup friendGroup) {
        this.friendGroup = friendGroup;
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
        model.put("friendGroup", FriendGroupInfoUtil.build(friendGroup, true));
        model.put("latestViewPrincipal", latestViewPrincipal);
    }

}
