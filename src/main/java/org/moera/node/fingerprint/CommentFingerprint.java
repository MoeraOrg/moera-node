package org.moera.node.fingerprint;

import org.moera.commons.crypto.Digest;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.model.CommentText;

@FingerprintVersion(objectType = FingerprintObjectType.COMMENT, version = 0)
public class CommentFingerprint extends Fingerprint {

    public static final short VERSION = 0;

    public String objectType = FingerprintObjectType.COMMENT.name();
    public String receiverName;
    public String ownerName;
    public Digest<PostingFingerprint> postingFingerprint = new Digest<>();
    public Digest<String> bodySrc = new Digest<>();
    public String bodySrcFormat;
    public String body;
    public String bodyFormat;
    public long createdAt;
    public byte permissions; // TODO for future use
    public byte attachments; // TODO for future use

    public CommentFingerprint(String receiverName, CommentText commentText, byte[] postingDigest) {
        super(0);
        this.receiverName = receiverName;
        ownerName = commentText.getOwnerName();
        postingFingerprint.setDigest(postingDigest);
        bodySrc.setValue(commentText.getBodySrc());
        bodySrcFormat = commentText.getBodySrcFormat().getValue();
        body = commentText.getBody().getEncoded();
        bodyFormat = commentText.getBodyFormat();
        createdAt = commentText.getCreatedAt();
    }

    public CommentFingerprint(String receiverName, CommentText commentText, PostingFingerprint postingFingerprint) {
        super(0);
        this.receiverName = receiverName;
        ownerName = commentText.getOwnerName();
        this.postingFingerprint.setValue(postingFingerprint);
        bodySrc.setValue(commentText.getBodySrc());
        bodySrcFormat = commentText.getBodySrcFormat().getValue();
        body = commentText.getBody().getEncoded();
        bodyFormat = commentText.getBodyFormat();
        createdAt = commentText.getCreatedAt();
    }

}
