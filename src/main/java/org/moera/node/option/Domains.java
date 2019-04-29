package org.moera.node.option;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Domain;
import org.moera.node.data.DomainRepository;
import org.moera.node.data.OptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class Domains {

    public static final String DEFAULT_DOMAIN = "_default_";

    private static Logger log = LoggerFactory.getLogger(Options.class);

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

    private void configureDomain(Domain domain) {
        Options options = new Options(domain.getNodeId(), optionsMetadata, optionRepository);
        domainOptions.put(domain.getName(), options);
    }

    public String getDomainEffectiveName(String name) {
        return domainOptions.containsKey(name) ? name : DEFAULT_DOMAIN;
    }

    public UUID getDomainNodeId(String name) {
        Options options = domainOptions.get(name);
        return options != null ? options.nodeId() : null;
    }

    public Options getDomainOptions(String name) {
        Options options = domainOptions.get(name);
        return options != null ? options : domainOptions.get(DEFAULT_DOMAIN);
    }

    public Set<String> getAllDomainNames() {
        return domainOptions.keySet();
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
        domainOptions.remove(name);
    }

}
