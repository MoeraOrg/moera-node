package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.auth.AuthenticationManager;
import org.moera.node.data.BlockedByUser;
import org.moera.node.data.BlockedUser;
import org.moera.node.data.Contact;
import org.moera.node.data.ContactRelated;
import org.moera.node.data.ContactRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.task.Jobs;
import org.moera.node.util.ParametrizedLock;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ContactOperations {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationManager.class);

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private Transaction tx;

    @Inject
    private Jobs jobs;

    private final ParametrizedLock<Pair<UUID, String>> lock = new ParametrizedLock<>();

    private Contact updateAtomically(UUID nodeId, String remoteNodeName, Consumer<Contact> updater) {
        if (remoteNodeName == null) {
            return null;
        }

        lock.lock(Pair.of(nodeId, remoteNodeName));
        try {
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
        } finally {
            lock.unlock(Pair.of(nodeId, remoteNodeName));
        }
    }

    public Contact find(String remoteNodeName) {
        return updateCloseness(remoteNodeName, 0);
    }

    public Contact find(UUID nodeId, String remoteNodeName) {
        return updateCloseness(nodeId, remoteNodeName, 0);
    }

    public Contact updateCloseness(String remoteNodeName, float delta) {
        return updateCloseness(universalContext.nodeId(), remoteNodeName, delta);
    }

    public Contact updateCloseness(UUID nodeId, String remoteNodeName, float delta) {
        return updateAtomically(nodeId, remoteNodeName, contact -> contact.updateCloseness(delta));
    }

    public void asyncUpdateCloseness(String remoteNodeName, float delta) {
        asyncUpdateCloseness(universalContext.nodeId(), remoteNodeName, delta);
    }

    public void asyncUpdateCloseness(UUID nodeId, String remoteNodeName, float delta) {
        jobs.runNoPersist(UpdateClosenessJob.class, new UpdateClosenessJob.Parameters(remoteNodeName, delta), nodeId);
    }

    public Contact assignCloseness(String remoteNodeName, float closeness) {
        return assignCloseness(universalContext.nodeId(), remoteNodeName, closeness);
    }

    public Contact assignCloseness(UUID nodeId, String remoteNodeName, float closeness) {
        return updateAtomically(nodeId, remoteNodeName, contact -> contact.setCloseness(closeness));
    }

    public Contact updateFeedSubscriptionCount(String remoteNodeName, int delta) {
        return updateAtomically(universalContext.nodeId(), remoteNodeName,
                contact -> contact.setFeedSubscriptionCount(Math.max(contact.getFeedSubscriptionCount() + delta, 0)));
    }

    public Contact updateFeedSubscriberCount(String remoteNodeName, int delta) {
        return updateAtomically(universalContext.nodeId(), remoteNodeName,
                contact -> contact.setFeedSubscriberCount(Math.max(contact.getFeedSubscriberCount() + delta, 0)));
    }

    public Contact updateFriendCount(String remoteNodeName, int delta) {
        return updateAtomically(universalContext.nodeId(), remoteNodeName,
                contact -> contact.setFriendCount(Math.max(contact.getFriendCount() + delta, 0)));
    }

    public Contact updateFriendOfCount(String remoteNodeName, int delta) {
        return updateAtomically(universalContext.nodeId(), remoteNodeName,
                contact -> contact.setFriendOfCount(Math.max(contact.getFriendOfCount() + delta, 0)));
    }

    public Contact updateBlockedUserCount(String remoteNodeName, int delta) {
        return updateAtomically(universalContext.nodeId(), remoteNodeName,
                contact -> contact.setBlockedUserCount(Math.max(contact.getBlockedUserCount() + delta, 0)));
    }

    public Contact updateBlockedUserPostingCount(String remoteNodeName, int delta) {
        return updateAtomically(universalContext.nodeId(), remoteNodeName,
                contact -> contact.setBlockedUserPostingCount(
                        Math.max(contact.getBlockedUserPostingCount() + delta, 0)));
    }

    public Contact updateBlockedUserCounts(BlockedUser blockedUser, int delta) {
        if (blockedUser.isGlobal()) {
            return updateBlockedUserCount(blockedUser.getRemoteNodeName(), delta);
        } else {
            return updateBlockedUserPostingCount(blockedUser.getRemoteNodeName(), delta);
        }
    }

    public Contact updateBlockedByUserCount(String remoteNodeName, int delta) {
        return updateAtomically(universalContext.nodeId(), remoteNodeName,
                contact -> contact.setBlockedByUserCount(Math.max(contact.getBlockedByUserCount() + delta, 0)));
    }

    public Contact updateBlockedByUserPostingCount(String remoteNodeName, int delta) {
        return updateAtomically(universalContext.nodeId(), remoteNodeName,
                contact -> contact.setBlockedByUserPostingCount(
                        Math.max(contact.getBlockedByUserPostingCount() + delta, 0)));
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
        return updateAtomically(universalContext.nodeId(), remoteNodeName, contact -> {
            if (!Objects.equals(contact.getRemoteFullName(), remoteFullName)
                    || !Objects.equals(contact.getRemoteGender(), remoteGender)) {
                contact.setRemoteFullName(remoteFullName);
                contact.setRemoteGender(remoteGender);
                if (changed != null) {
                    changed.run();
                }
            }
        });
    }

    public Contact updateAvatar(String remoteNodeName, MediaFile remoteAvatarMediaFile, String remoteAvatarShape) {
        return updateAvatar(remoteNodeName, remoteAvatarMediaFile, remoteAvatarShape, null);
    }

    public Contact updateAvatar(String remoteNodeName, MediaFile remoteAvatarMediaFile, String remoteAvatarShape,
                                Runnable changed) {
        return updateAtomically(universalContext.nodeId(), remoteNodeName, contact -> {
            String oldMediaFileId = contact.getRemoteAvatarMediaFile() != null
                    ? contact.getRemoteAvatarMediaFile().getId()
                    : null;
            String newMediaFileId = remoteAvatarMediaFile != null ? remoteAvatarMediaFile.getId() : null;
            if (!Objects.equals(oldMediaFileId, newMediaFileId)
                    || !Objects.equals(contact.getRemoteAvatarShape(), remoteAvatarShape)) {
                contact.setRemoteAvatarMediaFile(remoteAvatarMediaFile);
                contact.setRemoteAvatarShape(remoteAvatarShape);
                if (changed != null) {
                    changed.run();
                }
            }
        });
    }

    @Scheduled(fixedDelayString = "P1D")
    @Transactional
    public void closenessMaintenance() {
        try (var ignored = requestCounter.allot()) {
            log.info("Recalculating closeness of contacts");

            Collection<Contact> contacts = contactRepository.findAllUpdatedBefore(
                    Timestamp.from(Instant.now().minus(14, ChronoUnit.DAYS)));
            for (Contact contact : contacts) {
                contact.setCloseness(contact.getCloseness() - 0.2f * contact.getClosenessBase());
                contact.setClosenessBase(contact.getCloseness());
                contact.setUpdatedAt(Util.now());
            }
        }
    }

}
