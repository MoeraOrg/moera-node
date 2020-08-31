package org.moera.node.text.markdown.spoiler.internal;

import org.moera.node.text.markdown.spoiler.Spoiler;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;
import org.jetbrains.annotations.NotNull;
import org.moera.node.text.markdown.spoiler.SpoilerExtension;

import java.util.HashSet;
import java.util.Set;

public class SpoilerNodeRenderer implements NodeRenderer {

    private final String spoilerStyleHtmlOpen;
    private final String spoilerStyleHtmlClose;

    public SpoilerNodeRenderer(DataHolder options) {
        spoilerStyleHtmlOpen = SpoilerExtension.SPOILER_STYLE_HTML_OPEN.get(options);
        spoilerStyleHtmlClose = SpoilerExtension.SPOILER_STYLE_HTML_CLOSE.get(options);
    }

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        HashSet<NodeRenderingHandler<?>> set = new HashSet<>();
        set.add(new NodeRenderingHandler<>(Spoiler.class, this::render));
        return set;
    }

    private void render(Spoiler node, NodeRendererContext context, HtmlWriter html) {
        if (spoilerStyleHtmlOpen == null || spoilerStyleHtmlClose == null) {
            if (context.getHtmlOptions().sourcePositionParagraphLines) {
                html.withAttr().tag("mr-spoiler");
            } else {
                html.srcPos(node.getText()).withAttr().tag("mr-spoiler");
            }
            context.renderChildren(node);
            html.tag("/mr-spoiler");
        } else {
            html.raw(spoilerStyleHtmlOpen);
            context.renderChildren(node);
            html.raw(spoilerStyleHtmlClose);
        }
    }

    public static class Factory implements NodeRendererFactory {
        @NotNull
        @Override
        public NodeRenderer apply(@NotNull DataHolder options) {
            return new SpoilerNodeRenderer(options);
        }
    }

}
