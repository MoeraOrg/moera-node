package org.moera.node.plugin;

import java.net.http.HttpRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.Scope;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UniversalContext;
import org.moera.node.option.Options;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PluginContext {

    private boolean rootAdmin;
    private List<String> adminScope;
    private List<String> clientScope;
    private String clientName;
    private boolean owner;
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
        adminScope = Scope.toValues(requestContext.getAdminScope());
        clientScope = Scope.toValues(requestContext.getClientScope());
        clientName = Optional.ofNullable(requestContext.getClientName(Scope.IDENTIFY)).orElse("");
        owner = requestContext.isOwner();
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

    public List<String> getAdminScope() {
        return adminScope;
    }

    public void setAdminScope(List<String> adminScope) {
        this.adminScope = adminScope;
    }

    public List<String> getClientScope() {
        return clientScope;
    }

    public void setClientScope(List<String> clientScope) {
        this.clientScope = clientScope;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
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
        String headerValue = "";
        headerValue += "root-admin=" + rootAdmin + ";";
        headerValue += "admin-scope=" + (adminScope != null ? String.join(",", adminScope) : "") + ";";
        headerValue += "auth-scope=" + (clientScope != null ? String.join(",", clientScope) : "") + ";";
        headerValue += "client-name=" + Optional.ofNullable(clientName).orElse("") + ";";
        headerValue += "owner=" + owner + ";";
        headerValue += "remote-address=" + Optional.ofNullable(remoteAddress).orElse("") + ";";
        headerValue += "user-agent=" + Optional.ofNullable(userAgent).orElse("unknown") + ";";
        headerValue += "user-agent-os=" + Optional.ofNullable(userAgentOs).orElse("unknown") + ";";
        headerValue += "node-id=" + nodeId + ";";
        headerValue += "node-name=" + Optional.ofNullable(nodeName).orElse("") + ";";
        headerValue += "domain-name=" + Optional.ofNullable(domainName).orElse("") + ";";
        requestBuilder.header("X-Moera-Auth", headerValue);
        requestBuilder.header("X-Moera-Origin", originUrl);
    }

}
