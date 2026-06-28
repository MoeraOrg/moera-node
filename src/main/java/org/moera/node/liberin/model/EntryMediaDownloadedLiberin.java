package org.moera.node.liberin.model;

import java.util.Map;
import java.util.UUID;

import org.moera.node.liberin.Liberin;

public class EntryMediaDownloadedLiberin extends Liberin {

    private UUID postingId;
    private UUID commentId;
    private UUID mediaId;
    private String remoteMediaNodeName;
    private String remoteMediaId;
    private String title;
    private String textContent;

    public EntryMediaDownloadedLiberin(
        UUID postingId,
        UUID commentId,
        UUID mediaId,
        String remoteMediaNodeName,
        String remoteMediaId,
        String title,
        String textContent
    ) {
        this.postingId = postingId;
        this.commentId = commentId;
        this.mediaId = mediaId;
        this.remoteMediaNodeName = remoteMediaNodeName;
        this.remoteMediaId = remoteMediaId;
        this.title = title;
        this.textContent = textContent;
    }

    public UUID getPostingId() {
        return postingId;
    }

    public void setPostingId(UUID postingId) {
        this.postingId = postingId;
    }

    public UUID getCommentId() {
        return commentId;
    }

    public void setCommentId(UUID commentId) {
        this.commentId = commentId;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public void setMediaId(UUID mediaId) {
        this.mediaId = mediaId;
    }

    public String getRemoteMediaNodeName() {
        return remoteMediaNodeName;
    }

    public void setRemoteMediaNodeName(String remoteMediaNodeName) {
        this.remoteMediaNodeName = remoteMediaNodeName;
    }

    public String getRemoteMediaId() {
        return remoteMediaId;
    }

    public void setRemoteMediaId(String remoteMediaId) {
        this.remoteMediaId = remoteMediaId;
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
        model.put("commentId", commentId);
        model.put("mediaId", mediaId);
        model.put("remoteMediaNodeName", remoteMediaNodeName);
        model.put("remoteMediaId", remoteMediaId);
        model.put("title", title);
        model.put("textContent", textContent);
    }

}
