package org.moera.node.model;

import javax.validation.constraints.NotBlank;

import org.moera.node.plugin.PluginDescriptor;

public class PluginDescription {

    @NotBlank
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void toDescriptor(PluginDescriptor descriptor) {
        descriptor.setName(name);
    }

}
