package org.moera.node.text.shorten;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

public class TextPosition implements Cloneable, Comparable<TextPosition> {

    private static final int LINE_LENGTH = 212;
    private static final float LINE_HEIGHT = 21.5f;

    private int line;
    private int chr;

    public TextPosition() {
        this(0, 0);
    }

    public TextPosition(int line, int chr) {
        this.line = line;
        this.chr = chr;
    }

    public int getLine() {
        return line;
    }

    public int getChr() {
        return chr;
    }

    public int getAbsolute() {
        return line * LINE_LENGTH + chr;
    }

    public boolean isZero() {
        return line == 0 && chr == 0;
    }

    public TextPosition plus(int amount) {
        int total = chr + amount;
        return new TextPosition(line + total / LINE_LENGTH, total % LINE_LENGTH);
    }

    public TextPosition newLine() {
        return chr == 0 ? clone() : new TextPosition(line + 1, 0);
    }

    public TextPosition space(int amount) { // in pixels
        int start = chr == 0 ? line : line + 1;
        return new TextPosition(start + Math.round(amount / LINE_HEIGHT), 0);
    }

    public int distance(TextPosition peer) {
        return Math.abs(getAbsolute() - peer.getAbsolute());
    }

    public boolean less(TextPosition peer) {
        return getAbsolute() < peer.getAbsolute();
    }

    public boolean lessOrEquals(TextPosition peer) {
        return getAbsolute() <= peer.getAbsolute();
    }

    public boolean greater(TextPosition peer) {
        return getAbsolute() > peer.getAbsolute();
    }

    public boolean greaterOrEquals(TextPosition peer) {
        return getAbsolute() >= peer.getAbsolute();
    }

    @Override
    public int compareTo(@NotNull TextPosition peer) {
        return getAbsolute() - peer.getAbsolute();
    }

    @Override
    public boolean equals(Object peer) {
        if (this == peer) {
            return true;
        }
        if (peer == null || getClass() != peer.getClass()) {
            return false;
        }
        TextPosition that = (TextPosition) peer;
        return line == that.line && chr == that.chr;
    }

    @Override
    public int hashCode() {
        return Objects.hash(line, chr);
    }

    @Override
    public TextPosition clone() {
        return new TextPosition(line, chr);
    }

}
