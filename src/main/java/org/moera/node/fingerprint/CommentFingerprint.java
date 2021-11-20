package org.moera.node.fingerprint;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.moera.commons.crypto.Digest;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentRevisionInfo;
import org.moera.node.model.CommentText;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.PrivateMediaFileInfo;

@FingerprintVersion(objectType = FingerprintObjectType.COMMENT, version = 0)
public class CommentFingerprint extends EntryFingerprint {

    public static final short VERSION = 0;

    public String objectType = FingerprintObjectType.COMMENT.name();
    public String ownerName;
    public Digest<Fingerprint> postingFingerprint = new Digest<>();
    public Digest<Fingerprint> repliedToFingerprint = new Digest<>();
    public Digest<String> bodySrc = new Digest<>();
    public String bodySrcFormat;
    public String body;
    public String bodyFormat;
    public long createdAt;
    public byte permissions; // TODO for future use
    public List<Digest<Fingerprint>> attachments;

    public CommentFingerprint(CommentText commentText, byte[] postingDigest, byte[] repliedToDigest,
                              Function<UUID, byte[]> mediaDigest) {
        super(0);
        ownerName = commentText.getOwnerName();
        postingFingerprint.setDigest(postingDigest);
        repliedToFingerprint.setDigest(repliedToDigest);
        bodySrc.setValue(commentText.getBodySrc());
        bodySrcFormat = commentText.getBodySrcFormat().getValue();
        body = commentText.getBody();
        bodyFormat = commentText.getBodyFormat();
        createdAt = commentText.getCreatedAt();
        attachments = mediaAttachmentsFingerprint(commentText.getMedia(), mediaDigest);
    }

    public CommentFingerprint(CommentText commentText, Fingerprint postingFingerprint, byte[] repliedToDigest,
                              Function<UUID, byte[]> mediaDigest) {
        super(0);
        ownerName = commentText.getOwnerName();
        this.postingFingerprint.setValue(postingFingerprint);
        this.repliedToFingerprint.setDigest(repliedToDigest);
        bodySrc.setValue(commentText.getBodySrc());
        bodySrcFormat = commentText.getBodySrcFormat().getValue();
        body = commentText.getBody();
        bodyFormat = commentText.getBodyFormat();
        createdAt = commentText.getCreatedAt();
        attachments = mediaAttachmentsFingerprint(commentText.getMedia(), mediaDigest);
    }

    public CommentFingerprint(CommentInfo commentInfo, Fingerprint postingFingerprint,
                              Function<PrivateMediaFileInfo, byte[]> mediaDigest) {
        super(0);
        ownerName = commentInfo.getOwnerName();
        this.postingFingerprint.setValue(postingFingerprint);
        byte[] repliedToDigest = commentInfo.getRepliedTo() != null ? commentInfo.getRepliedTo().getDigest() : null;
        this.repliedToFingerprint.setDigest(repliedToDigest);
        bodySrc.setDigest(commentInfo.getBodySrcHash());
        bodySrcFormat = commentInfo.getBodySrcFormat().getValue();
        body = commentInfo.getBody().getEncoded();
        bodyFormat = commentInfo.getBodyFormat();
        createdAt = commentInfo.getRevisionCreatedAt();
        attachments = mediaAttachmentsFingerprint(commentInfo.getMedia(), mediaDigest);
    }

    public CommentFingerprint(CommentInfo commentInfo, CommentRevisionInfo commentRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> commentMediaDigest,
                              PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> postingMediaDigest) {
        super(0);
        ownerName = commentInfo.getOwnerName();
        this.postingFingerprint.setValue(Fingerprints.posting(postingRevisionInfo.getSignatureVersion())
                .create(postingInfo, postingRevisionInfo, postingMediaDigest));
        byte[] repliedToDigest = commentInfo.getRepliedTo() != null ? commentInfo.getRepliedTo().getDigest() : null;
        this.repliedToFingerprint.setDigest(repliedToDigest);
        bodySrc.setDigest(commentRevisionInfo.getBodySrcHash());
        bodySrcFormat = commentRevisionInfo.getBodySrcFormat().getValue();
        body = commentRevisionInfo.getBody().getEncoded();
        bodyFormat = commentRevisionInfo.getBodyFormat();
        createdAt = commentRevisionInfo.getCreatedAt();
        attachments = mediaAttachmentsFingerprint(commentInfo.getMedia(), commentMediaDigest);
    }

    public CommentFingerprint(CommentInfo commentInfo,
                              Function<PrivateMediaFileInfo, byte[]> commentMediaDigest,
                              PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> postingMediaDigest, byte[] repliedToDigest) {
        super(0);
        ownerName = commentInfo.getOwnerName();
        this.postingFingerprint.setValue(Fingerprints.posting(postingRevisionInfo.getSignatureVersion())
                .create(postingInfo, postingRevisionInfo, postingMediaDigest));
        this.repliedToFingerprint.setDigest(repliedToDigest);
        bodySrc.setDigest(commentInfo.getBodySrcHash());
        bodySrcFormat = commentInfo.getBodySrcFormat().getValue();
        body = commentInfo.getBody().getEncoded();
        bodyFormat = commentInfo.getBodyFormat();
        createdAt = commentInfo.getRevisionCreatedAt();
        attachments = mediaAttachmentsFingerprint(commentInfo.getMedia(), commentMediaDigest);
    }

    public CommentFingerprint(CommentInfo commentInfo, CommentRevisionInfo commentRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> commentMediaDigest,
                              PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> postingMediaDigest, byte[] repliedToDigest) {
        super(0);
        ownerName = commentInfo.getOwnerName();
        this.postingFingerprint.setValue(Fingerprints.posting(postingRevisionInfo.getSignatureVersion())
                .create(postingInfo, postingRevisionInfo, postingMediaDigest));
        this.repliedToFingerprint.setDigest(repliedToDigest);
        bodySrc.setDigest(commentRevisionInfo.getBodySrcHash());
        bodySrcFormat = commentRevisionInfo.getBodySrcFormat().getValue();
        body = commentRevisionInfo.getBody().getEncoded();
        bodyFormat = commentRevisionInfo.getBodyFormat();
        createdAt = commentRevisionInfo.getCreatedAt();
        attachments = mediaAttachmentsFingerprint(commentInfo.getMedia(), commentMediaDigest);
    }

    private List<Digest<Fingerprint>> mediaAttachmentsFingerprint(UUID[] mediaIds, Function<UUID, byte[]> mediaDigest) {
        if (mediaIds == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(mediaIds)
                .map(mediaDigest)
                .map(AttachmentFingerprint::new)
                .map(this::mediaAttachmentFingerprint)
                .collect(Collectors.toList());
    }

}
