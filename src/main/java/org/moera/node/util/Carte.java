package org.moera.node.util;

import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.naming.NodeName;
import org.moera.lib.node.Fingerprints;

public class Carte {

    private static final Duration TTL = Duration.of(10, ChronoUnit.MINUTES);

    public static Instant getDeadline(Instant beginning) {
        return beginning.plus(TTL);
    }

    public static String generate(String ownerName, InetAddress address, Instant beginning, PrivateKey signingKey,
                                  String nodeName, long clientScope, long adminScope) {
        var salt = new byte[8];
        new SecureRandom().nextBytes(salt);
        byte[] fingerprint = Fingerprints.carte(
            NodeName.expand(ownerName),
            address,
            Timestamp.from(beginning),
            Timestamp.from(getDeadline(beginning)),
            NodeName.expand(nodeName),
            clientScope,
            adminScope,
            salt
        );
        byte[] signature = CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey);
        byte[] carte = new byte[fingerprint.length + signature.length];
        System.arraycopy(fingerprint, 0, carte, 0, fingerprint.length);
        System.arraycopy(signature, 0, carte, fingerprint.length, signature.length);
        return Util.base64urlencode(carte);
    }

}
