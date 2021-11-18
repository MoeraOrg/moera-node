package org.moera.node.fingerprint;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.moera.commons.crypto.Fingerprint;

public class FingerprintFactory {

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
            return null;
        }
    }

    protected Fingerprint create(Constructor<? extends Fingerprint> constructor, Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

}
