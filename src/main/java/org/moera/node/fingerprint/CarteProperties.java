package org.moera.node.fingerprint;

import java.net.InetAddress;

public interface CarteProperties {

    String getObjectType();

    String getOwnerName();

    InetAddress getAddress();

    long getBeginning();

    long getDeadline();

    default String getNodeName() {
        return null;
    }

    default long getClientScope() {
        return 0;
    }

    default long getAdminScope() {
        return 0;
    }

}
