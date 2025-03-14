package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import org.moera.lib.node.types.StoryType;

@Entity
@Table(name = "reminders")
public class Reminder {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Enumerated
    private StoryType storyType;

    @NotNull
    private int priority;

    private Timestamp publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private Story story;

    private Timestamp readAt;

    @NotNull
    private int readCount;

    @NotNull
    private Timestamp nextAt;

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

    public StoryType getStoryType() {
        return storyType;
    }

    public void setStoryType(StoryType storyType) {
        this.storyType = storyType;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Timestamp getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Timestamp publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Story getStory() {
        return story;
    }

    public void setStory(Story story) {
        this.story = story;
    }

    public Timestamp getReadAt() {
        return readAt;
    }

    public void setReadAt(Timestamp readAt) {
        this.readAt = readAt;
    }

    public int getReadCount() {
        return readCount;
    }

    public void setReadCount(int readCount) {
        this.readCount = readCount;
    }

    public Timestamp getNextAt() {
        return nextAt;
    }

    public void setNextAt(Timestamp nextAt) {
        this.nextAt = nextAt;
    }

}
