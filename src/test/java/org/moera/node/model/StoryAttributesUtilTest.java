package org.moera.node.model;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.moera.lib.node.types.StoryAttributes;
import org.moera.lib.node.types.StoryOperations;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.Posting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoryAttributesUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {"unset", "public", "signed", "subscribed", "private", "admin", "f:friends"})
    void viewingPermissionIsStoredAndReturned(String value) {
        Story story = story();
        StoryAttributesUtil.toStory(attributes(value), story);

        assertEquals(new Principal(value), view(story));
    }

    @ParameterizedTest
    @ValueSource(strings = {"none", "owner", "secret", "senior", "major", "enigma", "node:alice", "invalid"})
    void invalidViewingPermissionIsRejectedBeforeOtherAttributesAreApplied(String value) {
        Story story = story();
        StoryAttributes attributes = attributes(value);
        attributes.setPinned(true);

        ValidationFailure failure = assertThrows(
            ValidationFailure.class, () -> StoryAttributesUtil.toStory(attributes, story)
        );

        assertEquals("story.operations.wrong-principal", failure.getErrorCode());
        assertFalse(story.isPinned());
    }

    @Test
    void omittedPermissionPreservesValueAndUnsetRestoresDefault() {
        Story story = story();
        assertEquals(Principal.UNSET, view(story));
        StoryAttributesUtil.toStory(attributes("signed"), story);
        StoryAttributesUtil.toStory(new StoryAttributes(), story);
        StoryAttributes emptyOperations = new StoryAttributes();
        emptyOperations.setOperations(new StoryOperations());
        StoryAttributesUtil.toStory(emptyOperations, story);
        assertEquals(Principal.SIGNED, view(story));

        StoryAttributesUtil.toStory(attributes("unset"), story);

        assertEquals(Principal.UNSET, view(story));
    }

    private static StoryAttributes attributes(String value) {
        StoryAttributes attributes = new StoryAttributes();
        StoryOperations operations = new StoryOperations();
        operations.setView(new Principal(value));
        attributes.setOperations(operations);
        return attributes;
    }

    private static Story story() {
        Story story = new Story();
        story.setId(UUID.randomUUID());
        story.setMoment(1L);
        story.setEntry(new Posting());
        return story;
    }

    private static Principal view(Story story) {
        return StoryInfoUtil.build(story, AccessCheckers.ADMIN, ignored -> null, null).getOperations().getView();
    }

    @Test
    void nullExternalSourceIsRejectedBeforeApplyingAttributes() {
        Story story = new Story();
        story.setFeedName(Feed.NEWS);
        StoryAttributes attributes = new StoryAttributes();
        attributes.setFeedName(Feed.TIMELINE);
        attributes.setExternalSourceUri(Arrays.asList("https://valid.example/story", null));

        ValidationFailure failure = assertThrows(
            ValidationFailure.class,
            () -> StoryAttributesUtil.toStory(attributes, story)
        );

        assertEquals("story.external-source-uri.missing", failure.getErrorCode());
        assertEquals(Feed.NEWS, story.getFeedName());
    }

}
