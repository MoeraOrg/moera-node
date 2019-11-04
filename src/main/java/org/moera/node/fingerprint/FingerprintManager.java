package org.moera.node.fingerprint;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import org.moera.commons.crypto.Fingerprint;
import org.reflections.Reflections;
import org.springframework.stereotype.Component;

@Component
public class FingerprintManager {

    private Map<FingerprintId, Class<? extends Fingerprint>> fingerints;

    @PostConstruct
    public void init() {
        Reflections reflections = new Reflections("org.moera.node.fingerprint");
        fingerints = reflections.getTypesAnnotatedWith(FingerprintVersion.class).stream()
                .filter(Fingerprint.class::isAssignableFrom)
                .map(klass -> (Class<? extends Fingerprint>) klass)
                .collect(Collectors.toMap(
                        klass -> new FingerprintId(klass.getAnnotation(FingerprintVersion.class)),
                        Function.identity()
                ));
    }

    public Class<? extends Fingerprint> get(FingerprintObjectType objectType, byte version) {
        return fingerints.get(new FingerprintId(objectType, version));
    }

}
