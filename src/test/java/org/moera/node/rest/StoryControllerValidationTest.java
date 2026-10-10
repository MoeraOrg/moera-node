package org.moera.node.rest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.PostingText;
import org.moera.lib.node.types.StoryAttributes;
import org.moera.lib.node.types.validate.ValidationFailure;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoryControllerValidationTest {

    @Test
    void putRejectsLongExternalSourceUriBeforeAccessingTheStory() {
        StoryAttributes attributes = attributesWithLongUri();

        ValidationFailure failure = assertThrows(
            ValidationFailure.class,
            () -> new StoryController().put(UUID.randomUUID().toString(), attributes)
        );

        assertEquals("story.external-source-uri.wrong-size", failure.getErrorCode());
    }

    @Test
    void putRejectsNullExternalSourceBeforeAccessingTheStory() {
        StoryAttributes attributes = new StoryAttributes();
        attributes.setExternalSourceUri(Arrays.asList("https://valid.example/story", null));

        ValidationFailure failure = assertThrows(
            ValidationFailure.class,
            () -> new StoryController().put(UUID.randomUUID().toString(), attributes)
        );

        assertEquals("story.external-source-uri.missing", failure.getErrorCode());
    }

    @Test
    void postingCreationValidatesPublicationExternalSources() {
        PostingText text = new PostingText();
        text.setPublications(List.of(attributesWithLongUri()));

        ValidationFailure failure = assertThrows(ValidationFailure.class, text::validate);

        assertEquals("story.external-source-uri.wrong-size", failure.getErrorCode());
    }

    @Test
    void publicationAcceptsExternalSourceUriAtTheLengthLimit() {
        StoryAttributes attributes = new StoryAttributes();
        attributes.setExternalSourceUri(List.of("u".repeat(1024)));

        assertDoesNotThrow(attributes::validate);
    }

    private static StoryAttributes attributesWithLongUri() {
        StoryAttributes attributes = new StoryAttributes();
        attributes.setExternalSourceUri(List.of("https://valid.example/story", "u".repeat(1025)));
        return attributes;
    }

}
