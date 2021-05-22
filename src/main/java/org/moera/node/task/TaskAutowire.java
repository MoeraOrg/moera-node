package org.moera.node.task;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.config.Config;
import org.moera.node.domain.Domains;
import org.moera.node.global.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class TaskAutowire {

    private static Logger log = LoggerFactory.getLogger(TaskAutowire.class);

    @Inject
    private Config config;

    @Inject
    private RequestContext requestContext;

    @Inject
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Inject
    private Domains domains;

    public void autowire(Task task) {
        autowireCapableBeanFactory.autowireBean(task);
        task.setNodeId(requestContext.nodeId());
        task.setLocalAddr(requestContext.getLocalAddr());
    }

    public void autowireWithoutRequest(Task task, UUID nodeId) {
        autowireCapableBeanFactory.autowireBean(task);
        task.setNodeId(nodeId);
        task.setLocalAddr(getLocalAddr(domains.getDomainName(nodeId)));
    }

    private InetAddress getLocalAddr(String domainName) {
        domainName = domainName != null && !domainName.equals(Domains.DEFAULT_DOMAIN) ? domainName : config.getAddress();
        if (domainName != null) {
            try {
                InetAddress[] ips = InetAddress.getAllByName(domainName);
                if (ips != null && ips.length > 0) {
                    return ips[0];
                }
            } catch (UnknownHostException e) {
            }
        }

        String local;
        try {
            local = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            local = "127.0.0.1";
        }
        try {
            return InetAddress.getByName(local);
        } catch (UnknownHostException e) {
            return null;
        }
    }

}
