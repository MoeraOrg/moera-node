package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.GrantInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;

public class GrantUpdatedEvent extends Event {

    private GrantInfo grant;

    public GrantUpdatedEvent(GrantInfo grant) {
        super(EventType.GRANT_UPDATED, Scope.OTHER, Principal.ADMIN);
        this.grant = grant;
    }

    public GrantInfo getGrant() {
        return grant;
    }

    public void setGrant(GrantInfo grant) {
        this.grant = grant;
    }

    @Override
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("nodeName", LogUtil.format(grant.getNodeName())));
        parameters.add(new LogParameter("scope", LogUtil.format(String.join(",", grant.getScope()))));
    }

}
