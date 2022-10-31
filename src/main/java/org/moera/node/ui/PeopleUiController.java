package org.moera.node.ui;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;

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
                sr -> sr.getRemoteFullName() != null ? sr.getRemoteFullName() : NodeName.shorten(sr.getRemoteNodeName()));
        List<SubscriberInfo> subscribers = Collections.emptyList();
        if (Subscriber.getViewAllE(requestContext.getOptions()).isPublic()) {
            subscribers = subscriberRepository.findAllByType(requestContext.nodeId(), SubscriptionType.FEED).stream()
                    .sorted(comparator)
                    .filter(s -> requestContext.isPrincipal(s.getViewE()))
                    .map(s -> new SubscriberInfo(s, requestContext))
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
                sr -> sr.getRemoteFullName() != null ? sr.getRemoteFullName() : NodeName.shorten(sr.getRemoteNodeName()));
        List<SubscriptionInfo> subscriptions = Collections.emptyList();
        if (UserSubscription.getViewAllE(requestContext.getOptions()).isPublic()) {
            subscriptions = userSubscriptionRepository.findAllByType(requestContext.nodeId(), SubscriptionType.FEED)
                    .stream()
                    .sorted(comparator)
                    .filter(s -> requestContext.isPrincipal(s.getViewE()))
                    .map(SubscriptionInfo::new)
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
        return new PeopleGeneralInfo(subscribersTotal, subscriptionsTotal, requestContext.getOptions(), requestContext);
    }

}
