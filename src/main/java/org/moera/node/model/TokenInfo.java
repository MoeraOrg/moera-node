package org.moera.node.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.Scope;
import org.moera.node.data.Token;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenInfo {

    private String id;
    private String token;
    private String name;
    private List<String> permissions;
    private String pluginName;
    private Long createdAt;
    private Long deadline;
    private Long lastUsedAt;
    private String lastUsedBrowser;
    private String lastUsedIp;

    public TokenInfo() {
    }

    public TokenInfo(Token tokenData, boolean includeToken) {
        id = tokenData.getId().toString();
        token = tokenData.getToken();
        name = tokenData.getName();
        if (!includeToken) {
            token = token.substring(0, 4) + '\u2026';
        }
        permissions = Scope.toValues(tokenData.getAuthScope());
        pluginName = tokenData.getPluginName();
        createdAt = Util.toEpochSecond(tokenData.getCreatedAt());
        deadline = Util.toEpochSecond(tokenData.getDeadline());
        lastUsedAt = Util.toEpochSecond(tokenData.getLastUsedAt());
        lastUsedBrowser = tokenData.getLastUsedBrowser();
        if (tokenData.getLastUsedIp() != null) {
            lastUsedIp = tokenData.getLastUsedIp().getAddress();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public Long getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Long lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public String getLastUsedBrowser() {
        return lastUsedBrowser;
    }

    public void setLastUsedBrowser(String lastUsedBrowser) {
        this.lastUsedBrowser = lastUsedBrowser;
    }

    public String getLastUsedIp() {
        return lastUsedIp;
    }

    public void setLastUsedIp(String lastUsedIp) {
        this.lastUsedIp = lastUsedIp;
    }

}
