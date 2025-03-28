package org.moera.node.fingerprint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.moera.lib.node.Fingerprints;
import org.moera.lib.node.types.MediaAttachment;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.MediaFileOwner;

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

    public static byte[] build(EntryAttachment attachment) {
        return build(LATEST_VERSION, attachment);
    }

    public static byte[] build(short version, EntryAttachment attachment) {
        return build(version, attachment.getMediaFileOwner());
    }

    public static List<byte[]> build(
        MediaFileOwner parentMedia,
        Collection<EntryAttachment> entryAttachments
    ) {
        if (entryAttachments == null) {
            entryAttachments = Collections.emptyList();
        }

        List<byte[]> digests = new ArrayList<>();
        if (parentMedia != null) {
            digests.add(build(parentMedia));
        }
        entryAttachments.stream()
            .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
            .filter(ea -> ea.getMediaFileOwner().getMediaFile().getDigest() != null)
            .map(AttachmentFingerprintBuilder::build)
            .forEach(digests::add);
        return digests;
    }

    public static List<byte[]> build(
        byte[] parentMediaDigest,
        Collection<MediaAttachment> mediaAttachments,
        Function<PrivateMediaFileInfo, byte[]> mediaDigest
    ) {
        if (mediaAttachments == null) {
            mediaAttachments = Collections.emptyList();
        }

        List<byte[]> digests = new ArrayList<>();
        if (parentMediaDigest != null) {
            digests.add(build(parentMediaDigest));
        }
        mediaAttachments.stream()
            .map(MediaAttachment::getMedia)
            .map(mediaDigest)
            .map(AttachmentFingerprintBuilder::build)
            .forEach(digests::add);
        return digests;
    }

    public static List<byte[]> buildFromIds(
        byte[] parentMediaDigest,
        Collection<String> mediaIds,
        Function<UUID, byte[]> mediaDigest
    ) {
        if (mediaIds == null) {
            mediaIds = Collections.emptyList();
        }

        List<byte[]> digests = new ArrayList<>();
        if (parentMediaDigest != null) {
            digests.add(build(parentMediaDigest));
        }
        mediaIds.stream()
            .map(UUID::fromString)
            .map(mediaDigest)
            .map(AttachmentFingerprintBuilder::build)
            .forEach(digests::add);
        return digests;
    }

}
