package org.moera.node.model.notification;

import java.util.List;
import javax.validation.constraints.Size;

import org.moera.lib.util.LogUtil;
import org.springframework.data.util.Pair;

public class UserListItemNotification extends SubscriberNotification {

    @Size(max = 63)
    private String listName;

    @Size(max = 63)
    private String nodeName;

    public UserListItemNotification(NotificationType type) {
        super(type);
    }

    public UserListItemNotification(NotificationType type, String listName, String nodeName) {
        super(type);
        this.listName = listName;
        this.nodeName = nodeName;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("listName", LogUtil.format(listName)));
        parameters.add(Pair.of("nodeName", LogUtil.format(nodeName)));
    }

}
