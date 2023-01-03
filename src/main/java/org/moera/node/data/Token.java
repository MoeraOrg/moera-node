package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.vladmihalcea.hibernate.type.basic.Inet;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLInetType;
import org.hibernate.annotations.TypeDef;
import org.moera.node.util.Util;

@Entity
@Table(name = "tokens")
@TypeDef(name = "Inet", typeClass = PostgreSQLInetType.class, defaultForType = Inet.class)
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
    private long authCategory;

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

    public long getAuthCategory() {
        return authCategory;
    }

    public void setAuthCategory(long authCategory) {
        this.authCategory = authCategory;
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
