package org.moera.node.fingerprint;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.List;

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

    public List<InetAddress> getAddresses() {
        var addresses = (List<InetAddress>) fingerprint.get("address");
        if (addresses != null) {
            return addresses;
        }
        var address = (InetAddress) fingerprint.get("address");
        return address != null ? List.of(address) : null;
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
