package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.PluginInfo;
import org.moera.node.plugin.PluginDescriptor;

public class PluginAddedLiberin extends Liberin {

    private PluginDescriptor descriptor;

    public PluginAddedLiberin(PluginDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public PluginDescriptor getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(PluginDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("plugin", new PluginInfo(descriptor));
    }

}
