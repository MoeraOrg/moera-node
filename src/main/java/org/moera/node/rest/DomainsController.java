package org.moera.node.rest;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.RootAdmin;
import org.moera.node.data.Domain;
import org.moera.node.domain.Domains;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.DomainInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/domains")
public class DomainsController {

    private static Logger log = LoggerFactory.getLogger(DomainsController.class);

    @Value("${registrar.domain:#{null}}")
    private String registrarDomain;

    @Inject
    private RequestContext requestContext;

    @Inject
    private Domains domains;

    @RootAdmin
    @GetMapping
    public List<DomainInfo> get() {
        log.info("GET /domains");

        return domains.getAllDomainNames().stream()
                .map(name -> new DomainInfo(name, domains.getDomainNodeId(name)))
                .sorted(Comparator.comparing(DomainInfo::getName))
                .collect(Collectors.toList());
    }

    @RootAdmin
    @GetMapping("/{name}")
    public DomainInfo get(@PathVariable String name) {
        log.info("GET /domains/{}", name);

        name = name.toLowerCase();
        UUID nodeId = domains.getDomainNodeId(name);
        if (nodeId == null) {
            throw new ObjectNotFoundFailure("domain.not-found");
        }
        return new DomainInfo(name, nodeId.toString());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<DomainInfo> post(@RequestBody @Valid DomainInfo domainInfo) throws AuthenticationException {
        log.info("POST /domains");

        if (StringUtils.isEmpty(registrarDomain) && !requestContext.isRootAdmin()) {
            throw new AuthenticationException();
        }
        if (StringUtils.isEmpty(domainInfo.getName())) {
            throw new ValidationFailure("domainInfo.name.blank");
        }
        String name = domainInfo.getName().toLowerCase();
        UUID nodeId = StringUtils.isEmpty(domainInfo.getNodeId())
                ? UUID.randomUUID() : UUID.fromString(domainInfo.getNodeId());

        domains.lockWrite();
        Domain domain;
        try {
            if (domains.getDomainNodeId(name) != null) {
                throw new OperationFailure("domain.already-exists");
            }
            domain = domains.createDomain(name, nodeId);
        } finally {
            domains.unlockWrite();
        }
        return ResponseEntity.created(URI.create("/domains/" + domain.getName()))
                .body(new DomainInfo(domain.getName(), domain.getNodeId()));
    }

    @RootAdmin
    @PutMapping("/{name}")
    @Transactional
    public DomainInfo put(@PathVariable String name, @RequestBody @Valid DomainInfo domainInfo) {
        log.info("PUT /domains/{}", name);

        name = name.toLowerCase();
        String newName = !StringUtils.isEmpty(domainInfo.getName()) ? domainInfo.getName().toLowerCase() : name;
        UUID nodeId = StringUtils.isEmpty(domainInfo.getNodeId())
                ? UUID.randomUUID() : UUID.fromString(domainInfo.getNodeId());

        domains.lockWrite();
        Domain domain;
        try {
            if (domains.getDomainNodeId(name) == null) {
                throw new ObjectNotFoundFailure("domain.not-found");
            }
            if (!name.equals(newName)) {
                if (name.equals(Domains.DEFAULT_DOMAIN)) {
                    throw new OperationFailure("domain.cannot-rename-default");
                }
                if (domains.getDomainNodeId(newName) != null) {
                    throw new OperationFailure("domain.already-exists");
                }
            }
            domains.deleteDomain(name);
            domain = domains.createDomain(newName, nodeId);
        } finally {
            domains.unlockWrite();
        }
        return new DomainInfo(domain.getName(), domain.getNodeId());
    }

    @RootAdmin
    @DeleteMapping("/{name}")
    @Transactional
    public Result delete(@PathVariable String name) {
        log.info("DELETE /domains/{}", name);

        name = name.toLowerCase();
        if (name.equals(Domains.DEFAULT_DOMAIN)) {
            throw new OperationFailure("domain.cannot-delete-default");
        }

        domains.lockWrite();
        try {
            if (domains.getDomainNodeId(name) == null) {
                throw new ObjectNotFoundFailure("domain.not-found");
            }
            domains.deleteDomain(name);
        } finally {
            domains.unlockWrite();
        }
        return Result.OK;
    }

}
