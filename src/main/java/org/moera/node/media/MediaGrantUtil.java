package org.moera.node.media;

import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.sql.Timestamp;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.naming.NodeName;
import org.moera.lib.node.Fingerprints;
import org.moera.lib.util.Util;

/**
 * This class provides functionality for generating and handling a cryptographic media grant.
 * A media grant is a signed short-living token that authorizes access to a specific media file.
 */
public class MediaGrantUtil {

    /**
     * Generates a cryptographically signed media grant.
     *
     * @param nodeName   name of the node that grants media access, <code>null</code> for the target node itself
     * @param postingId  ID of the posting that grants media access
     * @param commentId  ID of the comment that grants media access, if any
     * @param mediaId    ID of the accessed media on the target node
     * @param expires    timestamp when the grant expires
     * @param download   whether the media should be downloaded
     * @param fileName   the preferred name of the media file when it is downloaded
     * @param signingKey the private key used to sign the media grant
     * @return the signed media grant
     */
    public static String generate(
        String nodeName,
        String postingId,
        String commentId,
        String mediaId,
        Timestamp expires,
        boolean download,
        String fileName,
        PrivateKey signingKey
    ) {
        var salt = new byte[8];
        new SecureRandom().nextBytes(salt);
        byte[] fingerprint = Fingerprints.mediaGrant(
            NodeName.expand(nodeName),
            postingId,
            commentId,
            mediaId,
            expires,
            download,
            fileName,
            salt
        );
        byte[] signature = CryptoUtil.sign(fingerprint, (ECPrivateKey) signingKey);
        byte[] grant = new byte[fingerprint.length + signature.length];
        System.arraycopy(fingerprint, 0, grant, 0, fingerprint.length);
        System.arraycopy(signature, 0, grant, fingerprint.length, signature.length);
        return Util.base64urlencode(grant);
    }

}
