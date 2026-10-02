package org.moera.node.model;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.moera.lib.node.types.StoryInfo;
import org.moera.lib.node.types.StoryType;
import org.moera.node.data.EntrySharing;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.util.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StoryInfoUtilTest {

    @Test
    void sharedPostingReturnsRemoteStoryMoment() {
        Story story = story(StoryType.POSTING_SHARED);
        EntrySharing sharing = new EntrySharing();
        sharing.setRemoteStoryMoment(123456789L);
        story.setSharing(sharing);

        StoryInfo info = StoryInfoUtil.build(story, true, st -> null, null);

        assertEquals(123456789L, info.getRemoteStoryMoment());
    }

    @Test
    void sharedPostingWithoutSharingHasNoRemoteStoryMoment() {
        Story story = story(StoryType.POSTING_SHARED);

        StoryInfo info = StoryInfoUtil.build(story, true, st -> null, null);

        assertNull(info.getRemoteStoryMoment());
    }

    @Test
    void sharedPostingWithoutRemoteMomentHasNoRemoteStoryMoment() {
        Story story = story(StoryType.POSTING_SHARED);
        story.setSharing(new EntrySharing());

        StoryInfo info = StoryInfoUtil.build(story, true, st -> null, null);

        assertNull(info.getRemoteStoryMoment());
    }

    @ParameterizedTest
    @EnumSource(value = StoryType.class, names = {"REACTION_ADDED_POSITIVE", "REACTION_ADDED_NEGATIVE"})
    void reactionsDoNotReturnRemoteStoryMoment(StoryType storyType) {
        Story story = story(storyType);
        EntrySharing sharing = new EntrySharing();
        sharing.setRemoteStoryMoment(123456789L);
        story.setSharing(sharing);

        StoryInfo info = StoryInfoUtil.build(story, true, st -> null, null);

        assertNull(info.getRemoteStoryMoment());
    }

    private static Story story(StoryType storyType) {
        Story story = new Story(UUID.randomUUID(), UUID.randomUUID(), storyType);
        story.setCreatedAt(Util.now());
        story.setPublishedAt(Util.now());
        story.setMoment(987654321L);
        story.setSummary("");
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        story.setEntry(posting);
        return story;
    }

}
