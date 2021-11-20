package org.moera.node.fingerprint;

import java.util.UUID;
import java.util.function.Function;

import org.moera.commons.crypto.Fingerprint;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentRevisionInfo;
import org.moera.node.model.CommentText;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.PrivateMediaFileInfo;

public class CommentFingerprintFactory extends FingerprintFactory {

    public CommentFingerprintFactory(Class<? extends Fingerprint> klass) {
        super(klass);
    }

    public Fingerprint create(CommentText commentText, byte[] postingDigest, byte[] repliedToDigest,
                              Function<UUID, byte[]> mediaDigest) {
        var constructor = getConstructor(CommentText.class, byte[].class, byte[].class, Function.class);
        return constructor != null
                ? create(constructor, commentText, postingDigest, repliedToDigest, mediaDigest)
                : null;
    }

    public Fingerprint create(CommentText commentText, Fingerprint postingFingerprint, byte[] repliedToDigest,
                              Function<UUID, byte[]> mediaDigest) {
        var constructor = getConstructor(CommentText.class, Fingerprint.class, byte[].class, Function.class);
        return constructor != null
                ? create(constructor, commentText, postingFingerprint, repliedToDigest, mediaDigest)
                : null;
    }

    public Fingerprint create(CommentInfo commentInfo, Fingerprint postingFingerprint,
                              Function<PrivateMediaFileInfo, byte[]> mediaDigest) {
        var constructor = getConstructor(CommentInfo.class, Fingerprint.class, Function.class);
        return constructor != null ? create(constructor, commentInfo, postingFingerprint, mediaDigest) : null;
    }

    public Fingerprint create(CommentInfo commentInfo, CommentRevisionInfo commentRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> commentMediaDigest,
                              PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> postingMediaDigest) {
        var constructor = getConstructor(CommentInfo.class, CommentRevisionInfo.class, Function.class,
                PostingInfo.class, PostingRevisionInfo.class, Function.class);
        return constructor != null
                ? create(constructor, commentInfo, commentRevisionInfo, commentMediaDigest, postingInfo,
                         postingRevisionInfo, postingMediaDigest)
                : null;
    }

    public Fingerprint create(CommentInfo commentInfo,
                              Function<PrivateMediaFileInfo, byte[]> commentMediaDigest,
                              PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> postingMediaDigest, byte[] repliedToDigest) {
        var constructor = getConstructor(CommentInfo.class, Function.class, PostingInfo.class,
                PostingRevisionInfo.class, Function.class, byte[].class);
        return constructor != null
                ? create(constructor, commentInfo, commentMediaDigest, postingInfo, postingRevisionInfo,
                         postingMediaDigest, repliedToDigest)
                : null;
    }

    public Fingerprint create(CommentInfo commentInfo, CommentRevisionInfo commentRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> commentMediaDigest,
                              PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> postingMediaDigest, byte[] repliedToDigest) {
        var constructor = getConstructor(CommentInfo.class, CommentRevisionInfo.class, Function.class,
                PostingInfo.class, PostingRevisionInfo.class, Function.class, byte[].class);
        return constructor != null
                ? create(constructor, commentInfo, commentRevisionInfo, commentMediaDigest, postingInfo,
                         postingRevisionInfo, postingMediaDigest, repliedToDigest)
                : null;
    }

}
