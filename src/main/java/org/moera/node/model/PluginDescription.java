package org.moera.node.model;

import java.util.Arrays;
import java.util.HashSet;
import javax.validation.constraints.NotBlank;

import org.moera.node.plugin.PluginDescriptor;

public class PluginDescription {

    @NotBlank
    private String name;

    private String location;

    private String[] acceptedEvents;

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

    public String[] getAcceptedEvents() {
        return acceptedEvents;
    }

    public void setAcceptedEvents(String[] acceptedEvents) {
        this.acceptedEvents = acceptedEvents;
    }

    public void toDescriptor(PluginDescriptor descriptor) {
        descriptor.setName(name);
        descriptor.setLocation(location);
        if (acceptedEvents != null) {
            descriptor.setAcceptedEvents(new HashSet<>(Arrays.asList(acceptedEvents)));
        }
    }

}
