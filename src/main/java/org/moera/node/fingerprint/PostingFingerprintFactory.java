package org.moera.node.fingerprint;

import java.util.function.Function;

import org.moera.commons.crypto.Fingerprint;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.PrivateMediaFileInfo;

public class PostingFingerprintFactory extends FingerprintFactory {

    public PostingFingerprintFactory(Class<? extends Fingerprint> klass) {
        super(klass);
    }

    public Fingerprint create(Posting posting, EntryRevision revision) {
        var constructor = getConstructor(Posting.class, EntryRevision.class);
        return constructor != null ? create(constructor, posting, revision) : null;
    }

    public Fingerprint create(PostingInfo postingInfo, Function<PrivateMediaFileInfo, byte[]> mediaDigest) {
        var constructor = getConstructor(PostingInfo.class, Function.class);
        return constructor != null ? create(constructor, postingInfo, mediaDigest) : null;
    }

    public Fingerprint create(PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> mediaDigest) {
        var constructor = getConstructor(PostingInfo.class, PostingRevisionInfo.class, Function.class);
        return constructor != null ? create(constructor, postingInfo, postingRevisionInfo, mediaDigest) : null;
    }

}
