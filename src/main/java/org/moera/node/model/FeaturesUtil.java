package org.moera.node.model;

import java.util.List;

import org.moera.lib.node.types.AskSubject;
import org.moera.lib.node.types.Features;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Friend;
import org.moera.node.data.FriendGroup;
import org.moera.node.option.Options;
import org.springframework.util.ObjectUtils;

public class FeaturesUtil {

    public static Features build(
        Options options,
        List<String> plugins,
        FriendGroup[] nodeGroups,
        Friend[] clientGroups,
        AccessChecker accessChecker,
        List<AskSubject> ask,
        boolean subscribed
    ) {
        Features features = new Features();
        features.setPosting(PostingFeaturesUtil.build(options, accessChecker));
        if (!ObjectUtils.isEmpty(plugins)) {
            features.setPlugins(plugins);
        }
        features.setFeedWidth(options.getInt("feed.width"));
        features.setFriendGroups(accessChecker.isPrincipal(Principal.ADMIN, Scope.VIEW_PEOPLE)
            ? FriendGroupsFeaturesUtil.forAdmin(nodeGroups)
            : FriendGroupsFeaturesUtil.forRegular(nodeGroups, clientGroups));
        features.setAsk(ask);
        features.setSubscribed(subscribed);
        return features;
    }

}
