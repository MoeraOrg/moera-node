package org.moera.node.rest.task;

import java.util.List;
import javax.inject.Inject;

import org.moera.node.api.node.NodeApiUnknownNameException;
import org.moera.node.data.ContactUpgrade;
import org.moera.node.data.ContactUpgradeRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.UpgradeType;
import org.moera.node.liberin.model.RemoteNodeAvatarChangedLiberin;
import org.moera.node.liberin.model.RemoteNodeFullNameChangedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.WhoAmI;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class ContactsUpgradeTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(ContactsUpgradeTask.class);

    private static final int PAGE_SIZE = 1024;

    @Inject
    private ContactUpgradeRepository contactUpgradeRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private MediaManager mediaManager;

    public ContactsUpgradeTask() {
    }

    @Override
    protected void execute() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "nodeId"));
        List<ContactUpgrade> upgrades;
        try {
            do {
                upgrades = inTransaction(() ->
                        contactUpgradeRepository.findPending(UpgradeType.PROFILE_DOWNLOAD, pageable));
                upgrades.forEach(this::download);
            } while (upgrades.size() > 0);
        } catch (Throwable e) {
            log.error("Cannot fetch contact upgrades", e);
        }
    }

    private void download(ContactUpgrade upgrade) {
        setNodeId(upgrade.getNodeId());
        universalContext.associate(this);

        try {
            WhoAmI whoAmI = nodeApi.whoAmI(upgrade.getRemoteNodeName());
            if (whoAmI.getFullName() != null || whoAmI.getGender() != null) {
                contactOperations.updateDetails(upgrade.getRemoteNodeName(), whoAmI.getFullName(), whoAmI.getGender());
                send(new RemoteNodeFullNameChangedLiberin(upgrade.getRemoteNodeName(), whoAmI.getFullName()));
            }
            AvatarImage targetAvatar = whoAmI.getAvatar();
            MediaFile mediaFile = mediaManager.downloadPublicMedia(upgrade.getRemoteNodeName(), targetAvatar);
            if (mediaFile != null) {
                contactOperations.updateAvatar(upgrade.getRemoteNodeName(), mediaFile, targetAvatar.getShape());
                send(new RemoteNodeAvatarChangedLiberin(
                        upgrade.getRemoteNodeName(), new AvatarImage(mediaFile, targetAvatar.getShape())));
            }
            success(upgrade);
        } catch (Throwable e) {
            error(upgrade, e);
        }
    }

    private void success(ContactUpgrade upgrade) {
        try {
            inTransaction(() -> {
                contactUpgradeRepository.delete(upgrade);
                return null;
            });
        } catch (Throwable e) {
            error(upgrade, e);
        }
        log.info("Succeeded to download the profile of node {}", upgrade.getRemoteNodeName());
    }

    private void error(ContactUpgrade upgrade, Throwable e) {
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", upgrade.getRemoteNodeName());
        } else {
            log.error("Error downloading the profile of node {}: {}", upgrade.getRemoteNodeName(), e.getMessage());
        }
    }

}
