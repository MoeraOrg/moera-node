package org.moera.node.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.option.Options;
import org.springframework.util.ObjectUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Features {

    private PostingFeatures posting;
    private List<String> plugins;
    private int feedWidth;

    public Features(Options options, List<String> plugins) {
        posting = new PostingFeatures(options);
        if (!ObjectUtils.isEmpty(plugins)) {
            this.plugins = plugins;
        }
        feedWidth = options.getInt("feed.width");
    }

    public PostingFeatures getPosting() {
        return posting;
    }

    public void setPosting(PostingFeatures posting) {
        this.posting = posting;
    }

    public List<String> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<String> plugins) {
        this.plugins = plugins;
    }

    public int getFeedWidth() {
        return feedWidth;
    }

    public void setFeedWidth(int feedWidth) {
        this.feedWidth = feedWidth;
    }

}
