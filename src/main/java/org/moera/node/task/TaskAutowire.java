package org.moera.node.task;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.util.LogUtil;
import org.moera.node.config.Config;
import org.moera.node.domain.Domains;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UniversalContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class TaskAutowire {

    private static final Logger log = LoggerFactory.getLogger(TaskAutowire.class);

    @Inject
    private Config config;

    @Inject
    private RequestContext requestContext;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Inject
    private Domains domains;

    public void autowire(Task task) {
        autowireCapableBeanFactory.autowireBean(task);
        task.setNodeId(universalContext.nodeId());
        task.setLocalAddrs(getLocalAddrs(domains.getDomainDnsName(universalContext.nodeId())));
    }

    public void autowireWithoutRequest(Task task, UUID nodeId) {
        autowireCapableBeanFactory.autowireBean(task);
        task.setNodeId(nodeId);
        task.setLocalAddrs(getLocalAddrs(domains.getDomainDnsName(nodeId)));
    }

    public void autowireWithoutRequestAndDomain(Task task) {
        autowireCapableBeanFactory.autowireBean(task);
    }

    private List<InetAddress> getConfiguredAddresses() {
        if (config.getAddresses() == null) {
            return null;
        }

        List<InetAddress> addresses = new ArrayList<>();
        for (String address : config.getAddresses()) {
            try {
                addresses.addAll(Arrays.asList(InetAddress.getAllByName(address)));
            } catch (UnknownHostException e) {
                log.error("Configured IP address {} is invalid", LogUtil.format(address));
            }
        }
        return addresses;
    }

    private List<InetAddress> getLocalAddrs(String domainName) {
        List<InetAddress> configuredAddresses = getConfiguredAddresses();
        if (!ObjectUtils.isEmpty(configuredAddresses)) {
            return configuredAddresses;
        }

        if (domainName != null && !domainName.equals(Domains.DEFAULT_DOMAIN)) {
            try {
                InetAddress[] ips = InetAddress.getAllByName(domainName);
                if (ips != null && ips.length > 0) {
                    return List.of(ips);
                }
            } catch (UnknownHostException e) {
                log.error("Could not resolve our domain {}", LogUtil.format(domainName));
            }
        }

        String local;
        try {
            local = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            local = "127.0.0.1";
        }
        try {
            return List.of(InetAddress.getAllByName(local));
        } catch (UnknownHostException e) {
            return null;
        }
    }

}
