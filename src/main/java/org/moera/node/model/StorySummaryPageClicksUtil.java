package org.moera.node.model;

import org.moera.lib.node.types.StorySummaryPageClicks;

public class StorySummaryPageClicksUtil {

    public static StorySummaryPageClicks build(String heading, String href, int clicks) {
        StorySummaryPageClicks instance = new StorySummaryPageClicks();
        instance.setHeading(heading);
        instance.setHref(href);
        instance.setClicks(clicks);
        return instance;
    }

}
