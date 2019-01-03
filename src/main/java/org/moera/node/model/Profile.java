package org.moera.node.model;

import javax.validation.constraints.Size;

import org.moera.naming.rpc.Rules;
import org.moera.node.option.Options;

public class Profile {

    @Size(max = Rules.NAME_MAX_LENGTH)
    private String registeredName;

    private Integer registeredNameGeneration;

    @Size(max = 255)
    private String fullName;

    @Size(max = 31)
    private String gender;

    public Profile() {
    }

    public Profile(Options options) {
        registeredName = options.getString("profile.registered-name");
        registeredNameGeneration = options.getInt("profile.registered-name.generation");
        fullName = options.getString("profile.full-name");
        gender = options.getString("profile.gender");
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void toOptions(Options options) {
        if (getRegisteredName() != null) {
            options.set("profile.registered-name", getRegisteredName());
        }
        if (getRegisteredNameGeneration() != null) {
            options.set("profile.registered-name.generation", getRegisteredNameGeneration());
        }
        if (getFullName() != null) {
            options.set("profile.full-name", getFullName());
        }
        if (getGender() != null) {
            options.set("profile.gender", getGender());
        }
    }

}
