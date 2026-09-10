package org.moera.node.liberin.model;

import java.util.ArrayList;
import java.util.List;
import org.moera.node.data.Contact;
import org.moera.node.data.FriendOf;
import org.moera.node.liberin.Liberin;

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

}
