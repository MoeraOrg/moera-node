package org.moera.node.model;

import javax.validation.constraints.NotBlank;

import org.moera.node.plugin.PluginDescriptor;

public class PluginDescription {

    @NotBlank
    private String name;

    private String location;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void toDescriptor(PluginDescriptor descriptor) {
        descriptor.setName(name);
        descriptor.setLocation(location);
    }

}
