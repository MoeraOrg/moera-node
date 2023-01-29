package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.BlockedUser;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.BlockedUserInfo;

public class BlockedUserDeletedLiberin extends Liberin {

    private BlockedUser blockedUser;

    public BlockedUserDeletedLiberin(BlockedUser blockedUser) {
        this.blockedUser = blockedUser;
    }

    public BlockedUser getBlockedUser() {
        return blockedUser;
    }

    public void setBlockedUser(BlockedUser blockedUser) {
        this.blockedUser = blockedUser;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("blockedUser", new BlockedUserInfo(blockedUser, getPluginContext().getOptions()));
    }

}
