package org.moera.node.data;

public interface ContactRelated {

    String getRemoteNodeName();

    Contact getContact();

    void setContact(Contact contact);

    void toContactViewPrincipal(Contact contact);

}
