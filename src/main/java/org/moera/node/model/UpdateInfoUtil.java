package org.moera.node.model;

import org.moera.lib.node.types.UpdateInfo;
import org.moera.node.data.Draft;
import org.moera.node.data.EntryRevision;
import org.springframework.util.ObjectUtils;

public class UpdateInfoUtil {
    
    public static UpdateInfo build(EntryRevision revision) {
        UpdateInfo updateInfo = new UpdateInfo();
        if (revision.isUpdateImportant()) {
            updateInfo.setImportant(revision.isUpdateImportant());
        }
        updateInfo.setDescription(revision.getUpdateDescription());
        return updateInfo;
    }
    
    public static UpdateInfo build(Draft draft) {
        UpdateInfo updateInfo = new UpdateInfo();
        if (draft.isUpdateImportant()) {
            updateInfo.setImportant(draft.isUpdateImportant());
        }
        updateInfo.setDescription(draft.getUpdateDescription());
        return updateInfo;
    }

    public static boolean isEmpty(EntryRevision revision) {
        return !revision.isUpdateImportant() && ObjectUtils.isEmpty(revision.getUpdateDescription());
    }

    public static boolean isEmpty(Draft draft) {
        return !draft.isUpdateImportant() && ObjectUtils.isEmpty(draft.getUpdateDescription());
    }

}
