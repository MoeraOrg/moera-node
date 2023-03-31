package org.moera.node.operations;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.global.RequestContext;
import org.moera.node.model.FeedInfo;
import org.moera.node.model.FeedReference;
import org.moera.node.model.PostingInfo;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class FeedOperations {

    @Inject
    private RequestContext requestContext;

    public List<String> getFeedSheriffs(String feedName) {
        if (feedName.equals(Feed.TIMELINE)) {
            String sheriffs = requestContext.getOptions().getString("sheriffs.timeline");
            if (ObjectUtils.isEmpty(sheriffs)) {
                return Collections.emptyList();
            }
            return Arrays.stream(sheriffs.split(","))
                    .map(String::strip)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public boolean isFeedSheriff(String feedName) {
        return getFeedSheriffs(feedName).stream().anyMatch(requestContext::isClient);
    }

    public List<String> getAllPossibleSheriffs() {
        return getFeedSheriffs(Feed.TIMELINE);
    }

    public void fillFeedSheriffs(FeedInfo feedInfo) {
        List<String> sheriffs = getFeedSheriffs(feedInfo.getFeedName());
        if (!sheriffs.isEmpty()) {
            feedInfo.setSheriffs(sheriffs);
        }
    }

    public void fillFeedSheriffs(PostingInfo postingInfo) {
        for (FeedReference feedReference : postingInfo.getFeedReferences()) {
            List<String> sheriffs = getFeedSheriffs(feedReference.getFeedName());
            if (!sheriffs.isEmpty()) {
                feedReference.setSheriffs(sheriffs);
            }
        }
    }

    public boolean isSheriffAllowed(List<Story> stories, Principal principal) {
        return isSheriffAllowed(() -> stories, principal);
    }

    public boolean isSheriffAllowed(Supplier<List<Story>> storiesSupplier, Principal principal) {
        // This method is called after a regular access check and checks for sheriff permissions that override
        // the regular ones. In any other case it should return false.
        if (!principal.isFriends() && !principal.isSubscribed()) {
            return false;
        }
        return isSheriff(storiesSupplier);
    }

    public boolean isSheriff(List<Story> stories) {
        return isSheriff(() -> stories);
    }

    public boolean isSheriff(Supplier<List<Story>> storiesSupplier) {
        // This method should be used if object permission is not known at the moment of executing the method. Otherwise
        // isSheriffAllowed() method is faster.
        if (!requestContext.isPossibleSheriff()) {
            return false;
        }
        List<Story> stories = storiesSupplier.get();
        if (ObjectUtils.isEmpty(stories)) {
            return false;
        }
        Set<String> feedNames = stories.stream().map(Story::getFeedName).collect(Collectors.toSet());
        return feedNames.stream()
                .map(this::getFeedSheriffs)
                .flatMap(Collection::stream)
                .anyMatch(requestContext::isClient);
    }

}
