package org.moera.node.ui;

import com.github.jknack.handlebars.Handlebars;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PaginationItemTest {

    @Test
    void exposesRecordComponentsToHandlebars() throws Exception {
        PaginationItem item = PaginationItem.pageLink("2", 123L, true);

        String rendered = new Handlebars()
            .compileInline("{{title}}/{{moment}}/{{active}}/{{dots}}")
            .apply(item);

        Assertions.assertEquals("2/123/true/false", rendered);
    }

}
