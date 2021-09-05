package org.moera.node.model;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Draft;
import org.moera.node.data.EntryRevision;
import org.springframework.util.ObjectUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateInfo {

    private Boolean important;

    @Size(max = 128)
    private String description;

    public UpdateInfo() {
    }

    public UpdateInfo(EntryRevision revision) {
        if (revision.isUpdateImportant()) {
            important = revision.isUpdateImportant();
        }
        description = revision.getUpdateDescription();
    }

    public UpdateInfo(Draft draft) {
        if (draft.isUpdateImportant()) {
            important = draft.isUpdateImportant();
        }
        description = draft.getUpdateDescription();
    }

    public static boolean isEmpty(EntryRevision revision) {
        return !revision.isUpdateImportant() && ObjectUtils.isEmpty(revision.getUpdateDescription());
    }

    public static boolean isEmpty(Draft draft) {
        return !draft.isUpdateImportant() && ObjectUtils.isEmpty(draft.getUpdateDescription());
    }

    public Boolean getImportant() {
        return important;
    }

    public void setImportant(Boolean important) {
        this.important = important;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
