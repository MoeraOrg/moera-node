package org.moera.node.model.event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.event.EventSubscriber;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.ConstructorDetector;
import tools.jackson.databind.json.JsonMapper;

public abstract class Event implements Cloneable {

    // Domain constructors may accept types different from the serialized payload properties.
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .constructorDetector(ConstructorDetector.DEFAULT.withRequireAnnotation(true))
        .build();

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

    public final Event copy() {
        try {
            // Cloning preserves permission state and supports events without a default constructor.
            // Updating from JSON replaces nested payload objects with independent copies.
            return OBJECT_MAPPER.updateValue((Event) super.clone(), this);
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Cannot copy event", e);
        }
    }

    public void protect(EventSubscriber eventSubscriber) {
    }

    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isPrincipal(filter, scope);
    }

    public final String toLogMessage() {
        List<LogParameter> parameters = new ArrayList<>();
        logParameters(parameters);
        if (parameters.isEmpty()) {
            return getType().toString();
        }
        String params = parameters.stream()
            .map(p -> p.name() + " = " + p.value())
            .collect(Collectors.joining(", "));
        return "%s (%s)".formatted(getType(), params);
    }

    public void logParameters(List<LogParameter> parameters) {
    }

}
