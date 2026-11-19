package org.moera.node.operations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.moera.lib.node.types.SharingType;
import org.moera.lib.node.types.validate.ValidationFailure;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SharingOperationsTest {

    @Test
    void acceptsPostingStoryAndCombinedReferences() {
        assertDoesNotThrow(() -> validate(null, null, null, "posting"));
        assertDoesNotThrow(() -> validate("timeline", "story", 0L, null));
        assertDoesNotThrow(() -> validate("timeline", "story", 0L, "posting"));
    }

    @ParameterizedTest
    @CsvSource({
        "timeline, , 1, sharing.remote-story-id.missing",
        "timeline, '', 1, sharing.remote-story-id.missing",
        ", story, 1, sharing.remote-feed-name.missing",
        "'', story, 1, sharing.remote-feed-name.missing",
        "timeline, story, , sharing.remote-story-moment.missing"
    })
    void requiresACompleteStoryEvenWhenPostingIsProvided(String feed, String story, Long moment, String errorCode) {
        var failure = assertThrows(ValidationFailure.class, () -> validate(feed, story, moment, "posting"));
        assertEquals(errorCode, failure.getErrorCode());
    }

    @Test
    void rejectsMissingOrEmptyReferencesAndType() {
        assertThrows(ValidationFailure.class, () -> validate(null, null, null, null));
        assertThrows(ValidationFailure.class, () -> validate(null, null, null, ""));
        assertThrows(ValidationFailure.class, () -> SharingOperations.validate(
            null, "posting", null, null, null, null, "shared"
        ));
        assertThrows(ValidationFailure.class, () -> SharingOperations.validate(
            SharingType.REPUBLISH, "posting", "invalid", null, null, null, "shared"
        ));
    }

    private static void validate(String feed, String story, Long moment, String posting) {
        SharingOperations.validate(SharingType.REPUBLISH, "original", null, feed, story, moment, posting);
    }

}
