package org.moera.node.operations;

import java.util.List;
import java.util.function.Function;

import org.moera.lib.node.types.PrincipalFlag;
import org.moera.node.auth.principal.Principal;
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

    public static final List<Pair<String, Integer>> FRIEND_OPERATIONS = List.of(
            Pair.of("view",
                    PrincipalFlag.PUBLIC | PrincipalFlag.SIGNED | PrincipalFlag.SUBSCRIBED | PrincipalFlag.PRIVATE)
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

}
