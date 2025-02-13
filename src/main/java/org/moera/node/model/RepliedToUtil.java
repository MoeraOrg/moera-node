package org.moera.node.model;

import org.moera.lib.node.types.RepliedTo;
import org.moera.node.data.Comment;

public class RepliedToUtil {
    
    public static RepliedTo build(Comment comment) {
        RepliedTo repliedTo = new RepliedTo();
        
        if (comment.getRepliedTo() != null) {
            repliedTo.setId(comment.getRepliedTo().getId().toString());
            
            if (comment.getRepliedToRevision() != null) {
                repliedTo.setRevisionId(comment.getRepliedToRevision().getId().toString());
            }
            
            repliedTo.setName(comment.getRepliedToName());
            repliedTo.setFullName(comment.getRepliedToFullName());
            repliedTo.setGender(comment.getRepliedToGender());
            
            if (comment.getRepliedToAvatarMediaFile() != null) {
                repliedTo.setAvatar(AvatarImageUtil.build(
                    comment.getRepliedToAvatarMediaFile(), 
                    comment.getRepliedToAvatarShape()
                ));
            }
            
            repliedTo.setHeading(comment.getRepliedToHeading());
            repliedTo.setDigest(comment.getRepliedToDigest());
        }
        
        return repliedTo;
    }

}
