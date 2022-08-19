package org.moera.node.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.option.OptionsMetadata;
import org.moera.node.plugin.PluginDescriptor;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PluginInfo {

    private String nodeId;
    private Boolean local;
    private String name;
    private String title;
    private String description;
    private String location;
    private Set<String> acceptedEvents;
    private List<SettingMetaInfo> settings;
    private String tokenId;

    public PluginInfo() {
    }

    public PluginInfo(PluginDescriptor descriptor) {
        this(descriptor, null, false);
    }

    public PluginInfo(PluginDescriptor descriptor, OptionsMetadata optionsMetadata, boolean isRootAdmin) {
        if (isRootAdmin) {
            nodeId = Objects.toString(descriptor.getNodeId(), null);
        } else {
            local = descriptor.getNodeId() != null;
        }
        name = descriptor.getName();
        title = descriptor.getTitle();
        description = descriptor.getDescription();
        location = descriptor.getLocation();
        acceptedEvents = descriptor.getAcceptedEvents();
        if (optionsMetadata != null) {
            settings = optionsMetadata.getPluginDescriptors(descriptor.getName()).values().stream()
                    .map(SettingMetaInfo::new)
                    .collect(Collectors.toList());
        }
        tokenId = Objects.toString(descriptor.getTokenId(), null);
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Boolean getLocal() {
        return local;
    }

    public void setLocal(Boolean local) {
        this.local = local;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public List<SettingMetaInfo> getSettings() {
        return settings;
    }

    public void setSettings(List<SettingMetaInfo> settings) {
        this.settings = settings;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

}
