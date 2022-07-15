package org.moera.node.rest;

import java.util.UUID;
import javax.inject.Inject;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.ProviderApi;
import org.moera.node.global.RequestContext;
import org.moera.node.model.PluginDescription;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.plugin.DuplicatePluginException;
import org.moera.node.plugin.PluginDescriptor;
import org.moera.node.plugin.Plugins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/plugins")
@NoCache
public class PluginController {

    private static final Logger log = LoggerFactory.getLogger(PluginController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private Plugins plugins;

    @ProviderApi
    @PostMapping
    public Result post(@RequestBody @Valid PluginDescription pluginDescription) {
        log.info("POST /plugins (name = {})", LogUtil.format(pluginDescription.getName()));

        UUID nodeId;
        if (requestContext.isRootAdmin()) {
            nodeId = null;
        } else if (requestContext.isAdmin()) {
            nodeId = requestContext.nodeId();
        } else {
            throw new AuthenticationException();
        }

        PluginDescriptor descriptor = new PluginDescriptor(nodeId);
        pluginDescription.toDescriptor(descriptor);
        try {
            plugins.add(descriptor);
        } catch (DuplicatePluginException e) {
            throw new ValidationFailure("plugin.already-exists");
        }

        return Result.OK;
    }

}
