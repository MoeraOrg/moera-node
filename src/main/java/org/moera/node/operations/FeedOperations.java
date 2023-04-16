package org.moera.node.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Feed;
import org.moera.node.data.SheriffMark;
import org.moera.node.data.Story;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.LiberinManager;
import org.moera.node.liberin.model.FeedSheriffDataUpdatedLiberin;
import org.moera.node.model.FeedInfo;
import org.moera.node.model.FeedReference;
import org.moera.node.model.PostingInfo;
import org.moera.node.option.OptionHook;
import org.moera.node.option.OptionValueChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class FeedOperations {

    private static final Logger log = LoggerFactory.getLogger(FeedOperations.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private LiberinManager liberinManager;

    public List<String> getFeedSheriffs(String feedName) {
        if (feedName.equals(Feed.TIMELINE)) {
            String sheriffs = universalContext.getOptions().getString("sheriffs.timeline");
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

    public String getFeedSheriffMarksOption(String feedName) {
        return String.format("sheriffs.%s.marks", feedName);
    }

    public SheriffMark[] getFeedSheriffMarks(String feedName) {
        String marks = universalContext.getOptions().getString(getFeedSheriffMarksOption(feedName));
        if (!ObjectUtils.isEmpty(marks)) {
            try {
                return objectMapper.readValue(marks, SheriffMark[].class);
            } catch (JsonProcessingException e) {
                log.error(String.format("Error deserializing feed '%s' sheriff marks", feedName), e);
            }
        }
        return null;
    }

    public List<String> getAllPossibleSheriffs() {
        return getFeedSheriffs(Feed.TIMELINE);
    }

    public void fillFeedSheriffs(FeedInfo feedInfo) {
        List<String> sheriffs = getFeedSheriffs(feedInfo.getFeedName());
        if (!sheriffs.isEmpty()) {
            feedInfo.setSheriffs(sheriffs);
        }
        feedInfo.setSheriffMarks(getFeedSheriffMarks(feedInfo.getFeedName()));
    }

    public void fillFeedSheriffs(PostingInfo postingInfo) {
        if (postingInfo == null || postingInfo.getFeedReferences() == null) {
            return;
        }
        for (FeedReference feedReference : postingInfo.getFeedReferences()) {
            fillFeedSheriffs(postingInfo, feedReference.getFeedName());
        }
    }

    public void fillFeedSheriffs(PostingInfo postingInfo, String feedName) {
        if (postingInfo == null) {
            return;
        }
        List<String> sheriffs = getFeedSheriffs(feedName);
        if (!sheriffs.isEmpty()) {
            if (postingInfo.getSheriffs() == null) {
                postingInfo.setSheriffs(new ArrayList<>());
            }
            postingInfo.getSheriffs().addAll(sheriffs);
        }
        SheriffMark[] sheriffMarks = getFeedSheriffMarks(feedName);
        if (!ObjectUtils.isEmpty(sheriffMarks)) {
            if (postingInfo.getSheriffMarks() == null) {
                postingInfo.setSheriffMarks(new ArrayList<>());
            }
            Collections.addAll(postingInfo.getSheriffMarks(), sheriffMarks);
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

    @OptionHook({"sheriffs.timeline", "sheriffs.timeline.marks"})
    public void timelineSheriffDataChanged(OptionValueChange change) {
        universalContext.associate(change.getNodeId());
        liberinManager.send(new FeedSheriffDataUpdatedLiberin(Feed.TIMELINE, this)
                .withNodeId(change.getNodeId()));
    }

}
