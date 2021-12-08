package org.moera.node.model;

import java.util.UUID;

public class MediaWithDigest {

    private UUID id;
    private String digest;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

}
