package org.moera.node.fingerprint;

import java.net.InetAddress;
import java.time.Instant;

import org.moera.commons.crypto.Fingerprint;

public class CarteFingerprintFactory extends FingerprintFactory {

    public CarteFingerprintFactory(Class<? extends Fingerprint> klass) {
        super(klass);
    }

    public Fingerprint create() {
        var constructor = getConstructor();
        return constructor != null ? create(constructor) : null;
    }

    public Fingerprint create(String ownerName, InetAddress address, Instant beginning, Instant deadline,
                              String nodeName) {
        var constructor = getConstructor(String.class, InetAddress.class, Instant.class, Instant.class, String.class);
        return constructor != null ? create(constructor, ownerName, address, beginning, deadline, nodeName) : null;
    }

}
