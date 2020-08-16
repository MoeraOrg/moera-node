package org.moera.node.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.inject.Inject;

import org.moera.node.data.Domain;
import org.moera.node.data.DomainRepository;
import org.moera.node.data.OptionRepository;
import org.moera.node.option.Options;
import org.moera.node.option.OptionsMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class Domains {

    public static final String DEFAULT_DOMAIN = "_default_";

    private static Logger log = LoggerFactory.getLogger(Domains.class);

    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<UUID, String> domainNames = new HashMap<>();
    private Map<String, Options> domainOptions = new HashMap<>();

    @Inject
    private ApplicationEventPublisher applicationEventPublisher;

    @Inject
    private OptionsMetadata optionsMetadata;

    @Inject
    private DomainRepository domainRepository;

    @Inject
    private OptionRepository optionRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        if (domainRepository.count() == 0) {
            createDomain(DEFAULT_DOMAIN, UUID.randomUUID());
        } else {
            domainRepository.findAll().forEach(this::configureDomain);
        }
        applicationEventPublisher.publishEvent(new DomainsConfiguredEvent(this));

    }

    public AutoCloseable lockRead() {
        lock.readLock().lock();
        return this::unlockRead;
    }

    public void unlockRead() {
        lock.readLock().unlock();
    }

    public AutoCloseable lockWrite() {
        lock.writeLock().lock();
        return this::unlockWrite;
    }

    public void unlockWrite() {
        lock.writeLock().unlock();
    }

    private void configureDomain(Domain domain) {
        domainNames.put(domain.getNodeId(), domain.getName());
        Options options = new Options(domain.getNodeId(), optionsMetadata, optionRepository);
        domainOptions.put(domain.getName(), options);
    }

    public boolean isDomainDefined(String name) {
        lockRead();
        try {
            return domainOptions.containsKey(name);
        } finally {
            unlockRead();
        }
    }

    public String getDomainEffectiveName(String name) {
        return isDomainDefined(name) ? name : DEFAULT_DOMAIN;
    }

    public UUID getDomainNodeId(String name) {
        Options options;
        lockRead();
        try {
            options = domainOptions.get(name);
        } finally {
            unlockRead();
        }
        return options != null ? options.nodeId() : null;
    }

    public Options getDomainOptions(String name) {
        lockRead();
        try {
            Options options = domainOptions.get(name);
            return options != null ? options : domainOptions.get(DEFAULT_DOMAIN);
        } finally {
            unlockRead();
        }
    }

    public Options getDomainOptions(UUID nodeId) {
        return getDomainOptions(getDomainName(nodeId));
    }

    public Set<String> getAllDomainNames() {
        lockRead();
        try {
            return domainOptions.keySet();
        } finally {
            unlockRead();
        }
    }

    public String getDomainName(UUID nodeId) {
        lockRead();
        try {
            return domainNames.get(nodeId);
        } finally {
            unlockRead();
        }
    }

    public Domain createDomain(String name, UUID nodeId) {
        Domain domain = new Domain(name, nodeId);
        domainRepository.saveAndFlush(domain);
        log.info("Created domain {} with id = {}", domain.getName(), domain.getNodeId());
        configureDomain(domain);
        return domain;
    }

    public void deleteDomain(String name) {
        Domain domain = domainRepository.findById(name).orElse(null);
        if (domain == null) {
            return;
        }
        domainRepository.delete(domain);
        domainRepository.flush();
        log.info("Deleted domain {}", domain.getName());
        domainNames.remove(domain.getNodeId());
        domainOptions.remove(name);
    }

}
