package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Comment;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepliedTo {

    private String id;
    private String revisionId;
    private String name;
    private String fullName;
    private String gender;
    private AvatarImage avatar;
    private String heading;
    private byte[] digest;

    public RepliedTo() {
    }

    public RepliedTo(Comment comment) {
        if (comment.getRepliedTo() != null) {
            id = comment.getRepliedTo().getId().toString();
            if (comment.getRepliedToRevision() != null) {
                revisionId = comment.getRepliedToRevision().getId().toString();
            }
            name = comment.getRepliedToName();
            fullName = comment.getRepliedToFullName();
            gender = comment.getRepliedToGender();
            if (comment.getRepliedToAvatarMediaFile() != null) {
                avatar = new AvatarImage(comment.getRepliedToAvatarMediaFile(), comment.getRepliedToAvatarShape());
            }
            heading = comment.getRepliedToHeading();
            digest = comment.getRepliedToDigest();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(String revisionId) {
        this.revisionId = revisionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public AvatarImage getAvatar() {
        return avatar;
    }

    public void setAvatar(AvatarImage avatar) {
        this.avatar = avatar;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public byte[] getDigest() {
        return digest;
    }

    public void setDigest(byte[] digest) {
        this.digest = digest;
    }

}
