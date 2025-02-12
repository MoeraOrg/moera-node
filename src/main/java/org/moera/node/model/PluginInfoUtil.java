package org.moera.node.model;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

import org.moera.lib.node.types.PluginInfo;
import org.moera.node.option.OptionsMetadata;
import org.moera.node.plugin.PluginDescriptor;

public class PluginInfoUtil {

    public static PluginInfo build(PluginDescriptor descriptor) {
        return build(descriptor, null, false);
    }

    public static PluginInfo build(PluginDescriptor descriptor, OptionsMetadata optionsMetadata, boolean isRootAdmin) {
        PluginInfo info = new PluginInfo();
        if (isRootAdmin) {
            info.setNodeId(Objects.toString(descriptor.getNodeId(), null));
        } else {
            info.setLocal(descriptor.getNodeId() != null);
        }
        info.setName(descriptor.getName());
        info.setTitle(descriptor.getTitle());
        info.setDescription(descriptor.getDescription());
        info.setLocation(descriptor.getLocation());
        info.setAcceptedEvents(new ArrayList<>(descriptor.getAcceptedEvents()));
        if (optionsMetadata != null) {
            info.setSettings(
                optionsMetadata.getPluginDescriptors(descriptor.getName()).values().stream()
                .map(SettingMetaInfoUtil::build)
                .collect(Collectors.toList())
            );
        }
        info.setTokenId(Objects.toString(descriptor.getTokenId(), null));
        return info;
    }

}
