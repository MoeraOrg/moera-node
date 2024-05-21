package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.moera.node.util.Util;

@Entity
@Table(name = "sitemap_records")
public class SitemapRecord {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    private UUID sitemapId;

    @OneToOne(fetch = FetchType.LAZY)
    private Entry entry;

    @NotNull
    private Timestamp createdAt = Util.now();

    @NotNull
    private Timestamp modifiedAt = Util.now();

    @NotNull
    private int totalUpdates;

    @NotNull
    private boolean visible;

    public SitemapRecord() {
    }

    public SitemapRecord(UUID sitemapId, Posting posting) {
        id = UUID.randomUUID();
        nodeId = posting.getNodeId();
        this.sitemapId = sitemapId;
        entry = posting;
        createdAt = posting.getCreatedAt();
        modifiedAt = posting.getEditedAt();
        totalUpdates = Math.max(posting.getTotalRevisions(), 1);
        visible = posting.getViewCompound().isPublic();
    }

    public void update(Posting posting) {
        modifiedAt = posting.getEditedAt();
        totalUpdates = totalUpdates + 1;
        visible = posting.getViewCompound().isPublic();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public UUID getSitemapId() {
        return sitemapId;
    }

    public void setSitemapId(UUID sitemapId) {
        this.sitemapId = sitemapId;
    }

    public Entry getEntry() {
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Timestamp modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public int getTotalUpdates() {
        return totalUpdates;
    }

    public void setTotalUpdates(int totalUpdates) {
        this.totalUpdates = totalUpdates;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}
