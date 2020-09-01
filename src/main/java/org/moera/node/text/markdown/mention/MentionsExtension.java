package org.moera.node.text.markdown.mention;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.DataKey;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import org.jetbrains.annotations.NotNull;
import org.moera.node.naming.NamingCache;
import org.moera.node.text.markdown.mention.internal.MentionsInlineParserExtension;
import org.moera.node.text.markdown.mention.internal.MentionsNodeRenderer;

public final class MentionsExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

    public static final DataKey<NamingCache> NAMING_CACHE = new DataKey<>("NAMING_CACHE", (NamingCache) null);

    private MentionsExtension() {
    }

    public static MentionsExtension create() {
        return new MentionsExtension();
    }

    @Override
    public void rendererOptions(@NotNull MutableDataHolder options) {
    }

    @Override
    public void parserOptions(MutableDataHolder options) {
    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
        parserBuilder.customInlineParserExtensionFactory(new MentionsInlineParserExtension.Factory());
    }

    @Override
    public void extend(HtmlRenderer.Builder htmlRendererBuilder, @NotNull String rendererType) {
        if (htmlRendererBuilder.isRendererType("HTML")) {
            htmlRendererBuilder.nodeRendererFactory(new MentionsNodeRenderer.Factory());
        }
    }

}
