package org.moera.node.operations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.data.Feed;
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

}
