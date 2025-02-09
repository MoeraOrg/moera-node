package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.hypersistence.utils.hibernate.type.basic.Inet;
import io.hypersistence.utils.hibernate.type.basic.PostgreSQLInetType;
import org.hibernate.annotations.Type;
import org.moera.node.util.Util;

@Entity
@Table(name = "tokens")
public class Token {

    @Id
    private UUID id;

    @Size(max = 45)
    private String token;

    @NotNull
    private UUID nodeId;

    @Size(max = 127)
    private String name;

    @NotNull
    private long authScope;

    @Type(PostgreSQLInetType.class)
    @Column(columnDefinition = "inet")
    private Inet ip;

    @Size(max = 48)
    private String pluginName;

    @NotNull
    private Timestamp createdAt = Util.now();

    private Timestamp deadline;

    private Timestamp lastUsedAt;

    private String lastUsedBrowser;

    private Inet lastUsedIp;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getAuthScope() {
        return authScope;
    }

    public void setAuthScope(long authScope) {
        this.authScope = authScope;
    }

    public Inet getIp() {
        return ip;
    }

    public void setIp(Inet ip) {
        this.ip = ip;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }

    public Timestamp getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Timestamp lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public String getLastUsedBrowser() {
        return lastUsedBrowser;
    }

    public void setLastUsedBrowser(String lastUsedBrowser) {
        this.lastUsedBrowser = lastUsedBrowser;
    }

    public Inet getLastUsedIp() {
        return lastUsedIp;
    }

    public void setLastUsedIp(Inet lastUsedIp) {
        this.lastUsedIp = lastUsedIp;
    }

}
