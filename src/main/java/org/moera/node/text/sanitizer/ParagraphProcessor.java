package org.moera.node.text.sanitizer;

import java.util.ArrayList;
import java.util.List;

import org.owasp.html.HtmlStreamEventReceiver;
import org.owasp.html.HtmlStreamEventReceiverWrapper;

class ParagraphProcessor extends HtmlStreamEventReceiverWrapper {

    ParagraphProcessor(HtmlStreamEventReceiver underlying) {
        super(underlying);
    }

    @Override
    public void openTag(String elementName, List<String> attrs) {
        List<String> newAttrs = attrs;
        if (elementName.equalsIgnoreCase("p")
                || elementName.equalsIgnoreCase("ol")
                || elementName.equalsIgnoreCase("ul")) {
            newAttrs = new ArrayList<>();
            for (int i = 0; i < attrs.size(); i += 2) {
                if (!attrs.get(i).equalsIgnoreCase("dir")) {
                    newAttrs.add(attrs.get(i));
                    newAttrs.add(attrs.get(i + 1));
                }
            }
            newAttrs.add("dir");
            newAttrs.add("auto");
        }
        super.openTag(elementName, newAttrs);
    }

}
