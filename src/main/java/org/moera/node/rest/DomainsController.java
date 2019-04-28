package org.moera.node.rest;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;

import org.moera.node.data.Domain;
import org.moera.node.global.ApiController;
import org.moera.node.global.RootAdmin;
import org.moera.node.model.DomainInfo;
import org.moera.node.model.DomainName;
import org.moera.node.model.OperationFailure;
import org.moera.node.option.Domains;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@ApiController
@RequestMapping("/moera/api/domains")
public class DomainsController {

    private static Logger log = LoggerFactory.getLogger(DomainsController.class);

    @Inject
    private Domains domains;

    @RootAdmin
    @GetMapping
    @ResponseBody
    public List<DomainInfo> get() {
        log.info("GET /domains");

        return domains.getAllDomainNames().stream()
                .map(name -> new DomainInfo(name, domains.getDomainNodeId(name)))
                .sorted(Comparator.comparing(DomainInfo::getName))
                .collect(Collectors.toList());
    }

    @RootAdmin
    @GetMapping("/{name}")
    @ResponseBody
    public DomainInfo get(@PathVariable String name) {
        log.info("GET /domains/{}", name);

        name = name.toLowerCase();
        UUID nodeId = domains.getDomainNodeId(name);
        if (nodeId == null) {
            throw new OperationFailure("domain.not-found");
        }
        return new DomainInfo(name, nodeId.toString());
    }

    @RootAdmin
    @PostMapping
    @ResponseBody
    public DomainInfo post(@RequestBody @Valid DomainName domainName) {
        log.info("POST /domains");

        Domain domain = domains.createDomain(domainName.getName().toLowerCase());
        return new DomainInfo(domain.getName(), domain.getNodeId());
    }

}
