package org.moera.node.model;

import org.moera.lib.node.types.PostingSourceInfo;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.EntrySource;
import org.moera.node.util.Util;

public class PostingSourceInfoUtil {
    
    public static PostingSourceInfo build(EntrySource entrySource, DirectServeConfig config) {
        PostingSourceInfo postingSourceInfo = new PostingSourceInfo();
        
        postingSourceInfo.setNodeName(entrySource.getRemoteNodeName());
        postingSourceInfo.setFullName(entrySource.getRemoteFullName());
        
        if (entrySource.getRemoteAvatarMediaFile() != null) {
            postingSourceInfo.setAvatar(AvatarImageUtil.build(
                entrySource.getRemoteAvatarMediaFile(), 
                entrySource.getRemoteAvatarShape(),
                config
            ));
        }
        
        postingSourceInfo.setFeedName(entrySource.getRemoteFeedName());
        postingSourceInfo.setPostingId(entrySource.getRemotePostingId());
        postingSourceInfo.setCreatedAt(Util.toEpochSecond(entrySource.getCreatedAt()));
        
        return postingSourceInfo;
    }

}
