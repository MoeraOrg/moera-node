package org.moera.node.ui;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.FriendOfRepository;
import org.moera.node.data.FriendRepository;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UserSubscription;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UiController;
import org.moera.node.global.VirtualPage;
import org.moera.node.model.PeopleGeneralInfo;
import org.moera.node.model.SubscriberInfo;
import org.moera.node.model.SubscriptionInfo;
import org.moera.node.naming.NodeName;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@UiController
public class PeopleUiController {

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

    @GetMapping("/people")
    public String people() {
        return "redirect:/people/subscribers";
    }

    @GetMapping("/people/subscribers")
    @VirtualPage
    @Transactional
    public String subscribers(Model model) {
        PeopleGeneralInfo totals = getTotals();
        Comparator<Subscriber> comparator = Comparator.comparing(
                sr -> sr.getContact().getRemoteFullName() != null
                        ? sr.getContact().getRemoteFullName()
                        : NodeName.shorten(sr.getRemoteNodeName()));
        List<SubscriberInfo> subscribers = Collections.emptyList();
        if (Subscriber.getViewAllE(requestContext.getOptions()).isPublic()) {
            subscribers = subscriberRepository.findAllByType(requestContext.nodeId(), SubscriptionType.FEED).stream()
                    .sorted(comparator)
                    .filter(s -> requestContext.isPrincipal(s.getViewE()))
                    .map(s -> new SubscriberInfo(s, requestContext.getOptions(), requestContext))
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

    @GetMapping("/people/subscriptions")
    @VirtualPage
    @Transactional
    public String subscriptions(Model model) {
        PeopleGeneralInfo totals = getTotals();
        Comparator<UserSubscription> comparator = Comparator.comparing(
                sr -> sr.getContact().getRemoteFullName() != null
                        ? sr.getContact().getRemoteFullName()
                        : NodeName.shorten(sr.getRemoteNodeName()));
        List<SubscriptionInfo> subscriptions = Collections.emptyList();
        if (UserSubscription.getViewAllE(requestContext.getOptions()).isPublic()) {
            subscriptions = userSubscriptionRepository.findAllByType(requestContext.nodeId(), SubscriptionType.FEED)
                    .stream()
                    .sorted(comparator)
                    .filter(s -> requestContext.isPrincipal(s.getViewE()))
                    .map(s -> new SubscriptionInfo(s, requestContext.getOptions(), requestContext))
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
        return new PeopleGeneralInfo(subscribersTotal, subscriptionsTotal, friendsTotal, friendOfsTotal,
                requestContext.getOptions(), requestContext);
    }

}
