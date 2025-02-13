package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.BlockedInstant;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.BlockedInstantInfoUtil;

public class BlockedInstantAddedLiberin extends Liberin {

    private BlockedInstant blockedInstant;

    public BlockedInstantAddedLiberin(BlockedInstant blockedInstant) {
        this.blockedInstant = blockedInstant;
    }

    public BlockedInstant getBlockedInstant() {
        return blockedInstant;
    }

    public void setBlockedInstant(BlockedInstant blockedInstant) {
        this.blockedInstant = blockedInstant;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("blockedInstant", BlockedInstantInfoUtil.build(blockedInstant));
    }

}
