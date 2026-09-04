package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;

public class DeleteNodeStatusUpdatedEvent extends Event {

    private boolean requested;

    public DeleteNodeStatusUpdatedEvent() {
        super(EventType.DELETE_NODE_STATUS_UPDATED, Scope.OTHER, Principal.ADMIN);
    }

    public DeleteNodeStatusUpdatedEvent(boolean requested) {
        this();

        this.requested = requested;
    }

    public boolean isRequested() {
        return requested;
    }

    public void setRequested(boolean requested) {
        this.requested = requested;
    }

    @Override
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("requested", LogUtil.format(requested)));
    }

}
