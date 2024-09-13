package org.moera.node.model.notification;

import java.sql.Timestamp;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.node.data.SearchEngine;
import org.moera.node.data.SearchEngineStatistics;
import org.moera.node.util.Util;

public class SearchEngineClickedNotification extends Notification {

    @NotNull
    private String searchEngine;

    @Size(max = 40)
    private String postingId;

    @Size(max = 40)
    private String commentId;

    @Size(max = 40)
    private String mediaId;

    @NotBlank
    @Size(max = 255)
    private String heading;

    @NotNull
    private long clickedAt;

    public SearchEngineClickedNotification() {
        super(NotificationType.SEARCH_ENGINE_CLICKED);
    }

    public SearchEngineClickedNotification(SearchEngine searchEngine, String postingId, String commentId,
                                           String mediaId, String heading, Timestamp clickedAt) {
        super(NotificationType.SEARCH_ENGINE_CLICKED);
        this.searchEngine = searchEngine.getValue();
        this.postingId = postingId;
        this.commentId = commentId;
        this.mediaId = mediaId;
        this.heading = heading;
        this.clickedAt = Util.toEpochSecond(clickedAt);
    }

    public SearchEngineClickedNotification(SearchEngineStatistics statistics) {
        this(statistics.getEngine(), statistics.getPostingId(), statistics.getCommentId(), statistics.getMediaId(),
                statistics.getHeading(), statistics.getClickedAt());
    }

    public String getSearchEngine() {
        return searchEngine;
    }

    public void setSearchEngine(String searchEngine) {
        this.searchEngine = searchEngine;
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

    public long getClickedAt() {
        return clickedAt;
    }

    public void setClickedAt(long clickedAt) {
        this.clickedAt = clickedAt;
    }

}
