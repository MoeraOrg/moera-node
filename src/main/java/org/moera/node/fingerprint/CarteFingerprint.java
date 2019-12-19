package org.moera.node.fingerprint;

import java.net.InetAddress;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

import org.moera.commons.crypto.Fingerprint;

public class CarteFingerprint extends Fingerprint {

    public static final short VERSION = 0;

    public String objectType = FingerprintObjectType.CARTE.name();
    public String ownerName;
    public InetAddress address;
    public long deadline;
    public byte permissions; // TODO for future use
    public byte[] salt;

    public CarteFingerprint(String ownerName, InetAddress address, Duration ttl) {
        super(0);
        this.ownerName = ownerName;
        this.address = address;
        deadline = Instant.now().plus(ttl).getEpochSecond();
        salt = new byte[8];
        new SecureRandom().nextBytes(salt);
    }

}
