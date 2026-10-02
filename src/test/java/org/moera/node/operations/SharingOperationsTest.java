package org.moera.node.operations;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.SharingType;
import org.moera.lib.node.types.validate.ValidationFailure;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SharingOperationsTest {

    @Test
    void acceptsPostingStoryAndCombinedReferences() {
        assertDoesNotThrow(() -> validate(null, null, null, "posting"));
        assertDoesNotThrow(() -> validate("timeline", "story", 0L, null));
        assertDoesNotThrow(() -> validate("timeline", "story", 0L, "posting"));
    }

    @Test
    void requiresACompleteStoryEvenWhenPostingIsProvided() {
        assertThrows(ValidationFailure.class, () -> validate("timeline", null, 1L, "posting"));
        assertThrows(ValidationFailure.class, () -> validate(null, "story", 1L, "posting"));
        assertThrows(ValidationFailure.class, () -> validate("timeline", "story", null, "posting"));
        assertThrows(ValidationFailure.class, () -> validate("", "story", 1L, "posting"));
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
