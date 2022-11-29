package org.moera.node.operations;

import javax.inject.Inject;

import org.moera.node.data.ContactRepository;
import org.moera.node.data.FriendOfRepository;
import org.moera.node.data.FriendRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.UniversalContext;
import org.springframework.stereotype.Component;

@Component
public class RemoteProfileOperations {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private FriendRepository friendRepository;

    @Inject
    private FriendOfRepository friendOfRepository;

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

}
