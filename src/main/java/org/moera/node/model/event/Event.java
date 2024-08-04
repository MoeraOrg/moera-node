package org.moera.node.model.event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.Principal;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.event.EventSubscriber;
import org.springframework.data.util.Pair;

public abstract class Event {

    private EventType type;
    @JsonIgnore
    private Scope scope;
    @JsonIgnore
    private PrincipalFilter filter;

    protected Event(EventType type, Scope scope) {
        this(type, scope, Principal.PUBLIC);
    }

    protected Event(EventType type, Scope scope, PrincipalFilter filter) {
        this.type = type;
        this.scope = scope;
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

    public void protect(EventSubscriber eventSubscriber) {
    }

    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isPrincipal(filter, scope);
    }

    public final String toLogMessage() {
        List<Pair<String, String>> parameters = new ArrayList<>();
        logParameters(parameters);
        if (parameters.isEmpty()) {
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
