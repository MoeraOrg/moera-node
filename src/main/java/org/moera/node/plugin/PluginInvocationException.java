package org.moera.node.plugin;

public class PluginInvocationException extends Exception {

    public PluginInvocationException(Throwable cause) {
        super("Plugin invocation failed: " + cause.getMessage(), cause);
    }

}
