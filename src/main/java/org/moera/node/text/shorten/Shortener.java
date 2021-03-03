package org.moera.node.text.shorten;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.moera.node.model.Body;
import org.moera.node.util.Util;

public class Shortener {

    private static final int SHORT_TITLE_MAX = 80;

    private static final int SHORT_TEXT_MIN = 400;
    private static final int SHORT_TEXT_AVG = 700;
    private static final int SHORT_TEXT_MAX = 1000;

    private static boolean isShort(Body body) {
        return (body.getSubject() == null || body.getSubject().length() <= SHORT_TITLE_MAX)
                && body.getText().length() <= SHORT_TEXT_MAX;
    }

    private static boolean isShortened(Body body, Body shortBody) {
        return body.getSubject() != null && body.getSubject().length() > SHORT_TITLE_MAX
                && shortBody.getSubject() != null && shortBody.getSubject().length() < body.getSubject().length()
               || shortBody.getText() != null /* null is a signal that the text is short enough */;
    }

    public static Body shorten(Body body) {
        if (isShort(body)) {
            return null;
        }

        Body shortBody = new Body();
        if (body.getSubject() != null && body.getSubject().length() > SHORT_TITLE_MAX) {
            shortBody.setSubject(Util.ellipsize(body.getSubject(), SHORT_TITLE_MAX));
        }
        if (body.getText().length() > SHORT_TEXT_MAX) {
            shortBody.setText(shorten(body.getText()));
        }
        return isShortened(body, shortBody) ? shortBody : null;
    }

    private static String shorten(String html) {
        if (html.length() <= SHORT_TEXT_MAX) {
            return null;
        }

        Document document = Jsoup.parseBodyFragment(html);
        Measurer measurer = new Measurer(SHORT_TEXT_MIN, SHORT_TEXT_AVG, SHORT_TEXT_MAX);
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
