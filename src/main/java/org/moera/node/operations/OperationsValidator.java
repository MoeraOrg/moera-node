package org.moera.node.operations;

import java.util.function.BiConsumer;

import org.moera.lib.node.types.CommentOperations;
import org.moera.lib.node.types.FriendGroupOperations;
import org.moera.lib.node.types.FriendOperations;
import org.moera.lib.node.types.PostingOperations;
import org.moera.lib.node.types.ProfileOperations;
import org.moera.lib.node.types.ReactionOperations;
import org.moera.lib.node.types.SubscriberOperations;
import org.moera.lib.node.types.SubscriptionOperations;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.principal.PrincipalFlag;
import org.moera.lib.node.types.validate.ValidationUtil;

public class OperationsValidator {

    public static void validateOperations(ProfileOperations operations, boolean includeUnset, String errorCode) {
        if (operations == null) {
            return;
        }

        BiConsumer<Principal, Integer> v = (principal, flags) ->
            validatePrincipal(principal, flags, includeUnset, errorCode);

        v.accept(
            operations.getEdit(),
            PrincipalFlag.ADMIN
        );
        v.accept(
            operations.getViewEmail(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.ADMIN
        );
    }

    public static void validateOperations(PostingOperations operations, boolean includeUnset, String errorCode) {
        if (operations == null) {
            return;
        }

        BiConsumer<Principal, Integer> v = (principal, flags) ->
            validatePrincipal(principal, flags, includeUnset, errorCode);

        v.accept(
            operations.getView(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.PRIVATE
        );
        v.accept(
            operations.getViewComments(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.PRIVATE | PrincipalFlag.NONE
        );
        v.accept(
            operations.getAddComment(),
            PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS | PrincipalFlag.PRIVATE
            | PrincipalFlag.NONE
        );
        v.accept(
            operations.getOverrideComment(),
            PrincipalFlag.OWNER | PrincipalFlag.NONE
        );
        v.accept(
            operations.getViewReactions(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.PRIVATE | PrincipalFlag.NONE
        );
        v.accept(
            operations.getViewNegativeReactions(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.PRIVATE | PrincipalFlag.NONE
        );
        v.accept(
            operations.getViewReactionTotals(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.PRIVATE | PrincipalFlag.NONE
        );
        v.accept(
            operations.getViewNegativeReactionTotals(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.PRIVATE | PrincipalFlag.NONE
        );
        v.accept(
            operations.getViewReactionRatios(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.PRIVATE | PrincipalFlag.NONE
        );
        v.accept(
            operations.getViewNegativeReactionRatios(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.PRIVATE | PrincipalFlag.NONE
        );
        v.accept(
            operations.getAddReaction(),
            PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS | PrincipalFlag.NONE
        );
        v.accept(
            operations.getAddNegativeReaction(),
            PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS | PrincipalFlag.NONE
        );
        v.accept(
            operations.getOverrideReaction(),
            PrincipalFlag.OWNER | PrincipalFlag.NONE
        );
    }

    public static void validateOperations(CommentOperations operations, boolean includeUnset, String errorCode) {
        if (operations == null) {
            return;
        }

        BiConsumer<Principal, Integer> v = (principal, flags) ->
            validatePrincipal(principal, flags, includeUnset, errorCode);

        v.accept(
            operations.getView(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.SECRET | PrincipalFlag.PRIVATE
        );
        v.accept(
            operations.getEdit(),
            PrincipalFlag.OWNER | PrincipalFlag.NONE
        );
        v.accept(
            operations.getDelete(),
            PrincipalFlag.PRIVATE | PrincipalFlag.SECRET | PrincipalFlag.SENIOR | PrincipalFlag.OWNER
            | PrincipalFlag.ADMIN | PrincipalFlag.NONE
        );
        v.accept(
            operations.getViewReactions(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.SECRET | PrincipalFlag.PRIVATE | PrincipalFlag.NONE
        );
        v.accept(
            operations.getViewNegativeReactions(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.SECRET | PrincipalFlag.PRIVATE | PrincipalFlag.NONE
        );
        v.accept(
            operations.getViewReactionTotals(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.SECRET | PrincipalFlag.PRIVATE | PrincipalFlag.NONE
        );
        v.accept(
            operations.getViewNegativeReactionTotals(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.SECRET | PrincipalFlag.PRIVATE | PrincipalFlag.NONE
        );
        v.accept(
            operations.getViewReactionRatios(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.SECRET | PrincipalFlag.PRIVATE | PrincipalFlag.NONE
        );
        v.accept(
            operations.getViewNegativeReactionRatios(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.SECRET | PrincipalFlag.PRIVATE | PrincipalFlag.NONE
        );
        v.accept(
            operations.getAddReaction(),
            PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS | PrincipalFlag.PRIVATE
            | PrincipalFlag.SECRET | PrincipalFlag.SENIOR | PrincipalFlag.ADMIN | PrincipalFlag.NONE
        );
        v.accept(
            operations.getAddNegativeReaction(),
            PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS | PrincipalFlag.PRIVATE
            | PrincipalFlag.SECRET | PrincipalFlag.SENIOR | PrincipalFlag.ADMIN | PrincipalFlag.NONE
        );
        v.accept(
            operations.getOverrideReaction(),
            PrincipalFlag.OWNER | PrincipalFlag.NONE
        );
    }

    public static void validateOperations(
        boolean isComment, ReactionOperations operations, boolean includeUnset, String errorCode
    ) {
        if (operations == null) {
            return;
        }

        BiConsumer<Principal, Integer> v = (principal, flags) ->
            validatePrincipal(principal, flags, includeUnset, errorCode);

        if (!isComment) {
            // posting reactions
            v.accept(
                operations.getView(),
                PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                | PrincipalFlag.PRIVATE | PrincipalFlag.SECRET | PrincipalFlag.NONE
            );
            v.accept(
                operations.getDelete(),
                PrincipalFlag.PRIVATE | PrincipalFlag.SECRET | PrincipalFlag.SENIOR | PrincipalFlag.OWNER
                | PrincipalFlag.ADMIN | PrincipalFlag.NONE
            );
        } else {
            // comment reactions
            v.accept(
                operations.getView(),
                PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                | PrincipalFlag.PRIVATE | PrincipalFlag.SECRET | PrincipalFlag.ENIGMA | PrincipalFlag.NONE
            );
            v.accept(
                operations.getDelete(),
                PrincipalFlag.PRIVATE | PrincipalFlag.SECRET | PrincipalFlag.SENIOR | PrincipalFlag.ENIGMA
                | PrincipalFlag.MAJOR | PrincipalFlag.OWNER | PrincipalFlag.ADMIN | PrincipalFlag.NONE
            );
        }
    }

    public static void validateOperations(SubscriberOperations operations, boolean includeUnset, String errorCode) {
        if (operations == null) {
            return;
        }

        BiConsumer<Principal, Integer> v = (principal, flags) ->
            validatePrincipal(principal, flags, includeUnset, errorCode);

        v.accept(
            operations.getView(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.PRIVATE
        );
    }

    public static void validateOperations(SubscriptionOperations operations, boolean includeUnset, String errorCode) {
        if (operations == null) {
            return;
        }

        BiConsumer<Principal, Integer> v = (principal, flags) ->
            validatePrincipal(principal, flags, includeUnset, errorCode);

        v.accept(
            operations.getView(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
            | PrincipalFlag.PRIVATE
        );
    }

    public static void validateOperations(FriendOperations operations, boolean includeUnset, String errorCode) {
        if (operations == null) {
            return;
        }

        BiConsumer<Principal, Integer> v = (principal, flags) ->
            validatePrincipal(principal, flags, includeUnset, errorCode);

        v.accept(
            operations.getView(),
            PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.PRIVATE
        );
    }

    public static void validateOperations(FriendGroupOperations operations, boolean includeUnset, String errorCode) {
        if (operations == null) {
            return;
        }

        BiConsumer<Principal, Integer> v = (principal, flags) ->
            validatePrincipal(principal, flags, includeUnset, errorCode);

        v.accept(
            operations.getView(),
            PrincipalFlag.PUBLIC | PrincipalFlag.PRIVATE | PrincipalFlag.ADMIN
        );
    }

    private static void validatePrincipal(Principal principal, int flags, boolean includeUnset, String errorCode) {
        ValidationUtil.assertion(
            principal == null || principal.isOneOf(flags) || includeUnset && principal.isUnset(),
            errorCode
        );
    }

}
