package org.moera.node.fingerprint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.moera.lib.node.Fingerprints;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.media.LocalRemoteMedia;

public class AttachmentFingerprintBuilder {

    public static final short LATEST_VERSION = 0;

    public static byte[] build(byte[] digest) {
        return build(LATEST_VERSION, digest);
    }

    public static byte[] build(short version, byte[] digest) {
        return Fingerprints.attachment(digest);
    }

    public static byte[] build(LocalRemoteMedia media) {
        return build(LATEST_VERSION, media);
    }

    public static byte[] build(short version, LocalRemoteMedia media) {
        return Fingerprints.attachment(media.digest());
    }

    public static List<byte[]> build(
        MediaFileOwner parentMedia,
        RemoteMediaFile parentRemoteMedia,
        Collection<EntryAttachment> entryAttachments
    ) {
        LocalRemoteMedia parent = parentMedia != null || parentRemoteMedia != null
            ? new LocalRemoteMedia(parentMedia, parentRemoteMedia)
            : null;
        return build(parent, entryAttachments);
    }

    public static List<byte[]> build(
        LocalRemoteMedia parentMedia,
        Collection<EntryAttachment> entryAttachments
    ) {
        if (entryAttachments == null) {
            entryAttachments = Collections.emptyList();
        }

        List<byte[]> digests = new ArrayList<>();
        if (parentMedia != null) {
            byte[] digest = parentMedia.digest();
            if (digest != null) {
                digests.add(build(parentMedia));
            }
        }
        entryAttachments.stream()
            .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
            .map(EntryAttachment::getLocalRemoteMedia)
            .filter(lrm -> lrm.digest() != null)
            .map(AttachmentFingerprintBuilder::build)
            .forEach(digests::add);
        return digests;
    }

    public static <T> List<byte[]> build(
        byte[] parentMediaDigest,
        Collection<T> mediaAttachments,
        Function<T, byte[]> mediaDigest
    ) {
        if (mediaAttachments == null) {
            mediaAttachments = Collections.emptyList();
        }

        List<byte[]> digests = new ArrayList<>();
        if (parentMediaDigest != null) {
            digests.add(build(parentMediaDigest));
        }
        mediaAttachments.stream()
            .map(mediaDigest)
            .map(AttachmentFingerprintBuilder::build)
            .forEach(digests::add);
        return digests;
    }

}
