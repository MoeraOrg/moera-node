package org.moera.node.model;

import org.moera.lib.node.types.AvatarDescription;
import org.moera.lib.node.types.MediaCaptionText;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PostingSourceText;

public class PostingSourceTextUtil {

    public static PostingSourceText build(PostingSourceText parentSourceText, MediaCaptionText caption) {
        var sourceText = new PostingSourceText();
        sourceText.setOwnerAvatar(parentSourceText.getOwnerAvatar());
        sourceText.setBodySrc(caption.getCaptionSrc());
        sourceText.setBodySrcFormat(caption.getCaptionSrcFormat());
        sourceText.setRejectedReactions(parentSourceText.getRejectedReactions());
        sourceText.setCommentRejectedReactions(parentSourceText.getCommentRejectedReactions());
        sourceText.setOperations(parentSourceText.getOperations());
        sourceText.setCommentOperations(parentSourceText.getCommentOperations());
        sourceText.setReactionOperations(parentSourceText.getReactionOperations());
        sourceText.setCommentReactionOperations(parentSourceText.getCommentReactionOperations());
        return sourceText;
    }

    public static PostingSourceText build(
        PostingInfo parentSourceText, AvatarDescription avatar, MediaCaptionText caption
    ) {
        var sourceText = new PostingSourceText();
        sourceText.setOwnerAvatar(avatar);
        sourceText.setBodySrc(caption.getCaptionSrc());
        sourceText.setBodySrcFormat(caption.getCaptionSrcFormat());
        sourceText.setRejectedReactions(parentSourceText.getRejectedReactions());
        sourceText.setCommentRejectedReactions(parentSourceText.getCommentRejectedReactions());
        sourceText.setOperations(parentSourceText.getOperations());
        sourceText.setCommentOperations(parentSourceText.getCommentOperations());
        sourceText.setReactionOperations(parentSourceText.getReactionOperations());
        sourceText.setCommentReactionOperations(parentSourceText.getCommentReactionOperations());
        return sourceText;
    }

}
