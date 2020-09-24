package org.moera.node.text.markdown.mention.internal;

import org.jetbrains.annotations.NotNull;
import org.moera.naming.rpc.Rules;
import org.moera.node.text.markdown.mention.MentionNode;
import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.InlineParserExtension;
import com.vladsch.flexmark.parser.InlineParserExtensionFactory;
import com.vladsch.flexmark.parser.LightInlineParser;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.Set;
import java.util.regex.Pattern;

public class MentionsInlineParserExtension implements InlineParserExtension {

    private static final Pattern LATIN_CHARS = Pattern.compile("^[A-Za-z]+$");
    private static final String END_PUNCTUATION = ".,:;!?";

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
        final int startingIndex = inlineParser.getIndex();
        if (startingIndex != 0) {
            char c = inlineParser.getInput().charAt(startingIndex - 1);
            if (Character.isUnicodeIdentifierPart(c) || c == '-' || c == '.') {
                return false; // Mention can't be in the middle of a word
            }
        }

        BasedSequence inputText = inlineParser.getInput();
        final int nameStartingSequence = startingIndex + 1;
        int i = nameStartingSequence;
        while (i < inputText.length() && Rules.isNameCharacterValid(inputText.charAt(i))) {
            i++;
        }
        if (i > nameStartingSequence && END_PUNCTUATION.indexOf(inputText.charAt(i - 1)) >= 0) {
            i--;
        }
        if (i - nameStartingSequence == 0) {
            return false; // Name is empty
        }

        BasedSequence foundName = inputText.subSequence(nameStartingSequence, i);
        if (foundName.length() <= 3 && LATIN_CHARS.matcher(foundName).matches()) {
            return false; // Short latin names are reserved
        }

        inlineParser.setIndex(i);
        inlineParser.flushTextNode();
        BasedSequence openMarker = inputText.subSequence(startingIndex, nameStartingSequence);
        MentionNode mentionNode = new MentionNode(openMarker, foundName);
        inlineParser.getBlock().appendChild(mentionNode);
        return true;
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
