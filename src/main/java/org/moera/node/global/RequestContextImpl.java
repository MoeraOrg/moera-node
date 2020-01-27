package org.moera.node.global;

import java.util.Objects;
import java.util.UUID;

import org.moera.node.option.Options;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@RequestScope(proxyMode = ScopedProxyMode.INTERFACES)
@Component
public class RequestContextImpl implements RequestContext {

    private boolean browserExtension;
    private boolean rootAdmin;
    private boolean admin;
    private Options options;
    private String siteUrl;
    private String clientId;
    private String clientName;

    @Override
    public boolean isBrowserExtension() {
        return browserExtension;
    }

    @Override
    public void setBrowserExtension(boolean browserExtension) {
        this.browserExtension = browserExtension;
    }

    @Override
    public boolean isRootAdmin() {
        return rootAdmin;
    }

    @Override
    public void setRootAdmin(boolean rootAdmin) {
        this.rootAdmin = rootAdmin;
    }

    @Override
    public boolean isAdmin() {
        return admin;
    }

    @Override
    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public void setOptions(Options options) {
        this.options = options;
    }

    @Override
    public String getSiteUrl() {
        return siteUrl;
    }

    @Override
    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public String getClientName() {
        return isAdmin() ? nodeName() : clientName;
    }

    @Override
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public boolean isClient(String name) {
        return Objects.equals(this.clientName, name);
    }

    @Override
    public RequestContext getPublic() {
        RequestContext context = new RequestContextImpl();
        context.setBrowserExtension(false);
        context.setAdmin(false);
        context.setOptions(options);
        context.setSiteUrl(siteUrl);
        return context;
    }

    @Override
    public UUID nodeId() {
        return options != null ? options.nodeId() : null;
    }

    @Override
    public String nodeName() {
        return options != null ? options.nodeName() : null;
    }

}
