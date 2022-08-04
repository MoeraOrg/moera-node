package org.moera.node.model.event;

import org.moera.node.model.Features;

public class FeaturesUpdatedEvent extends Event {

    private Features features;

    public FeaturesUpdatedEvent() {
        super(EventType.FEATURES_UPDATED);
    }

    public FeaturesUpdatedEvent(Features features) {
        super(EventType.FEATURES_UPDATED);
        this.features = features;
    }

    public Features getFeatures() {
        return features;
    }

    public void setFeatures(Features features) {
        this.features = features;
    }

}
