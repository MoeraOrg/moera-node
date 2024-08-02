package org.moera.node.auth;

import org.moera.node.fingerprint.CarteProperties;

public class CarteAuthInfo {

    private String clientName;
    private long authScope;

    public CarteAuthInfo(CarteProperties properties) {
        clientName = properties.getOwnerName();
        authScope = properties.getAuthScope();
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public long getAuthScope() {
        return authScope;
    }

    public void setAuthScope(long authScope) {
        this.authScope = authScope;
    }

}
