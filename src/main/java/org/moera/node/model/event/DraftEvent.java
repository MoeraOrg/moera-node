package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.DraftType;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.Draft;
import org.springframework.data.util.Pair;

public class DraftEvent extends Event {

    private String id;
    private DraftType draftType;
    private String receiverName;
    private String receiverPostingId;
    private String receiverCommentId;

    protected DraftEvent(EventType type) {
        super(type, Scope.DRAFTS, Principal.ADMIN);
    }

    protected DraftEvent(EventType type, Draft draft) {
        super(type, Scope.DRAFTS, Principal.ADMIN);
        id = draft.getId().toString();
        draftType = draft.getDraftType();
        receiverName = draft.getReceiverName();
        receiverPostingId = draft.getReceiverPostingId();
        receiverCommentId = draft.getReceiverCommentId();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DraftType getDraftType() {
        return draftType;
    }

    public void setDraftType(DraftType draftType) {
        this.draftType = draftType;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPostingId() {
        return receiverPostingId;
    }

    public void setReceiverPostingId(String receiverPostingId) {
        this.receiverPostingId = receiverPostingId;
    }

    public String getReceiverCommentId() {
        return receiverCommentId;
    }

    public void setReceiverCommentId(String receiverCommentId) {
        this.receiverCommentId = receiverCommentId;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("id", LogUtil.format(id)));
        parameters.add(Pair.of("draftType", LogUtil.format(draftType.toString())));
        parameters.add(Pair.of("receiverName", LogUtil.format(receiverName)));
        parameters.add(Pair.of("receiverPostingId", LogUtil.format(receiverPostingId)));
        parameters.add(Pair.of("receiverCommentId", LogUtil.format(receiverCommentId)));
    }

}
