package org.moera.node.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.ContactFilter;
import org.moera.lib.node.types.ContactInfo;
import org.moera.lib.node.types.ContactWithRelationships;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.config.Config;
import org.moera.node.data.BlockedByUser;
import org.moera.node.data.BlockedByUserRepository;
import org.moera.node.data.BlockedUser;
import org.moera.node.data.BlockedUserRepository;
import org.moera.node.data.Contact;
import org.moera.node.data.ContactRepository;
import org.moera.node.data.Feed;
import org.moera.node.data.Friend;
import org.moera.node.data.FriendOf;
import org.moera.node.data.FriendOfRepository;
import org.moera.node.data.FriendRepository;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.UserSubscription;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.BlockedByUserInfoUtil;
import org.moera.node.model.BlockedUserInfoUtil;
import org.moera.node.model.ContactInfoUtil;
import org.moera.node.model.FriendGroupDetailsUtil;
import org.moera.node.model.FriendInfoUtil;
import org.moera.node.model.FriendOfInfoUtil;
import org.moera.node.model.SubscriberInfoUtil;
import org.moera.node.model.SubscriptionInfoUtil;
import org.moera.node.operations.ContactSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/people/contacts")
@NoCache
public class ContactsController {

    private static final Logger log = LoggerFactory.getLogger(ContactsController.class);

    @Inject
    private Config config;

    @Inject
    private RequestContext requestContext;

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    @Inject
    private FriendRepository friendRepository;

    @Inject
    private FriendOfRepository friendOfRepository;

    @Inject
    private BlockedUserRepository blockedUserRepository;

    @Inject
    private BlockedByUserRepository blockedByUserRepository;

    @Inject
    private ContactSearch contactSearch;

    @GetMapping
    @Admin(Scope.VIEW_PEOPLE)
    @Transactional
    public List<ContactInfo> getAll(
        @RequestParam(defaultValue = "") String query,
        @RequestParam(required = false) Integer limit
    ) {
        log.info("GET /people/contacts (query = {}, limit = {})", LogUtil.format(query), LogUtil.format(limit));

        limit = limit != null && limit <= ContactSearch.MAX_CONTACTS_PER_REQUEST
            ? limit
            : ContactSearch.MAX_CONTACTS_PER_REQUEST;
        ValidationUtil.assertion(limit >= 0, "limit.invalid");
        if (limit == 0) {
            return Collections.emptyList();
        }

        return contactSearch.search(requestContext.nodeId(), query, limit).stream()
            .map(c -> ContactInfoUtil.build(
                c, requestContext.getOptions(), requestContext, config.getMedia().getDirectServe()
            ))
            .toList();
    }

    @PostMapping("/fetch")
    @Admin(Scope.VIEW_PEOPLE)
    @Transactional
    public List<ContactWithRelationships> fetchAll(@RequestBody ContactFilter filter) {
        log.info("POST /people/contacts/fetch");

        filter.validate();

        if (ObjectUtils.isEmpty(filter.getNodeNames())) {
            return Collections.emptyList();
        }

        Map<String, ContactWithRelationships> info = new HashMap<>();

        Collection<Contact> contacts = contactRepository.findByRemoteNodes(
            requestContext.nodeId(), filter.getNodeNames()
        );
        for (Contact c : contacts) {
            ContactWithRelationships cr = new ContactWithRelationships();
            cr.setContact(ContactInfoUtil.build(
                c, requestContext.getOptions(), requestContext, config.getMedia().getDirectServe()
            ));
            info.put(c.getRemoteNodeName(), cr);
        }

        Collection<Subscriber> subscribers = subscriberRepository.findByRemoteNodes(
            requestContext.nodeId(), SubscriptionType.FEED, Feed.TIMELINE, info.keySet()
        );
        for (Subscriber sr : subscribers) {
            ContactWithRelationships cr = info.get(sr.getRemoteNodeName());
            if (cr != null) {
                cr.setSubscriber(SubscriberInfoUtil.build(sr, (ContactInfo) null, requestContext));
            }
        }

        Collection<UserSubscription> subscriptions = userSubscriptionRepository.findByNodes(
            requestContext.nodeId(), SubscriptionType.FEED, info.keySet(), Feed.TIMELINE
        );
        for (UserSubscription sr : subscriptions) {
            ContactWithRelationships cr = info.get(sr.getRemoteNodeName());
            if (cr != null) {
                cr.setSubscription(SubscriptionInfoUtil.build(sr, null, requestContext.getOptions(), requestContext));
            }
        }

        List<Friend> friends = friendRepository.findByNames(requestContext.nodeId(), info.keySet());
        groupByName(
            friends,
            Friend::getRemoteNodeName,
            fr -> FriendGroupDetailsUtil.build(fr, true),
            (cr, groups) -> cr.setFriend(FriendInfoUtil.build(cr.getContact().getNodeName(), null, groups)),
            info
        );

        List<FriendOf> friendOfs = friendOfRepository.findByRemoteNodes(requestContext.nodeId(), info.keySet());
        groupByName(
            friendOfs,
            FriendOf::getRemoteNodeName,
            FriendGroupDetailsUtil::build,
            (cr, groups) -> cr.setFriendOf(FriendOfInfoUtil.build(cr.getContact().getNodeName(), null, groups)),
            info
        );

        List<BlockedUser> blockedUsers = blockedUserRepository.findByRemoteNodes(
            requestContext.nodeId(), info.keySet()
        );
        groupByName(
            blockedUsers,
            BlockedUser::getRemoteNodeName,
            bu -> BlockedUserInfoUtil.build(bu, (ContactInfo) null, requestContext),
            ContactWithRelationships::setBlocked,
            info
        );

        List<BlockedByUser> blockedByUsers = blockedByUserRepository.findByRemoteNodes(
            requestContext.nodeId(), info.keySet()
        );
        groupByName(
            blockedByUsers,
            BlockedByUser::getRemoteNodeName,
            bu -> BlockedByUserInfoUtil.build(bu, (ContactInfo) null, requestContext),
            ContactWithRelationships::setBlockedBy,
            info
        );

        return new ArrayList<>(info.values());
    }

    private <R, I> void groupByName(
        List<R> records,
        Function<R, String> nameExtractor,
        Function<R, I> converter,
        BiConsumer<ContactWithRelationships, List<I>> saver,
        Map<String, ContactWithRelationships> info
    ) {
        String nodeName = null;
        List<I> group = new ArrayList<>();
        for (R r : records) {
            String remoteName = nameExtractor.apply(r);
            if (nodeName != null && !nodeName.equals(remoteName)) {
                ContactWithRelationships cr = info.get(nodeName);
                if (cr != null) {
                    saver.accept(cr, group);
                }
                group.clear();
            }
            nodeName = remoteName;
            group.add(converter.apply(r));
        }
        if (!group.isEmpty()) {
            ContactWithRelationships cr = info.get(nodeName);
            if (cr != null) {
                saver.accept(cr, group);
            }
        }
    }

}
