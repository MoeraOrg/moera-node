package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.EntryAttachment;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RemoteMediaInfo {

    private String id;
    private String hash;
    private String digest;

    public RemoteMediaInfo() {
    }

    public RemoteMediaInfo(EntryAttachment attachment) {
        id = attachment.getRemoteMediaId();
        hash = attachment.getRemoteMediaHash();
        digest = Util.base64encode(attachment.getRemoteMediaDigest());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

}
