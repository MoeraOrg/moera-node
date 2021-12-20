package org.moera.node.rest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import com.querydsl.core.BooleanBuilder;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.QSubscriber;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.instant.SubscriberInstants;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.Result;
import org.moera.node.model.SubscriberDescription;
import org.moera.node.model.SubscriberInfo;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.event.PeopleChangedEvent;
import org.moera.node.model.event.SubscriberAddedEvent;
import org.moera.node.model.event.SubscriberDeletedEvent;
import org.moera.node.model.notification.PostingUpdatedNotification;
import org.moera.node.notification.send.Directions;
import org.moera.node.rest.task.RemoteAvatarDownloadTask;
import org.moera.node.rest.task.RemoteProfileSubscriptionTask;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/people/subscribers")
@NoCache
public class SubscriberController {

    private static final Logger log = LoggerFactory.getLogger(SubscriberController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private SubscriberInstants subscriberInstants;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @GetMapping
    @Transactional
    public List<SubscriberInfo> getAll(@RequestParam(required = false) String nodeName,
                                       @RequestParam(required = false) SubscriptionType type) {
        log.info("GET /people/subscribers (nodeName = {}, type = {})",
                LogUtil.format(nodeName), LogUtil.format(SubscriptionType.toValue(type)));

        QSubscriber subscriber = QSubscriber.subscriber;
        BooleanBuilder where = new BooleanBuilder();
        where.and(subscriber.nodeId.eq(requestContext.nodeId()));
        if (!ObjectUtils.isEmpty(nodeName)) {
            where.and(subscriber.remoteNodeName.eq(nodeName));
        }
        if (type != null) {
            where.and(subscriber.subscriptionType.eq(type));
        }

        return StreamSupport.stream(subscriberRepository.findAll(where).spliterator(), false)
                .map(SubscriberInfo::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Transactional
    public SubscriberInfo get(@PathVariable UUID id) {
        log.info("GET /people/subscribers/{id} (id = {})", LogUtil.format(id));

        Subscriber subscriber = subscriberRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("subscriber.not-found"));
        String ownerName = requestContext.getClientName();
        if (!requestContext.isAdmin()
                && (ObjectUtils.isEmpty(ownerName) || !ownerName.equals(subscriber.getRemoteNodeName()))) {
            throw new AuthenticationException();
        }

        return new SubscriberInfo(subscriber);
    }

    @PostMapping
    @Entitled
    @Transactional
    public SubscriberInfo post(@Valid @RequestBody SubscriberDescription subscriberDescription) {
        log.info("POST /people/subscribers (type = {}, feedName = {}, postingId = {})",
                LogUtil.format(SubscriptionType.toValue(subscriberDescription.getType())),
                LogUtil.format(subscriberDescription.getFeedName()),
                LogUtil.format(subscriberDescription.getPostingId()));

        if (subscriberDescription.getType() == null) {
            throw new ValidationFailure("subscriberDescription.type.blank");
        }
        String ownerName = requestContext.getClientName();
        if (ObjectUtils.isEmpty(ownerName)) {
            throw new AuthenticationException();
        }
        if (similarExists(subscriberDescription)) {
            throw new OperationFailure("subscriber.already-exists");
        }
        validate(subscriberDescription);

        Subscriber subscriber = new Subscriber();
        subscriber.setId(UUID.randomUUID());
        subscriber.setNodeId(requestContext.nodeId());
        subscriber.setRemoteNodeName(ownerName);
        subscriberDescription.toSubscriber(subscriber);
        if (subscriberDescription.getPostingId() != null) {
            Posting posting = postingRepository.findByNodeIdAndId(
                    requestContext.nodeId(), subscriberDescription.getPostingId())
                    .orElseThrow(() -> new ValidationFailure("subscriberDescription.postingId.not-found"));
            subscriber.setEntry(posting);

            if (subscriber.getSubscriptionType() == SubscriptionType.POSTING) {
                if (!Util.toEpochSecond(posting.getEditedAt()).equals(subscriberDescription.getLastUpdatedAt())) {
                    PostingUpdatedNotification notification = new PostingUpdatedNotification(posting.getId());
                    notification.setSubscriberId(subscriber.getId().toString());
                    notification.setSubscriptionCreatedAt(Util.now());
                    requestContext.send(Directions.single(ownerName), notification);
                }
            }
        }
        subscriber = subscriberRepository.save(subscriber);

        if (subscriber.getSubscriptionType() == SubscriptionType.FEED) {
            var profileTask = new RemoteProfileSubscriptionTask(ownerName);
            taskAutowire.autowire(profileTask);
            taskExecutor.execute(profileTask);
        }

        if (subscriber.getRemoteAvatarMediaFile() == null) {
            var avatarTask = new RemoteAvatarDownloadTask(subscriber.getRemoteNodeName());
            taskAutowire.autowire(avatarTask);
            taskExecutor.execute(avatarTask);
        }

        subscriberInstants.added(subscriber);
        requestContext.send(new SubscriberAddedEvent(subscriber));
        sendPeopleChangedEvent();

        return new SubscriberInfo(subscriber);
    }

    private boolean similarExists(SubscriberDescription description) {
        switch (description.getType()) {
            case FEED:
                return subscriberRepository.countByFeedName(requestContext.nodeId(), requestContext.getClientName(),
                        description.getType(), description.getFeedName()) > 0;
            case POSTING:
            case POSTING_COMMENTS:
                return subscriberRepository.countByEntryId(requestContext.nodeId(), requestContext.getClientName(),
                        description.getType(), description.getPostingId()) > 0;
            case PROFILE:
                return subscriberRepository.countByType(requestContext.nodeId(), requestContext.getClientName(),
                        description.getType()) > 0;
        }
        return false; // Should never be reached
    }

    private void validate(SubscriberDescription description) {
        mediaOperations.validateAvatar(
                description.getOwnerAvatar(),
                description::setOwnerAvatarMediaFile,
                () -> new ValidationFailure("subscriberDescription.ownerAvatar.mediaId.not-found"));

        switch (description.getType()) {
            case FEED:
                if (ObjectUtils.isEmpty(description.getFeedName())) {
                    throw new ValidationFailure("subscriberDescription.feedName.blank");
                }
                if (!Feed.isStandard(description.getFeedName())) {
                    throw new ValidationFailure("subscriberDescription.feedName.not-found");
                }
                if (Feed.isAdmin(description.getFeedName())) {
                    throw new ValidationFailure("subscriberDescription.feedName.not-found");
                }
                break;
            case POSTING:
            case POSTING_COMMENTS:
                if (description.getPostingId() == null) {
                    throw new ValidationFailure("subscriberDescription.postingId.blank");
                }
                break;
            case PROFILE:
                break;
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /people/subscribers/{id} (id = {})", LogUtil.format(id));

        Subscriber subscriber = subscriberRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("subscriber.not-found"));
        String ownerName = requestContext.getClientName();
        if (!requestContext.isAdmin()
                && (ObjectUtils.isEmpty(ownerName) || !ownerName.equals(subscriber.getRemoteNodeName()))) {
            throw new AuthenticationException();
        }

        subscriberRepository.delete(subscriber);

        if (subscriber.getSubscriptionType() == SubscriptionType.FEED) {
            var profileTask = new RemoteProfileSubscriptionTask(ownerName);
            taskAutowire.autowire(profileTask);
            taskExecutor.execute(profileTask);
        }

        subscriberInstants.deleted(subscriber);
        requestContext.send(new SubscriberDeletedEvent(subscriber));
        sendPeopleChangedEvent();

        return Result.OK;
    }

    private void sendPeopleChangedEvent() {
        int subscribersTotal = subscriberRepository.countAllByType(requestContext.nodeId(), SubscriptionType.FEED);
        int subscriptionsTotal = subscriptionRepository.countByType(requestContext.nodeId(), SubscriptionType.FEED);
        requestContext.send(new PeopleChangedEvent(subscribersTotal, subscriptionsTotal));
    }

}
