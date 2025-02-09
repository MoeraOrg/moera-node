package org.moera.node.model;

import jakarta.validation.constraints.NotBlank;

public class NameToRegister {

    @NotBlank
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
