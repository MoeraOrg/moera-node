package org.moera.node.model;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.option.OptionDescriptor;
import org.moera.node.option.OptionsMetadata;
import org.moera.node.plugin.PluginDescriptor;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PluginInfo {

    private String nodeId;
    private String name;
    private String location;
    private Set<String> acceptedEvents;
    private Collection<OptionDescriptor> options;

    public PluginInfo() {
    }

    public PluginInfo(PluginDescriptor descriptor, OptionsMetadata optionsMetadata) {
        nodeId = Objects.toString(descriptor.getNodeId(), null);
        name = descriptor.getName();
        location = descriptor.getLocation();
        acceptedEvents = descriptor.getAcceptedEvents();
        options = optionsMetadata.getPluginDescriptors(descriptor.getName()).values();
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

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

    public Set<String> getAcceptedEvents() {
        return acceptedEvents;
    }

    public void setAcceptedEvents(Set<String> acceptedEvents) {
        this.acceptedEvents = acceptedEvents;
    }

    public Collection<OptionDescriptor> getOptions() {
        return options;
    }

    public void setOptions(Collection<OptionDescriptor> options) {
        this.options = options;
    }

}
