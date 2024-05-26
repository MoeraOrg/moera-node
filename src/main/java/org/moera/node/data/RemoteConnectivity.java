package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.node.util.Util;

@Entity
@Table(name = "remote_connectivity")
public class RemoteConnectivity {

    @Id
    private UUID id;

    @NotNull
    @Size(max = 63)
    private String remoteNodeName;

    @NotNull
    @Enumerated
    private ConnectivityStatus status;

    @NotNull
    private Timestamp updatedAt = Util.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public ConnectivityStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectivityStatus status) {
        this.status = status;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

}
