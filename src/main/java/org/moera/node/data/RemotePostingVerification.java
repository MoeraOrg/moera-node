package org.moera.node.data;

import java.util.UUID;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("0")
public class RemotePostingVerification extends RemoteVerification {

    public RemotePostingVerification() {
        setVerificationType(VerificationType.POSTING);
    }

    public RemotePostingVerification(UUID nodeId, String nodeName, String postingId, String revisionId) {
        this();
        setId(UUID.randomUUID());
        setNodeId(nodeId);
        setNodeName(nodeName);
        setPostingId(postingId);
        setRevisionId(revisionId);
    }

}
