package org.moera.node.model;

import jakarta.validation.constraints.NotBlank;

public class ClientCarte {

    private String clientName;

    @NotBlank
    private String carte;

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getCarte() {
        return carte;
    }

    public void setCarte(String carte) {
        this.carte = carte;
    }

}
