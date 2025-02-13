package org.moera.node.model;

import org.moera.lib.node.types.DomainAvailable;

public class DomainAvailableUtil {
    
    public static DomainAvailable build(String name) {
        DomainAvailable domainAvailable = new DomainAvailable();
        domainAvailable.setName(name);
        return domainAvailable;
    }

}
