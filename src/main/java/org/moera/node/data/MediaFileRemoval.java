package org.moera.node.data;

import java.sql.Timestamp;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.moera.node.util.Util;

@Entity
@Table(name = "media_file_removals")
public class MediaFileRemoval {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private long id;

    @NotNull
    @Size(max = 40)
    private String mediaFileId;

    @Size(max = 50)
    private String fileName;

    @Size(max = 65)
    private String cloudFileName;

    @NotNull
    private Timestamp createdAt = Util.now();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMediaFileId() {
        return mediaFileId;
    }

    public void setMediaFileId(String mediaFileId) {
        this.mediaFileId = mediaFileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getCloudFileName() {
        return cloudFileName;
    }

    public void setCloudFileName(String cloudFileName) {
        this.cloudFileName = cloudFileName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

}
