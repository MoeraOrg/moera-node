package org.moera.node.media;

import java.sql.Timestamp;

import org.moera.node.util.ExtendedDuration;

public interface MediaGrantSupplier {

    String generateLocal(String mediaId, ExtendedDuration duration, boolean download, String fileName);

    String generateRemote(String mediaId, ExtendedDuration duration, boolean download, String fileName);

    Timestamp expires(ExtendedDuration duration);

}
