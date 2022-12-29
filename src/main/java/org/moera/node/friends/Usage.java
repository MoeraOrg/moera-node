package org.moera.node.friends;

import java.time.Instant;
import java.util.Objects;

public class Usage<T> {

    public T value;
    public Instant usedAt = Instant.now();

    Usage(T value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object peer) {
        if (this == peer) {
            return true;
        }
        if (peer == null || getClass() != peer.getClass()) {
            return false;
        }
        Usage<?> usage = (Usage<?>) peer;
        return value.equals(usage.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

}
