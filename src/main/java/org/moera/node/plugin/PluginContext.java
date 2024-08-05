package org.moera.node.plugin;

import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.auth.Scope;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UniversalContext;
import org.moera.node.option.Options;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PluginContext {

    private boolean rootAdmin;
    private boolean admin;
    private List<String> authScope;
    private String clientName;
    private String remoteAddress;
    private String userAgent;
    private String userAgentOs;
    private UUID nodeId;
    private String nodeName;
    private String domainName;
    private String originUrl;
    @JsonIgnore
    private Options options;

    public PluginContext() {
    }

    public PluginContext(RequestContext requestContext) {
        rootAdmin = requestContext.isRootAdmin();
        admin = requestContext.isAdmin(Scope.IDENTIFY);
        authScope = Scope.toValues(requestContext.getAuthScope());
        clientName = Optional.ofNullable(requestContext.getClientName(Scope.IDENTIFY)).orElse("");
        remoteAddress = getRemoteAddress(requestContext);
        userAgent = requestContext.getUserAgent().name().toLowerCase();
        userAgentOs = requestContext.getUserAgentOs().name().toLowerCase();
        nodeId = requestContext.nodeId();
        nodeName = requestContext.nodeName();
        domainName = requestContext.getDomainName();
        originUrl = requestContext.getUrl();
        options = requestContext.getOptions();
    }

    public PluginContext(UniversalContext universalContext) {
        nodeId = universalContext.nodeId();
        nodeName = universalContext.nodeName();
        domainName = universalContext.getDomainName();
        options = universalContext.getOptions();
    }

    private String getRemoteAddress(RequestContext requestContext) {
        return requestContext.getRemoteAddr() != null ? requestContext.getRemoteAddr().getHostAddress() : "";
    }

    public boolean isRootAdmin() {
        return rootAdmin;
    }

    public void setRootAdmin(boolean rootAdmin) {
        this.rootAdmin = rootAdmin;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public List<String> getAuthScope() {
        return authScope;
    }

    public void setAuthScope(List<String> authScope) {
        this.authScope = authScope;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getUserAgentOs() {
        return userAgentOs;
    }

    public void setUserAgentOs(String userAgentOs) {
        this.userAgentOs = userAgentOs;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public void addContextHeaders(HttpRequest.Builder requestBuilder) {
        var vars = Map.<String, Object>of(
                "root-admin", rootAdmin,
                "admin", admin,
                "auth-scope", authScope != null ? String.join(",", authScope) : "",
                "client-name", Optional.ofNullable(clientName).orElse(""),
                "remote-address", Optional.ofNullable(remoteAddress).orElse(""),
                "user-agent", Optional.ofNullable(userAgent).orElse("unknown"),
                "user-agent-os", Optional.ofNullable(userAgentOs).orElse("unknown"),
                "node-id", nodeId,
                "node-name", Optional.ofNullable(nodeName).orElse(""),
                "domain-name", Optional.ofNullable(domainName).orElse("")
        );
        String headerValue = vars.entrySet().stream()
                .map(v -> v.getKey() + "=" + v.getValue())
                .collect(Collectors.joining(";"));
        requestBuilder.header("X-Moera-Auth", headerValue);
        requestBuilder.header("X-Moera-Origin", originUrl);
    }

}
