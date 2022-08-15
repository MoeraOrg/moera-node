package org.moera.node.model;

import java.util.Arrays;
import java.util.HashSet;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.moera.node.option.OptionDescriptor;
import org.moera.node.plugin.PluginDescriptor;

public class PluginDescription {

    private static final String NAME_PATTERN = "^[a-z0-9-]+$";

    @NotBlank
    @Size(max = 48)
    @Pattern(regexp = NAME_PATTERN)
    private String name;

    @Size(max = 80)
    private String title;

    @Size(max = 256)
    private String description;

    private String location;

    private String[] acceptedEvents;

    private OptionDescriptor[] options;

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

    public String[] getAcceptedEvents() {
        return acceptedEvents;
    }

    public void setAcceptedEvents(String[] acceptedEvents) {
        this.acceptedEvents = acceptedEvents;
    }

    public OptionDescriptor[] getOptions() {
        return options;
    }

    public void setOptions(OptionDescriptor[] options) {
        this.options = options;
    }

    public void toDescriptor(PluginDescriptor descriptor) {
        descriptor.setName(name);
        descriptor.setTitle(title);
        descriptor.setDescription(description);
        descriptor.setLocation(location);
        if (acceptedEvents != null) {
            descriptor.setAcceptedEvents(new HashSet<>(Arrays.asList(acceptedEvents)));
        }
    }

}
