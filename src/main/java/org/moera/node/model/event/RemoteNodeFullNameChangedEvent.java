package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;

public class RemoteNodeFullNameChangedEvent extends Event {

    private String name;
    private String fullName;
    private String nodeSourceUri;
    private String title;

    public RemoteNodeFullNameChangedEvent() {
        super(EventType.REMOTE_NODE_FULL_NAME_CHANGED, Scope.VIEW_PEOPLE);
    }

    public RemoteNodeFullNameChangedEvent(String name, String fullName, String nodeSourceUri, String title) {
        this();
        this.name = name;
        this.fullName = fullName;
        this.nodeSourceUri = nodeSourceUri;
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

    public String getNodeSourceUri() {
        return nodeSourceUri;
    }

    public void setNodeSourceUri(String nodeSourceUri) {
        this.nodeSourceUri = nodeSourceUri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("name", LogUtil.format(name)));
        parameters.add(new LogParameter("fullName", LogUtil.format(fullName)));
        parameters.add(new LogParameter("nodeSourceUri", LogUtil.format(nodeSourceUri)));
        parameters.add(new LogParameter("title", LogUtil.format(title)));
    }

}
