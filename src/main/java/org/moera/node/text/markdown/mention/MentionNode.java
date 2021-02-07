package org.moera.node.text.markdown.mention;

import com.vladsch.flexmark.util.ast.DoNotDecorate;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;

public class MentionNode extends Node implements DoNotDecorate {

    protected BasedSequence openingMarker;
    protected BasedSequence name;
    protected BasedSequence textOpen;
    protected BasedSequence text;
    protected BasedSequence textClose;

    public MentionNode(BasedSequence openingMarker, BasedSequence name) {
        this(openingMarker, name, BasedSequence.NULL, BasedSequence.NULL, BasedSequence.NULL);
    }

    public MentionNode(BasedSequence openingMarker, BasedSequence name,
                       BasedSequence textOpen, BasedSequence text, BasedSequence textClose) {
        super(spanningChars(openingMarker, name, textOpen, text, textClose));
        this.openingMarker = openingMarker;
        this.name = name;
        this.textOpen = textOpen;
        this.text = text;
        this.textClose = textClose;
    }

    @NotNull
    @Override
    public BasedSequence[] getSegments() {
        return new BasedSequence[] { openingMarker, name, textOpen, text, textClose};
    }

    @Override
    public void getAstExtra(@NotNull StringBuilder out) {
        delimitedSegmentSpanChars(out, openingMarker, name, BasedSequence.NULL, "name");
        delimitedSegmentSpanChars(out, textOpen, text, textClose, "text");
    }

    public BasedSequence getName() {
        return name;
    }

    public void setName(BasedSequence name) {
        this.name = name;
    }

    public BasedSequence getText() {
        return text;
    }

    public void setText(BasedSequence text) {
        this.text = text;
    }

}
