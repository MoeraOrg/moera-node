package org.moera.node.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.moera.lib.node.types.ContactFilter;
import org.moera.lib.node.types.ContactInfo;
import org.moera.lib.node.types.ContactWithRelationships;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
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
import org.moera.node.data.QContact;
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
import org.moera.node.util.Util;
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

    public static final int MAX_CONTACTS_PER_REQUEST = 100;
    private static final int ARRANGEMENT_DEPTH = 5;

    private static final Logger log = LoggerFactory.getLogger(ContactsController.class);

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
    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping
    @Admin(Scope.VIEW_PEOPLE)
    @Transactional
    public List<ContactInfo> getAll(
        @RequestParam(defaultValue = "") String query,
        @RequestParam(required = false) Integer limit
    ) {
        log.info("GET /people/contacts (query = {}, limit = {})", LogUtil.format(query), LogUtil.format(limit));

        limit = limit != null && limit <= MAX_CONTACTS_PER_REQUEST ? limit : MAX_CONTACTS_PER_REQUEST;
        ValidationUtil.assertion(limit >= 0, "limit.invalid");
        if (limit == 0) {
            return Collections.emptyList();
        }

        query = query.trim();
        String[] words = query.split("\\s+");

        QContact contact = QContact.contact;
        BooleanBuilder where = new BooleanBuilder();
        where.and(contact.nodeId.eq(requestContext.nodeId()));
        if (!ObjectUtils.isEmpty(query)) {
            for (String word : words) {
                String pattern = "%" + Util.le(word) + "%";
                where.andAnyOf(
                    contact.remoteFullName.likeIgnoreCase(pattern),
                    contact.remoteNodeName.likeIgnoreCase(pattern)
                );
            }
        }

        var request = new JPAQueryFactory(entityManager)
            .selectFrom(contact)
            .leftJoin(contact.remoteAvatarMediaFile).fetchJoin()
            .where(where)
            .orderBy(contact.distance.asc())
            .limit(limit);

        List<Pattern> regexes = Arrays.stream(words)
            .map(word -> Pattern.compile("(?:^|\\s)" + Util.re(word), Pattern.CASE_INSENSITIVE))
            .collect(Collectors.toList());

        int offset = 0;
        List<ContactInfo> result = new ArrayList<>();
        while (true) {
            List<Contact> page = request.offset(offset).fetch();
            if (page.isEmpty()) {
                return result;
            }
            page.stream()
                .filter(ct -> contactMatch(ct, regexes))
                .limit(limit - result.size())
                .map(c -> ContactInfoUtil.build(c, requestContext.getOptions(), requestContext))
                .forEach(result::add);
            if (result.size() >= limit) {
                return result;
            }
            offset += page.size();
        }
    }

    private boolean contactMatch(Contact contact, List<Pattern> regexes) {
        String haystack = !ObjectUtils.isEmpty(contact.getRemoteFullName())
            ? contact.getRemoteFullName() + " " + contact.getRemoteNodeName()
            : contact.getRemoteNodeName();
        List<Matcher> matchers = regexes.stream().map(regex -> regex.matcher(haystack)).toList();
        boolean allFound = matchers.stream().allMatch(Matcher::find);
        if (!allFound) {
            return false;
        }
        if (regexes.size() <= 1) {
            return true;
        }
        matchers.forEach(Matcher::reset);
        List<int[]> matches = matchers.stream()
            .map(m -> m.results().mapToInt(MatchResult::start).toArray())
            .collect(Collectors.toList());
        return hasArrangement(matches);
    }

    private boolean hasArrangement(List<int[]> values) {
        int size = Math.min(values.size(), ARRANGEMENT_DEPTH);
        int[] indexes = new int[size];
        Set<Integer> used = new HashSet<>();
        while (true) {
            used.clear();
            for (int i = 0; i < size; i++) {
                int value = values.get(i)[indexes[i]];
                if (used.contains(value)) {
                    break;
                }
                used.add(value);
            }
            if (used.size() == size) {
                return true;
            }
            for (int i = 0; i < size; i++) {
                indexes[i]++;
                if (indexes[i] < values.get(i).length) {
                    break;
                }
                if (i == size - 1) {
                    return false;
                }
                indexes[i] = 0;
            }
        }
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
            cr.setContact(ContactInfoUtil.build(c, requestContext.getOptions(), requestContext));
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
