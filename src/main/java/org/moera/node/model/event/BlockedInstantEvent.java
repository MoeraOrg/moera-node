package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.BlockedInstantInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;

public class BlockedInstantEvent extends Event {

    private BlockedInstantInfo blockedInstant;

    protected BlockedInstantEvent(EventType type) {
        super(type, Scope.OTHER, Principal.ADMIN);
    }

    protected BlockedInstantEvent(EventType type, BlockedInstantInfo blockedInstant) {
        super(type, Scope.OTHER, Principal.ADMIN);
        this.blockedInstant = blockedInstant;
    }

    public BlockedInstantInfo getBlockedInstant() {
        return blockedInstant;
    }

    public void setBlockedInstant(BlockedInstantInfo blockedInstant) {
        this.blockedInstant = blockedInstant;
    }

    @Override
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("id", LogUtil.format(blockedInstant.getId())));
        parameters.add(new LogParameter("storyType", LogUtil.format(blockedInstant.getStoryType().toString())));
        parameters.add(new LogParameter("entryId", LogUtil.format(blockedInstant.getEntryId())));
    }

}
