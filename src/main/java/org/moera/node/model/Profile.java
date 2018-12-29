package org.moera.node.model;

public class Profile {

    private String registeredName;
    private Integer registeredNameGeneration;
    private boolean signingKeyDefined;

    public String getRegisteredName() {
        return registeredName;
    }

    public void setRegisteredName(String registeredName) {
        this.registeredName = registeredName;
    }

    public Integer getRegisteredNameGeneration() {
        return registeredNameGeneration;
    }

    public void setRegisteredNameGeneration(Integer registeredNameGeneration) {
        this.registeredNameGeneration = registeredNameGeneration;
    }

    public boolean isSigningKeyDefined() {
        return signingKeyDefined;
    }

    public void setSigningKeyDefined(boolean signingKeyDefined) {
        this.signingKeyDefined = signingKeyDefined;
    }

}
