package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.Posting;
import org.moera.node.util.Util;

public class PostingInfo {

    private UUID id;
    private UUID revisionId;
    private int totalRevisions;
    private String ownerName;
    private int ownerGeneration;
    private String bodyPreviewHtml;
    private byte[] bodySrcHash;
    private String bodySrcFormat;
    private String bodyHtml;
    private String heading;
    private long createdAt;
    private long editedAt;
    private Long deletedAt;
    private long publishedAt;
    private byte[] signature;
    private long moment;
    private Map<String, String[]> operations;

    public PostingInfo() {
        operations = new HashMap<>();
        operations.put("edit", new String[]{"owner"});
        operations.put("delete", new String[]{"owner"});
    }

    public PostingInfo(Posting posting) {
        this();

        id = posting.getId();
        revisionId = posting.getCurrentRevision().getId();
        totalRevisions = posting.getTotalRevisions();
        ownerName = posting.getOwnerName();
        ownerGeneration = posting.getOwnerGeneration();
        bodyPreviewHtml = posting.getCurrentRevision().getBodyPreviewHtml();
        bodySrcHash = CryptoUtil.digest(posting.getCurrentRevision().getBodySrc());
        bodySrcFormat = posting.getCurrentRevision().getBodySrcFormat().getValue();
        bodyHtml = posting.getCurrentRevision().getBodyHtml();
        heading = posting.getCurrentRevision().getHeading();
        createdAt = Util.toEpochSecond(posting.getCreatedAt());
        editedAt = Util.toEpochSecond(posting.getCurrentRevision().getCreatedAt());
        deletedAt = Util.toEpochSecond(posting.getDeletedAt());
        publishedAt = Util.toEpochSecond(posting.getCurrentRevision().getPublishedAt());
        signature = posting.getCurrentRevision().getSignature();
        moment = posting.getCurrentRevision().getMoment();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(UUID revisionId) {
        this.revisionId = revisionId;
    }

    public int getTotalRevisions() {
        return totalRevisions;
    }

    public void setTotalRevisions(int totalRevisions) {
        this.totalRevisions = totalRevisions;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(long editedAt) {
        this.editedAt = editedAt;
    }

    public Long getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Long deletedAt) {
        this.deletedAt = deletedAt;
    }

    public long getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(long publishedAt) {
        this.publishedAt = publishedAt;
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

    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
    }

}
