package org.moera.node.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import jakarta.inject.Inject;

import org.moera.lib.node.types.DomainInfo;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Domain;
import org.moera.node.data.DomainRepository;
import org.moera.node.data.FriendGroup;
import org.moera.node.data.FriendGroupRepository;
import org.moera.node.data.OptionRepository;
import org.moera.node.model.DomainInfoUtil;
import org.moera.node.operations.InitialRecommendationOperations;
import org.moera.node.operations.ReminderOperations;
import org.moera.node.option.OptionHookManager;
import org.moera.node.option.Options;
import org.moera.node.option.OptionsMetadata;
import org.moera.node.option.OptionsMetadataConfiguredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class Domains {

    public static final String DEFAULT_DOMAIN = "_default_";

    private static final Logger log = LoggerFactory.getLogger(Domains.class);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<UUID, DomainInfo> domains = new HashMap<>();
    private final Map<String, Options> domainOptions = new HashMap<>();

    @Inject
    private ApplicationEventPublisher applicationEventPublisher;

    @Inject
    private OptionsMetadata optionsMetadata;

    @Inject
    private DomainRepository domainRepository;

    @Inject
    private OptionRepository optionRepository;

    @Inject
    private FriendGroupRepository friendGroupRepository;

    @Lazy
    @Inject
    private OptionHookManager optionHookManager;

    @Lazy
    @Inject
    private ReminderOperations reminderOperations;

    @Lazy
    @Inject
    private InitialRecommendationOperations initialRecommendationOperations;

    @EventListener(OptionsMetadataConfiguredEvent.class)
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
        lockWrite();
        try {
            domains.put(domain.getNodeId(), DomainInfoUtil.build(domain));
            Options options = new Options(domain.getNodeId(), optionsMetadata, optionRepository, optionHookManager);
            domainOptions.put(domain.getName(), options);
        } finally {
            unlockWrite();
        }
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

    public String getDomainEffectiveName(UUID nodeId) {
        String name = getDomainName(nodeId);
        return name != null ? name : DEFAULT_DOMAIN;
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

    public List<String> getAllDomainNames() {
        lockRead();
        try {
            return new ArrayList<>(domainOptions.keySet());
        } finally {
            unlockRead();
        }
    }

    public List<String> getWarmDomainNames() {
        lockRead();
        try {
            return domainOptions.entrySet().stream()
                    .filter(e -> !e.getValue().isFrozen())
                    .map(Map.Entry::getKey)
                    .toList();
        } finally {
            unlockRead();
        }
    }

    public DomainInfo getDomain(UUID nodeId) {
        lockRead();
        try {
            return domains.get(nodeId);
        } finally {
            unlockRead();
        }
    }

    public DomainInfo getDomain(String name) {
        UUID nodeId = getDomainNodeId(name);
        return nodeId != null ? getDomain(nodeId) : null;
    }

    public String getDomainName(UUID nodeId) {
        DomainInfo domainInfo = getDomain(nodeId);
        return domainInfo != null ? domainInfo.getName() : null;
    }

    // WARNING: information on the server may differ from the information in the naming system if the node
    // was moved to another server
    public String findDomainByNodeName(String nodeName) {
        if (ObjectUtils.isEmpty(nodeName)) {
            return null;
        }
        return getAllDomainNames().stream()
                .filter(name -> Objects.equals(getDomainOptions(name).nodeName(), nodeName))
                .findFirst()
                .orElse(null);
    }

    public Domain createDomain(String name, UUID nodeId) {
        Domain domain = new Domain(name, nodeId);
        domainRepository.saveAndFlush(domain);
        log.info("Created domain {} with id = {}", domain.getName(), domain.getNodeId());
        initializeDomain(nodeId);
        configureDomain(domain);
        populateDomain(nodeId);
        return domain;
    }

    private void initializeDomain(UUID nodeId) {
        int totalGroups = friendGroupRepository.countByNodeId(nodeId);
        if (totalGroups == 0) {
            FriendGroup friendGroup = new FriendGroup();
            friendGroup.setId(UUID.randomUUID());
            friendGroup.setNodeId(nodeId);
            friendGroup.setTitle(FriendGroup.FRIENDS);
            friendGroup.setViewPrincipal(Principal.PUBLIC);
            friendGroupRepository.save(friendGroup);
        }
        reminderOperations.initializeNode(nodeId);
    }

    private void populateDomain(UUID nodeId) {
        initialRecommendationOperations.populateNewsfeed(nodeId);
    }

    public void deleteDomain(String name) {
        Domain domain = domainRepository.findById(name).orElse(null);
        if (domain == null) {
            return;
        }
        domainRepository.delete(domain);
        domainRepository.flush();
        log.info("Deleted domain {}", domain.getName());
        lockWrite();
        try {
            domains.remove(domain.getNodeId());
            domainOptions.remove(name);
        } finally {
            unlockWrite();
        }
    }

}
