package org.moera.node.fingerprint;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.moera.commons.crypto.Digest;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.data.Comment;
import org.moera.node.data.EntryAttachment;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentRevisionInfo;
import org.moera.node.model.CommentText;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.util.Util;

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

    public CommentFingerprint(Comment comment) {
        super(0);
        ownerName = comment.getOwnerName();
        postingFingerprint.setDigest(comment.getPosting().getCurrentRevision().getDigest());
        byte[] repliedToDigest = comment.getRepliedTo() != null
                ? comment.getRepliedTo().getCurrentRevision().getDigest() : null;
        repliedToFingerprint.setDigest(repliedToDigest);
        bodySrc.setValue(comment.getCurrentRevision().getBodySrc());
        bodySrcFormat = comment.getCurrentRevision().getBodySrcFormat().getValue();
        body = comment.getCurrentRevision().getBody();
        bodyFormat = comment.getCurrentRevision().getBodyFormat();
        createdAt = Util.toEpochSecond(comment.getCurrentRevision().getCreatedAt());
        attachments = comment.getCurrentRevision().getAttachments().stream()
                .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
                .map(EntryAttachment::getMediaFileOwner)
                .map(this::mediaAttachmentFingerprint)
                .collect(Collectors.toList());
    }

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
        attachments = mediaAttachmentsFingerprint(null, commentText.getMedia(), mediaDigest);
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
        attachments = mediaAttachmentsFingerprint(null, commentText.getMedia(), mediaDigest);
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
        attachments = mediaAttachmentsFingerprint(null, commentInfo.getMedia(), mediaDigest);
    }

    public CommentFingerprint(CommentInfo commentInfo, CommentRevisionInfo commentRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> commentMediaDigest,
                              PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                              byte[] postingParentMediaDigest,
                              Function<PrivateMediaFileInfo, byte[]> postingMediaDigest) {
        super(0);
        ownerName = commentInfo.getOwnerName();
        this.postingFingerprint.setValue(Fingerprints.posting(postingRevisionInfo.getSignatureVersion())
                .create(postingInfo, postingRevisionInfo, postingParentMediaDigest, postingMediaDigest));
        byte[] repliedToDigest = commentInfo.getRepliedTo() != null ? commentInfo.getRepliedTo().getDigest() : null;
        this.repliedToFingerprint.setDigest(repliedToDigest);
        bodySrc.setDigest(commentRevisionInfo.getBodySrcHash());
        bodySrcFormat = commentRevisionInfo.getBodySrcFormat().getValue();
        body = commentRevisionInfo.getBody().getEncoded();
        bodyFormat = commentRevisionInfo.getBodyFormat();
        createdAt = commentRevisionInfo.getCreatedAt();
        attachments = mediaAttachmentsFingerprint(null, commentInfo.getMedia(), commentMediaDigest);
    }

    public CommentFingerprint(CommentInfo commentInfo,
                              Function<PrivateMediaFileInfo, byte[]> commentMediaDigest,
                              PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                              byte[] postingParentMediaDigest,
                              Function<PrivateMediaFileInfo, byte[]> postingMediaDigest, byte[] repliedToDigest) {
        super(0);
        ownerName = commentInfo.getOwnerName();
        this.postingFingerprint.setValue(Fingerprints.posting(postingRevisionInfo.getSignatureVersion())
                .create(postingInfo, postingRevisionInfo, postingParentMediaDigest, postingMediaDigest));
        this.repliedToFingerprint.setDigest(repliedToDigest);
        bodySrc.setDigest(commentInfo.getBodySrcHash());
        bodySrcFormat = commentInfo.getBodySrcFormat().getValue();
        body = commentInfo.getBody().getEncoded();
        bodyFormat = commentInfo.getBodyFormat();
        createdAt = commentInfo.getRevisionCreatedAt();
        attachments = mediaAttachmentsFingerprint(null, commentInfo.getMedia(), commentMediaDigest);
    }

    public CommentFingerprint(CommentInfo commentInfo, CommentRevisionInfo commentRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> commentMediaDigest,
                              PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                              byte[] postingParentMediaDigest,
                              Function<PrivateMediaFileInfo, byte[]> postingMediaDigest, byte[] repliedToDigest) {
        super(0);
        ownerName = commentInfo.getOwnerName();
        this.postingFingerprint.setValue(Fingerprints.posting(postingRevisionInfo.getSignatureVersion())
                .create(postingInfo, postingRevisionInfo, postingParentMediaDigest, postingMediaDigest));
        this.repliedToFingerprint.setDigest(repliedToDigest);
        bodySrc.setDigest(commentRevisionInfo.getBodySrcHash());
        bodySrcFormat = commentRevisionInfo.getBodySrcFormat().getValue();
        body = commentRevisionInfo.getBody().getEncoded();
        bodyFormat = commentRevisionInfo.getBodyFormat();
        createdAt = commentRevisionInfo.getCreatedAt();
        attachments = mediaAttachmentsFingerprint(null, commentInfo.getMedia(), commentMediaDigest);
    }

}
