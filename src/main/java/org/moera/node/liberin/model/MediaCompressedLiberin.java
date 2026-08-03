package org.moera.node.liberin.model;

import java.util.Map;
import java.util.UUID;

import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.node.liberin.Liberin;

public class MediaCompressedLiberin extends Liberin {

    private UUID originalMediaId;
    private String originalMediaHash;
    private PrivateMediaFileInfo media;

    public MediaCompressedLiberin(UUID originalMediaId, String originalMediaHash, PrivateMediaFileInfo media) {
        this.originalMediaId = originalMediaId;
        this.originalMediaHash = originalMediaHash;
        this.media = media;
    }

    public UUID getOriginalMediaId() {
        return originalMediaId;
    }

    public void setOriginalMediaId(UUID originalMediaId) {
        this.originalMediaId = originalMediaId;
    }

    public String getOriginalMediaHash() {
        return originalMediaHash;
    }

    public void setOriginalMediaHash(String originalMediaHash) {
        this.originalMediaHash = originalMediaHash;
    }

    public PrivateMediaFileInfo getMedia() {
        return media;
    }

    public void setMedia(PrivateMediaFileInfo media) {
        this.media = media;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        model.put("originalMediaId", originalMediaId);
        model.put("originalMediaHash", originalMediaHash);
        model.put("media", media);
    }

}
