package org.moera.node.rest.task.upgrade;

import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.SettingDescriptor;
import org.moera.node.data.DomainUpgrade;
import org.moera.node.data.DomainUpgradeRepository;
import org.moera.node.data.UpgradeType;
import org.moera.node.domain.Domains;
import org.moera.node.option.Options;
import org.moera.node.option.OptionsMetadata;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptAllOptionsJob extends Job<EncryptAllOptionsJob.Parameters, Object> {

    private static final Logger log = LoggerFactory.getLogger(EncryptAllOptionsJob.class);

    @Inject
    private DomainUpgradeRepository domainUpgradeRepository;

    @Inject
    private Domains domains;

    @Inject
    private OptionsMetadata optionsMetadata;

    public static class Parameters {

        public Parameters() {
        }

    }

    public EncryptAllOptionsJob() {
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) throws JsonProcessingException {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) throws JsonProcessingException {
        this.state = null;
    }

    @Override
    protected void started() {
        super.started();
        log.info("Encrypting all secret options");
    }

    @Override
    protected void execute() {
        Set<DomainUpgrade> upgrades = domainUpgradeRepository.findPending(UpgradeType.ENCRYPT_OPTIONS);
        for (DomainUpgrade upgrade : upgrades) {
            UUID nodeId = upgrade.getNodeId();
            Options options = domains.getDomainOptions(nodeId);
            optionsMetadata.getDescriptorsForNode(nodeId).stream()
                .filter(d -> Boolean.TRUE.equals(d.getEncrypted()))
                .map(SettingDescriptor::getName)
                .forEach(options::resave);
            tx.executeWriteQuietly(
                () -> domainUpgradeRepository.deleteByTypeAndNode(UpgradeType.ENCRYPT_OPTIONS, nodeId),
                e -> log.error("Error deleting domain upgrade record: {}", e.getMessage())
            );
        }
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        log.info("Encryption of all secret options finished successfully");
    }

}
