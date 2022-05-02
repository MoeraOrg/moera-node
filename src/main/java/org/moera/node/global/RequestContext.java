package org.moera.node.global;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Avatar;
import org.moera.node.liberin.Liberin;
import org.moera.node.option.Options;

public interface RequestContext {

    boolean isRegistrar();

    void setRegistrar(boolean registrar);

    boolean isUndefinedDomain();

    boolean isBrowserExtension();

    void setBrowserExtension(boolean browserExtension);

    boolean isRootAdmin();

    void setRootAdmin(boolean rootAdmin);

    boolean isAdmin();

    void setAdmin(boolean admin);

    long getAuthCategory();

    void setAuthCategory(long authCategory);

    Options getOptions();

    void setOptions(Options options);

    String getUrl();

    void setUrl(String url);

    String getSiteUrl();

    void setSiteUrl(String siteUrl);

    String getClientId();

    void setClientId(String clientId);

    String getClientName();

    void setClientName(String clientName);

    boolean isClient(String name);

    InetAddress getLocalAddr();

    void setLocalAddr(InetAddress localAddr);

    UserAgent getUserAgent();

    void setUserAgent(UserAgent userAgent);

    UserAgentOs getUserAgentOs();

    void setUserAgentOs(UserAgentOs userAgentOs);

    RequestContext getPublic();

    UUID nodeId();

    String nodeName();

    String fullName();

    UUID avatarId();

    Avatar getAvatar();

    boolean isPrincipal(Principal principal);

    void send(Liberin liberin);

    List<Liberin> getAfterCommitLiberins();

}
