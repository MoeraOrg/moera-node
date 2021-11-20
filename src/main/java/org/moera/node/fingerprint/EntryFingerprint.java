package org.moera.node.fingerprint;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.moera.commons.crypto.Digest;
import org.moera.commons.crypto.Fingerprint;
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

    protected List<Digest<Fingerprint>> mediaAttachmentsFingerprint(
            MediaAttachment[] mediaAttachments, Function<PrivateMediaFileInfo, byte[]> mediaDigest) {

        if (mediaAttachments == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(mediaAttachments)
                .map(MediaAttachment::getMedia)
                .map(mediaDigest)
                .map(AttachmentFingerprint::new)
                .map(this::mediaAttachmentFingerprint)
                .collect(Collectors.toList());
    }

}
