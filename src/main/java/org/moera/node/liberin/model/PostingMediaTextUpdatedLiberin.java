package org.moera.node.liberin.model;

import java.util.Map;
import java.util.UUID;

import org.moera.node.liberin.Liberin;

public class PostingMediaTextUpdatedLiberin extends Liberin {

    private UUID postingId;
    private UUID mediaId;
    private String title;
    private String textContent;

    public PostingMediaTextUpdatedLiberin(UUID postingId, UUID mediaId, String title, String textContent) {
        this.postingId = postingId;
        this.mediaId = mediaId;
        this.title = title;
        this.textContent = textContent;
    }

    public UUID getPostingId() {
        return postingId;
    }

    public void setPostingId(UUID postingId) {
        this.postingId = postingId;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public void setMediaId(UUID mediaId) {
        this.mediaId = mediaId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("postingId", postingId);
        model.put("mediaId", mediaId);
        model.put("title", title);
        model.put("textContent", textContent);
    }

}
