package org.moera.node.data.fingerprint;

import org.moera.commons.crypto.Digest;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.util.Util;

public class PostingFingerprint extends Fingerprint {

    public String receiverName;
    public String ownerName;
    public Digest<String> bodySrc = new Digest<>();
    public String bodySrcFormat;
    public String body;
    public String bodyFormat;
    public long createdAt;
    public byte attachments; // TODO for future use

    public PostingFingerprint(Posting posting, EntryRevision revision) {
        super(0);
        receiverName = posting.getReceiverName();
        ownerName = posting.getOwnerName();
        bodySrc.setValue(revision.getBodySrc());
        bodySrcFormat = revision.getBodySrcFormat().getValue();
        body = revision.getBody();
        bodyFormat = revision.getBodyFormat();
        createdAt = Util.toEpochSecond(revision.getCreatedAt());
    }

}
