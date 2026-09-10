package org.moera.node.liberin.model;

import java.util.UUID;

import org.moera.node.liberin.Liberin;

public class CommentMediaTextUpdatedLiberin extends Liberin {

    private UUID commentId;
    private UUID mediaId;
    private String mediaNodeName;
    private String title;
    private String textContent;

    public CommentMediaTextUpdatedLiberin(UUID commentId, UUID mediaId, String title, String textContent) {
        this(commentId, mediaId, null, title, textContent);
    }

    public CommentMediaTextUpdatedLiberin(
        UUID commentId, UUID mediaId, String mediaNodeName, String title, String textContent
    ) {
        this.commentId = commentId;
        this.mediaId = mediaId;
        this.mediaNodeName = mediaNodeName;
        this.title = title;
        this.textContent = textContent;
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

    public String getMediaNodeName() {
        return mediaNodeName;
    }

    public void setMediaNodeName(String mediaNodeName) {
        this.mediaNodeName = mediaNodeName;
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

}
