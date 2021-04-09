package org.moera.node.media;

import java.io.IOException;

public class ThresholdReachedException extends IOException {

    public ThresholdReachedException() {
        super("Threshold reached");
    }

}
