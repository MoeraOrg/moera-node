package org.moera.node.model.body;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class BodyDecoded {

    private String subject;
    private String text;
    private List<LinkPreview> linkPreviews;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<LinkPreview> getLinkPreviews() {
        return linkPreviews;
    }

    public void setLinkPreviews(List<LinkPreview> linkPreviews) {
        this.linkPreviews = linkPreviews;
    }

}
