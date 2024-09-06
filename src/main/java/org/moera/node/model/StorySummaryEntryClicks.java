package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorySummaryEntryClicks {

    private String heading;
    private String href;
    private long clicks;

    public StorySummaryEntryClicks() {
    }

    public StorySummaryEntryClicks(String heading, String href, long clicks) {
        this.heading = heading;
        this.href = href;
        this.clicks = clicks;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public long getClicks() {
        return clicks;
    }

    public void setClicks(long clicks) {
        this.clicks = clicks;
    }

}
