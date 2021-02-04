package org.moera.node.data;

import java.util.UUID;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("2")
public class RemoteReactionVerification extends RemoteVerification {

    public RemoteReactionVerification() {
        setVerificationType(VerificationType.REACTION);
    }

    public RemoteReactionVerification(UUID nodeId, String nodeName, String postingId, String commentId,
                                      String reactionOwnerName) {
        this();
        setId(UUID.randomUUID());
        setNodeId(nodeId);
        setNodeName(nodeName);
        setPostingId(postingId);
        setCommentId(commentId);
        setOwnerName(reactionOwnerName);
    }

    public RemoteReactionVerification(UUID nodeId, String nodeName, String postingId, String reactionOwnerName) {
        this(nodeId, nodeName, postingId, null, reactionOwnerName);
    }

}
