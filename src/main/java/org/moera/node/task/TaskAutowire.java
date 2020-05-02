package org.moera.node.task;

import javax.inject.Inject;

import org.moera.node.global.RequestContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class TaskAutowire {

    @Inject
    private RequestContext requestContext;

    @Inject
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    public void autowire(Task task) {
        autowireCapableBeanFactory.autowireBean(task);
        task.setNodeId(requestContext.nodeId());
        task.setNodeName(requestContext.nodeName());
    }

}
