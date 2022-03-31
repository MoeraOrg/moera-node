package org.moera.node.liberin.model;

import org.moera.node.data.Draft;
import org.moera.node.liberin.Liberin;

public class DraftDeletedLiberin extends Liberin {

    private Draft draft;

    public DraftDeletedLiberin(Draft draft) {
        this.draft = draft;
    }

    public Draft getDraft() {
        return draft;
    }

    public void setDraft(Draft draft) {
        this.draft = draft;
    }

}