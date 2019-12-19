package org.moera.node.util;

import java.net.InetAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.fingerprint.CarteFingerprint;

public class Carte {

    private static final Duration TTL = Duration.of(15, ChronoUnit.MINUTES);

    public String generate(String ownerName, InetAddress address, byte[] signingKey) {
        CarteFingerprint fingerprint = new CarteFingerprint(ownerName, address, TTL);
        byte[] content = CryptoUtil.fingerprint(fingerprint);
        byte[] signature = CryptoUtil.sign(fingerprint, signingKey);
        byte[] carte = new byte[content.length + signature.length];
        System.arraycopy(content, 0, carte, 0, content.length);
        System.arraycopy(signature, 0, carte, content.length, signature.length);
        return Util.base64encode(carte);
    }

}
