package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.Contact;
import org.moera.node.data.ContactRepository;
import org.moera.node.data.FriendOfRepository;
import org.moera.node.data.FriendRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.util.ParametrizedLock;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class ContactOperations {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private FriendRepository friendRepository;

    @Inject
    private FriendOfRepository friendOfRepository;

    @Inject
    private PlatformTransactionManager txManager;

    private final ParametrizedLock<Pair<UUID, String>> lock = new ParametrizedLock<>();

    public Contact updateCloseness(String remoteNodeName, float delta) {
        return updateCloseness(universalContext.nodeId(), remoteNodeName, delta);
    }

    public Contact updateCloseness(UUID nodeId, String remoteNodeName, float delta) {
        if (remoteNodeName == null) {
            return null;
        }

        lock.lock(Pair.of(nodeId, remoteNodeName));
        try {
            return Transaction.execute(txManager, () -> {
                Contact contact = contactRepository.findByRemoteNode(nodeId, remoteNodeName).orElse(null);
                if (contact == null) {
                    contact = new Contact();
                    contact.setId(UUID.randomUUID());
                    contact.setNodeId(nodeId);
                    contact.setRemoteNodeName(remoteNodeName);
                    contact.setCloseness(delta);
                    return contactRepository.save(contact);
                }
                contact.updateCloseness(delta);
                return contact;
            });
        } catch (Throwable e) {
            throw new ContactUpdateException(e);
        } finally {
            lock.unlock(Pair.of(nodeId, remoteNodeName));
        }
    }

    public void updateFeedSubscriptionCount(String remoteNodeName, int delta) {
        updateFeedSubscriptionCount(universalContext.nodeId(), remoteNodeName, delta);
    }

    public void updateFeedSubscriptionCount(UUID nodeId, String remoteNodeName, int delta) {
        if (remoteNodeName == null) {
            return;
        }

        lock.lock(Pair.of(nodeId, remoteNodeName));
        try {
            Transaction.execute(txManager, () -> {
                contactRepository.findByRemoteNode(nodeId, remoteNodeName).ifPresent(
                        contact -> contact.setFeedSubscriptionCount(contact.getFeedSubscriptionCount() + delta));
                return null;
            });
        } catch (Throwable e) {
            throw new ContactUpdateException(e);
        } finally {
            lock.unlock(Pair.of(nodeId, remoteNodeName));
        }
    }

    public void updateFeedSubscriberCount(String remoteNodeName, int delta) {
        updateFeedSubscriberCount(universalContext.nodeId(), remoteNodeName, delta);
    }

    public void updateFeedSubscriberCount(UUID nodeId, String remoteNodeName, int delta) {
        if (remoteNodeName == null) {
            return;
        }

        lock.lock(Pair.of(nodeId, remoteNodeName));
        try {
            Transaction.execute(txManager, () -> {
                contactRepository.findByRemoteNode(nodeId, remoteNodeName).ifPresent(
                        contact -> contact.setFeedSubscriberCount(contact.getFeedSubscriberCount() + delta));
                return null;
            });
        } catch (Throwable e) {
            throw new ContactUpdateException(e);
        } finally {
            lock.unlock(Pair.of(nodeId, remoteNodeName));
        }
    }

    public void updateFriendCount(String remoteNodeName, int delta) {
        updateFriendCount(universalContext.nodeId(), remoteNodeName, delta);
    }

    public void updateFriendCount(UUID nodeId, String remoteNodeName, int delta) {
        if (remoteNodeName == null) {
            return;
        }

        lock.lock(Pair.of(nodeId, remoteNodeName));
        try {
            Transaction.execute(txManager, () -> {
                contactRepository.findByRemoteNode(nodeId, remoteNodeName).ifPresent(
                        contact -> contact.setFriendCount(contact.getFriendCount() + delta));
                return null;
            });
        } catch (Throwable e) {
            throw new ContactUpdateException(e);
        } finally {
            lock.unlock(Pair.of(nodeId, remoteNodeName));
        }
    }

    public void updateFriendOfCount(String remoteNodeName, int delta) {
        updateFriendOfCount(universalContext.nodeId(), remoteNodeName, delta);
    }

    public void updateFriendOfCount(UUID nodeId, String remoteNodeName, int delta) {
        if (remoteNodeName == null) {
            return;
        }

        lock.lock(Pair.of(nodeId, remoteNodeName));
        try {
            Transaction.execute(txManager, () -> {
                contactRepository.findByRemoteNode(nodeId, remoteNodeName).ifPresent(
                        contact -> contact.setFriendOfCount(contact.getFriendOfCount() + delta));
                return null;
            });
        } catch (Throwable e) {
            throw new ContactUpdateException(e);
        } finally {
            lock.unlock(Pair.of(nodeId, remoteNodeName));
        }
    }

    public void updateDetails(String remoteNodeName, String remoteFullName, String remoteGender) {
        subscriberRepository.updateRemoteFullNameAndGender(
                universalContext.nodeId(), remoteNodeName, remoteFullName, remoteGender);
        userSubscriptionRepository.updateRemoteFullNameAndGender(
                universalContext.nodeId(), remoteNodeName, remoteFullName, remoteGender);
        contactRepository.updateRemoteFullNameAndGender(
                universalContext.nodeId(), remoteNodeName, remoteFullName, remoteGender);
        friendRepository.updateRemoteFullNameAndGender(
                universalContext.nodeId(), remoteNodeName, remoteFullName, remoteGender);
        friendOfRepository.updateRemoteFullNameAndGender(
                universalContext.nodeId(), remoteNodeName, remoteFullName, remoteGender);
    }

    public void updateAvatar(String remoteNodeName, MediaFile remoteAvatarMediaFile, String remoteAvatarShape) {
        subscriberRepository.updateRemoteAvatar(
                universalContext.nodeId(), remoteNodeName, remoteAvatarMediaFile, remoteAvatarShape);
        userSubscriptionRepository.updateRemoteAvatar(
                universalContext.nodeId(), remoteNodeName, remoteAvatarMediaFile, remoteAvatarShape);
        contactRepository.updateRemoteAvatar(
                universalContext.nodeId(), remoteNodeName, remoteAvatarMediaFile, remoteAvatarShape);
        friendRepository.updateRemoteAvatar(
                universalContext.nodeId(), remoteNodeName, remoteAvatarMediaFile, remoteAvatarShape);
        friendOfRepository.updateRemoteAvatar(
                universalContext.nodeId(), remoteNodeName, remoteAvatarMediaFile, remoteAvatarShape);
    }

    @Scheduled(fixedDelayString = "P1D")
    @Transactional
    public void closenessMaintenance() {
        Collection<Contact> contacts = contactRepository.findAllUpdatedBefore(
                Timestamp.from(Instant.now().minus(30, ChronoUnit.DAYS)));
        for (Contact contact : contacts) {
            contact.setCloseness(contact.getCloseness() - 0.2f * contact.getClosenessBase());
            contact.setClosenessBase(contact.getCloseness());
            contact.setUpdatedAt(Util.now());
        }
    }

}
