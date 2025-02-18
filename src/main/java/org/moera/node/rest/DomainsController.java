package org.moera.node.rest;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.github.slugify.Slugify;
import org.moera.lib.node.types.DomainAttributes;
import org.moera.lib.node.types.DomainAvailable;
import org.moera.lib.node.types.DomainInfo;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.RootAdmin;
import org.moera.node.config.Config;
import org.moera.node.data.Domain;
import org.moera.node.domain.Domains;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.ProviderApi;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.LiberinManager;
import org.moera.node.liberin.model.DomainAddedLiberin;
import org.moera.node.liberin.model.DomainDeletedLiberin;
import org.moera.node.liberin.model.DomainUpdatedLiberin;
import org.moera.node.model.DomainAvailableUtil;
import org.moera.node.model.DomainInfoUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/domains")
@NoCache
public class DomainsController {

    private static final Logger log = LoggerFactory.getLogger(DomainsController.class);

    @Inject
    private Config config;

    @Inject
    private RequestContext requestContext;

    @Inject
    private Domains domains;

    @Inject
    private LiberinManager liberinManager;

    @ProviderApi
    @RootAdmin
    @GetMapping
    public List<DomainInfo> get() {
        log.info("GET /domains");

        return domains.getAllDomainNames().stream()
                .map(domains::getDomain)
                .sorted(Comparator.comparing(DomainInfo::getName))
                .collect(Collectors.toList());
    }

    @ProviderApi
    @GetMapping("/{name}")
    public DomainInfo get(@PathVariable String name) {
        log.info("GET /domains/{}", name);

        if (!config.isRegistrationPublic() && !requestContext.isRootAdmin()) {
            throw new AuthenticationException();
        }
        name = name.toLowerCase();
        DomainInfo info = domains.getDomain(name);
        if (info == null) {
            throw new ObjectNotFoundFailure("domain.not-found");
        }
        return info;
    }

    @ProviderApi
    @PostMapping
    @Transactional
    public ResponseEntity<DomainInfo> post(@RequestBody DomainAttributes domainAttributes) {
        log.info(
            "POST /domains (name = {}, nodeId = {})",
            LogUtil.format(domainAttributes.getName()), LogUtil.format(domainAttributes.getNodeId())
        );

        domainAttributes.validate();
        ValidationUtil.notBlank(domainAttributes.getName(), "domain.name.blank");

        if (!config.isRegistrationPublic() && !requestContext.isRootAdmin()) {
            throw new AuthenticationException();
        }
        String name = domainAttributes.getName().toLowerCase();
        UUID nodeId = domainAttributes.getNodeId() == null ? UUID.randomUUID() : domainAttributes.getNodeId();

        domains.lockWrite();
        Domain domain;
        try {
            if (domains.getDomainNodeId(name) != null) {
                throw new OperationFailure("domain.already-exists");
            }
            if (domains.getDomainName(nodeId) != null) {
                throw new OperationFailure("domain.node-id-used");
            }
            domain = domains.createDomain(name, nodeId);
        } finally {
            domains.unlockWrite();
        }

        liberinManager.send(new DomainAddedLiberin(name).withNodeId(nodeId));

        return ResponseEntity.created(URI.create("/domains/" + domain.getName())).body(DomainInfoUtil.build(domain));
    }

    @ProviderApi
    @RootAdmin
    @PutMapping("/{name}")
    @Transactional
    public DomainInfo put(@PathVariable String name, @RequestBody DomainAttributes domainAttributes) {
        log.info("PUT /domains/{}", name);

        domainAttributes.validate();

        name = name.toLowerCase();
        String newName = !ObjectUtils.isEmpty(domainAttributes.getName())
            ? domainAttributes.getName().toLowerCase()
            : name;
        UUID nodeId = domainAttributes.getNodeId() == null ? UUID.randomUUID() : domainAttributes.getNodeId();

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
            if (!domains.getDomainName(nodeId).equals(name)) {
                throw new OperationFailure("domain.node-id-used");
            }
            domains.deleteDomain(name);
            domain = domains.createDomain(newName, nodeId);
        } finally {
            domains.unlockWrite();
        }

        liberinManager.send(new DomainUpdatedLiberin(name, newName).withNodeId(domain.getNodeId()));

        return DomainInfoUtil.build(domain);
    }

    @ProviderApi
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

        liberinManager.send(new DomainDeletedLiberin(name));

        return Result.OK;
    }

    @ProviderApi
    @GetMapping("/available")
    public DomainAvailable findAvailable(@RequestParam String nodeName) {
        log.info("GET /domains/available (nodeName = {})", nodeName);

        if (!config.isRegistrationPublic()) {
            throw new OperationFailure("domains.registration-not-available");
        }

        Slugify slugify = Slugify.builder().transliterator(true).build();
        String domainName = slugify.slugify(nodeName);
        if (ObjectUtils.isEmpty(domainName)) {
            domainName = "x";
        }
        String fqdn = domainName + "." + config.getRegistrar().getDomain();
        if (domainName.equals("x") || domains.isDomainDefined(fqdn)) {
            int i = -1;
            do {
                i++;
                fqdn = domainName + i + "." + config.getRegistrar().getDomain();
            } while (domains.isDomainDefined(fqdn));
        }
        return DomainAvailableUtil.build(fqdn);
    }

}
