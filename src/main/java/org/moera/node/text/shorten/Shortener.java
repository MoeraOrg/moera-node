package org.moera.node.text.shorten;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.moera.node.model.Body;
import org.moera.node.util.Util;

public class Shortener {

    private static final int SHORT_TITLE_MAX = 80;

    private static boolean isShortened(Body body, Body shortBody) {
        return body.getSubject() != null && body.getSubject().length() > SHORT_TITLE_MAX
                && shortBody.getSubject() != null && shortBody.getSubject().length() < body.getSubject().length()
               || shortBody.getText() != null /* null is a signal that the text is short enough */;
    }

    public static Body shorten(Body body) {
        Body shortBody = new Body();
        if (body.getSubject() != null && body.getSubject().length() > SHORT_TITLE_MAX) {
            shortBody.setSubject(Util.ellipsize(body.getSubject(), SHORT_TITLE_MAX));
        }
        shortBody.setText(shorten(body.getText()));
        return isShortened(body, shortBody) ? shortBody : null;
    }

    private static String shorten(String html) {
        Document document = Jsoup.parseBodyFragment(html.trim());
        Measurer measurer = new Measurer();
        document.body().filter(measurer);

        if (measurer.isTextShort()) {
            return null;
        }

        Document result = Document.createShell("");
        Cutter cutter = new Cutter(measurer.getCut(), measurer.needsEllipsis(), result.body());
        document.body().filter(cutter);
        return result.body().html();
    }

}
