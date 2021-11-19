package org.moera.node.fingerprint;

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

    public Fingerprint create(CommentText commentText, byte[] postingDigest, byte[] repliedToDigest) {
        var constructor = getConstructor(CommentText.class, byte[].class, byte[].class);
        return constructor != null ? create(constructor, commentText, postingDigest, repliedToDigest) : null;
    }

    public Fingerprint create(CommentText commentText, Fingerprint postingFingerprint, byte[] repliedToDigest) {
        var constructor = getConstructor(CommentText.class, Fingerprint.class, byte[].class);
        return constructor != null ? create(constructor, commentText, postingFingerprint, repliedToDigest) : null;
    }

    public Fingerprint create(CommentInfo commentInfo, Fingerprint postingFingerprint) {
        var constructor = getConstructor(CommentInfo.class, Fingerprint.class);
        return constructor != null ? create(constructor, commentInfo, postingFingerprint) : null;
    }

    public Fingerprint create(CommentInfo commentInfo, CommentRevisionInfo commentRevisionInfo,
                              PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> postingMediaDigest) {
        var constructor = getConstructor(CommentInfo.class, CommentRevisionInfo.class, PostingInfo.class,
                PostingRevisionInfo.class, Function.class);
        return constructor != null
                ? create(constructor, commentInfo, commentRevisionInfo, postingInfo, postingRevisionInfo,
                         postingMediaDigest)
                : null;
    }

    public Fingerprint create(CommentInfo commentInfo,
                              PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> postingMediaDigest, byte[] repliedToDigest) {
        var constructor = getConstructor(CommentInfo.class, PostingInfo.class, PostingRevisionInfo.class,
                Function.class, byte[].class);
        return constructor != null
                ? create(constructor, commentInfo, postingInfo, postingRevisionInfo, postingMediaDigest,
                         repliedToDigest)
                : null;
    }

    public Fingerprint create(CommentInfo commentInfo, CommentRevisionInfo commentRevisionInfo,
                              PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> postingMediaDigest, byte[] repliedToDigest) {
        var constructor = getConstructor(CommentInfo.class, CommentRevisionInfo.class, PostingInfo.class,
                PostingRevisionInfo.class, Function.class, byte[].class);
        return constructor != null
                ? create(constructor, commentInfo, commentRevisionInfo, postingInfo, postingRevisionInfo,
                         postingMediaDigest, repliedToDigest)
                : null;
    }

}
