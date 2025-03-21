package org.moera.node.ui;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.naming.NodeName;
import org.moera.lib.node.types.PeopleGeneralInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SubscriberInfo;
import org.moera.lib.node.types.SubscriptionInfo;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.node.data.BlockedByUserRepository;
import org.moera.node.data.BlockedUserRepository;
import org.moera.node.data.FriendOfRepository;
import org.moera.node.data.FriendRepository;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.UserSubscription;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UiController;
import org.moera.node.global.VirtualPage;
import org.moera.node.model.PeopleGeneralInfoUtil;
import org.moera.node.model.SubscriberInfoUtil;
import org.moera.node.model.SubscriptionInfoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@UiController
public class PeopleUiController {

    private static final Logger log = LoggerFactory.getLogger(PeopleUiController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private TitleBuilder titleBuilder;

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

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD}, path = "/people")
    @VirtualPage
    public String people() {
        return "redirect:/people/subscribers";
    }

    @RequestMapping(
        method = {RequestMethod.GET, RequestMethod.HEAD},
        path = "/people/subscribers",
        produces = "text/html"
    )
    @VirtualPage
    @Transactional
    public String subscribers(Model model) {
        log.info("UI /people/subscribers");

        PeopleGeneralInfo totals = getTotals();
        Comparator<Subscriber> comparator = Comparator.comparing(
            sr -> sr.getContact().getRemoteFullName() != null
                ? sr.getContact().getRemoteFullName()
                : NodeName.shorten(sr.getRemoteNodeName())
        );
        List<SubscriberInfo> subscribers = Collections.emptyList();
        if (Subscriber.getViewAllE(requestContext.getOptions()).isPublic()) {
            subscribers = subscriberRepository.findAllByType(requestContext.nodeId(), SubscriptionType.FEED).stream()
                .sorted(comparator)
                .filter(s -> requestContext.isPrincipal(s.getViewE(), Scope.VIEW_PEOPLE))
                .map(s -> SubscriberInfoUtil.build(s, requestContext.getOptions(), requestContext))
                .collect(Collectors.toList());
        }

        model.addAttribute("pageTitle", titleBuilder.build("Subscribers"));
        model.addAttribute("menuIndex", "people");
        model.addAttribute("subscribersTotal", totals.getFeedSubscribersTotal());
        model.addAttribute("subscriptionsTotal", totals.getFeedSubscriptionsTotal());
        model.addAttribute("subscribers", subscribers);

        model.addAttribute("ogUrl", requestContext.getSiteUrl() + "/people/subscribers");
        model.addAttribute("ogTitle", "Subscribers");

        return "subscribers";
    }

    @RequestMapping(
        method = {RequestMethod.GET, RequestMethod.HEAD},
        path = "/people/subscriptions",
        produces = "text/html"
    )
    @VirtualPage
    @Transactional
    public String subscriptions(Model model) {
        log.info("UI /people/subscriptions");

        PeopleGeneralInfo totals = getTotals();
        Comparator<UserSubscription> comparator = Comparator.comparing(
            sr -> sr.getContact().getRemoteFullName() != null
                ? sr.getContact().getRemoteFullName()
                : NodeName.shorten(sr.getRemoteNodeName())
        );
        List<SubscriptionInfo> subscriptions = Collections.emptyList();
        if (UserSubscription.getViewAllE(requestContext.getOptions()).isPublic()) {
            subscriptions = userSubscriptionRepository.findAllByType(requestContext.nodeId(), SubscriptionType.FEED)
                .stream()
                .sorted(comparator)
                .filter(s -> requestContext.isPrincipal(s.getViewE(), Scope.VIEW_PEOPLE))
                .map(s -> SubscriptionInfoUtil.build(s, requestContext.getOptions(), requestContext))
                .collect(Collectors.toList());
        }

        model.addAttribute("pageTitle", titleBuilder.build("Subscriptions"));
        model.addAttribute("menuIndex", "people");
        model.addAttribute("subscribersTotal", totals.getFeedSubscribersTotal());
        model.addAttribute("subscriptionsTotal", totals.getFeedSubscriptionsTotal());
        model.addAttribute("subscriptions", subscriptions);

        model.addAttribute("ogUrl", requestContext.getSiteUrl() + "/people/subscriptions");
        model.addAttribute("ogTitle", "Subscriptions");

        return "subscriptions";
    }

    private PeopleGeneralInfo getTotals() {
        int subscribersTotal = subscriberRepository.countAllByType(requestContext.nodeId(), SubscriptionType.FEED);
        int subscriptionsTotal = userSubscriptionRepository.countByType(requestContext.nodeId(), SubscriptionType.FEED);
        Map<String, Integer> friendsTotal = friendRepository.countGroupsByNodeId(requestContext.nodeId()).stream()
            .collect(Collectors.toMap(fg -> fg.getId().toString(), fg -> (int) fg.getTotal()));
        int friendOfsTotal = friendOfRepository.countByNodeId(requestContext.nodeId());
        int blockedTotal = blockedUserRepository.countByNodeId(requestContext.nodeId());
        int blockedByTotal = blockedByUserRepository.countByNodeId(requestContext.nodeId());
        return PeopleGeneralInfoUtil.build(
            subscribersTotal, subscriptionsTotal, friendsTotal, friendOfsTotal, blockedTotal, blockedByTotal,
            requestContext.getOptions(), requestContext
        );
    }

}
