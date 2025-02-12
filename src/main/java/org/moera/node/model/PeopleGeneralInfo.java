package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.BlockedByUser;
import org.moera.node.data.BlockedUser;
import org.moera.node.data.Friend;
import org.moera.node.data.FriendOf;
import org.moera.node.data.Subscriber;
import org.moera.node.data.UserSubscription;
import org.moera.node.option.Options;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PeopleGeneralInfo {

    private Integer feedSubscribersTotal;
    private Integer feedSubscriptionsTotal;
    private Map<String, Integer> friendsTotal;
    private Integer friendOfsTotal;
    private Integer blockedTotal;
    private Integer blockedByTotal;
    private Map<String, Principal> operations;

    public PeopleGeneralInfo() {
    }

    public PeopleGeneralInfo(
        Integer feedSubscribersTotal, Integer feedSubscriptionsTotal, Map<String, Integer> friendsTotal,
        Integer friendOfsTotal, Integer blockedTotal, Integer blockedByTotal, Options options,
        AccessChecker accessChecker
    ) {
        Principal viewSubscribers = Subscriber.getViewAllE(options);
        Principal viewSubscriptions = UserSubscription.getViewAllE(options);
        Principal viewFriends = Friend.getViewAllE(options);
        Principal viewFriendOfs = FriendOf.getViewAllE(options);
        Principal viewBlocked = BlockedUser.getViewAllE(options);
        Principal viewBlockedBy = BlockedByUser.getViewAllE(options);
        Principal viewSubscribersTotal = Subscriber.getViewTotalE(options);
        Principal viewSubscriptionsTotal = UserSubscription.getViewTotalE(options);
        Principal viewFriendsTotal = Friend.getViewTotalE(options);
        Principal viewFriendOfsTotal = FriendOf.getViewTotalE(options);

        this.feedSubscribersTotal = accessChecker.isPrincipal(viewSubscribers, Scope.VIEW_PEOPLE)
                || accessChecker.isPrincipal(viewSubscribersTotal, Scope.VIEW_PEOPLE) ? feedSubscribersTotal : null;
        this.feedSubscriptionsTotal = accessChecker.isPrincipal(viewSubscriptions, Scope.VIEW_PEOPLE)
                || accessChecker.isPrincipal(viewSubscriptionsTotal, Scope.VIEW_PEOPLE) ? feedSubscriptionsTotal : null;
        this.friendsTotal = accessChecker.isPrincipal(viewFriends, Scope.VIEW_PEOPLE)
                || accessChecker.isPrincipal(viewFriendsTotal, Scope.VIEW_PEOPLE) ? friendsTotal : null;
        this.friendOfsTotal = accessChecker.isPrincipal(viewFriendOfs, Scope.VIEW_PEOPLE)
                || accessChecker.isPrincipal(viewFriendOfsTotal, Scope.VIEW_PEOPLE) ? friendOfsTotal : null;
        this.blockedTotal = accessChecker.isPrincipal(viewBlocked, Scope.VIEW_PEOPLE) ? blockedTotal : null;
        this.blockedByTotal = accessChecker.isPrincipal(viewBlockedBy, Scope.VIEW_PEOPLE) ? blockedByTotal : null;
        operations = new HashMap<>();
        putOperation(operations, "viewSubscribers", viewSubscribers, Principal.PUBLIC);
        putOperation(operations, "viewSubscriptions", viewSubscriptions, Principal.PUBLIC);
        putOperation(operations, "viewFriends", viewFriends, Principal.PUBLIC);
        putOperation(operations, "viewFriendOfs", viewFriendOfs, Principal.PUBLIC);
        putOperation(operations, "viewBlocked", viewBlocked, Principal.PUBLIC);
        putOperation(operations, "viewBlockedBy", viewBlockedBy, Principal.ADMIN);
        putOperation(operations, "viewSubscribersTotal", viewSubscribersTotal, Principal.PUBLIC);
        putOperation(operations, "viewSubscriptionsTotal", viewSubscriptionsTotal, Principal.PUBLIC);
        putOperation(operations, "viewFriendsTotal", viewSubscriptionsTotal, Principal.PUBLIC);
        putOperation(operations, "viewFriendOfsTotal", viewSubscriptionsTotal, Principal.PUBLIC);
    }

    private static void putOperation(Map<String, Principal> operations, String operationName, Principal value,
                                     Principal defaultValue) {
        if (value != null && !value.equals(defaultValue)) {
            operations.put(operationName, value);
        }
    }

    public Integer getFeedSubscribersTotal() {
        return feedSubscribersTotal;
    }

    public void setFeedSubscribersTotal(Integer feedSubscribersTotal) {
        this.feedSubscribersTotal = feedSubscribersTotal;
    }

    public Integer getFeedSubscriptionsTotal() {
        return feedSubscriptionsTotal;
    }

    public void setFeedSubscriptionsTotal(Integer feedSubscriptionsTotal) {
        this.feedSubscriptionsTotal = feedSubscriptionsTotal;
    }

    public Map<String, Integer> getFriendsTotal() {
        return friendsTotal;
    }

    public void setFriendsTotal(Map<String, Integer> friendsTotal) {
        this.friendsTotal = friendsTotal;
    }

    public Integer getFriendOfsTotal() {
        return friendOfsTotal;
    }

    public void setFriendOfsTotal(Integer friendOfsTotal) {
        this.friendOfsTotal = friendOfsTotal;
    }

    public Integer getBlockedTotal() {
        return blockedTotal;
    }

    public void setBlockedTotal(Integer blockedTotal) {
        this.blockedTotal = blockedTotal;
    }

    public Integer getBlockedByTotal() {
        return blockedByTotal;
    }

    public void setBlockedByTotal(Integer blockedByTotal) {
        this.blockedByTotal = blockedByTotal;
    }

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

}
