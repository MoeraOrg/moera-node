package org.moera.node.fingerprint;

import java.net.InetAddress;
import java.security.SecureRandom;
import java.time.Instant;

import org.moera.commons.crypto.Fingerprint;

@FingerprintVersion(objectType = FingerprintObjectType.CARTE, version = 2)
public class CarteFingerprint extends Fingerprint {

    public static final short VERSION = 2;

    public String objectType = FingerprintObjectType.CARTE.name();
    public String ownerName;
    public InetAddress address;
    public long beginning;
    public long deadline;
    public String nodeName;
    public long clientScope;
    public long adminScope;
    public byte[] salt;

    public CarteFingerprint() {
        super(2);
    }

    public CarteFingerprint(String ownerName, InetAddress address, Instant beginning, Instant deadline,
                            String nodeName, long clientScope, long adminScope) {
        super(2);
        this.ownerName = ownerName;
        this.address = address;
        this.beginning = beginning.getEpochSecond();
        this.deadline = deadline.getEpochSecond();
        this.nodeName = nodeName;
        this.clientScope = clientScope;
        this.adminScope = adminScope;
        salt = new byte[8];
        new SecureRandom().nextBytes(salt);
    }

}
