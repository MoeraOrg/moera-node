package org.moera.node.model;

import java.util.UUID;

import org.moera.node.data.Posting;
import org.moera.node.util.Util;

public class PostingInfo {

    private UUID id;
    private String ownerName;
    private int ownerGeneration;
    private String bodySrc;
    private String bodyHtml;
    private long created;
    private byte[] signature;

    public PostingInfo() {
    }

    public PostingInfo(Posting posting) {
        id = posting.getId();
        ownerName = posting.getOwnerName();
        ownerGeneration = posting.getOwnerGeneration();
        bodySrc = posting.getBodySrc();
        bodyHtml = posting.getBodyHtml();
        created = Util.toEpochSecond(posting.getCreated());
        signature = posting.getSignature();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public int getOwnerGeneration() {
        return ownerGeneration;
    }

    public void setOwnerGeneration(int ownerGeneration) {
        this.ownerGeneration = ownerGeneration;
    }

    public String getBodySrc() {
        return bodySrc;
    }

    public void setBodySrc(String bodySrc) {
        this.bodySrc = bodySrc;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

}
