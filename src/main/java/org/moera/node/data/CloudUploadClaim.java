package org.moera.node.data;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.moera.node.media.MimeUtil;
import org.springframework.data.annotation.PersistenceCreator;

public class CloudUploadClaim {

    private final String id;
    private final Timestamp deadline;
    private final String fileName;
    private final String mimeType;
    private final long fileSize;
    private final Timestamp createdAt;
    private final boolean exposed;
    private final String cloudFileName;

    @PersistenceCreator
    public CloudUploadClaim(
        String id,
        LocalDateTime cloudUploadDeadline,
        String fileName,
        String mimeType,
        long fileSize,
        LocalDateTime createdAt,
        boolean exposed
    ) {
        this(
            id,
            Timestamp.valueOf(cloudUploadDeadline),
            fileName,
            mimeType,
            fileSize,
            Timestamp.valueOf(createdAt),
            exposed,
            buildFileName(id, mimeType, Timestamp.valueOf(createdAt))
        );
    }

    private CloudUploadClaim(
        String id,
        Timestamp deadline,
        String fileName,
        String mimeType,
        long fileSize,
        Timestamp createdAt,
        boolean exposed,
        String cloudFileName
    ) {
        this.id = id;
        this.deadline = deadline;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
        this.exposed = exposed;
        this.cloudFileName = cloudFileName;
    }

    public String getId() {
        return id;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public boolean isExposed() {
        return exposed;
    }

    public String getCloudFileName() {
        return cloudFileName;
    }

    private static String buildFileName(String id, String mimeType, Timestamp createdAt) {
        return "%s_%d.%s".formatted(id, createdAt.toInstant().getEpochSecond(), MimeUtil.extension(mimeType));
    }

    public CloudUploadClaim withDeadline(Timestamp newDeadline) {
        return new CloudUploadClaim(id, newDeadline, fileName, mimeType, fileSize, createdAt, exposed, cloudFileName);
    }

}
