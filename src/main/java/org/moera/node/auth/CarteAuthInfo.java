package org.moera.node.auth;

import org.moera.node.fingerprint.CarteProperties;

public class CarteAuthInfo {

    private String clientName;
    private long authCategory;

    public CarteAuthInfo(CarteProperties properties) {
        clientName = properties.getOwnerName();
        authCategory = properties.getAuthCategory();
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public long getAuthCategory() {
        return authCategory;
    }

    public void setAuthCategory(long authCategory) {
        this.authCategory = authCategory;
    }

}
