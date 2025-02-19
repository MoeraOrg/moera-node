package org.moera.node.model;

import java.util.HashSet;
import java.util.regex.Pattern;

import org.moera.lib.node.types.PluginDescription;
import org.moera.node.plugin.PluginDescriptor;

public class PluginDescriptionUtil {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9-]+$");

    public static boolean isNameValid(String name) {
        return NAME_PATTERN.matcher(name).matches();
    }

    public static void toDescriptor(PluginDescription description, PluginDescriptor descriptor) {
        descriptor.setName(description.getName());
        descriptor.setTitle(description.getTitle());
        descriptor.setDescription(description.getDescription());
        descriptor.setLocation(description.getLocation());
        if (description.getAcceptedEvents() != null) {
            descriptor.setAcceptedEvents(new HashSet<>(description.getAcceptedEvents()));
        }
    }

}
