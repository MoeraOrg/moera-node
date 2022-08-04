package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.Features;
import org.moera.node.option.Options;
import org.moera.node.plugin.Plugins;

public class FeaturesUpdatedLiberin extends Liberin {

    private Features features;

    public FeaturesUpdatedLiberin(Options options, Plugins plugins) {
        this.features = new Features(options, plugins.getNames(options.nodeId()));
    }

    public Features getFeatures() {
        return features;
    }

    public void setFeatures(Features features) {
        this.features = features;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("features", features);
    }

}
