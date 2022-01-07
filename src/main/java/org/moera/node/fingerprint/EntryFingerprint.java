package org.moera.node.fingerprint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.moera.commons.crypto.Digest;
import org.moera.commons.crypto.Fingerprint;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.model.MediaAttachment;
import org.moera.node.model.PrivateMediaFileInfo;

public class EntryFingerprint extends Fingerprint {

    public EntryFingerprint(int version) {
        super(version);
    }

    protected Digest<Fingerprint> mediaAttachmentFingerprint(AttachmentFingerprint af) {
        var digest = new Digest<Fingerprint>();
        digest.setValue(af);
        return digest;
    }

    protected Digest<Fingerprint> mediaAttachmentFingerprint(MediaFileOwner mediaFileOwner) {
        return mediaAttachmentFingerprint(new AttachmentFingerprint(mediaFileOwner));
    }

    protected List<Digest<Fingerprint>> mediaAttachmentsFingerprint(
            byte[] parentMediaDigest, MediaAttachment[] mediaAttachments,
            Function<PrivateMediaFileInfo, byte[]> mediaDigest) {

        if (mediaAttachments == null) {
            return Collections.emptyList();
        }

        List<Digest<Fingerprint>> digests = new ArrayList<>();
        if (parentMediaDigest != null) {
            digests.add(mediaAttachmentFingerprint(new AttachmentFingerprint(parentMediaDigest)));
        }
        Arrays.stream(mediaAttachments)
                .map(MediaAttachment::getMedia)
                .map(mediaDigest)
                .map(AttachmentFingerprint::new)
                .map(this::mediaAttachmentFingerprint)
                .forEach(digests::add);
        return digests;
    }

    protected List<Digest<Fingerprint>> mediaAttachmentsFingerprint(
            byte[] parentMediaDigest, UUID[] mediaIds, Function<UUID, byte[]> mediaDigest) {

        if (mediaIds == null) {
            return Collections.emptyList();
        }

        List<Digest<Fingerprint>> digests = new ArrayList<>();
        if (parentMediaDigest != null) {
            digests.add(mediaAttachmentFingerprint(new AttachmentFingerprint(parentMediaDigest)));
        }
        Arrays.stream(mediaIds)
                .map(mediaDigest)
                .map(AttachmentFingerprint::new)
                .map(this::mediaAttachmentFingerprint)
                .forEach(digests::add);
        return digests;
    }

}
