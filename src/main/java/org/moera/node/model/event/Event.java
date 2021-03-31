package org.moera.node.model.event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.moera.node.event.EventSubscriber;
import org.springframework.data.util.Pair;

public abstract class Event {

    private EventType type;

    protected Event(EventType type) {
        this.type = type;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public boolean isPermitted(EventSubscriber subscriber) {
        return true;
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
