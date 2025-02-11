package org.moera.node.rest;

import jakarta.inject.Inject;
import jakarta.validation.Valid;

import org.moera.lib.node.types.AskSubject;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.RemoteNodeAskedLiberin;
import org.moera.node.model.AskDescription;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/nodes/{nodeName}/ask")
@NoCache
public class RemoteAskController {

    private static final Logger log = LoggerFactory.getLogger(RemoteAskController.class);

    @Inject
    private RequestContext requestContext;

    @PostMapping
    @Admin(Scope.OTHER)
    @Entitled
    public Result post(@PathVariable String nodeName, @Valid @RequestBody AskDescription askDescription) {
        log.info("POST /nodes/{nodeName}/ask (nodeName = {}, subject = {})",
                LogUtil.format(nodeName),
                LogUtil.format(askDescription.getSubject().getValue()));

        if (askDescription.getSubject() == AskSubject.FRIEND
                && ObjectUtils.isEmpty(askDescription.getFriendGroupId())) {
            throw new ValidationFailure("askDescription.friendGroupId.blank");
        }

        requestContext.send(new RemoteNodeAskedLiberin(nodeName, askDescription));

        return Result.OK;
    }

}
