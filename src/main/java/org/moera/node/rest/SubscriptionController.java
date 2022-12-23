package org.moera.node.rest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.validation.Valid;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Contact;
import org.moera.node.data.Feed;
import org.moera.node.data.QContact;
import org.moera.node.data.QMediaFile;
import org.moera.node.data.QUserSubscription;
import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UserSubscription;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.SubscriptionAddedLiberin;
import org.moera.node.liberin.model.SubscriptionDeletedLiberin;
import org.moera.node.liberin.model.SubscriptionOperationsUpdatedLiberin;
import org.moera.node.model.ContactInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.RemoteFeed;
import org.moera.node.model.RemotePosting;
import org.moera.node.model.SubscriptionDescription;
import org.moera.node.model.SubscriptionFilter;
import org.moera.node.model.SubscriptionInfo;
import org.moera.node.model.SubscriptionOverride;
import org.moera.node.model.ValidationFailure;
import org.moera.node.operations.ContactOperations;
import org.moera.node.operations.OperationsValidator;
import org.moera.node.option.OptionHook;
import org.moera.node.option.OptionValueChange;
import org.moera.node.rest.task.AllRemoteSubscribersUpdateTask;
import org.moera.node.rest.task.RemoteFeedFetchTask;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
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
@RequestMapping("/moera/api/people/subscriptions")
@NoCache
public class SubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private PlatformTransactionManager txManager;

    @Inject
    @PersistenceContext
    private EntityManager entityManager;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @GetMapping
    @Transactional
    public List<SubscriptionInfo> getAll(@RequestParam(required = false) String nodeName,
                                         @RequestParam(required = false) SubscriptionType type) {
        log.info("GET /people/subscriptions (nodeName = {}, type = {})",
                LogUtil.format(nodeName), LogUtil.format(SubscriptionType.toValue(type)));

        if (type == SubscriptionType.FEED) {
            if (!requestContext.isPrincipal(UserSubscription.getViewAllE(requestContext.getOptions()))) {
                throw new AuthenticationException();
            }
        } else {
            if (ObjectUtils.isEmpty(nodeName) || !requestContext.isClient(nodeName)) {
                throw new AuthenticationException();
            }
        }

        QUserSubscription userSubscription = QUserSubscription.userSubscription;
        BooleanBuilder where = new BooleanBuilder();
        where.and(userSubscription.nodeId.eq(requestContext.nodeId()));
        if (!ObjectUtils.isEmpty(nodeName)) {
            where.and(userSubscription.remoteNodeName.eq(nodeName));
        }
        if (type != null) {
            where.and(userSubscription.subscriptionType.eq(type));
        }

        return fetchSubscriptions(where).stream()
                .filter(sr -> requestContext.isPrincipal(sr.getViewE()))
                .map(sr -> new SubscriptionInfo(sr, requestContext.getOptions(), requestContext))
                .collect(Collectors.toList());
    }

    @PostMapping
    @Admin
    @Entitled
    @Transactional
    public SubscriptionInfo post(@Valid @RequestBody SubscriptionDescription subscriptionDescription) throws Throwable {
        log.info("POST /people/subscriptions (type = {}, feedName = {}, remoteNodeName = {}, remotePostingId = {})",
                LogUtil.format(SubscriptionType.toValue(subscriptionDescription.getType())),
                LogUtil.format(subscriptionDescription.getFeedName()),
                LogUtil.format(subscriptionDescription.getRemoteNodeName()),
                LogUtil.format(subscriptionDescription.getRemotePostingId()));

        if (subscriptionDescription.getType() == null) {
            throw new ValidationFailure("subscriptionDescription.type.blank");
        }
        if (subscriptionDescription.getReason() == null) {
            throw new ValidationFailure("subscriptionDescription.reason.blank");
        }
        if (subscriptionDescription.getType() == SubscriptionType.FEED) {
            if (ObjectUtils.isEmpty(subscriptionDescription.getFeedName())) {
                throw new ValidationFailure("subscriptionDescription.feedName.blank");
            }
            if (!Feed.isStandard(subscriptionDescription.getFeedName())) {
                throw new ValidationFailure("subscriptionDescription.feedName.not-found");
            }
        }

        UserSubscription subscription;
        try {
            subscription = Transaction.execute(txManager, () -> {
                OperationsValidator.validateOperations(subscriptionDescription::getPrincipal,
                        OperationsValidator.SUBSCRIPTION_OPERATIONS, false,
                        "subscriptionDescription.operations.wrong-principal");

                UserSubscription userSubscription = new UserSubscription();
                userSubscription.setId(UUID.randomUUID());
                userSubscription.setNodeId(requestContext.nodeId());
                subscriptionDescription.toUserSubscription(userSubscription);
                userSubscription = userSubscriptionRepository.save(userSubscription);

                Contact contact;
                if (userSubscription.getSubscriptionType() == SubscriptionType.FEED) {
                    contactOperations.updateCloseness(userSubscription.getRemoteNodeName(), 800);
                    contactOperations.updateFeedSubscriptionCount(userSubscription.getRemoteNodeName(), 1);
                    contact = contactOperations.updateViewPrincipal(userSubscription);
                } else {
                    contact = contactOperations.updateCloseness(userSubscription.getRemoteNodeName(), 1);
                }
                contact.fill(userSubscription);

                return userSubscription;
            });
        } catch (DataIntegrityViolationException e) {
            throw new OperationFailure("subscription.already-exists");
        }

        requestContext.subscriptionsUpdated();
        requestContext.send(new SubscriptionAddedLiberin(subscription));

        if (subscription.getSubscriptionType() == SubscriptionType.FEED) {
            var fetchTask = new RemoteFeedFetchTask(subscription.getFeedName(), subscription.getRemoteNodeName(),
                    subscription.getRemoteFeedName());
            taskAutowire.autowire(fetchTask);
            taskExecutor.execute(fetchTask);
        }

        return new SubscriptionInfo(subscription, requestContext.getOptions(), requestContext);
    }

    @PutMapping("/{id}")
    @Transactional
    public SubscriptionInfo put(@PathVariable UUID id, @Valid @RequestBody SubscriptionOverride subscriptionOverride) {
        log.info("PUT /people/subscriptions/{id} (id = {})", LogUtil.format(id));

        UserSubscription subscription = userSubscriptionRepository.findAllByNodeIdAndId(
                        requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("subscription.not-found"));
        Principal latestView = subscription.getViewE();
        if (subscription.getSubscriptionType() != SubscriptionType.FEED) {
            throw new ObjectNotFoundFailure("not-supported");
        }
        if (!requestContext.isPrincipal(subscription.getEditOperationsE())) {
            throw new AuthenticationException();
        }
        OperationsValidator.validateOperations(subscriptionOverride::getPrincipal,
                OperationsValidator.SUBSCRIBER_OPERATIONS, false,
                "subscriptionOverride.operations.wrong-principal");

        subscriptionOverride.toUserSubscription(subscription);
        if (subscription.getSubscriptionType() == SubscriptionType.FEED) {
            contactOperations.updateViewPrincipal(subscription).fill(subscription);
        }

        requestContext.send(new SubscriptionOperationsUpdatedLiberin(subscription, latestView));

        return new SubscriptionInfo(subscription, requestContext.getOptions(), requestContext);
    }

    @DeleteMapping("/{id}")
    @Admin
    @Transactional
    public ContactInfo delete(@PathVariable UUID id) {
        log.info("DELETE /people/subscriptions/{id} (id = {})", LogUtil.format(id));

        UserSubscription subscription = userSubscriptionRepository.findAllByNodeIdAndId(
                        requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("subscription.not-found"));
        userSubscriptionRepository.delete(subscription);
        if (subscription.getSubscriptionType() == SubscriptionType.FEED) {
            contactOperations.updateFeedSubscriptionCount(subscription.getRemoteNodeName(), -1).fill(subscription);
        }

        requestContext.subscriptionsUpdated();
        requestContext.send(new SubscriptionDeletedLiberin(subscription));

        return new ContactInfo(subscription.getContact(), requestContext.getOptions(), requestContext);
    }

    @PostMapping("/search")
    @Transactional
    public List<SubscriptionInfo> search(@Valid @RequestBody SubscriptionFilter filter) {
        log.info("POST /people/subscriptions/search");

        if (ObjectUtils.isEmpty(filter.getFeeds()) && ObjectUtils.isEmpty(filter.getPostings())) {
            throw new ValidationFailure("subscription.filter.incomplete");
        }

        QUserSubscription userSubscription = QUserSubscription.userSubscription;
        BooleanBuilder where = new BooleanBuilder();
        where.and(userSubscription.nodeId.eq(requestContext.nodeId()));
        if (filter.getType() != null) {
            where.and(userSubscription.subscriptionType.eq(filter.getType()));
        }
        if (filter.getFeeds() != null) {
            List<String> remoteFeedNames = filter.getFeeds().stream()
                    .map(RemoteFeed::getFeedName)
                    .collect(Collectors.toList());
            where.and(userSubscription.remoteFeedName.in(remoteFeedNames));
        }
        if (filter.getPostings() != null) {
            List<String> remotePostingIds = filter.getPostings().stream()
                    .map(RemotePosting::getPostingId)
                    .collect(Collectors.toList());
            where.and(userSubscription.remoteEntryId.in(remotePostingIds));
        }

        return fetchSubscriptions(where).stream()
                .filter(r -> filter.getFeeds() == null || filter.getFeeds().contains(r.getRemoteFeed()))
                .filter(r -> filter.getPostings() == null || filter.getPostings().contains(r.getRemotePosting()))
                .map(sr -> new SubscriptionInfo(sr, requestContext.getOptions(), requestContext))
                .collect(Collectors.toList());
    }

    private List<UserSubscription> fetchSubscriptions(Predicate where) {
        QUserSubscription userSubscription = QUserSubscription.userSubscription;
        QContact contact = QContact.contact;
        QMediaFile mediaFile = QMediaFile.mediaFile;

        return new JPAQueryFactory(entityManager)
                .selectFrom(userSubscription)
                .leftJoin(userSubscription.contact, contact).fetchJoin()
                .leftJoin(contact.remoteAvatarMediaFile, mediaFile).fetchJoin()
                .where(where)
                .fetch();
    }

    @OptionHook("subscriptions.view")
    public void subscriptionVisibilityChanged(OptionValueChange change) {
        Principal ourView = (Principal) change.getNewValue();
        Principal theirView = ourView.isAdmin() ? Principal.PRIVATE : ourView;

        var task = new AllRemoteSubscribersUpdateTask(change.getNodeId(), theirView);
        taskAutowire.autowire(task);
        taskExecutor.execute(task);
    }

}
