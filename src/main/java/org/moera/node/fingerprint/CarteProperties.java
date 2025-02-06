package org.moera.node.fingerprint;

import java.net.InetAddress;
import java.sql.Timestamp;

import org.moera.lib.crypto.Fingerprint;

public class CarteProperties {

    private final Fingerprint fingerprint;

    public CarteProperties(Fingerprint fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getObjectType() {
        return (String) fingerprint.get("object_type");
    }

    public String getOwnerName() {
        return (String) fingerprint.get("owner_name");
    }

    public InetAddress getAddress() {
        return (InetAddress) fingerprint.get("address");
    }

    public Timestamp getBeginning() {
        return (Timestamp) fingerprint.get("beginning");
    }

    public Timestamp getDeadline() {
        return (Timestamp) fingerprint.get("deadline");
    }

    public String getNodeName() {
        return (String) fingerprint.get("node_name");
    }

    public long getClientScope() {
        return (Long) fingerprint.getOrDefault("client_scope", 0L);
    }

    public long getAdminScope() {
        return (Long) fingerprint.getOrDefault("admin_scope", 0L);
    }

}
