package org.moera.node.data;

import java.util.UUID;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("1")
public class RemoteCommentVerification extends RemoteVerification {

    public RemoteCommentVerification() {
        setVerificationType(VerificationType.COMMENT);
    }

    public RemoteCommentVerification(UUID nodeId, String nodeName, String postingId, String commentId,
                                     String revisionId) {
        this();
        setId(UUID.randomUUID());
        setNodeId(nodeId);
        setNodeName(nodeName);
        setPostingId(postingId);
        setCommentId(commentId);
        setRevisionId(revisionId);
    }

}
