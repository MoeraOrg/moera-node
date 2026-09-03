package org.moera.node.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.StoryInfo;
import org.moera.lib.node.types.StoryType;
import org.moera.node.data.Story;

class StorySourceUriTest {

    @Test
    void returnsRemoteAndSummarySourceUri() {
        Story story = new Story();
        story.setId(UUID.randomUUID());
        story.setStoryType(StoryType.POSTING_UPDATE_TASK_FAILED);
        story.setSummary("");
        story.setCreatedAt(Timestamp.from(Instant.now()));
        story.setPublishedAt(Timestamp.from(Instant.now()));
        story.setMoment(1L);
        story.setRemoteNodeName("remote.example");
        story.setRemoteFullName("Remote Owner");
        story.setRemoteSourceUri("https://source.example/remote-owner");

        StoryInfo info = StoryInfoUtil.build(story, false, ignored -> null, null);

        assertThat(info.getRemoteSourceUri()).isEqualTo("https://source.example/remote-owner");
        assertThat(info.getSummarySourceUri()).isEqualTo("https://source.example/remote-owner");
    }

}
