package org.moera.node.liberin.model;

import org.moera.node.data.Draft;
import org.moera.node.liberin.Liberin;

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

}
