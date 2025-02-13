package org.moera.node.model;

import org.moera.lib.node.types.DomainInfo;
import org.moera.node.data.Domain;
import org.moera.node.util.Util;

public class DomainInfoUtil {
    
    public static DomainInfo build(Domain domain) {
        DomainInfo domainInfo = new DomainInfo();
        domainInfo.setName(domain.getName());
        domainInfo.setNodeId(domain.getNodeId());
        domainInfo.setCreatedAt(Util.toEpochSecond(domain.getCreatedAt()));
        return domainInfo;
    }

}
