package org.moera.node.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.auth.principal.Principal;
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

    public Features(Options options, List<String> plugins, FriendGroup[] nodeGroups, Friend[] clientGroups,
                    AccessChecker accessChecker, List<AskSubject> ask) {
        posting = new PostingFeatures(options);
        if (!ObjectUtils.isEmpty(plugins)) {
            this.plugins = plugins;
        }
        feedWidth = options.getInt("feed.width");
        friendGroups = accessChecker.isPrincipal(Principal.ADMIN)
                ? FriendGroupsFeatures.forAdmin(nodeGroups)
                : FriendGroupsFeatures.forRegular(nodeGroups, clientGroups);
        this.ask = ask;
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

}
