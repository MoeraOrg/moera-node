package org.moera.node.global;

import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.UniversalLocation;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.Avatar;
import org.moera.node.data.AvatarRepository;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.FriendCacheInvalidation;
import org.moera.node.friends.FriendCachePart;
import org.moera.node.friends.SubscribedCache;
import org.moera.node.liberin.Liberin;
import org.moera.node.option.Options;
import org.moera.node.util.Nodes;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@RequestScope(proxyMode = ScopedProxyMode.INTERFACES)
@Component
public class RequestContextImpl implements RequestContext {

    private boolean rootAdmin;
    private long adminScope;
    private boolean possibleSheriff;
    private boolean subscribedToClient;
    private String[] friendGroups;
    private long clientScope;
    private boolean owner;
    private UUID tokenId;
    private String domainName;
    private Options options;
    private Avatar avatar;
    private String url;
    private String siteUrl;
    private String redirectorUrl;
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
    private boolean blockedUsersUpdated;
    private final Instant[] times = new Instant[3];

    @Inject
    private AvatarRepository avatarRepository;

    @Inject
    private FriendCache friendCache;

    @Inject
    private SubscribedCache subscribedCache;

    @Override
    public boolean isUndefinedDomain() {
        return options == null;
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
    public boolean isAdmin(Scope scope) {
        return scope.included(adminScope);
    }

    @Override
    public long getAdminScope() {
        return adminScope;
    }

    @Override
    public void setAdminScope(long adminScope) {
        this.adminScope = adminScope;
    }

    @Override
    public boolean isPossibleSheriff() {
        return possibleSheriff;
    }

    @Override
    public void setPossibleSheriff(boolean possibleSheriff) {
        this.possibleSheriff = possibleSheriff;
    }

    @Override
    public boolean isSubscribedToClient(Scope scope) {
        return subscribedToClient && hasClientScope(scope);
    }

    @Override
    public void setSubscribedToClient(boolean subscribedToClient) {
        this.subscribedToClient = subscribedToClient;
    }

    @Override
    public String[] getFriendGroups(Scope scope) {
        return hasClientScope(scope) ? friendGroups : new String[0];
    }

    @Override
    public void setFriendGroups(String[] friendGroups) {
        this.friendGroups = friendGroups;
    }

    @Override
    public boolean isMemberOf(UUID friendGroupId, Scope scope) {
        String targetId = friendGroupId.toString();
        for (String id : getFriendGroups(scope)) {
            if (id.equals(targetId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long getClientScope() {
        return clientScope;
    }

    @Override
    public void setClientScope(long clientScope) {
        this.clientScope = clientScope;
    }

    @Override
    public boolean hasClientScope(Scope scope) {
        return scope.included(clientScope);
    }

    @Override
    public boolean isOwner() {
        return owner;
    }

    @Override
    public void setOwner(boolean owner) {
        this.owner = owner;
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
    public String getRedirectorUrl() {
        if (redirectorUrl == null) {
            redirectorUrl = UniversalLocation.redirectTo(nodeName(), url);
        }
        return redirectorUrl;
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
    public String getClientName(Scope scope) {
        return hasClientScope(scope) ? clientName : null;
    }

    @Override
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public boolean isClient(String name, Scope scope) {
        return Objects.equals(getClientName(scope), name);
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
    public boolean isAutoClient() {
        Boolean webUiEnabled = getOptions().getBool("webui.enabled");
        Boolean redirectToClient = getOptions().getBool("webui.redirect-to-client");
        if (!webUiEnabled) {
            if (redirectToClient) {
                return true;
            } else {
                throw new PageNotFoundException();
            }
        }
        if (!redirectToClient) {
            return false;
        }
        return switch (getUserAgent()) {
            case FIREFOX, CHROME, YANDEX, BRAVE -> true;
            default -> false;
        };
    }

    @Override
    public RequestContext getPublic() {
        RequestContextImpl context = new RequestContextImpl();
        context.adminScope = 0;
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
    public boolean isPrincipal(PrincipalFilter principal, Scope scope) {
        return principal.includes(
            isAdmin(scope), getClientName(scope), isSubscribedToClient(scope), getFriendGroups(scope)
        );
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
        boolean owner = Objects.equals(nodeName, nodeName());
        setAdminScope(owner ? Scope.ALL.getMask() : 0);
        setClientName(nodeName);
        setOwner(owner);
        setFriendGroups(friendCache.getClientGroupIds(nodeName));
        setSubscribedToClient(subscribedCache.isSubscribed(nodeName));
        setClientScope(Scope.ALL.getMask());
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

    @Override
    public void blockedUsersUpdated() {
        blockedUsersUpdated = true;
    }

    @Override
    public boolean isBlockedUsersUpdated() {
        return blockedUsersUpdated;
    }

    @Override
    public Instant getTimes(int item) {
        return times[item];
    }

    @Override
    public void setTimes(int item, Instant time) {
        this.times[item] = time;
    }

}
