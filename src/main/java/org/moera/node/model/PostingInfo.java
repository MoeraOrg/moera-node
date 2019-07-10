package org.moera.node.model;

import java.util.UUID;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.Posting;
import org.moera.node.util.Util;

public class PostingInfo {

    private UUID id;
    private String ownerName;
    private int ownerGeneration;
    private String bodyPreviewHtml;
    private byte[] bodySrcHash;
    private String bodySrcFormat;
    private String bodyHtml;
    private String heading;
    private long created;
    private byte[] signature;
    private long moment;

    public PostingInfo() {
    }

    public PostingInfo(Posting posting) {
        id = posting.getId();
        ownerName = posting.getOwnerName();
        ownerGeneration = posting.getOwnerGeneration();
        bodyPreviewHtml = posting.getBodyPreviewHtml();
        bodySrcHash = CryptoUtil.digest(posting.getBodySrc());
        bodySrcFormat = posting.getBodySrcFormat().getValue();
        bodyHtml = posting.getBodyHtml();
        heading = posting.getHeading();
        created = Util.toEpochSecond(posting.getCreated());
        signature = posting.getSignature();
        moment = posting.getMoment();
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

    public String getBodyPreviewHtml() {
        return bodyPreviewHtml;
    }

    public void setBodyPreviewHtml(String bodyPreviewHtml) {
        this.bodyPreviewHtml = bodyPreviewHtml;
    }

    public byte[] getBodySrcHash() {
        return bodySrcHash;
    }

    public void setBodySrcHash(byte[] bodySrcHash) {
        this.bodySrcHash = bodySrcHash;
    }

    public String getBodySrcFormat() {
        return bodySrcFormat;
    }

    public void setBodySrcFormat(String bodySrcFormat) {
        this.bodySrcFormat = bodySrcFormat;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
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

    public long getMoment() {
        return moment;
    }

    public void setMoment(long moment) {
        this.moment = moment;
    }

}
