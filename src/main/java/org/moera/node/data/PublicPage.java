package org.moera.node.data;

import java.util.UUID;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "public_pages")
public class PublicPage {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private long id;

    @NotNull
    private UUID nodeId;

    @ManyToOne
    private Entry entry;

    @NotNull
    private long afterMoment;

    @NotNull
    private long beforeMoment;

    public PublicPage() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public Entry getEntry() {
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
    }

    public long getAfterMoment() {
        return afterMoment;
    }

    public void setAfterMoment(long afterMoment) {
        this.afterMoment = afterMoment;
    }

    public long getBeforeMoment() {
        return beforeMoment;
    }

    public void setBeforeMoment(long beforeMoment) {
        this.beforeMoment = beforeMoment;
    }

}
