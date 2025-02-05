package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Entry;
import org.moera.node.data.Posting;
import org.springframework.data.util.Pair;

public class PostingEvent extends Event {

    private String id;

    protected PostingEvent(EventType type) {
        super(type, Scope.VIEW_CONTENT);
    }

    protected PostingEvent(EventType type, PrincipalFilter filter) {
        super(type, Scope.VIEW_CONTENT, filter);
    }

    protected PostingEvent(EventType type, Posting posting) {
        super(type, Scope.VIEW_CONTENT);
        this.id = posting.getId().toString();
    }

    protected PostingEvent(EventType type, Entry posting, PrincipalFilter filter) {
        super(type, Scope.VIEW_CONTENT, filter);
        this.id = posting.getId().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("id", LogUtil.format(id)));
    }

}
