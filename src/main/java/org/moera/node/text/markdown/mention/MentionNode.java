package org.moera.node.text.markdown.mention;

import com.vladsch.flexmark.util.ast.DoNotDecorate;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;

public class MentionNode extends Node implements DoNotDecorate {

    protected BasedSequence openingMarker;
    protected BasedSequence text;

    public MentionNode(BasedSequence openingMarker, BasedSequence text) {
        super(spanningChars(openingMarker, text));
        this.openingMarker = openingMarker;
        this.text = text;
    }

    @NotNull
    @Override
    public BasedSequence[] getSegments() {
        return new BasedSequence[] { openingMarker, text };
    }

    @Override
    public void getAstExtra(@NotNull StringBuilder out) {
        delimitedSegmentSpanChars(out, openingMarker, text, BasedSequence.NULL, "text");
    }

    public BasedSequence getText() {
        return text;
    }

    public void setText(BasedSequence text) {
        this.text = text;
    }

}
