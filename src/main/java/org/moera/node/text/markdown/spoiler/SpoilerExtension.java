package org.moera.node.text.markdown.spoiler;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.NullableDataKey;
import org.jetbrains.annotations.NotNull;
import org.moera.node.text.markdown.spoiler.internal.SpoilerDelimiterProcessor;
import org.moera.node.text.markdown.spoiler.internal.SpoilerNodeRenderer;

/**
 * Extension for moera spoiler using ||.
 * <p>
 * Create it with {@link #create()} and then configure it on the builders
 * <p>
 * The parsed strikethrough text regions are turned into {@link Spoiler} nodes.
 */
public final class SpoilerExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

    public static final NullableDataKey<String> SPOILER_STYLE_HTML_OPEN =
            new NullableDataKey<>("SPOILER_STYLE_HTML_OPEN");
    public static final NullableDataKey<String> SPOILER_STYLE_HTML_CLOSE =
            new NullableDataKey<>("SPOILER_STYLE_HTML_CLOSE");

    private SpoilerExtension() {
    }

    public static SpoilerExtension create() {
        return new SpoilerExtension();
    }

    @Override
    public void rendererOptions(@NotNull MutableDataHolder options) {

    }

    @Override
    public void parserOptions(MutableDataHolder options) {

    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
        parserBuilder.customDelimiterProcessor(new SpoilerDelimiterProcessor());
    }

    @Override
    public void extend(@NotNull HtmlRenderer.Builder htmlRendererBuilder, @NotNull String rendererType) {
        if (!htmlRendererBuilder.isRendererType("HTML")) {
            throw new UnsupportedOperationException("supported only HTML render");
        }
        htmlRendererBuilder.nodeRendererFactory(new SpoilerNodeRenderer.Factory());
    }

}
