package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Comment;
import org.springframework.data.util.Pair;

public class CommentEvent extends Event {

    private String id;
    private String postingId;
    private Long moment;

    protected CommentEvent(EventType type) {
        super(type, Scope.VIEW_CONTENT);
    }

    protected CommentEvent(EventType type, PrincipalFilter filter) {
        super(type, Scope.VIEW_CONTENT, filter);
    }

    protected CommentEvent(EventType type, Comment comment, PrincipalFilter filter) {
        super(type, Scope.VIEW_CONTENT, filter);
        this.id = comment.getId().toString();
        this.postingId = comment.getPosting().getId().toString();
        this.moment = comment.getMoment();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public Long getMoment() {
        return moment;
    }

    public void setMoment(Long moment) {
        this.moment = moment;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("id", LogUtil.format(id)));
        parameters.add(Pair.of("postingId", LogUtil.format(postingId)));
        parameters.add(Pair.of("moment", LogUtil.format(moment)));
    }

}
