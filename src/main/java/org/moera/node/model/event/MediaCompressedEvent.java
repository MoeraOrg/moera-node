package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;
import org.springframework.data.util.Pair;

public class MediaCompressedEvent extends Event {

    private String originalMediaId;
    private String originalMediaHash;
    private PrivateMediaFileInfo media;

    public MediaCompressedEvent() {
        super(EventType.MEDIA_COMPRESSED, Scope.VIEW_CONTENT, Principal.ADMIN);
    }

    public MediaCompressedEvent(String originalMediaId, String originalMediaHash, PrivateMediaFileInfo media) {
        super(EventType.MEDIA_COMPRESSED, Scope.VIEW_CONTENT, Principal.ADMIN);
        this.originalMediaId = originalMediaId;
        this.originalMediaHash = originalMediaHash;
        this.media = media;
    }

    public String getOriginalMediaId() {
        return originalMediaId;
    }

    public void setOriginalMediaId(String originalMediaId) {
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
    public void logParameters(List<Pair<String, String>> parameters) {
        parameters.add(Pair.of("originalMediaId", LogUtil.format(originalMediaId)));
        parameters.add(Pair.of("originalMediaHash", LogUtil.format(originalMediaHash)));
        parameters.add(Pair.of("mediaId", LogUtil.format(media != null ? media.getId() : null)));
    }

}
