package org.moera.node.global;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.auth.AuthCategory;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Avatar;
import org.moera.node.data.AvatarRepository;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.FriendCacheInvalidation;
import org.moera.node.friends.FriendCachePart;
import org.moera.node.friends.Nodes;
import org.moera.node.friends.SubscribedCache;
import org.moera.node.liberin.Liberin;
import org.moera.node.option.Options;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@RequestScope(proxyMode = ScopedProxyMode.INTERFACES)
@Component
public class RequestContextImpl implements RequestContext {

    private boolean registrar;
    private boolean browserExtension;
    private boolean rootAdmin;
    private boolean admin;
    private boolean subscribedToClient;
    private String[] friendGroups;
    private long authCategory;
    private UUID tokenId;
    private String domainName;
    private Options options;
    private Avatar avatar;
    private String url;
    private String siteUrl;
    private String clientId;
    private String clientName;
    private InetAddress localAddr;
    private InetAddress remoteAddr;
    private UserAgent userAgent = UserAgent.UNKNOWN;
    private UserAgentOs userAgentOs = UserAgentOs.UNKNOWN;
    private final List<Liberin> afterCommitLiberins = new ArrayList<>();
    private boolean subscriptionsUpdated;
    private final List<FriendCacheInvalidation> friendCacheInvalidations = new ArrayList<>();
    private final List<Nodes> subscribedCacheInvalidations = new ArrayList<>();

    @Inject
    private AvatarRepository avatarRepository;

    @Inject
    private FriendCache friendCache;

    @Inject
    private SubscribedCache subscribedCache;

    @Override
    public boolean isRegistrar() {
        return registrar;
    }

    @Override
    public void setRegistrar(boolean registrar) {
        this.registrar = registrar;
    }

    @Override
    public boolean isUndefinedDomain() {
        return options == null;
    }

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
    public boolean isSubscribedToClient() {
        return subscribedToClient;
    }

    @Override
    public void setSubscribedToClient(boolean subscribedToClient) {
        this.subscribedToClient = subscribedToClient;
    }

    @Override
    public String[] getFriendGroups() {
        return friendGroups;
    }

    @Override
    public void setFriendGroups(String[] friendGroups) {
        this.friendGroups = friendGroups;
    }

    @Override
    public boolean isMemberOf(UUID friendGroupId) {
        String targetId = friendGroupId.toString();
        for (String id : friendGroups) {
            if (id.equals(targetId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long getAuthCategory() {
        return authCategory;
    }

    @Override
    public void setAuthCategory(long authCategory) {
        this.authCategory = authCategory;
    }

    @Override
    public UUID getTokenId() {
        return tokenId;
    }

    @Override
    public void setTokenId(UUID tokenId) {
        this.tokenId = tokenId;
    }

    @Override
    public String getDomainName() {
        return domainName;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
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
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
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
        return Objects.equals(getClientName(), name);
    }

    @Override
    public InetAddress getLocalAddr() {
        return localAddr;
    }

    @Override
    public void setLocalAddr(InetAddress localAddr) {
        this.localAddr = localAddr;
    }

    @Override
    public InetAddress getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public void setRemoteAddr(InetAddress remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    @Override
    public UserAgent getUserAgent() {
        return userAgent;
    }

    @Override
    public void setUserAgent(UserAgent userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public UserAgentOs getUserAgentOs() {
        return userAgentOs;
    }

    @Override
    public void setUserAgentOs(UserAgentOs userAgentOs) {
        this.userAgentOs = userAgentOs;
    }

    @Override
    public RequestContext getPublic() {
        RequestContextImpl context = new RequestContextImpl();
        context.browserExtension = false;
        context.admin = false;
        context.options = options;
        context.siteUrl = siteUrl;
        context.avatarRepository = avatarRepository;
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

    @Override
    public String fullName() {
        return options != null ? options.getString("profile.full-name") : null;
    }

    @Override
    public String gender() {
        return options != null ? options.getString("profile.gender") : null;
    }

    @Override
    public UUID avatarId() {
        return options != null ? options.getUuid("profile.avatar.id") : null;
    }

    @Override
    public Avatar getAvatar() {
        if (nodeId() == null || avatarId() == null) {
            return null;
        }
        if (avatar == null || !avatar.getId().equals(avatarId())) {
            avatar = avatarRepository.findByNodeIdAndId(nodeId(), avatarId()).orElse(null);
        }
        return avatar;
    }

    @Override
    public boolean isPrincipal(PrincipalFilter principal) {
        return principal.includes(isAdmin(), getClientName(), isSubscribedToClient(), getFriendGroups());
    }

    @Override
    public void send(Liberin liberin) {
        afterCommitLiberins.add(liberin);
    }

    @Override
    public List<Liberin> getAfterCommitLiberins() {
        return afterCommitLiberins;
    }

    @Override
    public void authenticatedWithSignature(String nodeName) {
        setAdmin(Objects.equals(nodeName, nodeName()));
        setClientName(nodeName);
        setFriendGroups(friendCache.getClientGroupIds(nodeName));
        setSubscribedToClient(subscribedCache.isSubscribed(nodeName));

        setAuthCategory(AuthCategory.ALL);
    }

    @Override
    public void subscriptionsUpdated() {
        subscriptionsUpdated = true;
    }

    @Override
    public boolean isSubscriptionsUpdated() {
        return subscriptionsUpdated;
    }

    @Override
    public void invalidateFriendCache(FriendCachePart part, String clientName) {
        friendCacheInvalidations.add(new FriendCacheInvalidation(part, nodeId(), clientName));
    }

    @Override
    public List<FriendCacheInvalidation> getFriendCacheInvalidations() {
        return friendCacheInvalidations;
    }

    @Override
    public void invalidateSubscribedCache(String clientName) {
        subscribedCacheInvalidations.add(new Nodes(nodeId(), clientName));
    }

    @Override
    public List<Nodes> getSubscribedCacheInvalidations() {
        return subscribedCacheInvalidations;
    }

}
