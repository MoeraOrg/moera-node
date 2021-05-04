package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.commons.util.Util;

@Entity
@Table(name = "own_comments")
public class OwnComment {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Size(max = 63)
    private String remoteNodeName = "";

    @Size(max = 96)
    private String remoteFullName;

    @ManyToOne
    private MediaFile remoteAvatarMediaFile;

    @Size(max = 8)
    private String remoteAvatarShape;

    @NotNull
    @Size(max = 40)
    private String remotePostingId = "";

    @NotNull
    @Size(max = 40)
    private String remoteCommentId = "";

    @Size(max = 63)
    private String remoteRepliedToName = "";

    @Size(max = 96)
    private String remoteRepliedToFullName;

    @ManyToOne
    private MediaFile remoteRepliedToAvatarMediaFile;

    @Size(max = 8)
    private String remoteRepliedToAvatarShape;

    @NotNull
    @Size(max = 255)
    private String heading = "";

    @NotNull
    private Timestamp createdAt = Util.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemoteFullName() {
        return remoteFullName;
    }

    public void setRemoteFullName(String remoteFullName) {
        this.remoteFullName = remoteFullName;
    }

    public MediaFile getRemoteAvatarMediaFile() {
        return remoteAvatarMediaFile;
    }

    public void setRemoteAvatarMediaFile(MediaFile remoteAvatarMediaFile) {
        this.remoteAvatarMediaFile = remoteAvatarMediaFile;
    }

    public String getRemoteAvatarShape() {
        return remoteAvatarShape;
    }

    public void setRemoteAvatarShape(String remoteAvatarShape) {
        this.remoteAvatarShape = remoteAvatarShape;
    }

    public String getRemotePostingId() {
        return remotePostingId;
    }

    public void setRemotePostingId(String remotePostingId) {
        this.remotePostingId = remotePostingId;
    }

    public String getRemoteCommentId() {
        return remoteCommentId;
    }

    public void setRemoteCommentId(String remoteCommentId) {
        this.remoteCommentId = remoteCommentId;
    }

    public String getRemoteRepliedToName() {
        return remoteRepliedToName;
    }

    public void setRemoteRepliedToName(String remoteRepliedToName) {
        this.remoteRepliedToName = remoteRepliedToName;
    }

    public String getRemoteRepliedToFullName() {
        return remoteRepliedToFullName;
    }

    public void setRemoteRepliedToFullName(String remoteRepliedToFullName) {
        this.remoteRepliedToFullName = remoteRepliedToFullName;
    }

    public MediaFile getRemoteRepliedToAvatarMediaFile() {
        return remoteRepliedToAvatarMediaFile;
    }

    public void setRemoteRepliedToAvatarMediaFile(MediaFile remoteRepliedToAvatarMediaFile) {
        this.remoteRepliedToAvatarMediaFile = remoteRepliedToAvatarMediaFile;
    }

    public String getRemoteRepliedToAvatarShape() {
        return remoteRepliedToAvatarShape;
    }

    public void setRemoteRepliedToAvatarShape(String remoteRepliedToAvatarShape) {
        this.remoteRepliedToAvatarShape = remoteRepliedToAvatarShape;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

}
