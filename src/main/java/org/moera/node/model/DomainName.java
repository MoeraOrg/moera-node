package org.moera.node.model;

import javax.validation.constraints.NotBlank;

public class DomainName {

    @NotBlank
    @Hostname
    private String name;

    public DomainName() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
