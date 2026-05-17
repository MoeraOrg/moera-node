package org.moera.node.media;

import java.security.PrivateKey;
import java.sql.Timestamp;

import org.moera.node.option.Options;
import org.moera.node.util.ExtendedDuration;

public class MediaGrantGenerator implements MediaGrantSupplier {

    private final String nodeName;
    private final PrivateKey signingKey;

    public MediaGrantGenerator(Options options) {
        this.nodeName = options.nodeName();
        this.signingKey = options.getPrivateKey("profile.signing-key");
    }

    @Override
    public String generateLocal(
        String mediaId,
        ExtendedDuration duration,
        boolean download,
        String fileName
    ) {
        return MediaGrantUtil.generate(null, mediaId, expires(duration), download, fileName, signingKey);
    }

    @Override
    public String generateRemote(
        String mediaId,
        ExtendedDuration duration,
        boolean download,
        String fileName
    ) {
        return MediaGrantUtil.generate(nodeName, mediaId, expires(duration), download, fileName, signingKey);
    }

    @Override
    public Timestamp expires(ExtendedDuration duration) {
        return MediaUtil.expirationTimestamp(duration);
    }

}
