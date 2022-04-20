package org.moera.node.model.event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.moera.node.auth.principal.Principal;
import org.moera.node.event.EventSubscriber;
import org.springframework.data.util.Pair;

public abstract class Event {

    private EventType type;
    @JsonIgnore
    private Principal principal = Principal.PUBLIC;

    protected Event(EventType type) {
        this.type = type;
    }

    public Event(EventType type, Principal principal) {
        this.type = type;
        this.principal = principal;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    public boolean isPermitted(EventSubscriber subscriber) {
        return principal.includes(subscriber.isAdmin(), subscriber.getClientName());
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
