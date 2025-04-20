package org.moera.node.operations;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import jakarta.inject.Inject;

import org.moera.node.data.BlockedByUser;
import org.moera.node.data.BlockedUser;
import org.moera.node.data.Contact;
import org.moera.node.data.ContactRelated;
import org.moera.node.data.ContactRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.domain.Domains;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.util.ParametrizedLock;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ContactOperations {

    private static final Logger log = LoggerFactory.getLogger(ContactOperations.class);

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private FavorOperations favorOperations;

    @Inject
    private Domains domains;

    @Inject
    private Transaction tx;

    private final ParametrizedLock<Pair<UUID, String>> lock = new ParametrizedLock<>();

    private Contact updateAtomically(UUID nodeId, String remoteNodeName, Consumer<Contact> updater) {
        if (remoteNodeName == null) {
            return null;
        }

        try (var ignored = lock.lock(Pair.of(nodeId, remoteNodeName))) {
            return tx.executeWrite(() -> {
                Contact contact = contactRepository.findByRemoteNode(nodeId, remoteNodeName).orElse(null);
                if (contact == null) {
                    contact = new Contact();
                    contact.setId(UUID.randomUUID());
                    contact.setNodeId(nodeId);
                    contact.setRemoteNodeName(remoteNodeName);
                    contact = contactRepository.save(contact);
                }
                updater.accept(contact);
                return contact;
            });
        } catch (Throwable e) {
            throw new ContactUpdateException(e);
        }
    }

    public Contact find(String remoteNodeName) {
        return find(universalContext.nodeId(), remoteNodeName);
    }

    public Contact find(UUID nodeId, String remoteNodeName) {
        return updateAtomically(nodeId, remoteNodeName, contact -> {});
    }

    public Contact assignDistance(String remoteNodeName, float distance) {
        return updateAtomically(
            universalContext.nodeId(),
            remoteNodeName,
            contact -> contact.setDistance(distance)
        );
    }

    public Contact updateFeedSubscriptionCount(String remoteNodeName, int delta) {
        return updateAtomically(
            universalContext.nodeId(),
            remoteNodeName,
            contact -> contact.setFeedSubscriptionCount(Math.max(contact.getFeedSubscriptionCount() + delta, 0))
        );
    }

    public Contact updateFeedSubscriberCount(String remoteNodeName, int delta) {
        return updateAtomically(
            universalContext.nodeId(),
            remoteNodeName,
            contact -> contact.setFeedSubscriberCount(Math.max(contact.getFeedSubscriberCount() + delta, 0))
        );
    }

    public Contact updateFriendCount(String remoteNodeName, int delta) {
        return updateAtomically(
            universalContext.nodeId(),
            remoteNodeName,
            contact -> contact.setFriendCount(Math.max(contact.getFriendCount() + delta, 0))
        );
    }

    public Contact updateFriendOfCount(String remoteNodeName, int delta) {
        return updateAtomically(
            universalContext.nodeId(),
            remoteNodeName,
            contact -> contact.setFriendOfCount(Math.max(contact.getFriendOfCount() + delta, 0))
        );
    }

    public Contact updateBlockedUserCount(String remoteNodeName, int delta) {
        return updateAtomically(
            universalContext.nodeId(),
            remoteNodeName,
            contact -> contact.setBlockedUserCount(Math.max(contact.getBlockedUserCount() + delta, 0))
        );
    }

    public Contact updateBlockedUserPostingCount(String remoteNodeName, int delta) {
        return updateAtomically(
            universalContext.nodeId(),
            remoteNodeName,
            contact -> contact.setBlockedUserPostingCount(Math.max(contact.getBlockedUserPostingCount() + delta, 0))
        );
    }

    public Contact updateBlockedUserCounts(BlockedUser blockedUser, int delta) {
        if (blockedUser.isGlobal()) {
            return updateBlockedUserCount(blockedUser.getRemoteNodeName(), delta);
        } else {
            return updateBlockedUserPostingCount(blockedUser.getRemoteNodeName(), delta);
        }
    }

    public Contact updateBlockedByUserCount(String remoteNodeName, int delta) {
        return updateAtomically(
            universalContext.nodeId(),
            remoteNodeName,
            contact -> contact.setBlockedByUserCount(Math.max(contact.getBlockedByUserCount() + delta, 0))
        );
    }

    public Contact updateBlockedByUserPostingCount(String remoteNodeName, int delta) {
        return updateAtomically(
            universalContext.nodeId(),
            remoteNodeName,
            contact -> contact.setBlockedByUserPostingCount(Math.max(contact.getBlockedByUserPostingCount() + delta, 0))
        );
    }

    public Contact updateBlockedByUserCounts(BlockedByUser blockedByUser, int delta) {
        if (blockedByUser.isGlobal()) {
            return updateBlockedByUserCount(blockedByUser.getRemoteNodeName(), delta);
        } else {
            return updateBlockedByUserPostingCount(blockedByUser.getRemoteNodeName(), delta);
        }
    }

    public Contact updateViewPrincipal(ContactRelated related) {
        return updateAtomically(universalContext.nodeId(), related.getRemoteNodeName(), related::toContactViewPrincipal);
    }

    public Contact updateDetails(String remoteNodeName, String remoteFullName, String remoteGender) {
        return updateDetails(remoteNodeName, remoteFullName, remoteGender, null);
    }

    public Contact updateDetails(String remoteNodeName, String remoteFullName, String remoteGender, Runnable changed) {
        return updateAtomically(
            universalContext.nodeId(),
            remoteNodeName,
            contact -> {
                if (
                    !Objects.equals(contact.getRemoteFullName(), remoteFullName)
                    || !Objects.equals(contact.getRemoteGender(), remoteGender)
                ) {
                    contact.setRemoteFullName(remoteFullName);
                    contact.setRemoteGender(remoteGender);
                    if (changed != null) {
                        changed.run();
                    }
                }
            }
        );
    }

    public Contact updateAvatar(String remoteNodeName, MediaFile remoteAvatarMediaFile, String remoteAvatarShape) {
        return updateAvatar(remoteNodeName, remoteAvatarMediaFile, remoteAvatarShape, null);
    }

    public Contact updateAvatar(
        String remoteNodeName, MediaFile remoteAvatarMediaFile, String remoteAvatarShape, Runnable changed
    ) {
        return updateAtomically(universalContext.nodeId(), remoteNodeName, contact -> {
            String oldMediaFileId = contact.getRemoteAvatarMediaFile() != null
                ? contact.getRemoteAvatarMediaFile().getId()
                : null;
            String newMediaFileId = remoteAvatarMediaFile != null ? remoteAvatarMediaFile.getId() : null;
            if (
                !Objects.equals(oldMediaFileId, newMediaFileId)
                || !Objects.equals(contact.getRemoteAvatarShape(), remoteAvatarShape)
            ) {
                contact.setRemoteAvatarMediaFile(remoteAvatarMediaFile);
                contact.setRemoteAvatarShape(remoteAvatarShape);
                if (changed != null) {
                    changed.run();
                }
            }
        });
    }

    @Scheduled(fixedDelayString = "PT12H")
    public void distanceMaintenance() {
        try (var ignored = requestCounter.allot()) {
            log.info("Recalculating distance of contacts");

            tx.executeWrite(() -> favorOperations.deleteExpired());
            for (String domainName : domains.getWarmDomainNames()) {
                UUID nodeId = domains.getDomainNodeId(domainName);
                universalContext.associate(nodeId);
                tx.executeWrite(() ->
                    contactRepository
                        .findAllByNodeId(nodeId)
                        .forEach(contact -> favorOperations.updateDistance(contact))
                );
            }
        }
    }

}
