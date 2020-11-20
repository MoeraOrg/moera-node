package org.moera.node.fingerprint;

import java.net.InetAddress;

public interface CarteProperties {

    String getObjectType();

    String getOwnerName();

    InetAddress getAddress();

    long getBeginning();

    long getDeadline();

    byte[] getSalt();

}
