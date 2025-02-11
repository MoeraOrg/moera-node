package org.moera.node.model.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.principal.Principal;
import org.moera.node.auth.principal.PrincipalFilter;
import org.springframework.data.util.Pair;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoryDeletedEvent extends Event {

    private String id;
    private StoryType storyType;
    private String feedName;
    private long moment;
    private String postingId;

    public StoryDeletedEvent() {
        super(EventType.STORY_DELETED, Scope.VIEW_FEEDS);
    }

    public StoryDeletedEvent(String id, StoryType storyType, String feedName, long moment, String postingId,
                             boolean isAdmin, PrincipalFilter viewFilter) {
        super(EventType.STORY_DELETED, Scope.VIEW_FEEDS,
                isAdmin ? Principal.ADMIN : viewFilter.a().andNot(Principal.ADMIN));
        this.id = id;
        this.storyType = storyType;
        this.feedName = feedName;
        this.moment = moment;
        this.postingId = postingId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public StoryType getStoryType() {
        return storyType;
    }

    public void setStoryType(StoryType storyType) {
        this.storyType = storyType;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public long getMoment() {
        return moment;
    }

    public void setMoment(long moment) {
        this.moment = moment;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("storyType", LogUtil.format(storyType.toString())));
        parameters.add(Pair.of("feedName", LogUtil.format(feedName)));
        parameters.add(Pair.of("moment", LogUtil.format(moment)));
        parameters.add(Pair.of("postingId", LogUtil.format(postingId)));
    }

}
