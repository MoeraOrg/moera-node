package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.springframework.data.util.Pair;

public class RemoteNodeFullNameChangedEvent extends Event {

    private String name;
    private String fullName;

    public RemoteNodeFullNameChangedEvent() {
        super(EventType.REMOTE_NODE_FULL_NAME_CHANGED, Scope.VIEW_PEOPLE);
    }

    public RemoteNodeFullNameChangedEvent(String name, String fullName) {
        this();
        this.name = name;
        this.fullName = fullName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("name", LogUtil.format(name)));
        parameters.add(Pair.of("fullName", LogUtil.format(fullName)));
    }

}
