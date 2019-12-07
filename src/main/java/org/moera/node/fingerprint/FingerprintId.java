package org.moera.node.fingerprint;

import java.util.Objects;

public class FingerprintId {

    private FingerprintObjectType objectType;
    private short version;

    public FingerprintId() {
    }

    public FingerprintId(FingerprintObjectType objectType, short version) {
        this.objectType = objectType;
        this.version = version;
    }

    public FingerprintId(FingerprintVersion fingerprintVersion) {
        this(fingerprintVersion.objectType(), fingerprintVersion.version());
    }

    public FingerprintObjectType getObjectType() {
        return objectType;
    }

    public void setObjectType(FingerprintObjectType objectType) {
        this.objectType = objectType;
    }

    public short getVersion() {
        return version;
    }

    public void setVersion(short version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FingerprintId peer = (FingerprintId) o;
        return objectType == peer.objectType && version == peer.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectType, version);
    }

}
