package org.moera.node.model;

import java.util.List;

import org.moera.node.data.SourceFormat;
import org.moera.node.option.Options;

public class PostingFeatures {

    private boolean subjectPresent;
    private List<Choice> sourceFormats = Choice.forEnum(SourceFormat.class);

    public PostingFeatures() {
    }

    public PostingFeatures(Options options) {
        subjectPresent = options.getBool("posting.subject.present");
    }

    public boolean isSubjectPresent() {
        return subjectPresent;
    }

    public void setSubjectPresent(boolean subjectPresent) {
        this.subjectPresent = subjectPresent;
    }

    public List<Choice> getSourceFormats() {
        return sourceFormats;
    }

}
