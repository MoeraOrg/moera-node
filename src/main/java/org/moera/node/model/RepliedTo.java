package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Comment;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepliedTo {

    private String id;
    private String name;
    private String heading;

    public RepliedTo() {
    }

    public RepliedTo(Comment comment) {
        if (comment.getRepliedTo() != null) {
            id = comment.getRepliedTo().getId().toString();
            name = comment.getRepliedToName();
            heading = comment.getRepliedToHeading();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

}
