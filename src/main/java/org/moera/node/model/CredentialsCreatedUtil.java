package org.moera.node.model;

import org.moera.lib.node.types.CredentialsCreated;

public class CredentialsCreatedUtil {

    public static CredentialsCreated build(boolean created) {
        CredentialsCreated credentialsCreated = new CredentialsCreated();
        credentialsCreated.setCreated(created);
        return credentialsCreated;
    }

}
