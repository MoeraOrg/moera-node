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
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.QSubscriber;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.SubscriberAddedLiberin;
import org.moera.node.liberin.model.SubscriberDeletedLiberin;
import org.moera.node.liberin.model.SubscriberOperationsUpdatedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.Result;
import org.moera.node.model.SubscriberDescription;
import org.moera.node.model.SubscriberInfo;
import org.moera.node.model.SubscriberOverride;
import org.moera.node.model.ValidationFailure;
import org.moera.node.operations.ContactOperations;
import org.moera.node.operations.OperationsValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private PostingRepository postingRepository;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private ContactOperations contactOperations;

    @GetMapping
    @Transactional
    public List<SubscriberInfo> getAll(@RequestParam(required = false) String nodeName,
                                       @RequestParam(required = false) SubscriptionType type,
                                       @RequestParam(required = false) String feedName,
                                       @RequestParam(required = false) UUID entryId) {
        log.info("GET /people/subscribers (nodeName = {}, type = {})",
                LogUtil.format(nodeName), LogUtil.format(SubscriptionType.toValue(type)));

        if (type == SubscriptionType.FEED) {
            if (!requestContext.isPrincipal(Subscriber.getViewAllE(requestContext.getOptions()))) {
                throw new AuthenticationException();
            }
        } else {
            if (ObjectUtils.isEmpty(nodeName) || !requestContext.isClient(nodeName)) {
                throw new AuthenticationException();
            }
        }

        QSubscriber subscriber = QSubscriber.subscriber;
        BooleanBuilder where = new BooleanBuilder();
        where.and(subscriber.nodeId.eq(requestContext.nodeId()));
        if (!ObjectUtils.isEmpty(nodeName)) {
            where.and(subscriber.remoteNodeName.eq(nodeName));
        }
        if (type != null) {
            where.and(subscriber.subscriptionType.eq(type));
        }
        if (feedName != null) {
            where.and(subscriber.feedName.eq(feedName));
        }
        if (entryId != null) {
            where.and(subscriber.entry.id.eq(entryId));
        }

        return StreamSupport.stream(subscriberRepository.findAll(where).spliterator(), false)
                .filter(s -> requestContext.isPrincipal(s.getViewE()))
                .map(s -> new SubscriberInfo(s, requestContext))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Transactional
    public SubscriberInfo get(@PathVariable UUID id) {
        log.info("GET /people/subscribers/{id} (id = {})", LogUtil.format(id));

        Subscriber subscriber = subscriberRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("subscriber.not-found"));
        if (subscriber.getSubscriptionType() == SubscriptionType.FEED) {
            if (!requestContext.isPrincipal(Subscriber.getViewAllE(requestContext.getOptions()))
                    && !requestContext.isClient(subscriber.getRemoteNodeName())
                    || !requestContext.isPrincipal(subscriber.getViewE())) {
                throw new AuthenticationException();
            }
        } else {
            if (!requestContext.isClient(subscriber.getRemoteNodeName())) {
                throw new AuthenticationException();
            }
        }

        return new SubscriberInfo(subscriber, requestContext);
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
            if (!requestContext.isPrincipal(posting.getViewE())) {
                throw new ObjectNotFoundFailure("posting.not-found");
            }
            subscriber.setEntry(posting);
        }
        subscriber = subscriberRepository.save(subscriber);

        contactOperations.updateCloseness(subscriber.getRemoteNodeName(),
                subscriber.getSubscriptionType() == SubscriptionType.FEED ? 1 : 0.25f);

        requestContext.subscriptionsUpdated();
        requestContext.send(new SubscriberAddedLiberin(subscriber, subscriberDescription.getLastUpdatedAt()));

        return new SubscriberInfo(subscriber, requestContext);
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

        OperationsValidator.validateOperations(description::getPrincipal,
                OperationsValidator.SUBSCRIBER_OPERATIONS, false,
                "subscriberDescription.operations.wrong-principal");
    }

    @PutMapping("/{id}")
    @Transactional
    public SubscriberInfo put(@PathVariable UUID id, @Valid @RequestBody SubscriberOverride subscriberOverride) {
        log.info("PUT /people/subscribers/{id} (id = {})", LogUtil.format(id));

        Subscriber subscriber = subscriberRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("subscriber.not-found"));
        Principal latestView = subscriber.getViewE();
        if (subscriber.getSubscriptionType() != SubscriptionType.FEED) {
            throw new ObjectNotFoundFailure("not-supported");
        }
        if (subscriberOverride.getOperations() != null && !subscriberOverride.getOperations().isEmpty()
                && !requestContext.isClient(subscriber.getRemoteNodeName())) {
            throw new AuthenticationException();
        }
        OperationsValidator.validateOperations(subscriberOverride::getPrincipal,
                OperationsValidator.SUBSCRIBER_OPERATIONS, false,
                "subscriberOverride.operations.wrong-principal");
        if (subscriberOverride.getAdminOperations() != null && !subscriberOverride.getAdminOperations().isEmpty()
                && !requestContext.isPrincipal(Subscriber.getOverrideE())) {
            throw new AuthenticationException();
        }
        OperationsValidator.validateOperations(subscriberOverride::getAdminPrincipal,
                OperationsValidator.SUBSCRIBER_OPERATIONS, true,
                "subscriberOverride.adminOperations.wrong-principal");

        subscriberOverride.toSubscriber(subscriber);

        requestContext.send(new SubscriberOperationsUpdatedLiberin(subscriber, latestView));

        return new SubscriberInfo(subscriber, requestContext);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /people/subscribers/{id} (id = {})", LogUtil.format(id));

        Subscriber subscriber = subscriberRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("subscriber.not-found"));
        if (!requestContext.isPrincipal(subscriber.getDeleteE())) {
            throw new AuthenticationException();
        }

        subscriberRepository.delete(subscriber);

        requestContext.subscriptionsUpdated();
        requestContext.send(new SubscriberDeletedLiberin(subscriber));

        return Result.OK;
    }

}
