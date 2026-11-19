package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.util.LogUtil;
import org.moera.node.data.Posting;

public class PostingRecommendationUpdatedEvent extends PostingEvent {

    private boolean recommended;

    public PostingRecommendationUpdatedEvent() {
        super(EventType.POSTING_RECOMMENDATION_UPDATED);
    }

    public PostingRecommendationUpdatedEvent(Posting posting) {
        super(EventType.POSTING_RECOMMENDATION_UPDATED, posting, posting.getViewE());
        recommended = posting.isRecommended();
    }

    public boolean isRecommended() {
        return recommended;
    }

    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }

    @Override
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("recommended", LogUtil.format(recommended)));
    }

}
