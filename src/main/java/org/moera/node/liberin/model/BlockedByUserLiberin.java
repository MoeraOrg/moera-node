package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.BlockedByUser;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.BlockedByUserInfo;

public class BlockedByUserLiberin extends Liberin {

    private BlockedByUser blockedByUser;
    private String entryHeading;

    public BlockedByUserLiberin(BlockedByUser blockedByUser, String entryHeading) {
        this.blockedByUser = blockedByUser;
        this.entryHeading = entryHeading;
    }

    public BlockedByUser getBlockedByUser() {
        return blockedByUser;
    }

    public void setBlockedByUser(BlockedByUser blockedByUser) {
        this.blockedByUser = blockedByUser;
    }

    public String getEntryHeading() {
        return entryHeading;
    }

    public void setEntryHeading(String entryHeading) {
        this.entryHeading = entryHeading;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("blockedByUser", new BlockedByUserInfo(blockedByUser, getPluginContext().getOptions()));
        model.put("entryHeading", entryHeading);
    }
}
