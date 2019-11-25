package org.moera.node.rest;

import javax.inject.Inject;

import org.moera.commons.util.LogUtil;
import org.moera.node.global.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
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
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @PostMapping("/{id}/verify")
    @Admin
    public Result executeVerifyTask(@PathVariable String nodeName, @PathVariable String id) {
        log.info("POST /nodes/{name}/postings/{id}/verify, (name = {}, id = {})",
                LogUtil.format(nodeName), LogUtil.format(id));

        return executeVerifyTask(nodeName, id, null);
    }

    @PostMapping("/{id}/revisions/{revisionId}/verify")
    @Admin
    public Result verifyRevision(@PathVariable String nodeName, @PathVariable String id,
                                 @PathVariable String revisionId) {
        log.info("POST /nodes/{name}/postings/{id}/revisions/{revisionId}/verify, (name = {}, id = {}, revisionId = {})",
                LogUtil.format(nodeName), LogUtil.format(id), LogUtil.format(revisionId));

        return executeVerifyTask(nodeName, id, revisionId);
    }

    private Result executeVerifyTask(String nodeName, String id, String revisionId) {
        RemotePostingVerifyTask task = new RemotePostingVerifyTask(requestContext.nodeId(), nodeName, id, revisionId);
        autowireCapableBeanFactory.autowireBean(task);
        taskExecutor.execute(task);

        return Result.OK;
    }

}
