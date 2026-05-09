package org.moera.node.media;

import java.security.PrivateKey;

import org.moera.node.option.Options;
import org.moera.node.util.ExtendedDuration;

public class MediaGrantGenerator implements MediaGrantSupplier {

    private final String nodeName;
    private final PrivateKey signingKey;

    public MediaGrantGenerator(String nodeName, Options options) {
        this.nodeName = nodeName;
        this.signingKey = options.getPrivateKey("profile.signing-key");
    }

    @Override
    public String generate(String mediaId, ExtendedDuration duration, boolean download, String fileName) {
        return MediaGrantUtil.generate(
            nodeName,
            mediaId,
            MediaUtil.expirationTimestamp(duration),
            download,
            fileName,
            signingKey
        );
    }

}
