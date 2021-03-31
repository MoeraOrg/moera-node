package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.Posting;
import org.springframework.data.util.Pair;

public class PostingEvent extends Event {

    private String id;

    protected PostingEvent(EventType type) {
        super(type);
    }

    protected PostingEvent(EventType type, Posting posting) {
        super(type);
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
