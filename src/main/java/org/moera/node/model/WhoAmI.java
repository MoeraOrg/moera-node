package org.moera.node.model;

import org.moera.node.option.Options;

public class WhoAmI {

    private String registeredName;
    private Integer registeredNameGeneration;

    public WhoAmI() {
    }

    public WhoAmI(Options options) {
        registeredName = options.getString("profile.registered-name");
        registeredNameGeneration = options.getInt("profile.registered-name.generation");
    }

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

}
