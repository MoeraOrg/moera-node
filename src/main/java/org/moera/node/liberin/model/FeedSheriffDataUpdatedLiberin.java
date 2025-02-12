package org.moera.node.liberin.model;

import java.util.List;
import java.util.Map;

import org.moera.lib.node.types.SheriffMark;
import org.moera.node.liberin.Liberin;
import org.moera.node.operations.FeedOperations;
import org.moera.node.option.Options;

public class FeedSheriffDataUpdatedLiberin extends Liberin {

    private String feedName;
    private List<String> sheriffs;
    private List<SheriffMark> sheriffMarks;

    public FeedSheriffDataUpdatedLiberin(String feedName, Options options) {
        this.feedName = feedName;
        sheriffs = FeedOperations.getFeedSheriffs(options, feedName).orElse(null);
        sheriffMarks = FeedOperations.getFeedSheriffMarks(options, feedName).orElse(null);
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public List<String> getSheriffs() {
        return sheriffs;
    }

    public void setSheriffs(List<String> sheriffs) {
        this.sheriffs = sheriffs;
    }

    public List<SheriffMark> getSheriffMarks() {
        return sheriffMarks;
    }

    public void setSheriffMarks(List<SheriffMark> sheriffMarks) {
        this.sheriffMarks = sheriffMarks;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("feedName", feedName);
        model.put("sheriffs", sheriffs);
        model.put("sheriffMarks", sheriffMarks);
    }

}
