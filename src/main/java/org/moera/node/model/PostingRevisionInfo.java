package org.moera.node.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.EntryRevision;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostingRevisionInfo {

    private UUID id;
    private String bodyPreviewHtml;
    private byte[] bodySrcHash;
    private String bodySrcFormat;
    private String bodyHtml;
    private String bodyHtmlFormat;
    private String heading;
    private long createdAt;
    private long publishedAt;
    private Long deletedAt;
    private byte[] signature;
    private long moment;

    public PostingRevisionInfo() {
    }

    public PostingRevisionInfo(EntryRevision revision) {
        id = revision.getId();
        bodyPreviewHtml = revision.getBodyPreviewHtml();
        bodySrcHash = CryptoUtil.digest(revision.getBodySrc());
        bodySrcFormat = revision.getBodySrcFormat().getValue();
        bodyHtml = revision.getBodyHtml();
        bodyHtmlFormat = revision.getBodyHtmlFormat();
        heading = revision.getHeading();
        createdAt = Util.toEpochSecond(revision.getCreatedAt());
        publishedAt = Util.toEpochSecond(revision.getPublishedAt());
        deletedAt = Util.toEpochSecond(revision.getDeletedAt());
        signature = revision.getSignature();
        moment = revision.getMoment();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getBodyHtmlFormat() {
        return bodyHtmlFormat;
    }

    public void setBodyHtmlFormat(String bodyHtmlFormat) {
        this.bodyHtmlFormat = bodyHtmlFormat;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(long publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Long getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Long deletedAt) {
        this.deletedAt = deletedAt;
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
