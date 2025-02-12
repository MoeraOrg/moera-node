package org.moera.node.liberin.model;

import java.util.List;
import java.util.Map;

import org.moera.lib.node.types.FriendGroupDetails;
import org.moera.node.data.Contact;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.ContactInfoUtil;

public class FriendshipUpdatedLiberin extends Liberin {

    private String friendNodeName;
    private List<FriendGroupDetails> friendGroups;
    private Contact contact;

    public FriendshipUpdatedLiberin(String friendNodeName, List<FriendGroupDetails> friendGroups, Contact contact) {
        this.friendNodeName = friendNodeName;
        this.friendGroups = friendGroups;
        this.contact = contact;
    }

    public String getFriendNodeName() {
        return friendNodeName;
    }

    public void setFriendNodeName(String friendNodeName) {
        this.friendNodeName = friendNodeName;
    }

    public List<FriendGroupDetails> getFriendGroups() {
        return friendGroups;
    }

    public void setFriendGroups(List<FriendGroupDetails> friendGroups) {
        this.friendGroups = friendGroups;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("friendNodeName", friendNodeName);
        model.put("friendGroups", friendGroups);
        model.put("contact", ContactInfoUtil.build(contact, getPluginContext().getOptions()));
    }

}
