package org.moera.node.fingerprint;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.moera.commons.crypto.Fingerprint;
import org.reflections.Reflections;

public class Fingerprints {

    private static final Map<FingerprintId, Class<? extends Fingerprint>> FINGERPRINTS;

    static {
        Reflections reflections = new Reflections("org.moera.node.fingerprint");
        FINGERPRINTS = reflections.getTypesAnnotatedWith(FingerprintVersion.class).stream()
                .filter(Fingerprint.class::isAssignableFrom)
                .map(klass -> (Class<? extends Fingerprint>) klass)
                .collect(Collectors.toMap(
                        klass -> new FingerprintId(klass.getAnnotation(FingerprintVersion.class)),
                        Function.identity()
                ));
    }

    public static PostingFingerprintFactory posting(short version) {
        return new PostingFingerprintFactory(get(FingerprintObjectType.POSTING, version));
    }

    public static ReactionFingerprintFactory reaction(short version) {
        return new ReactionFingerprintFactory(get(FingerprintObjectType.REACTION, version));
    }

    public static CommentFingerprintFactory comment(short version) {
        return new CommentFingerprintFactory(get(FingerprintObjectType.COMMENT, version));
    }

    public static Class<? extends Fingerprint> get(FingerprintObjectType objectType, short version) {
        return FINGERPRINTS.get(new FingerprintId(objectType, version));
    }

}
