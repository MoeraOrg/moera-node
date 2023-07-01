package org.moera.node.liberin.model;

import java.util.List;
import java.util.Map;

import org.moera.node.data.SheriffMark;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.AvatarImage;

public class ReplyCommentAddedLiberin extends Liberin {

    private String nodeName;
    private String postingOwnerName;
    private String postingOwnerFullName;
    private String postingOwnerGender;
    private AvatarImage postingOwnerAvatar;
    private String postingHeading;
    private List<String> postingSheriffs;
    private List<SheriffMark> postingSheriffMarks;
    private String postingId;
    private String repliedToHeading;
    private String repliedToId;
    private String commentOwnerName;
    private String commentOwnerFullName;
    private String commentOwnerGender;
    private AvatarImage commentOwnerAvatar;
    private List<SheriffMark> commentSheriffMarks;
    private String commentId;

    public ReplyCommentAddedLiberin(String nodeName, String postingOwnerName, String postingOwnerFullName,
                                    String postingOwnerGender, AvatarImage postingOwnerAvatar, String postingHeading,
                                    List<String> postingSheriffs, List<SheriffMark> postingSheriffMarks,
                                    String postingId, String repliedToHeading, String repliedToId,
                                    String commentOwnerName, String commentOwnerFullName, String commentOwnerGender,
                                    AvatarImage commentOwnerAvatar, List<SheriffMark> commentSheriffMarks,
                                    String commentId) {
        this.nodeName = nodeName;
        this.postingOwnerName = postingOwnerName;
        this.postingOwnerFullName = postingOwnerFullName;
        this.postingOwnerGender = postingOwnerGender;
        this.postingOwnerAvatar = postingOwnerAvatar;
        this.postingHeading = postingHeading;
        this.postingSheriffs = postingSheriffs;
        this.postingSheriffMarks = postingSheriffMarks;
        this.postingId = postingId;
        this.repliedToHeading = repliedToHeading;
        this.repliedToId = repliedToId;
        this.commentOwnerName = commentOwnerName;
        this.commentOwnerFullName = commentOwnerFullName;
        this.commentOwnerGender = commentOwnerGender;
        this.commentOwnerAvatar = commentOwnerAvatar;
        this.commentSheriffMarks = commentSheriffMarks;
        this.commentId = commentId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getPostingOwnerName() {
        return postingOwnerName;
    }

    public void setPostingOwnerName(String postingOwnerName) {
        this.postingOwnerName = postingOwnerName;
    }

    public String getPostingOwnerFullName() {
        return postingOwnerFullName;
    }

    public void setPostingOwnerFullName(String postingOwnerFullName) {
        this.postingOwnerFullName = postingOwnerFullName;
    }

    public String getPostingOwnerGender() {
        return postingOwnerGender;
    }

    public void setPostingOwnerGender(String postingOwnerGender) {
        this.postingOwnerGender = postingOwnerGender;
    }

    public AvatarImage getPostingOwnerAvatar() {
        return postingOwnerAvatar;
    }

    public void setPostingOwnerAvatar(AvatarImage postingOwnerAvatar) {
        this.postingOwnerAvatar = postingOwnerAvatar;
    }

    public String getPostingHeading() {
        return postingHeading;
    }

    public void setPostingHeading(String postingHeading) {
        this.postingHeading = postingHeading;
    }

    public List<String> getPostingSheriffs() {
        return postingSheriffs;
    }

    public void setPostingSheriffs(List<String> postingSheriffs) {
        this.postingSheriffs = postingSheriffs;
    }

    public List<SheriffMark> getPostingSheriffMarks() {
        return postingSheriffMarks;
    }

    public void setPostingSheriffMarks(List<SheriffMark> postingSheriffMarks) {
        this.postingSheriffMarks = postingSheriffMarks;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getRepliedToHeading() {
        return repliedToHeading;
    }

    public void setRepliedToHeading(String repliedToHeading) {
        this.repliedToHeading = repliedToHeading;
    }

    public String getRepliedToId() {
        return repliedToId;
    }

    public void setRepliedToId(String repliedToId) {
        this.repliedToId = repliedToId;
    }

    public String getCommentOwnerName() {
        return commentOwnerName;
    }

    public void setCommentOwnerName(String commentOwnerName) {
        this.commentOwnerName = commentOwnerName;
    }

    public String getCommentOwnerFullName() {
        return commentOwnerFullName;
    }

    public void setCommentOwnerFullName(String commentOwnerFullName) {
        this.commentOwnerFullName = commentOwnerFullName;
    }

    public String getCommentOwnerGender() {
        return commentOwnerGender;
    }

    public void setCommentOwnerGender(String commentOwnerGender) {
        this.commentOwnerGender = commentOwnerGender;
    }

    public AvatarImage getCommentOwnerAvatar() {
        return commentOwnerAvatar;
    }

    public void setCommentOwnerAvatar(AvatarImage commentOwnerAvatar) {
        this.commentOwnerAvatar = commentOwnerAvatar;
    }

    public List<SheriffMark> getCommentSheriffMarks() {
        return commentSheriffMarks;
    }

    public void setCommentSheriffMarks(List<SheriffMark> commentSheriffMarks) {
        this.commentSheriffMarks = commentSheriffMarks;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("postingOwnerName", postingOwnerName);
        model.put("postingOwnerFullName", postingOwnerFullName);
        model.put("postingOwnerGender", postingOwnerGender);
        model.put("postingOwnerAvatar", postingOwnerAvatar);
        model.put("postingHeading", postingHeading);
        model.put("postingSheriffs", postingSheriffs);
        model.put("postingSheriffMarks", postingSheriffMarks);
        model.put("postingId", postingId);
        model.put("repliedToHeading", repliedToHeading);
        model.put("repliedToId", repliedToId);
        model.put("commentOwnerName", commentOwnerName);
        model.put("commentOwnerFullName", commentOwnerFullName);
        model.put("commentOwnerGender", commentOwnerGender);
        model.put("commentOwnerAvatar", commentOwnerAvatar);
        model.put("commentSheriffMarks", commentSheriffMarks);
        model.put("commentId", commentId);
    }

}
