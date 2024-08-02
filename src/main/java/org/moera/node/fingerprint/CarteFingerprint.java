package org.moera.node.fingerprint;

import java.net.InetAddress;
import java.security.SecureRandom;
import java.time.Instant;

import org.moera.commons.crypto.Fingerprint;

@FingerprintVersion(objectType = FingerprintObjectType.CARTE, version = 1)
public class CarteFingerprint extends Fingerprint implements CarteProperties {

    public static final short VERSION = 1;

    public String objectType = FingerprintObjectType.CARTE.name();
    public String ownerName;
    public InetAddress address;
    public long beginning;
    public long deadline;
    public String nodeName;
    public long authScope;
    public byte[] salt;

    public CarteFingerprint() {
        super(1);
    }

    public CarteFingerprint(String ownerName, InetAddress address, Instant beginning, Instant deadline,
                            String nodeName, long authScope) {
        super(1);
        this.ownerName = ownerName;
        this.address = address;
        this.beginning = beginning.getEpochSecond();
        this.deadline = deadline.getEpochSecond();
        this.nodeName = nodeName;
        this.authScope = authScope;
        salt = new byte[8];
        new SecureRandom().nextBytes(salt);
    }

    @Override
    public String getObjectType() {
        return objectType;
    }

    @Override
    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public long getBeginning() {
        return beginning;
    }

    @Override
    public long getDeadline() {
        return deadline;
    }

    public String getNodeName() {
        return nodeName;
    }

    public long getAuthScope() {
        return authScope;
    }

    public byte[] getSalt() {
        return salt;
    }

}
