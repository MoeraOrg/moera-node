package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.Principal;
import org.moera.node.model.BlockedInstantInfo;
import org.springframework.data.util.Pair;

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
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("id", LogUtil.format(blockedInstant.getId())));
        parameters.add(Pair.of("storyType", LogUtil.format(blockedInstant.getStoryType().toString())));
        parameters.add(Pair.of("entryId", LogUtil.format(blockedInstant.getEntryId())));
    }

}
