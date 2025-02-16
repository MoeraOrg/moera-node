package org.moera.node.model;

import java.util.Map;

import org.moera.lib.node.types.PeopleGeneralInfo;
import org.moera.lib.node.types.PeopleOperations;
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

public class PeopleGeneralInfoUtil {

    public static PeopleGeneralInfo build(
        Integer feedSubscribersTotal,
        Integer feedSubscriptionsTotal,
        Map<String, Integer> friendsTotal,
        Integer friendOfsTotal,
        Integer blockedTotal,
        Integer blockedByTotal,
        Options options,
        AccessChecker accessChecker
    ) {
        PeopleGeneralInfo info = new PeopleGeneralInfo();
        
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

        if (
            accessChecker.isPrincipal(viewSubscribers, Scope.VIEW_PEOPLE)
            || accessChecker.isPrincipal(viewSubscribersTotal, Scope.VIEW_PEOPLE)
        ) {
            info.setFeedSubscribersTotal(feedSubscribersTotal);
        }
        
        if (
            accessChecker.isPrincipal(viewSubscriptions, Scope.VIEW_PEOPLE)
            || accessChecker.isPrincipal(viewSubscriptionsTotal, Scope.VIEW_PEOPLE)
        ) {
            info.setFeedSubscriptionsTotal(feedSubscriptionsTotal);
        }
        
        if (
            accessChecker.isPrincipal(viewFriends, Scope.VIEW_PEOPLE)
            || accessChecker.isPrincipal(viewFriendsTotal, Scope.VIEW_PEOPLE)
        ) {
            info.setFriendsTotal(friendsTotal);
        }
        
        if (
            accessChecker.isPrincipal(viewFriendOfs, Scope.VIEW_PEOPLE)
            || accessChecker.isPrincipal(viewFriendOfsTotal, Scope.VIEW_PEOPLE)
        ) {
            info.setFriendOfsTotal(friendOfsTotal);
        }
        
        if (accessChecker.isPrincipal(viewBlocked, Scope.VIEW_PEOPLE)) {
            info.setBlockedTotal(blockedTotal);
        }
        
        if (accessChecker.isPrincipal(viewBlockedBy, Scope.VIEW_PEOPLE)) {
            info.setBlockedByTotal(blockedByTotal);
        }

        PeopleOperations operations = new PeopleOperations();
        operations.setViewSubscribers(viewSubscribers, Principal.PUBLIC);
        operations.setViewSubscriptions(viewSubscriptions, Principal.PUBLIC);
        operations.setViewFriends(viewFriends, Principal.PUBLIC);
        operations.setViewFriendOfs(viewFriendOfs, Principal.PUBLIC);
        operations.setViewBlocked(viewBlocked, Principal.PUBLIC);
        operations.setViewBlockedBy(viewBlockedBy, Principal.ADMIN);
        operations.setViewSubscribersTotal(viewSubscribersTotal, Principal.PUBLIC);
        operations.setViewSubscriptionsTotal(viewSubscriptionsTotal, Principal.PUBLIC);
        operations.setViewFriendsTotal(viewFriendsTotal, Principal.PUBLIC);
        operations.setViewFriendOfsTotal(viewFriendOfsTotal, Principal.PUBLIC);
        info.setOperations(operations);
        
        return info;
    }

}
