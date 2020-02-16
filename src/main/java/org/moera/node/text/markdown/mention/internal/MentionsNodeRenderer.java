package org.moera.node.text.markdown.mention.internal;

import java.util.HashSet;
import java.util.Set;

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;
import org.jetbrains.annotations.NotNull;
import org.moera.node.naming.NamingCache;
import org.moera.node.naming.NamingNotAvailableException;
import org.moera.node.naming.RegisteredName;
import org.moera.node.naming.RegisteredNameDetails;
import org.moera.node.text.markdown.mention.MentionNode;

public class MentionsNodeRenderer implements NodeRenderer {

    private final MentionsOptions options;

    public MentionsNodeRenderer(DataHolder options) {
        this.options = new MentionsOptions(options);
    }

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        Set<NodeRenderingHandler<?>> set = new HashSet<>();
        // @formatter:off
        set.add(new NodeRenderingHandler<>(MentionNode.class, MentionsNodeRenderer.this::render));
        // @formatter:on
        return set;
    }

    private void render(MentionNode node, NodeRendererContext context, HtmlWriter html) {
        if (context.isDoNotRenderLinks()) {
            html.text(node.getChars());
        } else {
            String name = node.getText().toString();
            String nodeUri;
            try {
                boolean full = RegisteredName.parse(name).getGeneration() != null;
                RegisteredNameDetails details = full
                        ? options.namingCache.getFast(name)
                        : options.namingCache.get(name);
                nodeUri = details.getNodeUri() != null ? details.getNodeUri() : NamingCache.getRedirector(name);
                name = details.getNodeName() != null ? details.getNodeName() : name;
            } catch (NamingNotAvailableException e) {
                nodeUri = NamingCache.getRedirector(name);
            }
            html.srcPos(node.getChars())
                    .attr("href", nodeUri)
                    .attr("data-nodename", name)
                    .withAttr()
                    .tag("a");
            html.text(node.getChars());
            html.tag("/a");
        }
    }

    public static class Factory implements NodeRendererFactory {

        @NotNull
        @Override
        public NodeRenderer apply(@NotNull DataHolder options) {
            return new MentionsNodeRenderer(options);
        }

    }

}
