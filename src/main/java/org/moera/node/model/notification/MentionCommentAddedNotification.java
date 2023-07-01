package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.SheriffMark;
import org.moera.node.model.AvatarImage;
import org.moera.node.util.SheriffUtil;
import org.springframework.data.util.Pair;

public class MentionCommentAddedNotification extends MentionCommentNotification {

    @Size(max = 63)
    private String postingOwnerName;

    @Size(max = 96)
    private String postingOwnerFullName;

    @Size(max = 31)
    private String postingOwnerGender;

    @Valid
    private AvatarImage postingOwnerAvatar;

    @Size(max = 255)
    private String postingHeading;

    @Size(max = 4096)
    private List<String> postingSheriffs;

    @Size(max = 4096)
    private List<SheriffMark> postingSheriffMarks;

    @Size(max = 63)
    private String commentOwnerName;

    @Size(max = 96)
    private String commentOwnerFullName;

    @Size(max = 31)
    private String commentOwnerGender;

    @Valid
    private AvatarImage commentOwnerAvatar;

    @Size(max = 255)
    private String commentHeading;

    @Size(max = 4096)
    private List<SheriffMark> commentSheriffMarks;

    public MentionCommentAddedNotification() {
        super(NotificationType.MENTION_COMMENT_ADDED);
    }

    public MentionCommentAddedNotification(String postingOwnerName, String postingOwnerFullName,
                                           String postingOwnerGender, AvatarImage postingOwnerAvatar, UUID postingId,
                                           UUID commentId, String postingHeading, List<String> postingSheriffs,
                                           List<SheriffMark> postingSheriffMarks, String commentOwnerName,
                                           String commentOwnerFullName, String commentOwnerGender,
                                           AvatarImage commentOwnerAvatar, String commentHeading,
                                           List<SheriffMark> commentSheriffMarks) {
        super(NotificationType.MENTION_COMMENT_ADDED, postingId, commentId);
        this.postingOwnerName = postingOwnerName;
        this.postingOwnerFullName = postingOwnerFullName;
        this.postingOwnerGender = postingOwnerGender;
        this.postingOwnerAvatar = postingOwnerAvatar;
        this.postingHeading = postingHeading;
        this.postingSheriffs = postingSheriffs;
        this.postingSheriffMarks = postingSheriffMarks;
        this.commentOwnerName = commentOwnerName;
        this.commentOwnerFullName = commentOwnerFullName;
        this.commentOwnerGender = commentOwnerGender;
        this.commentOwnerAvatar = commentOwnerAvatar;
        this.commentHeading = commentHeading;
        this.commentSheriffMarks = commentSheriffMarks;
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

    public String getCommentHeading() {
        return commentHeading;
    }

    public void setCommentHeading(String commentHeading) {
        this.commentHeading = commentHeading;
    }

    public List<SheriffMark> getCommentSheriffMarks() {
        return commentSheriffMarks;
    }

    public void setCommentSheriffMarks(List<SheriffMark> commentSheriffMarks) {
        this.commentSheriffMarks = commentSheriffMarks;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("postingOwnerName", LogUtil.format(postingOwnerName)));
        parameters.add(Pair.of("postingHeading", LogUtil.format(postingHeading)));
        parameters.add(Pair.of("postingSheriffs",
                LogUtil.format(SheriffUtil.serializeSheriffs(postingSheriffs).orElse(null))));
        parameters.add(Pair.of("postingSheriffMarks",
                LogUtil.format(SheriffUtil.serializeSheriffMarks(postingSheriffMarks).orElse(null))));
        parameters.add(Pair.of("commentOwnerName", LogUtil.format(commentOwnerName)));
        parameters.add(Pair.of("commentHeading", LogUtil.format(commentHeading)));
        parameters.add(Pair.of("commentSheriffMarks",
                LogUtil.format(SheriffUtil.serializeSheriffMarks(commentSheriffMarks).orElse(null))));
    }

}
