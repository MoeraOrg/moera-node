package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.springframework.data.util.Pair;

public class RemoteNodeFullNameChangedEvent extends Event {

    private String name;
    private String fullName;
    private String title;

    public RemoteNodeFullNameChangedEvent() {
        super(EventType.REMOTE_NODE_FULL_NAME_CHANGED, Scope.VIEW_PEOPLE);
    }

    public RemoteNodeFullNameChangedEvent(String name, String fullName, String title) {
        this();
        this.name = name;
        this.fullName = fullName;
        this.title = title;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("name", LogUtil.format(name)));
        parameters.add(Pair.of("fullName", LogUtil.format(fullName)));
        parameters.add(Pair.of("title", LogUtil.format(title)));
    }

}
