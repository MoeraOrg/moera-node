package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.node.util.Util;

@Entity
@Table(name = "contacts")
public class Contact {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Size(max = 63)
    private String remoteNodeName = "";

    @Size(max = 96)
    private String remoteFullName;

    @Size(max = 31)
    private String remoteGender;

    @ManyToOne
    private MediaFile remoteAvatarMediaFile;

    @Size(max = 8)
    private String remoteAvatarShape;

    @NotNull
    private float closenessBase;

    @NotNull
    private float closeness;

    @NotNull
    private Timestamp createdAt = Util.now();

    @NotNull
    private Timestamp updatedAt = Util.now();

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

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemoteFullName() {
        return remoteFullName;
    }

    public void setRemoteFullName(String remoteFullName) {
        this.remoteFullName = remoteFullName;
    }

    public String getRemoteGender() {
        return remoteGender;
    }

    public void setRemoteGender(String remoteGender) {
        this.remoteGender = remoteGender;
    }

    public MediaFile getRemoteAvatarMediaFile() {
        return remoteAvatarMediaFile;
    }

    public void setRemoteAvatarMediaFile(MediaFile remoteAvatarMediaFile) {
        this.remoteAvatarMediaFile = remoteAvatarMediaFile;
    }

    public String getRemoteAvatarShape() {
        return remoteAvatarShape;
    }

    public void setRemoteAvatarShape(String remoteAvatarShape) {
        this.remoteAvatarShape = remoteAvatarShape;
    }

    public float getClosenessBase() {
        return closenessBase;
    }

    public void setClosenessBase(float closenessBase) {
        this.closenessBase = closenessBase;
    }

    public float getCloseness() {
        return closeness;
    }

    public void setCloseness(float closeness) {
        this.closeness = closeness;
    }

    public void updateCloseness(float delta) {
        setCloseness(Math.max(1, getCloseness() + delta));
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

}
