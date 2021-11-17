package org.moera.node.fingerprint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.moera.commons.crypto.Digest;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.data.SourceFormat;
import org.moera.node.model.MediaAttachment;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.util.Util;

@FingerprintVersion(objectType = FingerprintObjectType.POSTING, version = 0)
public class PostingFingerprint extends EntryFingerprint {

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
    public List<Digest<AttachmentFingerprint>> attachments = new ArrayList<>();

    public PostingFingerprint(Posting posting, EntryRevision revision) {
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
        if (revision.getAttachments() != null) {
            attachments = revision.getAttachments().stream()
                    .map(AttachmentFingerprint::new)
                    .filter(af -> af.digest != null)
                    .map(this::mediaAttachmentFingerprint)
                    .collect(Collectors.toList());
        }
    }

    public PostingFingerprint(PostingInfo postingInfo, Function<PrivateMediaFileInfo, byte[]> mediaDigest) {
        super(0);
        receiverName = postingInfo.getOwnerName();
        ownerName = postingInfo.getOwnerName();
        bodySrc.setDigest(postingInfo.getBodySrcHash());
        bodySrcFormat = SourceFormat.toValue(postingInfo.getBodySrcFormat());
        body = postingInfo.getBody().getEncoded();
        bodyFormat = postingInfo.getBodyFormat();
        createdAt = postingInfo.isOriginal()
                ? postingInfo.getRevisionCreatedAt() : postingInfo.getReceiverRevisionCreatedAt();
        if (postingInfo.getMedia() != null) {
            attachments = Arrays.stream(postingInfo.getMedia())
                    .map(MediaAttachment::getMedia)
                    .map(mediaDigest)
                    .map(AttachmentFingerprint::new)
                    .map(this::mediaAttachmentFingerprint)
                    .collect(Collectors.toList());
        }
    }

    public PostingFingerprint(PostingInfo postingInfo, PostingRevisionInfo postingRevisionInfo,
                              Function<PrivateMediaFileInfo, byte[]> mediaDigest) {
        super(0);
        receiverName = postingInfo.getOwnerName();
        ownerName = postingInfo.getOwnerName();
        bodySrc.setDigest(postingRevisionInfo.getBodySrcHash());
        bodySrcFormat = SourceFormat.toValue(postingRevisionInfo.getBodySrcFormat());
        body = postingRevisionInfo.getBody().getEncoded();
        bodyFormat = postingRevisionInfo.getBodyFormat();
        createdAt = postingInfo.isOriginal()
                ? postingRevisionInfo.getCreatedAt() : postingRevisionInfo.getReceiverCreatedAt();
        if (postingRevisionInfo.getMedia() != null) {
            attachments = Arrays.stream(postingRevisionInfo.getMedia())
                    .map(MediaAttachment::getMedia)
                    .map(mediaDigest)
                    .map(AttachmentFingerprint::new)
                    .map(this::mediaAttachmentFingerprint)
                    .collect(Collectors.toList());
        }
    }

    private Digest<AttachmentFingerprint> mediaAttachmentFingerprint(AttachmentFingerprint af) {
        var digest = new Digest<AttachmentFingerprint>();
        digest.setValue(af);
        return digest;
    }

}
