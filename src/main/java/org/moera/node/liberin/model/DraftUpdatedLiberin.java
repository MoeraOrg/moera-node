package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.Draft;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.DraftInfoUtil;

public class DraftUpdatedLiberin extends Liberin {

    private Draft draft;

    public DraftUpdatedLiberin(Draft draft) {
        this.draft = draft;
    }

    public Draft getDraft() {
        return draft;
    }

    public void setDraft(Draft draft) {
        this.draft = draft;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("draft", DraftInfoUtil.build(draft));
    }

}
