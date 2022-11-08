package org.moera.node.global;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.data.Avatar;
import org.moera.node.liberin.Liberin;
import org.moera.node.option.Options;

public interface RequestContext extends AccessChecker {

    boolean isRegistrar();

    void setRegistrar(boolean registrar);

    boolean isUndefinedDomain();

    boolean isBrowserExtension();

    void setBrowserExtension(boolean browserExtension);

    boolean isRootAdmin();

    void setRootAdmin(boolean rootAdmin);

    boolean isAdmin();

    void setAdmin(boolean admin);

    String[] getFriendGroups();

    void setFriendGroups(String[] friendGroups);

    long getAuthCategory();

    void setAuthCategory(long authCategory);

    UUID getTokenId();

    void setTokenId(UUID tokenId);

    String getDomainName();

    void setDomainName(String domainName);

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

    InetAddress getRemoteAddr();

    void setRemoteAddr(InetAddress remoteAddr);

    UserAgent getUserAgent();

    void setUserAgent(UserAgent userAgent);

    UserAgentOs getUserAgentOs();

    void setUserAgentOs(UserAgentOs userAgentOs);

    RequestContext getPublic();

    UUID nodeId();

    String nodeName();

    String fullName();

    String gender();

    UUID avatarId();

    Avatar getAvatar();

    void send(Liberin liberin);

    List<Liberin> getAfterCommitLiberins();

    void authenticatedWithSignature(String nodeName);

    void subscriptionsUpdated();

    boolean isSubscriptionsUpdated();

}
