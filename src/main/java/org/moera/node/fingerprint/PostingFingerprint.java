package org.moera.node.fingerprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.moera.commons.crypto.Digest;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.data.SourceFormat;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.PostingText;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.util.Util;

@FingerprintVersion(objectType = FingerprintObjectType.POSTING, version = 1)
public class PostingFingerprint extends EntryFingerprint {

    public static final short VERSION = 1;

    public String objectType = FingerprintObjectType.POSTING.name();
    public String receiverName;
    public String ownerName;
    public Digest<String> bodySrc = new Digest<>();
    public String bodySrcFormat;
    public String body;
    public String bodyFormat;
    public long createdAt;
    public byte permissions; // TODO for future use
    public List<Digest<Fingerprint>> attachments = Collections.emptyList();

    public PostingFingerprint(Posting posting, EntryRevision revision) {
        super(1);
        // TODO it should be posting.receiverName, if it is not null, and node name otherwise,
        // to support postings in group nodes (?)
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
        attachments = new ArrayList<>();
        if (posting.getParentMedia() != null) {
            attachments.add(mediaAttachmentFingerprint(posting.getParentMedia()));
        }
        if (revision.getAttachments() != null) {
            revision.getAttachments().stream()
                    .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
                    .map(AttachmentFingerprint::new)
                    .filter(af -> af.digest != null)
                    .map(this::mediaAttachmentFingerprint)
                    .forEach(attachments::add);
        }
    }

    public PostingFingerprint(PostingInfo postingInfo, byte[] parentMediaDigest,
                              Function<PrivateMediaFileInfo, byte[]> mediaDigest) {
        super(1);
        receiverName = postingInfo.getOwnerName();
        ownerName = postingInfo.getOwnerName();
        bodySrc.setDigest(postingInfo.getBodySrcHash());
        bodySrcFormat = SourceFormat.toValue(postingInfo.getBodySrcFormat());
        body = postingInfo.getBody().getEncoded();
        bodyFormat = postingInfo.getBodyFormat();
        createdAt = postingInfo.isOriginal()
                ? postingInfo.getRevisionCreatedAt() : postingInfo.getReceiverRevisionCreatedAt();
        attachments = mediaAttachmentsFingerprint(parentMediaDigest, postingInfo.getMedia(), mediaDigest);
    }

    public PostingFingerprint(PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                              byte[] parentMediaDigest, Function<PrivateMediaFileInfo, byte[]> mediaDigest) {
        super(1);
        receiverName = postingInfo.getOwnerName();
        ownerName = postingInfo.getOwnerName();
        bodySrc.setDigest(postingRevisionInfo.getBodySrcHash());
        bodySrcFormat = SourceFormat.toValue(postingRevisionInfo.getBodySrcFormat());
        body = postingRevisionInfo.getBody().getEncoded();
        bodyFormat = postingRevisionInfo.getBodyFormat();
        createdAt = postingInfo.isOriginal()
                ? postingRevisionInfo.getCreatedAt() : postingRevisionInfo.getReceiverCreatedAt();
        attachments = mediaAttachmentsFingerprint(parentMediaDigest, postingRevisionInfo.getMedia(), mediaDigest);
    }

    public PostingFingerprint(PostingText postingText, byte[] parentMediaDigest, Function<UUID, byte[]> mediaDigest) {
        super(1);
        receiverName = postingText.getOwnerName();
        ownerName = postingText.getOwnerName();
        bodySrc.setValue(postingText.getBodySrc());
        bodySrcFormat = postingText.getBodySrcFormat().getValue();
        body = postingText.getBody();
        bodyFormat = postingText.getBodyFormat();
        createdAt = postingText.getCreatedAt();
        attachments = mediaAttachmentsFingerprint(parentMediaDigest, postingText.getMedia(), mediaDigest);
    }

}
