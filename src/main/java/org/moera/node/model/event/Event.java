package org.moera.node.model.event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.moera.node.auth.principal.Principal;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.event.EventSubscriber;
import org.springframework.data.util.Pair;

public abstract class Event {

    private EventType type;
    @JsonIgnore
    private PrincipalFilter filter;

    protected Event(EventType type) {
        this(type, Principal.PUBLIC);
    }

    protected Event(EventType type, PrincipalFilter filter) {
        this.type = type;
        this.filter = filter;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public PrincipalFilter getFilter() {
        return filter;
    }

    public void setFilter(PrincipalFilter filter) {
        this.filter = filter;
    }

    public boolean isPermitted(EventSubscriber subscriber) {
        return filter.includes(subscriber.isAdmin(), subscriber.getClientName(), subscriber.getFriendsNames());
    }

    public final String toLogMessage() {
        List<Pair<String, String>> parameters = new ArrayList<>();
        logParameters(parameters);
        if (parameters.size() == 0) {
            return getType().toString();
        }
        String params = parameters.stream()
                .map(p -> p.getFirst() + " = " + p.getSecond())
                .collect(Collectors.joining(", "));
        return String.format("%s (%s)", getType(), params);
    }

    public void logParameters(List<Pair<String, String>> parameters) {
    }

}
