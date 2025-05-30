package org.moera.node.rest.task.upgrade;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.WhoAmI;
import org.moera.node.api.node.MoeraNodeUnknownNameException;
import org.moera.node.data.Contact;
import org.moera.node.data.ContactRepository;
import org.moera.node.data.DomainUpgradeRepository;
import org.moera.node.data.UpgradeType;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllRemoteGendersDownloadTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(AllRemoteGendersDownloadTask.class);

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private DomainUpgradeRepository domainUpgradeRepository;

    public AllRemoteGendersDownloadTask() {
    }

    @Override
    protected void execute() {
        Set<String> targetNodeNames = getTargetNodeNames();
        for (String targetNodeName : targetNodeNames) {
            Duration delay = Duration.ofSeconds(30);
            for (int i = 0; i < 5; i++) {
                try {
                    download(targetNodeName);
                    success(targetNodeName);
                    break;
                } catch (Throwable e) {
                    error(targetNodeName, e);
                }
                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException e) {
                    // ignore
                }
                delay = delay.multipliedBy(2);
            }
        }
        tx.executeWriteQuietly(
            () -> domainUpgradeRepository.deleteByTypeAndNode(UpgradeType.GENDER_DOWNLOAD, nodeId),
            e -> log.error("Error deleting domain upgrade record: {}", e.getMessage())
        );
    }

    private Set<String> getTargetNodeNames() {
        return contactRepository.findAllByNodeId(nodeId).stream()
                .map(Contact::getRemoteNodeName)
                .collect(Collectors.toSet());
    }

    private void download(String targetNodeName) throws MoeraNodeException {
        WhoAmI target = nodeApi.at(targetNodeName).whoAmI();
        String targetFullName = target.getFullName();
        String targetGender = target.getGender();
        if (targetGender != null) {
            tx.executeWrite(() ->
                contactRepository.updateRemoteFullNameAndGender(nodeId, targetNodeName, targetFullName, targetGender));
        }
    }

    private void success(String targetNodeName) {
        log.info("Succeeded to download gender of node {}", targetNodeName);
    }

    private void error(String targetNodeName, Throwable e) {
        if (e instanceof MoeraNodeUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error downloading gender of node {}: {}", targetNodeName, e.getMessage());
        }
    }

}
