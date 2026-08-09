package org.moera.node.plugin;

import java.util.UUID;

public record PluginKey(
    UUID nodeId,
    String name
) {
}
