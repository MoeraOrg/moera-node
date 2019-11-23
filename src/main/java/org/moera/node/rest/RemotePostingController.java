package org.moera.node.rest;

import javax.inject.Inject;

import org.moera.commons.util.LogUtil;
import org.moera.node.global.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.Result;
import org.moera.node.naming.NamingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/nodes/{nodeName}/postings")
public class RemotePostingController {

    private static Logger log = LoggerFactory.getLogger(RemotePostingController.class);

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private RequestContext requestContext;

    @Inject
    private NamingClient namingClient;

    @Inject
    private MessageSource messageSource;

    @PostMapping("/{id}/verify")
    @Admin
    public Result verify(@PathVariable String nodeName, @PathVariable String id) {
        log.info("POST /nodes/{name}/postings/{id}/verify, (name = {}, id = {})",
                LogUtil.format(nodeName), LogUtil.format(id));

        taskExecutor.execute(
                new RemotePostingVerifyTask(requestContext.nodeId(), nodeName, id, namingClient, messageSource));
        return Result.OK;
    }

}
