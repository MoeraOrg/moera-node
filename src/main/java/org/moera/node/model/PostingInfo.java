package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.Posting;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostingInfo {

    private UUID id;
    private UUID revisionId;
    private int totalRevisions;
    private String receiverName;
    private String ownerName;
    private String bodyPreviewHtml;
    private String bodySrc;
    private byte[] bodySrcHash;
    private String bodySrcFormat;
    private String body;
    private String bodyFormat;
    private String heading;
    private long createdAt;
    private long editedAt;
    private Long deletedAt;
    private long publishedAt;
    private byte[] signature;
    private long moment;
    private Map<String, String[]> operations;

    public PostingInfo() {
    }

    public PostingInfo(Posting posting) {
        this(posting, false);
    }

    public PostingInfo(Posting posting, boolean includeSource) {
        id = posting.getId();
        revisionId = posting.getCurrentRevision().getId();
        totalRevisions = posting.getTotalRevisions();
        receiverName = posting.getReceiverName();
        ownerName = posting.getOwnerName();
        bodyPreviewHtml = posting.getCurrentRevision().getBodyPreviewHtml();
        if (includeSource) {
            bodySrc = posting.getCurrentRevision().getBodySrc();
        }
        bodySrcHash = CryptoUtil.digest(posting.getCurrentRevision().getBodySrc());
        bodySrcFormat = posting.getCurrentRevision().getBodySrcFormat().getValue();
        body = posting.getCurrentRevision().getBody();
        bodyFormat = posting.getCurrentRevision().getBodyFormat();
        heading = posting.getCurrentRevision().getHeading();
        createdAt = Util.toEpochSecond(posting.getCreatedAt());
        editedAt = Util.toEpochSecond(posting.getCurrentRevision().getCreatedAt());
        deletedAt = Util.toEpochSecond(posting.getDeletedAt());
        publishedAt = Util.toEpochSecond(posting.getCurrentRevision().getPublishedAt());
        signature = posting.getCurrentRevision().getSignature();
        moment = posting.getCurrentRevision().getMoment();
        operations = new HashMap<>();
        operations.put("edit", new String[]{"owner"});
        operations.put("delete", new String[]{"owner", "admin"});
        operations.put("revisions", new String[0]);
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

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getBodyPreviewHtml() {
        return bodyPreviewHtml;
    }

    public void setBodyPreviewHtml(String bodyPreviewHtml) {
        this.bodyPreviewHtml = bodyPreviewHtml;
    }

    public String getBodySrc() {
        return bodySrc;
    }

    public void setBodySrc(String bodySrc) {
        this.bodySrc = bodySrc;
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

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBodyFormat() {
        return bodyFormat;
    }

    public void setBodyFormat(String bodyFormat) {
        this.bodyFormat = bodyFormat;
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
