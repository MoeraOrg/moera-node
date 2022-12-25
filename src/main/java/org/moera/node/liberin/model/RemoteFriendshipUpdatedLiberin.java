package org.moera.node.liberin.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.moera.node.data.Contact;
import org.moera.node.data.FriendOf;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.FriendOfInfo;

public class RemoteFriendshipUpdatedLiberin extends Liberin {

    private final List<FriendOf> added = new ArrayList<>();
    private final List<FriendOf> deleted = new ArrayList<>();
    private final List<FriendOf> current = new ArrayList<>();

    public List<FriendOf> getAdded() {
        return added;
    }

    public List<FriendOf> getDeleted() {
        return deleted;
    }

    public List<FriendOf> getCurrent() {
        return current;
    }

    public Contact getContact() {
        if (!added.isEmpty()) {
            return added.get(0).getContact();
        }
        if (!deleted.isEmpty()) {
            return deleted.get(0).getContact();
        }
        if (!current.isEmpty()) {
            return current.get(0).getContact();
        }
        return null;
    }

    public void setContact(Contact contact) {
        added.forEach(fo -> fo.setContact(contact));
        deleted.forEach(fo -> fo.setContact(contact));
        current.forEach(fo -> fo.setContact(contact));
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("added", toFriendOfInfos(added));
        model.put("deleted", toFriendOfInfos(deleted));
        model.put("current", toFriendOfInfos(current));
    }

    private List<FriendOfInfo> toFriendOfInfos(List<FriendOf> friendOfs) {
        return friendOfs.stream()
                .map(fo -> new FriendOfInfo(fo, getPluginContext().getOptions()))
                .collect(Collectors.toList());
    }

}
