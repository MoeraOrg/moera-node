package org.moera.node.text.delta.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.moera.node.text.delta.model.Delta;
import org.moera.node.text.delta.model.LineSeparator;
import org.moera.node.text.delta.model.OpList;

public class TextSlice {

    private final List<Block> blocks = new ArrayList<>();

    public List<Block> getBlocks() {
        return blocks;
    }

    public static TextSlice parse(Delta document) {
        TextSlice textSlice = new TextSlice();

        AtomicReference<Block> current = new AtomicReference<>();
        document.eachLine((line, lineAttributes) -> {
            if (isEmptyLine(line)) {
                current.set(null);
            } else {
                Map<String, Object> attributes = isBlockEmbed(line) ? line.getOps().get(0).argAsMap() : lineAttributes;
                Block block = current.get();
                int quoteLevel = getQuoteLevel(attributes);
                if (block == null
                        || !block.continuesWith(attributes)
                        || block.getQuoteLevel() != quoteLevel) {
                    block = createBlock(attributes, quoteLevel);
                    current.set(block);
                    textSlice.getBlocks().add(block);
                }
                block.addLine(new Line(line, attributes));
            }
            return true;
        }, new LineSeparator("\n", "horizontal-rule"));

        return textSlice;
    }

    private static boolean isEmptyLine(Delta line) {
        OpList ops = line.getOps();
        return ops.isEmpty()
                || ops.size() == 1 && ops.get(0).isTextInsert() && ops.get(0).argAsString().matches("\\s*\n");
    }

    private static boolean isBlockEmbed(Delta line) {
        OpList ops = line.getOps();
        return ops.size() == 1 && !ops.get(0).isTextInsert();
    }

    private static int getQuoteLevel(Map<String, Object> lineAttributes) {
        if (lineAttributes == null) {
            return 0;
        }
        if (lineAttributes.containsKey("quote-level")) {
            return Integer.parseInt((String) lineAttributes.get("quote-level"));
        }
        if (lineAttributes.containsKey("blockquote")) {
            return 1;
        }
        return 0;
    }

    private static Block createBlock(Map<String, Object> lineAttributes, int quoteLevel) {
        if (lineAttributes == null) {
            return new Paragraph(quoteLevel);
        }
        if (lineAttributes.containsKey("header")) {
            return new Header((Integer) lineAttributes.get("header"), quoteLevel);
        }
        if (lineAttributes.containsKey("list")) {
            return new MarkedList(quoteLevel);
        }
        if (lineAttributes.containsKey("horizontal-rule")) {
            return new HorizontalRule(quoteLevel);
        }
        return new Paragraph(quoteLevel);
    }

    public String toHtml() {
        StringBuilder buf = new StringBuilder();
        AtomicInteger quoteLevel = new AtomicInteger(0);
        blocks.forEach(block -> {
            buf.append("</blockquote>".repeat(Math.max(0, quoteLevel.get() - block.getQuoteLevel())));
            buf.append("<blockquote>".repeat(Math.max(0, block.getQuoteLevel() - quoteLevel.get())));
            buf.append(block.toHtml());
            quoteLevel.set(block.getQuoteLevel());
        });
        buf.append("</blockquote>".repeat(quoteLevel.get()));
        return buf.toString();
    }

}
