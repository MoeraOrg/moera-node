package org.moera.node.operations;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Feed;
import org.moera.node.data.SheriffMark;
import org.moera.node.data.Story;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.LiberinManager;
import org.moera.node.liberin.model.FeedSheriffDataUpdatedLiberin;
import org.moera.node.option.OptionHook;
import org.moera.node.option.OptionValueChange;
import org.moera.node.option.Options;
import org.moera.node.util.SheriffUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class FeedOperations {

    @Inject
    private RequestContext requestContext;

    @Inject
    private UniversalContext universalContext;

    @Inject
    @Lazy
    private LiberinManager liberinManager;

    public static Optional<List<String>> getFeedSheriffs(Function<String, String> optionsGetter, String feedName) {
        if (feedName == null) {
            return Optional.empty();
        }
        if (feedName.equals(Feed.TIMELINE)) {
            return SheriffUtil.deserializeSheriffs(optionsGetter.apply("sheriffs.timeline"));
        }
        return Optional.empty();
    }

    public static Optional<List<String>> getFeedSheriffs(Options options, String feedName) {
        return getFeedSheriffs(options::getString, feedName);
    }

    public Optional<List<String>> getFeedSheriffs(String feedName) {
        return getFeedSheriffs(universalContext.getOptions(), feedName);
    }

    public static List<String> getSheriffFeeds(Function<String, String> optionsGetter, String sheriffName) {
        List<String> sheriffs = getFeedSheriffs(optionsGetter, Feed.TIMELINE).orElse(null);
        if (sheriffs == null || !sheriffs.contains(sheriffName)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(Feed.TIMELINE);
    }

    public static List<String> getSheriffFeeds(Options options, String sheriffName) {
        if (options == null) {
            return Collections.emptyList();
        }
        return getSheriffFeeds(options::getString, sheriffName);
    }

    public List<String> getSheriffFeeds(String sheriffName) {
        return getSheriffFeeds(universalContext.getOptions(), sheriffName);
    }

    public boolean isFeedSheriff(String feedName) {
        return getFeedSheriffs(feedName).orElse(Collections.emptyList()).stream()
                .anyMatch(nodeName -> requestContext.isClient(nodeName, Scope.SHERIFF));
    }

    public static String getFeedSheriffMarksOption(String feedName) {
        return String.format("sheriffs.%s.marks", feedName);
    }

    public static Optional<List<SheriffMark>> getFeedSheriffMarks(Options options, String feedName) {
        return SheriffUtil.deserializeSheriffMarks(options.getString(getFeedSheriffMarksOption(feedName)));
    }

    public List<String> getAllPossibleSheriffs() {
        return getFeedSheriffs(Feed.TIMELINE).orElse(Collections.emptyList());
    }

    public boolean isSheriffAllowed(List<Story> stories, Principal principal) {
        return isSheriffAllowed(() -> stories, principal);
    }

    public boolean isSheriffAllowed(Supplier<List<Story>> storiesSupplier, Principal principal) {
        // This method is called after a regular access check and checks for sheriff permissions that override
        // the regular ones. In any other case, it should return false.
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
                .map(feedName -> getFeedSheriffs(feedName).orElse(Collections.emptyList()))
                .flatMap(Collection::stream)
                .anyMatch(nodeName -> requestContext.isClient(nodeName, Scope.SHERIFF));
    }

    @OptionHook({"sheriffs.timeline", "sheriffs.timeline.marks"})
    public void timelineSheriffDataChanged(OptionValueChange change) {
        universalContext.associate(change.getNodeId());
        liberinManager.send(new FeedSheriffDataUpdatedLiberin(Feed.TIMELINE, universalContext.getOptions())
                .withNodeId(change.getNodeId()));
    }

}
