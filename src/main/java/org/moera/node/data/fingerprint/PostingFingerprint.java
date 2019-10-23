package org.moera.node.data.fingerprint;

import org.moera.commons.crypto.Digest;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.util.Util;

public class PostingFingerprint extends Fingerprint {

    public String ownerName;
    public Digest<String> bodySrc = new Digest<>();
    public String bodySrcFormat;
    public String bodyHtml;
    public long createdAt;

    public PostingFingerprint(Posting posting, EntryRevision revision) {
        super(0);
        ownerName = posting.getOwnerName();
        bodySrc.setValue(revision.getBodySrc());
        bodySrcFormat = revision.getBodySrcFormat().getValue();
        bodyHtml = revision.getBodyHtml();
        createdAt = Util.toEpochSecond(revision.getCreatedAt());
    }

}
