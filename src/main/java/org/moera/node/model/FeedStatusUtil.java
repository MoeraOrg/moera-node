package org.moera.node.model;

import org.moera.lib.node.types.FeedStatus;

public class FeedStatusUtil {

    public static FeedStatus build(
        int total,
        int totalPinned,
        Long lastMoment,
        Integer notViewed,
        Integer notRead,
        Long notViewedMoment,
        Long notReadMoment
    ) {
        FeedStatus feedStatus = new FeedStatus();
        feedStatus.setTotal(total);
        feedStatus.setTotalPinned(totalPinned);
        feedStatus.setLastMoment(lastMoment);
        feedStatus.setNotViewed(notViewed);
        feedStatus.setNotRead(notRead);
        feedStatus.setNotViewedMoment(notViewedMoment);
        feedStatus.setNotReadMoment(notReadMoment);
        return feedStatus;
    }

    public static FeedStatus build(int total, int totalPinned, Long lastMoment) {
        FeedStatus feedStatus = new FeedStatus();
        feedStatus.setTotal(total);
        feedStatus.setTotalPinned(totalPinned);
        feedStatus.setLastMoment(lastMoment);
        return feedStatus;
    }

    public static FeedStatus notAdmin(FeedStatus status) {
        return build(status.getTotal(), status.getTotalPinned(), status.getLastMoment());
    }

}
