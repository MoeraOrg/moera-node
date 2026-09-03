package org.moera.node.data;

import java.lang.reflect.Field;
import java.util.List;
import jakarta.validation.constraints.Size;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SourceUriPersistenceModelTest {

    private static final List<Class<?>> ENTITIES = List.of(
        Contact.class,
        Draft.class,
        Entry.class,
        EntrySource.class,
        OwnComment.class,
        OwnPosting.class,
        OwnReaction.class,
        Reaction.class,
        SheriffComplaint.class,
        SheriffComplaintGroup.class,
        SheriffOrder.class,
        Story.class
    );

    @Test
    void everyPersistedFullNameHasSourceUri() throws Exception {
        for (Class<?> entity : ENTITIES) {
            for (Field field : entity.getDeclaredFields()) {
                if (field.getName().endsWith("FullName")) {
                    String prefix = field.getName().substring(0, field.getName().length() - "FullName".length());
                    Field sourceUri = entity.getDeclaredField(prefix + "SourceUri");
                    Assertions.assertEquals(1024, sourceUri.getAnnotation(Size.class).max());
                }
            }
        }
    }

}
