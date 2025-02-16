package org.moera.node.operations;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.moera.lib.node.types.CommentOperations;
import org.moera.lib.node.types.FriendOperations;
import org.moera.lib.node.types.ReactionOperations;
import org.moera.lib.node.types.SubscriberOperations;
import org.moera.lib.node.types.SubscriptionOperations;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.principal.PrincipalFlag;
import org.moera.node.model.ValidationFailure;
import org.springframework.data.util.Pair;

public class OperationsValidator {

    public static final List<Pair<String, Integer>> PROFILE_OPERATIONS = List.of(
            Pair.of("viewEmail",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.ADMIN)
    );

    public static final List<Pair<String, Integer>> POSTING_OPERATIONS = List.of(
            Pair.of("view",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.PRIVATE),
            Pair.of("viewComments",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
            Pair.of("addComment",
                    PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS | PrincipalFlag.PRIVATE
                    | PrincipalFlag.NONE),
            Pair.of("overrideComment",
                    PrincipalFlag.OWNER | PrincipalFlag.NONE),
            Pair.of("viewReactions",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
            Pair.of("viewNegativeReactions",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
            Pair.of("viewReactionTotals",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
            Pair.of("viewNegativeReactionTotals",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
            Pair.of("viewReactionRatios",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
            Pair.of("viewNegativeReactionRatios",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
            Pair.of("addReaction",
                    PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS | PrincipalFlag.NONE),
            Pair.of("addNegativeReaction",
                    PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS | PrincipalFlag.NONE),
            Pair.of("overrideReaction",
                    PrincipalFlag.OWNER | PrincipalFlag.NONE)
    );

    public static final List<Pair<String, Integer>> COMMENT_OPERATIONS = List.of(
            Pair.of("view",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.SECRET | PrincipalFlag.PRIVATE),
            Pair.of("edit",
                    PrincipalFlag.OWNER | PrincipalFlag.NONE),
            Pair.of("delete",
                    PrincipalFlag.PRIVATE | PrincipalFlag.SECRET | PrincipalFlag.SENIOR | PrincipalFlag.OWNER
                    | PrincipalFlag.ADMIN | PrincipalFlag.NONE),
            Pair.of("viewReactions",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.SECRET | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
            Pair.of("viewNegativeReactions",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.SECRET | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
            Pair.of("viewReactionTotals",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.SECRET | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
            Pair.of("viewNegativeReactionTotals",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.SECRET | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
            Pair.of("viewReactionRatios",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.SECRET | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
            Pair.of("viewNegativeReactionRatios",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.SECRET | PrincipalFlag.PRIVATE | PrincipalFlag.NONE),
            Pair.of("addReaction",
                    PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS | PrincipalFlag.PRIVATE
                    | PrincipalFlag.SECRET | PrincipalFlag.SENIOR | PrincipalFlag.ADMIN | PrincipalFlag.NONE),
            Pair.of("addNegativeReaction",
                    PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS | PrincipalFlag.PRIVATE
                    | PrincipalFlag.SECRET | PrincipalFlag.SENIOR | PrincipalFlag.ADMIN | PrincipalFlag.NONE),
            Pair.of("overrideReaction",
                    PrincipalFlag.OWNER | PrincipalFlag.NONE)
    );

    public static final List<Pair<String, Integer>> POSTING_REACTION_OPERATIONS = List.of(
            Pair.of("view",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.PRIVATE | PrincipalFlag.SECRET | PrincipalFlag.NONE),
            Pair.of("delete",
                    PrincipalFlag.PRIVATE | PrincipalFlag.SECRET | PrincipalFlag.SENIOR | PrincipalFlag.OWNER
                    | PrincipalFlag.ADMIN | PrincipalFlag.NONE)
    );

    public static final List<Pair<String, Integer>> COMMENT_REACTION_OPERATIONS = List.of(
            Pair.of("view",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.PRIVATE | PrincipalFlag.SECRET | PrincipalFlag.ENIGMA | PrincipalFlag.NONE),
            Pair.of("delete",
                    PrincipalFlag.PRIVATE | PrincipalFlag.SECRET | PrincipalFlag.SENIOR | PrincipalFlag.ENIGMA
                    | PrincipalFlag.MAJOR | PrincipalFlag.OWNER | PrincipalFlag.ADMIN | PrincipalFlag.NONE)
    );

    public static final List<Pair<String, Integer>> SUBSCRIBER_OPERATIONS = List.of(
            Pair.of("view",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.PRIVATE)
    );

    public static final List<Pair<String, Integer>> SUBSCRIPTION_OPERATIONS = List.of(
            Pair.of("view",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.FRIENDS
                    | PrincipalFlag.PRIVATE)
    );

    public static final List<Pair<String, Integer>> FRIEND_GROUP_OPERATIONS = List.of(
            Pair.of("view",
                    PrincipalFlag.PUBLIC | PrincipalFlag.PRIVATE | PrincipalFlag.ADMIN)
    );

    public static void validateOperations(Function<String, Principal> getPrincipal,
                                          List<Pair<String, Integer>> description,
                                          boolean includeUnset,
                                          String errorCode) {
        for (var desc : description) {
            Principal principal = getPrincipal.apply(desc.getFirst());
            if (principal != null && !principal.isOneOf(desc.getSecond()) && (!includeUnset || !principal.isUnset())) {
                throw new ValidationFailure(errorCode);
            }
        }
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

    private static void validatePrincipal(Principal principal, int flags, boolean includeUnset, String errorCode) {
        if (principal != null && !principal.isOneOf(flags) && (!includeUnset || !principal.isUnset())) {
            throw new ValidationFailure(errorCode);
        }
    }

}
