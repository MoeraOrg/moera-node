package org.moera.node.liberin.model;

import java.util.Map;
import java.util.UUID;

import org.moera.node.liberin.Liberin;

public class MediaRecognizedTextUpdatedLiberin extends Liberin {

    private UUID mediaId;
    private String textContent;

    public MediaRecognizedTextUpdatedLiberin(UUID mediaId, String textContent) {
        this.mediaId = mediaId;
        this.textContent = textContent;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public void setMediaId(UUID mediaId) {
        this.mediaId = mediaId;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        model.put("mediaId", mediaId);
        model.put("textContent", textContent);
    }

}
