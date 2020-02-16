package org.moera.node.text;

import java.util.Arrays;

import javax.inject.Inject;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiImageType;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.media.tags.MediaTagsExtension;
import com.vladsch.flexmark.ext.superscript.SuperscriptExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.moera.node.naming.NamingCache;
import org.moera.node.text.markdown.mention.MentionsExtension;
import org.springframework.stereotype.Component;

@Component
public class MarkdownConverter {

    static final MutableDataHolder OPTIONS = new MutableDataSet()
            .set(Parser.LISTS_ITEM_PREFIX_CHARS, "*")
            .set(HtmlRenderer.SOFT_BREAK, "<br/>")
            .set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_ONLY)
            .set(Parser.EXTENSIONS, Arrays.asList(
                    AutolinkExtension.create(),
                    DefinitionExtension.create(),
                    EmojiExtension.create(),
                    FootnoteExtension.create(),
                    MediaTagsExtension.create(),
                    MentionsExtension.create(),
                    SuperscriptExtension.create(),
                    TablesExtension.create(),
                    TocExtension.create()
            ));

    @Inject
    private NamingCache namingCache;

    public String toHtml(String source) {
        Parser parser = Parser.builder(OPTIONS.set(MentionsExtension.NAMING_CACHE, namingCache)).build();
        Node document = parser.parse(source);
        HtmlRenderer renderer = HtmlRenderer.builder(OPTIONS).build();
        return renderer.render(document);
    }

}
