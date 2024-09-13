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
    @Size(max = 63)
    private String nodeName;

    @NotNull
    @Enumerated
    private SearchEngine engine;

    @NotNull
    @Size(max = 63)
    private String ownerName;

    @Size(max = 40)
    private String postingId;

    @Size(max = 40)
    private String commentId;

    @Size(max = 40)
    private String mediaId;

    @Size(max = 255)
    private String heading;

    @NotNull
    private Timestamp clickedAt = Util.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
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

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public Timestamp getClickedAt() {
        return clickedAt;
    }

    public void setClickedAt(Timestamp clickedAt) {
        this.clickedAt = clickedAt;
    }

}
