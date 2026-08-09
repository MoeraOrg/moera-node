package org.moera.node.auth;

import org.moera.node.fingerprint.CarteProperties;

public record CarteAuthInfo(
    String clientName,
    long clientScope,
    long adminScope
) {

    public CarteAuthInfo(CarteProperties properties) {
        this(properties.getOwnerName(), properties.getClientScope(), properties.getAdminScope());
    }

}
