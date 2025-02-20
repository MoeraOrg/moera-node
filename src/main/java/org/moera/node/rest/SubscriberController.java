package org.moera.node.rest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.moera.lib.node.types.ContactInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SubscriberDescription;
import org.moera.lib.node.types.SubscriberInfo;
import org.moera.lib.node.types.SubscriberOverride;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.Contact;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.QContact;
import org.moera.node.data.QSubscriber;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.UserList;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.SubscriberAddedLiberin;
import org.moera.node.liberin.model.SubscriberDeletedLiberin;
import org.moera.node.liberin.model.SubscriberOperationsUpdatedLiberin;
import org.moera.node.model.ContactInfoUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.SubscriberDescriptionUtil;
import org.moera.node.model.SubscriberInfoUtil;
import org.moera.node.model.SubscriberOverrideUtil;
import org.moera.node.operations.ContactOperations;
import org.moera.node.operations.OperationsValidator;
import org.moera.node.util.Util;
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
    private ContactOperations contactOperations;

    @Inject
    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping
    @Transactional
    public List<SubscriberInfo> getAll(@RequestParam(required = false) String nodeName,
                                       @RequestParam(required = false) SubscriptionType type,
                                       @RequestParam(required = false) String feedName,
                                       @RequestParam(required = false) UUID entryId) {
        log.info("GET /people/subscribers (nodeName = {}, type = {})",
                LogUtil.format(nodeName), LogUtil.format(SubscriptionType.toValue(type)));

        if (type == SubscriptionType.FEED) {
            if (!requestContext.isPrincipal(Subscriber.getViewAllE(requestContext.getOptions()), Scope.VIEW_PEOPLE)) {
                throw new AuthenticationException();
            }
        } else {
            if (ObjectUtils.isEmpty(nodeName) || !requestContext.isClient(nodeName, Scope.VIEW_PEOPLE)) {
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

        return fetchSubscribers(where).stream()
                .filter(s -> requestContext.isPrincipal(s.getViewE(), Scope.VIEW_PEOPLE))
                .map(s -> SubscriberInfoUtil.build(s, requestContext.getOptions(), requestContext))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Transactional
    public SubscriberInfo get(@PathVariable UUID id) {
        log.info("GET /people/subscribers/{id} (id = {})", LogUtil.format(id));

        Subscriber subscriber = subscriberRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("subscriber.not-found"));
        if (subscriber.getSubscriptionType() == SubscriptionType.FEED) {
            if (!requestContext.isPrincipal(Subscriber.getViewAllE(requestContext.getOptions()), Scope.VIEW_PEOPLE)
                    && !requestContext.isClient(subscriber.getRemoteNodeName(), Scope.VIEW_PEOPLE)
                    || !requestContext.isPrincipal(subscriber.getViewE(), Scope.VIEW_PEOPLE)) {
                throw new AuthenticationException();
            }
        } else {
            if (!requestContext.isClient(subscriber.getRemoteNodeName(), Scope.VIEW_PEOPLE)) {
                throw new AuthenticationException();
            }
        }

        return SubscriberInfoUtil.build(subscriber, requestContext.getOptions(), requestContext);
    }

    @PostMapping
    @Entitled
    @Transactional
    public SubscriberInfo post(@RequestBody SubscriberDescription subscriberDescription) {
        log.info(
            "POST /people/subscribers (type = {}, feedName = {}, postingId = {})",
            LogUtil.format(SubscriptionType.toValue(subscriberDescription.getType())),
            LogUtil.format(subscriberDescription.getFeedName()),
            LogUtil.format(subscriberDescription.getPostingId())
        );

        subscriberDescription.validate();

        String ownerName = requestContext.getClientName(Scope.SUBSCRIBE);
        if (ObjectUtils.isEmpty(ownerName)) {
            throw new AuthenticationException();
        }
        validate(subscriberDescription);

        Subscriber subscriber = new Subscriber();
        subscriber.setId(UUID.randomUUID());
        subscriber.setNodeId(requestContext.nodeId());
        subscriber.setRemoteNodeName(ownerName);
        SubscriberDescriptionUtil.toSubscriber(subscriberDescription, subscriber);
        if (subscriberDescription.getPostingId() != null) {
            UUID postingId = Util.uuid(subscriberDescription.getPostingId())
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
            Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId)
                .orElseThrow(() -> new ObjectNotFoundFailure("posting.not-found"));
            if (!requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)) {
                throw new ObjectNotFoundFailure("posting.not-found");
            }
            subscriber.setEntry(posting);
        }
        subscriber = subscriberRepository.save(subscriber);

        Contact contact;
        if (subscriber.getSubscriptionType() == SubscriptionType.FEED) {
            contactOperations.updateCloseness(subscriber.getRemoteNodeName(), 1);
            contactOperations.updateFeedSubscriberCount(subscriber.getRemoteNodeName(), 1);
            contact = contactOperations.updateViewPrincipal(subscriber);
        } else {
            contact = contactOperations.updateCloseness(subscriber.getRemoteNodeName(), 0.25f);
        }
        contact.fill(subscriber);

        requestContext.subscriptionsUpdated();
        requestContext.send(new SubscriberAddedLiberin(subscriber, subscriberDescription.getLastUpdatedAt()));

        return SubscriberInfoUtil.build(subscriber, requestContext.getOptions(), requestContext);
    }

    private void validate(SubscriberDescription description) {
        switch (description.getType()) {
            case FEED:
                ValidationUtil.notBlank(description.getFeedName(), "subscriber.feed-name.blank");
                if (!Feed.isStandard(description.getFeedName()) || Feed.isAdmin(description.getFeedName())) {
                    throw new ObjectNotFoundFailure("feed.not-found");
                }
                break;
            case POSTING:
            case POSTING_COMMENTS:
                ValidationUtil.notBlank(description.getPostingId(), "subscriber.posting-id.blank");
                break;
            case PROFILE:
                break;
            case USER_LIST:
                ValidationUtil.notBlank(description.getFeedName(), "subscriber.feed-name.blank");
                ValidationUtil.assertion(UserList.isKnown(description.getFeedName()), "feed.not-found");
                if (!UserList.isKnown(description.getFeedName())) {
                    throw new ObjectNotFoundFailure("user-list.not-found");
                }
                break;
        }

        OperationsValidator.validateOperations(
            description.getOperations(),
            false,
            "subscriber.operations.wrong-principal"
        );
    }

    @PutMapping("/{id}")
    @Transactional
    public SubscriberInfo put(@PathVariable UUID id, @RequestBody SubscriberOverride subscriberOverride) {
        log.info("PUT /people/subscribers/{id} (id = {})", LogUtil.format(id));

        Subscriber subscriber = subscriberRepository.findByNodeIdAndId(requestContext.nodeId(), id)
            .orElseThrow(() -> new ObjectNotFoundFailure("subscriber.not-found"));
        Principal latestView = subscriber.getViewE();
        if (subscriber.getSubscriptionType() != SubscriptionType.FEED) {
            throw new ObjectNotFoundFailure("not-supported");
        }
        if (
            subscriberOverride.getOperations() != null
            && !subscriberOverride.getOperations().isEmpty()
            && !requestContext.isClient(subscriber.getRemoteNodeName(), Scope.SUBSCRIBE)
        ) {
            throw new AuthenticationException();
        }
        OperationsValidator.validateOperations(
            subscriberOverride.getOperations(),
            false,
            "subscriber.operations.wrong-principal"
        );
        if (
            subscriberOverride.getAdminOperations() != null
            && !subscriberOverride.getAdminOperations().isEmpty()
            && !requestContext.isPrincipal(Subscriber.getOverrideE(), Scope.VIEW_PEOPLE)
        ) {
            throw new AuthenticationException();
        }
        OperationsValidator.validateOperations(
            subscriberOverride.getAdminOperations(),
            true,
            "subscriber.admin-operations.wrong-principal"
        );

        SubscriberOverrideUtil.toSubscriber(subscriberOverride, subscriber);
        if (subscriber.getSubscriptionType() == SubscriptionType.FEED) {
            contactOperations.updateViewPrincipal(subscriber).fill(subscriber);
        }

        requestContext.send(new SubscriberOperationsUpdatedLiberin(subscriber, latestView));

        return SubscriberInfoUtil.build(subscriber, requestContext.getOptions(), requestContext);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ContactInfo delete(@PathVariable UUID id) {
        log.info("DELETE /people/subscribers/{id} (id = {})", LogUtil.format(id));

        Subscriber subscriber = subscriberRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("subscriber.not-found"));
        if (!requestContext.isPrincipal(subscriber.getDeleteE(), Scope.SUBSCRIBE)) {
            throw new AuthenticationException();
        }

        subscriberRepository.delete(subscriber);
        if (subscriber.getSubscriptionType() == SubscriptionType.FEED) {
            contactOperations.updateFeedSubscriberCount(subscriber.getRemoteNodeName(), -1).fill(subscriber);
        }

        requestContext.subscriptionsUpdated();
        requestContext.send(new SubscriberDeletedLiberin(subscriber));

        return ContactInfoUtil.build(subscriber.getContact(), requestContext.getOptions(), requestContext);
    }

    private List<Subscriber> fetchSubscribers(Predicate where) {
        QSubscriber subscriber = QSubscriber.subscriber;
        QContact contact = QContact.contact;

        return new JPAQueryFactory(entityManager)
                .selectFrom(subscriber)
                .leftJoin(subscriber.contact, contact).fetchJoin()
                .leftJoin(contact.remoteAvatarMediaFile).fetchJoin()
                .where(where)
                .fetch();
    }

}
