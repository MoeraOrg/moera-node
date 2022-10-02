package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.CommentRepository;
import org.moera.node.data.Contact;
import org.moera.node.data.ContactRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.OwnCommentRepository;
import org.moera.node.data.OwnReactionRepository;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.util.Util;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ContactOperations {

    @Inject
    private RequestContext requestContext;

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private OwnCommentRepository ownCommentRepository;

    @Inject
    private OwnReactionRepository ownReactionRepository;

    @Inject
    private CommentRepository commentRepository;

    public void createOrUpdateCloseness(String remoteNodeName, String remoteFullName, String remoteGender,
                                        MediaFile remoteAvatar, String remoteAvatarShape, float delta) {
        createOrUpdateCloseness(requestContext.nodeId(), requestContext.nodeName(), remoteNodeName, remoteFullName,
                remoteGender, remoteAvatar, remoteAvatarShape, delta);
    }

    public void createOrUpdateCloseness(UUID nodeId, String nodeName, String remoteNodeName, String remoteFullName,
                                        String remoteGender, MediaFile remoteAvatar, String remoteAvatarShape,
                                        float delta) {
        if (remoteNodeName == null) {
            return;
        }
        Contact contact = contactRepository.findByRemoteNode(nodeId, remoteNodeName).orElse(null);
        if (contact != null) {
            contact.setRemoteFullName(remoteFullName);
            contact.setRemoteGender(remoteGender);
            if (remoteAvatar != null) {
                contact.setRemoteAvatarMediaFile(remoteAvatar);
                contact.setRemoteAvatarShape(remoteAvatarShape);
            }
            contact.updateCloseness(delta);
            return;
        }
        contact = new Contact();
        contact.setId(UUID.randomUUID());
        contact.setNodeId(nodeId);
        contact.setRemoteNodeName(remoteNodeName);
        contact.setRemoteFullName(remoteFullName);
        contact.setRemoteGender(remoteGender);
        if (remoteAvatar != null) {
            contact.setRemoteAvatarMediaFile(remoteAvatar);
            contact.setRemoteAvatarShape(remoteAvatarShape);
        }
        float closeness = 0;
        closeness += subscriptionRepository.countByRemoteNode(nodeId, remoteNodeName);
        closeness += ownCommentRepository.countByRemoteNode(nodeId, remoteNodeName);
        closeness += ownReactionRepository.countByRemoteNode(nodeId, remoteNodeName) * 0.25;
        closeness += commentRepository.countByOwner(nodeId, remoteNodeName);
        closeness += commentRepository.countByOwnerAndRepliedToName(nodeId, nodeName, remoteNodeName);
        contact.setCloseness(closeness);
        contactRepository.save(contact);
    }

    public void updateCloseness(String remoteNodeName, float delta) {
        updateCloseness(requestContext.nodeId(), remoteNodeName, delta);
    }

    public void updateCloseness(UUID nodeId, String remoteNodeName, float delta) {
        if (remoteNodeName == null) {
            return;
        }
        contactRepository.findByRemoteNode(nodeId, remoteNodeName)
                .ifPresent(contact -> contact.updateCloseness(delta));
    }

    public void delete(String remoteNodeName) {
        delete(requestContext.nodeId(), remoteNodeName);
    }

    public void delete(UUID nodeId, String remoteNodeName) {
        if (remoteNodeName == null) {
            return;
        }
        contactRepository.deleteByRemoteNode(nodeId, remoteNodeName);
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
