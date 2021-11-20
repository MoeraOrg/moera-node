package org.moera.node.fingerprint;

import java.util.function.Function;

import org.moera.commons.crypto.Fingerprint;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentRevisionInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfo;

public class ReactionFingerprintFactory extends FingerprintFactory {

    public ReactionFingerprintFactory(Class<? extends Fingerprint> klass) {
        super(klass);
    }

    public Fingerprint create(ReactionDescription description, byte[] entryDigest) {
        var constructor = getConstructor(ReactionDescription.class, byte[].class);
        return constructor != null ? create(constructor, description, entryDigest) : null;
    }

    public Fingerprint create(String ownerName, ReactionAttributes attributes, EntryFingerprint entryFingerprint) {
        var constructor = getConstructor(String.class, ReactionAttributes.class, EntryFingerprint.class);
        return constructor != null ? create(constructor, ownerName, attributes, entryFingerprint) : null;
    }

    public Fingerprint create(ReactionInfo reactionInfo, PostingInfo postingInfo,
                              PostingRevisionInfo postingRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> postingMediaDigest) {
        var constructor = getConstructor(ReactionInfo.class, PostingInfo.class, PostingRevisionInfo.class,
                Function.class);
        return constructor != null
                ? create(constructor, reactionInfo, postingInfo, postingRevisionInfo, postingMediaDigest)
                : null;
    }

    public Fingerprint create(ReactionInfo reactionInfo, CommentInfo commentInfo,
                              CommentRevisionInfo commentRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> commentMediaDigest,
                              PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> postingMediaDigest) {
        var constructor = getConstructor(ReactionInfo.class, CommentInfo.class, CommentRevisionInfo.class,
                PostingInfo.class, PostingRevisionInfo.class, Function.class);
        return constructor != null
                ? create(constructor, reactionInfo, commentInfo, commentRevisionInfo, commentMediaDigest, postingInfo,
                         postingRevisionInfo, postingMediaDigest)
                : null;
    }

}
