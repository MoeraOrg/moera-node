package org.moera.node.model;

import org.moera.lib.node.types.CredentialsCreated;

public class CredentialsCreatedUtil {

    public static CredentialsCreated build(boolean created, boolean loginDisabled) {
        CredentialsCreated credentialsCreated = new CredentialsCreated();
        credentialsCreated.setCreated(created);
        if (loginDisabled) {
            credentialsCreated.setLoginDisabled(true);
        }
        return credentialsCreated;
    }

}
