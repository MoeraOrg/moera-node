package org.moera.node.media;

import java.security.PrivateKey;

import org.moera.lib.node.MediaGrant;
import org.moera.node.option.Options;
import org.moera.node.util.ExtendedDuration;

public class MediaGrantGenerator {

    private final String nodeName;
    private final String postingId;
    private final String commentId;
    private final PrivateKey signingKey;

    public MediaGrantGenerator(
        String nodeName,
        String postingId,
        String commentId,
        Options options
    ) {
        this.nodeName = nodeName;
        this.postingId = postingId;
        this.commentId = commentId;
        this.signingKey = options.getPrivateKey("profile.signing-key");
    }

    public String generate(String mediaId, ExtendedDuration duration, boolean download, String fileName) {
        return MediaGrant.generate(
            nodeName,
            postingId,
            commentId,
            mediaId,
            MediaUtil.expirationTimestamp(duration),
            download,
            fileName,
            signingKey
        );
    }

}
