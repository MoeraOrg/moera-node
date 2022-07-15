package org.moera.node.plugin;

public class DuplicatePluginException extends RuntimeException {

    private final String pluginName;

    public DuplicatePluginException(String pluginName) {
        super("Plugin is already registered: " + pluginName);
        this.pluginName = pluginName;
    }

    public String getPluginName() {
        return pluginName;
    }

}
