package org.moera.node.fingerprint;

import org.moera.commons.crypto.Digest;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.data.SourceFormat;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.util.Util;

@FingerprintVersion(objectType = FingerprintObjectType.POSTING, version = 0)
public class PostingFingerprint extends Fingerprint {

    public static final short VERSION = 0;

    public String objectType = FingerprintObjectType.POSTING.name();
    public String receiverName;
    public String ownerName;
    public Digest<String> bodySrc = new Digest<>();
    public String bodySrcFormat;
    public String body;
    public String bodyFormat;
    public long createdAt;
    public byte permissions; // TODO for future use
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

    public PostingFingerprint(PostingInfo postingInfo) {
        super(0);
        receiverName = postingInfo.getReceiverName();
        ownerName = postingInfo.getOwnerName();
        bodySrc.setDigest(postingInfo.getBodySrcHash());
        bodySrcFormat = SourceFormat.toValue(postingInfo.getBodySrcFormat());
        body = postingInfo.getBody().getEncoded();
        bodyFormat = postingInfo.getBodyFormat();
        createdAt = postingInfo.getEditedAt();
    }

    public PostingFingerprint(PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo) {
        super(0);
        receiverName = postingInfo.getReceiverName();
        ownerName = postingInfo.getOwnerName();
        bodySrc.setDigest(postingRevisionInfo.getBodySrcHash());
        bodySrcFormat = SourceFormat.toValue(postingRevisionInfo.getBodySrcFormat());
        body = postingRevisionInfo.getBody().getEncoded();
        bodyFormat = postingRevisionInfo.getBodyFormat();
        createdAt = postingRevisionInfo.getCreatedAt();
    }

}
