package org.moera.node.fingerprint;

import java.util.UUID;
import java.util.function.Function;

import org.moera.commons.crypto.Digest;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.data.SourceFormat;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.PostingText;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.util.Util;

@FingerprintVersion(objectType = FingerprintObjectType.POSTING, version = 0)
public class PostingFingerprint0 extends EntryFingerprint {

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

    public PostingFingerprint0(Posting posting, EntryRevision revision) {
        super(0);
        // TODO it should be posting.receiverName, if it is not null, and node name otherwise,
        // to support postings in group nodes
        receiverName = posting.getOwnerName();
        ownerName = posting.getOwnerName();
        if (posting.isOriginal()) {
            bodySrc.setValue(revision.getBodySrc());
        } else {
            bodySrc.setDigest(revision.getReceiverBodySrcHash());
        }
        bodySrcFormat = revision.getBodySrcFormat().getValue();
        body = revision.getBody();
        bodyFormat = revision.getBodyFormat();
        createdAt = Util.toEpochSecond(posting.isOriginal()
                ? revision.getCreatedAt() : revision.getReceiverCreatedAt());
    }

    public PostingFingerprint0(PostingInfo postingInfo, byte[] parentMediaDigest,
                               Function<PrivateMediaFileInfo, byte[]> mediaDigest) {
        this(postingInfo);
    }

    public PostingFingerprint0(PostingInfo postingInfo) {
        super(0);
        receiverName = postingInfo.getOwnerName();
        ownerName = postingInfo.getOwnerName();
        bodySrc.setDigest(postingInfo.getBodySrcHash());
        bodySrcFormat = SourceFormat.toValue(postingInfo.getBodySrcFormat());
        body = postingInfo.getBody().getEncoded();
        bodyFormat = postingInfo.getBodyFormat();
        createdAt = postingInfo.isOriginal()
                ? postingInfo.getRevisionCreatedAt() : postingInfo.getReceiverRevisionCreatedAt();
    }

    public PostingFingerprint0(PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                               byte[] parentMediaDigest, Function<PrivateMediaFileInfo, byte[]> mediaDigest) {
        this(postingInfo, postingRevisionInfo);
    }

    public PostingFingerprint0(PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo) {
        super(0);
        receiverName = postingInfo.getOwnerName();
        ownerName = postingInfo.getOwnerName();
        bodySrc.setDigest(postingRevisionInfo.getBodySrcHash());
        bodySrcFormat = SourceFormat.toValue(postingRevisionInfo.getBodySrcFormat());
        body = postingRevisionInfo.getBody().getEncoded();
        bodyFormat = postingRevisionInfo.getBodyFormat();
        createdAt = postingInfo.isOriginal()
                ? postingRevisionInfo.getCreatedAt() : postingRevisionInfo.getReceiverCreatedAt();
    }

    public PostingFingerprint0(PostingText postingText, byte[] parentMediaDigest, Function<UUID, byte[]> mediaDigest) {
        this(postingText);
    }

    private PostingFingerprint0(PostingText postingText) {
        super(1);
        receiverName = postingText.getOwnerName();
        ownerName = postingText.getOwnerName();
        bodySrc.setValue(postingText.getBodySrc());
        bodySrcFormat = postingText.getBodySrcFormat().getValue();
        body = postingText.getBody();
        bodyFormat = postingText.getBodyFormat();
        createdAt = postingText.getCreatedAt();
    }

}
