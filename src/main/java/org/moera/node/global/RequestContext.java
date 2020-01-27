package org.moera.node.global;

import java.util.List;
import java.util.UUID;

import org.moera.node.event.model.Event;
import org.moera.node.option.Options;

public interface RequestContext {

    boolean isBrowserExtension();

    void setBrowserExtension(boolean browserExtension);

    boolean isRootAdmin();

    void setRootAdmin(boolean rootAdmin);

    boolean isAdmin();

    void setAdmin(boolean admin);

    Options getOptions();

    void setOptions(Options options);

    String getSiteUrl();

    void setSiteUrl(String siteUrl);

    String getClientId();

    void setClientId(String clientId);

    String getClientName();

    void setClientName(String clientName);

    boolean isClient(String name);

    RequestContext getPublic();

    UUID nodeId();

    String nodeName();

    void send(Event event);

    List<Event> getAfterCommitEvents();

}
