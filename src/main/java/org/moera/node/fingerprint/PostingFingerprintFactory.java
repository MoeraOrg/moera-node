package org.moera.node.fingerprint;

import java.util.UUID;
import java.util.function.Function;

import org.moera.commons.crypto.Fingerprint;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.PostingText;
import org.moera.node.model.PrivateMediaFileInfo;

public class PostingFingerprintFactory extends FingerprintFactory {

    public PostingFingerprintFactory(Class<? extends Fingerprint> klass) {
        super(klass);
    }

    public Fingerprint create(Posting posting, EntryRevision revision) {
        var constructor = getConstructor(Posting.class, EntryRevision.class);
        return constructor != null ? create(constructor, posting, revision) : null;
    }

    public Fingerprint create(PostingInfo postingInfo, byte[] parentMediaDigest,
                              Function<PrivateMediaFileInfo, byte[]> mediaDigest) {
        var constructor = getConstructor(PostingInfo.class, byte[].class, Function.class);
        return constructor != null ? create(constructor, postingInfo, parentMediaDigest, mediaDigest) : null;
    }

    public Fingerprint create(PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                              byte[] parentMediaDigest, Function<PrivateMediaFileInfo, byte[]> mediaDigest) {
        var constructor = getConstructor(PostingInfo.class, PostingRevisionInfo.class, byte[].class, Function.class);
        return constructor != null
                ? create(constructor, postingInfo, postingRevisionInfo, parentMediaDigest, mediaDigest)
                : null;
    }

    public Fingerprint create(PostingText postingText, byte[] parentMediaDigest, Function<UUID, byte[]> mediaDigest) {
        var constructor = getConstructor(PostingText.class, byte[].class, Function.class);
        return constructor != null ? create(constructor, postingText, parentMediaDigest, mediaDigest) : null;
    }

}
