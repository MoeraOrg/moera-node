package org.moera.node.fingerprint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.moera.lib.node.Fingerprints;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.model.MediaAttachment;
import org.moera.node.model.PrivateMediaFileInfo;

public class AttachmentFingerprintBuilder {

    public static final short LATEST_VERSION = 0;

    public static byte[] build(byte[] digest) {
        return build(LATEST_VERSION, digest);
    }

    public static byte[] build(short version, byte[] digest) {
        return Fingerprints.attachment(digest);
    }

    public static byte[] build(MediaFileOwner mediaFileOwner) {
        return build(LATEST_VERSION, mediaFileOwner);
    }

    public static byte[] build(short version, MediaFileOwner mediaFileOwner) {
        return Fingerprints.attachment(mediaFileOwner.getMediaFile().getDigest());
    }

    public static byte[] build(short version, EntryAttachment attachment) {
        return build(version, attachment.getMediaFileOwner());
    }

    public static List<byte[]> build(
        byte[] parentMediaDigest,
        MediaAttachment[] mediaAttachments,
        Function<PrivateMediaFileInfo, byte[]> mediaDigest
    ) {
        if (mediaAttachments == null) {
            mediaAttachments = new MediaAttachment[0];
        }

        List<byte[]> digests = new ArrayList<>();
        if (parentMediaDigest != null) {
            digests.add(build(parentMediaDigest));
        }
        Arrays.stream(mediaAttachments)
            .map(MediaAttachment::getMedia)
            .map(mediaDigest)
            .map(AttachmentFingerprintBuilder::build)
            .forEach(digests::add);
        return digests;
    }

    public List<byte[]> build(
        byte[] parentMediaDigest,
        UUID[] mediaIds,
        Function<UUID, byte[]> mediaDigest
    ) {
        if (mediaIds == null) {
            mediaIds = new UUID[0];
        }

        List<byte[]> digests = new ArrayList<>();
        if (parentMediaDigest != null) {
            digests.add(build(parentMediaDigest));
        }
        Arrays.stream(mediaIds)
            .map(mediaDigest)
            .map(AttachmentFingerprintBuilder::build)
            .forEach(digests::add);
        return digests;
    }

}
