package org.moera.node.media;

import java.security.PrivateKey;
import java.sql.Timestamp;

import org.moera.node.option.Options;
import org.moera.node.util.ExtendedDuration;

public class MediaGrantGenerator implements MediaGrantSupplier {

    private final String nodeName;
    private final PrivateKey signingKey;
    private final Options options;

    public MediaGrantGenerator(Options options) {
        this.nodeName = options.nodeName();
        this.signingKey = options.getPrivateKey("profile.signing-key");
        this.options = options;
    }

    @Override
    public String generateLocal(
        String mediaId,
        ExtendedDuration duration,
        boolean download,
        String fileName
    ) {
        return MediaGrantUtil.generate(null, mediaId, expires(duration), download, fileName, salt(), signingKey);
    }

    @Override
    public String generateRemote(
        String mediaId,
        ExtendedDuration duration,
        boolean download,
        String fileName
    ) {
        return MediaGrantUtil.generate(nodeName, mediaId, expires(duration), download, fileName, salt(), signingKey);
    }

    public String generatePublicRemote(
        String mediaId,
        boolean download,
        String fileName
    ) {
        return MediaGrantUtil.generate(
            nodeName, mediaId, expires(ExtendedDuration.ALWAYS), download, fileName, new byte[0], signingKey
        );
    }

    @Override
    public Timestamp expires(ExtendedDuration duration) {
        return MediaUtil.expirationTimestamp(duration);
    }

    private byte[] salt() {
        return options.mediaGrantSalt();
    }

}
