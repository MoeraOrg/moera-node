package org.moera.node.text.markdown.mention;

import com.vladsch.flexmark.util.ast.VisitHandler;

public class MentionsVisitorExt {

    public static <V extends MentionsVisitor> VisitHandler<?>[] visitHandlers(V visitor) {
        return new VisitHandler<?>[] {
                new VisitHandler<>(MentionNode.class, visitor::visit)
        };
    }

}
