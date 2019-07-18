package org.moera.node.model;

import java.util.List;

import org.moera.node.data.SourceFormat;

public final class PostingFeatures {

    public static final PostingFeatures INSTANCE = new PostingFeatures();

    private List<Choice> sourceFormats = Choice.forEnum(SourceFormat.class);

    public List<Choice> getSourceFormats() {
        return sourceFormats;
    }

}
