package org.moera.node.util;

import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.lib.naming.NodeName;
import org.moera.node.fingerprint.CarteFingerprint;

public class Carte {

    private static final Duration TTL = Duration.of(10, ChronoUnit.MINUTES);

    public static Instant getDeadline(Instant beginning) {
        return beginning.plus(TTL);
    }

    public static String generate(String ownerName, InetAddress address, Instant beginning, PrivateKey signingKey,
                                  String nodeName, long clientScope, long adminScope) {
        CarteFingerprint fingerprint = new CarteFingerprint(
                NodeName.expand(ownerName), address, beginning, getDeadline(beginning), NodeName.expand(nodeName),
                clientScope, adminScope
        );
        byte[] content = CryptoUtil.fingerprint(fingerprint);
        byte[] signature = CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey);
        byte[] carte = new byte[content.length + signature.length];
        System.arraycopy(content, 0, carte, 0, content.length);
        System.arraycopy(signature, 0, carte, content.length, signature.length);
        return Util.base64urlencode(carte);
    }

}
