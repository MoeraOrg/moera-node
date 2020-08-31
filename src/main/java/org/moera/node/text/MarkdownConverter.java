package org.moera.node.text;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiImageType;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughSubscriptExtension;
import com.vladsch.flexmark.ext.gitlab.GitLabExtension;
import com.vladsch.flexmark.ext.media.tags.MediaTagsExtension;
import com.vladsch.flexmark.ext.superscript.SuperscriptExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.moera.node.naming.NamingCache;
import org.moera.node.text.markdown.mention.MentionsExtension;
import org.moera.node.text.markdown.spoiler.SpoilerExtension;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MarkdownConverter {

    static final DataHolder DEFAULT_OPTIONS = new MutableDataSet()
            .set(Parser.LISTS_ITEM_PREFIX_CHARS, "*")
            .set(Parser.HTML_BLOCK_TAGS, htmlBlockTags())
            .set(HtmlRenderer.SOFT_BREAK, "<br/>")
            .set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_ONLY)
            .set(GitLabExtension.INS_PARSER, false)
            .set(GitLabExtension.DEL_PARSER, false)
            .set(GitLabExtension.RENDER_BLOCK_MATH, false)
            .set(GitLabExtension.RENDER_BLOCK_MERMAID, false)
            .set(Parser.EXTENSIONS, List.of(
                    AutolinkExtension.create(),
                    DefinitionExtension.create(),
                    EmojiExtension.create(),
                    FootnoteExtension.create(),
                    GitLabExtension.create(),
                    MediaTagsExtension.create(),
                    MentionsExtension.create(),
                    StrikethroughSubscriptExtension.create(),
                    SuperscriptExtension.create(),
                    TablesExtension.create(),
                    TocExtension.create(),
                    SpoilerExtension.create()
            )).toImmutable();

    @Inject
    private NamingCache namingCache;

    private DataHolder options;

    @PostConstruct
    public void init() {
        options = DEFAULT_OPTIONS.toMutable().set(MentionsExtension.NAMING_CACHE, namingCache);
    }

    private static List<String> htmlBlockTags() {
        return Parser.HTML_BLOCK_TAGS.get(null).stream()
                .filter(tag -> !tag.equalsIgnoreCase("blockquote"))
                .collect(Collectors.toList());

    }

    public String toHtml(String source) {
        Parser parser = Parser.builder(options).build();
        Node document = parser.parse(source);
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        return renderer.render(document);
    }

}
