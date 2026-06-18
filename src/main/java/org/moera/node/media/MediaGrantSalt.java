package org.moera.node.media;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

public final class MediaGrantSalt {

    private static final int SALT_SIZE = 8;
    private static final Duration SALT_TTL = Duration.ofDays(1);
    private static final SecureRandom RANDOM = new SecureRandom();

    private byte[] value;
    private Instant deadline;

    public synchronized byte[] get() {
        if (value == null || Instant.now().isAfter(deadline)) {
            value = generate();
            deadline = Instant.now().plus(SALT_TTL);
        }
        return value.clone();
    }

    private byte[] generate() {
        byte[] salt = new byte[SALT_SIZE];
        RANDOM.nextBytes(salt);
        return salt;
    }

}
