package org.moera.node.fingerprint;

import java.net.InetAddress;
import java.security.SecureRandom;
import java.time.Instant;

import org.moera.commons.crypto.Fingerprint;

@FingerprintVersion(objectType = FingerprintObjectType.CARTE, version = 0)
public class CarteFingerprint0 extends Fingerprint {

    public String objectType = FingerprintObjectType.CARTE.name();
    public String ownerName;
    public InetAddress address;
    public long beginning;
    public long deadline;
    public byte permissions; // TODO for future use
    public byte[] salt;

    public CarteFingerprint0() {
        super(0);
    }

    public CarteFingerprint0(String ownerName, InetAddress address, Instant beginning, Instant deadline,
                             String nodeName) {
        this(ownerName, address, beginning, deadline);
    }

    public CarteFingerprint0(String ownerName, InetAddress address, Instant beginning, Instant deadline) {
        super(0);
        this.ownerName = ownerName;
        this.address = address;
        this.beginning = beginning.getEpochSecond();
        this.deadline = deadline.getEpochSecond();
        salt = new byte[8];
        new SecureRandom().nextBytes(salt);
    }

}
