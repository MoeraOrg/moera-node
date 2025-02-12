package org.moera.node.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.AskSubject;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Friend;
import org.moera.node.data.FriendGroup;
import org.moera.node.option.Options;
import org.springframework.util.ObjectUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Features {

    private PostingFeatures posting;
    private List<String> plugins;
    private int feedWidth;
    private FriendGroupsFeatures friendGroups;
    private List<AskSubject> ask;
    private boolean subscribed;

    public Features(Options options, List<String> plugins, FriendGroup[] nodeGroups, Friend[] clientGroups,
                    AccessChecker accessChecker, List<AskSubject> ask, boolean subscribed) {
        posting = new PostingFeatures(options, accessChecker);
        if (!ObjectUtils.isEmpty(plugins)) {
            this.plugins = plugins;
        }
        feedWidth = options.getInt("feed.width");
        friendGroups = accessChecker.isPrincipal(Principal.ADMIN, Scope.VIEW_PEOPLE)
                ? FriendGroupsFeatures.forAdmin(nodeGroups)
                : FriendGroupsFeatures.forRegular(nodeGroups, clientGroups);
        this.ask = ask;
        this.subscribed = subscribed;
    }

    public PostingFeatures getPosting() {
        return posting;
    }

    public void setPosting(PostingFeatures posting) {
        this.posting = posting;
    }

    public List<String> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<String> plugins) {
        this.plugins = plugins;
    }

    public int getFeedWidth() {
        return feedWidth;
    }

    public void setFeedWidth(int feedWidth) {
        this.feedWidth = feedWidth;
    }

    public FriendGroupsFeatures getFriendGroups() {
        return friendGroups;
    }

    public void setFriendGroups(FriendGroupsFeatures friendGroups) {
        this.friendGroups = friendGroups;
    }

    public List<AskSubject> getAsk() {
        return ask;
    }

    public void setAsk(List<AskSubject> ask) {
        this.ask = ask;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

}
