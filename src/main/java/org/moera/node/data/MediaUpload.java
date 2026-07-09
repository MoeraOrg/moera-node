package org.moera.node.data;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "media_uploads")
public class MediaUpload {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @Size(max = 80)
    @NotNull
    private String mimeType;

    @Size(max = 255)
    private String title;

    private int fileSize;

    private int chunkSize;

    @NotNull
    @JdbcTypeCode(SqlTypes.ARRAY)
    private Integer[] uploadedChunks = new Integer[0];

    @NotNull
    private Timestamp deadline;

    private Timestamp completedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getTotalChunks() {
        return (int) (((long) fileSize + chunkSize - 1) / chunkSize);
    }

    public Integer[] getUploadedChunks() {
        return uploadedChunks;
    }

    public void setUploadedChunks(Integer[] uploadedChunks) {
        this.uploadedChunks = uploadedChunks;
    }

    public void addChunk(int chunk) {
        List<Integer> chunks = new ArrayList<>(Arrays.asList(uploadedChunks));
        if (!chunks.contains(chunk)) {
            chunks.add(chunk);
            Collections.sort(chunks);
            uploadedChunks = chunks.toArray(Integer[]::new);
        }
    }

    public boolean isCompleted() {
        return uploadedChunks.length == getTotalChunks();
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

}
