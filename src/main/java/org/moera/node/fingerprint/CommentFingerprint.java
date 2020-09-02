package org.moera.node.fingerprint;

import org.moera.commons.crypto.Digest;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentRevisionInfo;
import org.moera.node.model.CommentText;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;

@FingerprintVersion(objectType = FingerprintObjectType.COMMENT, version = 0)
public class CommentFingerprint extends EntryFingerprint {

    public static final short VERSION = 0;

    public String objectType = FingerprintObjectType.COMMENT.name();
    public String ownerName;
    public Digest<PostingFingerprint> postingFingerprint = new Digest<>();
    public Digest<CommentFingerprint> repliedToFingerprint = new Digest<>();
    public Digest<String> bodySrc = new Digest<>();
    public String bodySrcFormat;
    public String body;
    public String bodyFormat;
    public long createdAt;
    public byte permissions; // TODO for future use
    public byte attachments; // TODO for future use

    public CommentFingerprint(CommentText commentText, byte[] postingDigest) {
        super(0);
        ownerName = commentText.getOwnerName();
        postingFingerprint.setDigest(postingDigest);
        bodySrc.setValue(commentText.getBodySrc());
        bodySrcFormat = commentText.getBodySrcFormat().getValue();
        body = commentText.getBody().getEncoded();
        bodyFormat = commentText.getBodyFormat();
        createdAt = commentText.getCreatedAt();
    }

    public CommentFingerprint(CommentText commentText, PostingFingerprint postingFingerprint) {
        super(0);
        ownerName = commentText.getOwnerName();
        this.postingFingerprint.setValue(postingFingerprint);
        bodySrc.setValue(commentText.getBodySrc());
        bodySrcFormat = commentText.getBodySrcFormat().getValue();
        body = commentText.getBody().getEncoded();
        bodyFormat = commentText.getBodyFormat();
        createdAt = commentText.getCreatedAt();
    }

    public CommentFingerprint(CommentInfo commentInfo, PostingFingerprint postingFingerprint) {
        super(0);
        ownerName = commentInfo.getOwnerName();
        this.postingFingerprint.setValue(postingFingerprint);
        bodySrc.setDigest(commentInfo.getBodySrcHash());
        bodySrcFormat = commentInfo.getBodySrcFormat().getValue();
        body = commentInfo.getBody().getEncoded();
        bodyFormat = commentInfo.getBodyFormat();
        createdAt = commentInfo.getRevisionCreatedAt();
    }

    public CommentFingerprint(CommentInfo commentInfo, CommentRevisionInfo commentRevisionInfo,
                              PostingFingerprint postingFingerprint) {
        super(0);
        ownerName = commentInfo.getOwnerName();
        this.postingFingerprint.setValue(postingFingerprint);
        bodySrc.setDigest(commentRevisionInfo.getBodySrcHash());
        bodySrcFormat = commentRevisionInfo.getBodySrcFormat().getValue();
        body = commentRevisionInfo.getBody().getEncoded();
        bodyFormat = commentRevisionInfo.getBodyFormat();
        createdAt = commentRevisionInfo.getCreatedAt();
    }

    public CommentFingerprint(CommentInfo commentInfo, PostingInfo postingInfo,
                              PostingRevisionInfo postingRevisionInfo) {
        super(0);
        ownerName = commentInfo.getOwnerName();
        this.postingFingerprint.setValue(new PostingFingerprint(postingInfo, postingRevisionInfo));
        bodySrc.setDigest(commentInfo.getBodySrcHash());
        bodySrcFormat = commentInfo.getBodySrcFormat().getValue();
        body = commentInfo.getBody().getEncoded();
        bodyFormat = commentInfo.getBodyFormat();
        createdAt = commentInfo.getRevisionCreatedAt();
    }

    public CommentFingerprint(CommentInfo commentInfo, CommentRevisionInfo commentRevisionInfo, PostingInfo postingInfo,
                              PostingRevisionInfo postingRevisionInfo) {
        super(0);
        ownerName = commentInfo.getOwnerName();
        this.postingFingerprint.setValue(new PostingFingerprint(postingInfo, postingRevisionInfo));
        bodySrc.setDigest(commentRevisionInfo.getBodySrcHash());
        bodySrcFormat = commentRevisionInfo.getBodySrcFormat().getValue();
        body = commentRevisionInfo.getBody().getEncoded();
        bodyFormat = commentRevisionInfo.getBodyFormat();
        createdAt = commentRevisionInfo.getCreatedAt();
    }

}
