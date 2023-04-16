package org.moera.node.liberin.model;

import java.util.List;
import java.util.Map;

import org.moera.node.data.SheriffMark;
import org.moera.node.liberin.Liberin;
import org.moera.node.operations.FeedOperations;

public class FeedSheriffDataUpdatedLiberin extends Liberin {

    private String feedName;
    private List<String> sheriffs;
    private SheriffMark[] sheriffMarks;

    public FeedSheriffDataUpdatedLiberin(String feedName, FeedOperations feedOperations) {
        this.feedName = feedName;
        sheriffs = feedOperations.getFeedSheriffs(feedName);
        sheriffMarks = feedOperations.getFeedSheriffMarks(feedName);
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

    public SheriffMark[] getSheriffMarks() {
        return sheriffMarks;
    }

    public void setSheriffMarks(SheriffMark[] sheriffMarks) {
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
