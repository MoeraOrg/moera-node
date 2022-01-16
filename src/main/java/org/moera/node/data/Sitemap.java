package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

public class Sitemap {

    private UUID id;
    private int total;
    private Timestamp modifiedAt;

    public Sitemap() {
    }

    public Sitemap(UUID id, long total, Date modifiedAt) {
        this.id = id;
        this.total = (int) total;
        this.modifiedAt = new Timestamp(modifiedAt.getTime());
    }

    public Sitemap(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public Timestamp getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Timestamp modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

}
