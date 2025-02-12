package org.moera.node.global;

import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.node.data.Avatar;
import org.moera.node.friends.FriendCacheInvalidation;
import org.moera.node.friends.FriendCachePart;
import org.moera.node.liberin.Liberin;
import org.moera.node.option.Options;
import org.moera.node.util.Nodes;

public interface RequestContext extends AccessChecker {

    class Times {

        public static final int RECEIVED = 0;
        public static final int STARTED = 1;
        public static final int FINISHED = 2;

    }

    boolean isRegistrar();

    void setRegistrar(boolean registrar);

    boolean isUndefinedDomain();

    boolean isRootAdmin();

    void setRootAdmin(boolean rootAdmin);

    boolean isAdmin(Scope scope);

    long getAdminScope();

    void setAdminScope(long adminScope);

    boolean isPossibleSheriff();

    void setPossibleSheriff(boolean possibleSheriff);

    boolean isSubscribedToClient(Scope scope);

    void setSubscribedToClient(boolean subscribedToClient);

    String[] getFriendGroups(Scope scope);

    void setFriendGroups(String[] friendGroups);

    boolean isMemberOf(UUID friendGroupId, Scope scope);

    long getClientScope();

    void setClientScope(long clientScope);

    boolean hasClientScope(Scope scope);

    boolean isOwner();

    void setOwner(boolean owner);

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

    String getRedirectorUrl();

    String getClientId();

    void setClientId(String clientId);

    String getClientName(Scope scope);

    void setClientName(String clientName);

    boolean isClient(String name, Scope scope);

    InetAddress getLocalAddr();

    void setLocalAddr(InetAddress localAddr);

    InetAddress getRemoteAddr();

    void setRemoteAddr(InetAddress remoteAddr);

    UserAgent getUserAgent();

    void setUserAgent(UserAgent userAgent);

    UserAgentOs getUserAgentOs();

    void setUserAgentOs(UserAgentOs userAgentOs);

    boolean isAutoClient();

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

    void invalidateFriendCache(FriendCachePart part, String clientName);

    List<FriendCacheInvalidation> getFriendCacheInvalidations();

    void invalidateSubscribedCache(String clientName);

    List<Nodes> getSubscribedCacheInvalidations();

    void blockedUsersUpdated();

    boolean isBlockedUsersUpdated();

    Instant getTimes(int item);

    void setTimes(int item, Instant time);

}
