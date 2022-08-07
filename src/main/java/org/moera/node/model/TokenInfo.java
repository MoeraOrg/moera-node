package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.auth.AuthCategory;
import org.moera.node.data.Token;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenInfo {

    private String id;
    private String token;
    private String[] permissions;
    private String pluginName;
    private Long createdAt;
    private Long deadline;

    public TokenInfo() {
    }

    public TokenInfo(Token tokenData, boolean includeToken) {
        id = tokenData.getId().toString();
        if (includeToken) {
            token = tokenData.getToken();
        }
        permissions = AuthCategory.toStrings(tokenData.getAuthCategory());
        pluginName = tokenData.getPluginName();
        createdAt = Util.toEpochSecond(tokenData.getCreatedAt());
        deadline = Util.toEpochSecond(tokenData.getDeadline());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String[] getPermissions() {
        return permissions;
    }

    public void setPermissions(String[] permissions) {
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

}
