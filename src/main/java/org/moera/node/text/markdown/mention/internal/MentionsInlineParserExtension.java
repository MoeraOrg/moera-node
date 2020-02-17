package org.moera.node.text.markdown.mention.internal;

import org.jetbrains.annotations.NotNull;
import org.moera.node.text.markdown.mention.MentionNode;
import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.InlineParserExtension;
import com.vladsch.flexmark.parser.InlineParserExtensionFactory;
import com.vladsch.flexmark.parser.LightInlineParser;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.Set;
import java.util.regex.Pattern;

public class MentionsInlineParserExtension implements InlineParserExtension {

    private static final Pattern MENTION = Pattern.compile("^(@)(\\S+)\\b",
            Pattern.CASE_INSENSITIVE);

    public MentionsInlineParserExtension(LightInlineParser inlineParser) {
    }

    @Override
    public void finalizeDocument(@NotNull InlineParser inlineParser) {
    }

    @Override
    public void finalizeBlock(@NotNull InlineParser inlineParser) {
    }

    @Override
    public boolean parse(LightInlineParser inlineParser) {
        int index = inlineParser.getIndex();
        boolean isPossible = index == 0;
        if (!isPossible) {
            char c = inlineParser.getInput().charAt(index - 1);
            if (!Character.isUnicodeIdentifierPart(c) && c != '-' && c != '.') {
                isPossible = true;
            }
        }
        if (isPossible) {
            BasedSequence[] matches = inlineParser.matchWithGroups(MENTION);
            if (matches != null) {
                inlineParser.flushTextNode();

                BasedSequence openMarker = matches[1];
                BasedSequence text = matches[2];

                MentionNode mentionNode = new MentionNode(openMarker, text);
                inlineParser.getBlock().appendChild(mentionNode);
                return true;
            }
        }
        return false;
    }

    public static class Factory implements InlineParserExtensionFactory {

        @Override
        public Set<Class<?>> getAfterDependents() {
            return null;
        }

        @NotNull
        @Override
        public CharSequence getCharacters() {
            return "@";
        }

        @Override
        public Set<Class<?>> getBeforeDependents() {
            return null;
        }

        @NotNull
        @Override
        public InlineParserExtension apply(@NotNull LightInlineParser lightInlineParser) {
            return new MentionsInlineParserExtension(lightInlineParser);
        }

        @Override
        public boolean affectsGlobalScope() {
            return false;
        }

    }

}
