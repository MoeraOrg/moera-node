package org.moera.node.text;

import java.util.Arrays;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.media.tags.MediaTagsExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.superscript.SuperscriptExtension;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class MarkdownConverter {

    static final MutableDataHolder OPTIONS = new MutableDataSet()
            .set(Parser.EXTENSIONS, Arrays.asList(
                    AutolinkExtension.create(),
                    DefinitionExtension.create(),
                    EmojiExtension.create(),
                    FootnoteExtension.create(),
                    MediaTagsExtension.create(),
                    SuperscriptExtension.create(),
                    TablesExtension.create(),
                    TocExtension.create()
            ));

    public static String toHtml(String source) {
        Parser parser = Parser.builder(OPTIONS).build();
        Node document = parser.parse(source);
        HtmlRenderer renderer = HtmlRenderer.builder(OPTIONS).build();
        return renderer.render(document);
    }

}
