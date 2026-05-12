package org.moera.node.media;

import org.moera.node.util.ExtendedDuration;

public interface MediaGrantSupplier {

    String generate(String nodeName, String mediaId, ExtendedDuration duration, boolean download, String fileName);

}
