package org.moera.node.global;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import org.moera.node.data.Avatar;
import org.moera.node.data.AvatarRepository;
import org.moera.node.mail.Mail;
import org.moera.node.model.event.Event;
import org.moera.node.model.notification.Notification;
import org.moera.node.notification.send.DirectedNotification;
import org.moera.node.notification.send.Direction;
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
    private Options options;
    private Avatar avatar;
    private String url;
    private String siteUrl;
    private String clientId;
    private String clientName;
    private InetAddress localAddr;
    private UserAgent userAgent = UserAgent.UNKNOWN;
    private UserAgentOs userAgentOs = UserAgentOs.UNKNOWN;
    private List<Event> afterCommitEvents = new ArrayList<>();
    private List<DirectedNotification> afterCommitNotifications = new ArrayList<>();
    private List<Mail> afterCommitMails = new ArrayList<>();

    @Inject
    private AvatarRepository avatarRepository;

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
        return Objects.equals(this.clientName, name);
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
    public void send(Event event) {
        afterCommitEvents.add(event);
    }

    @Override
    public List<Event> getAfterCommitEvents() {
        return afterCommitEvents;
    }

    @Override
    public void send(Direction direction, Notification notification) {
        afterCommitNotifications.add(new DirectedNotification(direction.nodeId(nodeId()), notification));
    }

    @Override
    public List<DirectedNotification> getAfterCommitNotifications() {
        return afterCommitNotifications;
    }

    @Override
    public void send(Mail mail) {
        afterCommitMails.add(mail);
    }

    @Override
    public List<Mail> getAfterCommitMails() {
        return afterCommitMails;
    }

}
