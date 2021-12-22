package org.moera.node.fingerprint;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.moera.commons.crypto.Fingerprint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FingerprintFactory {

    private static final Logger log = LoggerFactory.getLogger(FingerprintFactory.class);

    protected final Class<? extends Fingerprint> klass;

    protected FingerprintFactory(Class<? extends Fingerprint> klass) {
        this.klass = klass;
    }

    protected Constructor<? extends Fingerprint> getConstructor(Class<?>... parameterTypes) {
        if (klass == null) {
            return null;
        }
        try {
            return klass.getConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            log.error("Cannot find a fingerprint constructor", e);
            return null;
        }
    }

    protected Fingerprint create(Constructor<? extends Fingerprint> constructor, Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.error("Cannot create a fingerprint", e);
            return null;
        }
    }

}
