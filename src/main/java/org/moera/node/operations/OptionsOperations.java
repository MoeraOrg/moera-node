package org.moera.node.operations;

import jakarta.inject.Inject;

import org.moera.node.domain.Domains;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.LiberinManager;
import org.moera.node.liberin.model.NodeSettingsMetadataChangedLiberin;
import org.springframework.stereotype.Component;

@Component
public class OptionsOperations {

    @Inject
    private Domains domains;

    @Inject
    private RequestContext requestContext;

    @Inject
    private LiberinManager liberinManager;

    public void reloadOptions() {
        domains.getAllDomainNames().stream()
                .map(domains::getDomainOptions)
                .forEach(options -> {
                    options.reload();
                    liberinManager.send(new NodeSettingsMetadataChangedLiberin()
                            .withNodeId(options.nodeId())
                            .withPluginContext(requestContext));
                });
    }

}
