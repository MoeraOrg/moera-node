package org.moera.node.operations;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.lib.node.types.StoryAttributes;
import org.moera.lib.node.types.StoryInfo;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.StoryInfoUtil;
import org.moera.node.userlist.SheriffUserListOperations;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryOperationsTest {

    @Mock
    private UniversalContext universalContext;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private SheriffUserListOperations sheriffUserListOperations;

    @InjectMocks
    private StoryOperations storyOperations;

    @Test
    void publicationStoresStoryExternalSourcesIndependentlyOfPosting() {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setExternalSourceUri(new String[]{"https://posting.example/post"});
        StoryAttributes attributes = new StoryAttributes();
        attributes.setFeedName(Feed.TIMELINE);
        attributes.setExternalSourceUri(List.of("https://one.example/story", "https://two.example/story"));
        when(universalContext.nodeId()).thenReturn(UUID.randomUUID());
        when(storyRepository.saveAndFlush(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));

        storyOperations.publish(posting, List.of(attributes));

        assertEquals(1, posting.getStories().size());
        Story story = posting.getStories().iterator().next();
        StoryInfo info = StoryInfoUtil.build(story, AccessCheckers.ADMIN, ignored -> null, null);
        assertEquals(List.of("https://one.example/story", "https://two.example/story"), info.getExternalSourceUri());
        assertArrayEquals(new String[]{"https://posting.example/post"}, posting.getExternalSourceUri());
    }

}
