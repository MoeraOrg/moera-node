package org.moera.node.media;

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.inject.Inject;

import org.moera.lib.crypto.CryptoException;
import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.crypto.Fingerprint;
import org.moera.lib.crypto.FingerprintException;
import org.moera.lib.crypto.RestoredFingerprint;
import org.moera.lib.node.Fingerprints;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.lib.util.LogUtil;
import org.moera.node.api.naming.NamingCache;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class MediaGrantValidator {

    private static final Logger log = LoggerFactory.getLogger(MediaGrantValidator.class);

    private final Map<UUID, ECPublicKey> nodePublicKeys = new ConcurrentHashMap<>();

    @Inject
    private RequestContext requestContext;

    @Inject
    @Lazy
    private NamingCache namingCache;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private CommentRepository commentRepository;

    public MediaGrantProperties validate(String grantS, UUID mediaId) {
        if (ObjectUtils.isEmpty(grantS)) {
            return null;
        }

        byte[] grant = decode(grantS);
        if (grant.length == 0) {
            return null;
        }

        Fingerprint fingerprint;
        byte[] signature;
        try {
            RestoredFingerprint restored = CryptoUtil.restoreFingerprint(
                grant, version -> Fingerprints.getSchema("MEDIA_GRANT", version)
            );
            fingerprint = restored.fingerprint();
            if (fingerprint == null) {
                throw new ValidationFailure("media-grant.unknown-fingerprint");
            }
            signature = new byte[restored.available()];
            System.arraycopy(grant, grant.length - signature.length, signature, 0, signature.length);
        } catch (CryptoException | FingerprintException e) {
            log.info("Media grant: unknown fingerprint");
            throw new ValidationFailure("media-grant.unknown-fingerprint");
        }

        MediaGrantProperties properties = new MediaGrantProperties(fingerprint);
        validateFingerprint(properties, mediaId != null ? mediaId.toString() : null);
        validateEntry(properties);
        validateSignature(fingerprint, signature, properties);

        return properties;
    }

    private byte[] decode(String grantS) {
        try {
            return Util.base64urldecode(grantS);
        } catch (IllegalArgumentException e) {
            log.info("Media grant: invalid encoding");
            throw new ValidationFailure("media-grant.invalid");
        }
    }

    private void validateFingerprint(MediaGrantProperties grant, String mediaId) {
        if (!"MEDIA_GRANT".equals(grant.getObjectType())) {
            log.info("Media grant: not a media grant fingerprint");
            throw new ValidationFailure("media-grant.invalid");
        }
        if (ObjectUtils.isEmpty(grant.getPostingId())) {
            log.info("Media grant: posting ID is missing");
            throw new ValidationFailure("media-grant.posting-id.missing");
        }
        Timestamp expires = grant.getExpires();
        if (expires == null) {
            log.info("Media grant: expiration timestamp is missing");
            throw new ValidationFailure("media-grant.expires.missing");
        }
        if (Instant.now().isAfter(expires.toInstant())) {
            log.info("Media grant: expired at {}", LogUtil.format(expires));
            throw new ValidationFailure("media-grant.expired");
        }
        if (!Objects.equals(grant.getMediaId(), mediaId)) {
            log.info(
                "Media grant: media ID {} does not match {}",
                LogUtil.format(grant.getMediaId()), LogUtil.format(mediaId)
            );
            throw new ValidationFailure("media-grant.wrong-media");
        }
    }

    private void validateEntry(MediaGrantProperties grant) {
        UUID postingId = Util.uuid(
            grant.getPostingId(),
            () -> new ValidationFailure("media-grant.posting-id.invalid")
        );
        if (!ObjectUtils.isEmpty(grant.getCommentId())) {
            UUID commentId = Util.uuid(
                grant.getCommentId(),
                () -> new ValidationFailure("media-grant.comment-id.invalid")
            );
            Comment comment = commentRepository.findByNodeIdAndId(requestContext.nodeId(), commentId)
                .orElseThrow(() -> {
                    log.info("Media grant: comment {} is not found", LogUtil.format(commentId));
                    return new ValidationFailure("media-grant.comment-not-found");
                });
            if (comment.getPosting() == null || !Objects.equals(comment.getPosting().getId(), postingId)) {
                log.info("Media grant: comment {} belongs to a wrong posting", LogUtil.format(commentId));
                throw new ValidationFailure("media-grant.wrong-posting");
            }
        }
        if (postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId).isEmpty()) {
            log.info("Media grant: posting {} is not found", LogUtil.format(postingId));
            throw new ValidationFailure("media-grant.posting-not-found");
        }
    }

    private void validateSignature(Fingerprint fingerprint, byte[] signature, MediaGrantProperties grant) {
        if (signature.length == 0) {
            log.info("Media grant: signature is missing");
            throw new ValidationFailure("media-grant.invalid-signature");
        }

        byte[] fingerprintBytes = CryptoUtil.fingerprint(
            fingerprint, Fingerprints.getSchema("MEDIA_GRANT", fingerprint.getVersion())
        );
        if (!verifySignature(fingerprintBytes, signature, grant)) {
            log.info("Media grant: signature verification failed");
            throw new ValidationFailure("media-grant.invalid-signature");
        }
    }

    private boolean verifySignature(byte[] fingerprint, byte[] signature, MediaGrantProperties grant) {
        if (!ObjectUtils.isEmpty(grant.getNodeName())) {
            byte[] signingKey = namingCache.get(grant.getNodeName()).getSigningKey();
            if (signingKey == null) {
                log.info("Media grant: signing key for node {} is unknown", LogUtil.format(grant.getNodeName()));
                throw new ValidationFailure("media-grant.unknown-signing-key");
            }
            return CryptoUtil.verifySignature(fingerprint, signature, signingKey);
        }

        return CryptoUtil.verifySignature(fingerprint, signature, currentNodePublicKey());
    }

    private ECPublicKey currentNodePublicKey() {
        UUID nodeId = requestContext.nodeId();
        if (nodeId == null) {
            log.info("Media grant: current node ID is unknown");
            throw new ValidationFailure("media-grant.unknown-signing-key");
        }
        return nodePublicKeys.computeIfAbsent(nodeId, id -> calcCurrentNodePublicKey());
    }

    private ECPublicKey calcCurrentNodePublicKey() {
        PrivateKey signingKey = requestContext.getOptions() != null
            ? requestContext.getOptions().getPrivateKey("profile.signing-key")
            : null;
        if (signingKey == null) {
            log.info("Media grant: signing key for the current node is unknown");
            throw new ValidationFailure("media-grant.unknown-signing-key");
        }
        return CryptoUtil.privateToPublicKey((ECPrivateKey) signingKey);
    }

}
