package org.moera.node.media;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import org.moera.lib.crypto.Fingerprint;
import org.moera.node.util.ExtendedDuration;

public class MediaGrantProperties {

    private final Fingerprint fingerprint;

    public MediaGrantProperties(Fingerprint fingerprint) {
        this.fingerprint = fingerprint;
    }

    public int getVersion() {
        return fingerprint.getVersion();
    }

    public String getObjectType() {
        return (String) fingerprint.get("object_type");
    }

    public String getNodeName() {
        return (String) fingerprint.get("node_name");
    }

    public String getPostingId() {
        return (String) fingerprint.get("posting_id");
    }

    public String getCommentId() {
        return (String) fingerprint.get("comment_id");
    }

    public String getMediaId() {
        return (String) fingerprint.get("media_id");
    }

    public Timestamp getExpires() {
        return (Timestamp) fingerprint.get("expires");
    }

    public boolean isDownload() {
        return Boolean.TRUE.equals(fingerprint.get("download"));
    }

    public String getFileName() {
        return (String) fingerprint.get("file_name");
    }

    public static ExtendedDuration cacheDuration(MediaGrantProperties grant, boolean unrestrictedMedia) {
        if (grant == null) {
            return unrestrictedMedia ? ExtendedDuration.ALWAYS : ExtendedDuration.NEVER;
        }

        Duration duration = Duration.between(Instant.now(), grant.getExpires().toInstant());
        return duration.isNegative() ? ExtendedDuration.NEVER : new ExtendedDuration(duration);
    }

}
