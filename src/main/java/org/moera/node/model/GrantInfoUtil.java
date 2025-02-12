package org.moera.node.model;

import org.moera.lib.node.types.GrantInfo;
import org.moera.lib.node.types.Scope;
import org.moera.node.data.Grant;

public class GrantInfoUtil {

    public static GrantInfo build(String nodeName, long scope) {
        GrantInfo grantInfo = new GrantInfo();
        grantInfo.setNodeName(nodeName);
        grantInfo.setScope(Scope.toValues(scope));
        return grantInfo;
    }

    public static GrantInfo build(Grant grant) {
        GrantInfo grantInfo = new GrantInfo();
        grantInfo.setNodeName(grant.getNodeName());
        grantInfo.setScope(Scope.toValues(grant.getAuthScope()));
        return grantInfo;
    }

}
