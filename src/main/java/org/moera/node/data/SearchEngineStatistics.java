package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.node.util.Util;

@Entity
@Table(name = "search_engine_statistics")
public class SearchEngineStatistics {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Enumerated
    private SearchEngine engine;

    @NotNull
    @Size(max = 63)
    private String ownerName;

    private UUID postingId;

    private UUID commentId;

    private UUID mediaId;

    @NotNull
    private Timestamp clickedAt = Util.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public SearchEngine getEngine() {
        return engine;
    }

    public void setEngine(SearchEngine engine) {
        this.engine = engine;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
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

    public Timestamp getClickedAt() {
        return clickedAt;
    }

    public void setClickedAt(Timestamp clickedAt) {
        this.clickedAt = clickedAt;
    }

}
