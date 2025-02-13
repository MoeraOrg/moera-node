package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.ReactionInfo;
import org.moera.node.liberin.Liberin;

public class RemotePostingReactionAddedLiberin extends Liberin {

    private String nodeName;
    private String postingId;
    private ReactionInfo reactionInfo;

    public RemotePostingReactionAddedLiberin(String nodeName, String postingId, ReactionInfo reactionInfo) {
        this.nodeName = nodeName;
        this.postingId = postingId;
        this.reactionInfo = reactionInfo;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public ReactionInfo getReactionInfo() {
        return reactionInfo;
    }

    public void setReactionInfo(ReactionInfo reactionInfo) {
        this.reactionInfo = reactionInfo;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("postingId", postingId);
        model.put("reaction", reactionInfo);
    }

}
