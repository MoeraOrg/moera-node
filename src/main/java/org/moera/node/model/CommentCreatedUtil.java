package org.moera.node.model;

import java.util.List;
import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.CommentCreated;
import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.Comment;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.moera.node.option.Options;

public class CommentCreatedUtil {
    
    public static CommentCreated build(
        Comment comment,
        Integer total,
        MediaAttachmentsProvider mediaAttachmentsProvider,
        AccessChecker accessChecker,
        List<BlockedOperation> blockedOperations,
        Options options,
        DirectServeConfig config
    ) {
        CommentCreated commentCreated = new CommentCreated();
        
        if (comment != null) {
            CommentInfo commentInfo = CommentInfoUtil.build(
                comment, mediaAttachmentsProvider, accessChecker, options, config
            );
            CommentInfoUtil.putBlockedOperations(commentInfo, blockedOperations);
            commentCreated.setComment(commentInfo);
        }
        
        commentCreated.setTotal(total);

        return commentCreated;
    }

}
