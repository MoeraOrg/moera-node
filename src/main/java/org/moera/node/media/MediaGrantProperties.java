package org.moera.node.media;

import java.sql.Timestamp;

import org.moera.lib.crypto.Fingerprint;

public class MediaGrantProperties {

    private final Fingerprint fingerprint;

    public MediaGrantProperties(Fingerprint fingerprint) {
        this.fingerprint = fingerprint;
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

}
