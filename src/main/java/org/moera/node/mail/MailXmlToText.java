package org.moera.node.mail;

import org.moera.node.xml.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class MailXmlToText extends DefaultHandler {

    private enum FormatterState {
        GLOBAL,
        SUBJECT,
        BODY,
        P
    }

    public static final class Result {

        private final CharSequence subject;
        private final CharSequence body;

        private Result(CharSequence subject, CharSequence body) {
            this.subject = subject;
            this.body = body;
        }

        public CharSequence getSubject() {
            return subject;
        }

        public CharSequence getBody() {
            return body;
        }

    }

    private final StringBuilder subject = new StringBuilder();
    private final StringBuilder body = new StringBuilder();

    private FormatterState state = FormatterState.GLOBAL;
    private StringBuilder current = new StringBuilder();

    public Result getResult() {
        return new Result(subject, body);
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        body.append("*** %s ***".formatted(e.getMessage()));
    }

    private void flushCurrent() {
        body.append(current);
        body.append("\n");
        current = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (state == FormatterState.GLOBAL && qName.equals("subject")) {
            state = FormatterState.SUBJECT;
        } else if (state == FormatterState.GLOBAL && qName.equals("content")) {
            state = FormatterState.BODY;
        } else if (state == FormatterState.BODY && qName.equals("p")) {
            state = FormatterState.P;
        } else if (state == FormatterState.P && qName.equals("br")) {
            flushCurrent();
        } else if (state == FormatterState.SUBJECT || state == FormatterState.BODY || state == FormatterState.P) {
            body.append(XmlUtils.makeTag(qName, attributes));
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (state == FormatterState.SUBJECT && qName.equals("subject")) {
            subject.append(current);
            current = new StringBuilder();
            state = FormatterState.GLOBAL;
        } else if (state == FormatterState.BODY && qName.equals("content")) {
            state = FormatterState.GLOBAL;
        } else if (state == FormatterState.P && qName.equals("p")) {
            flushCurrent();
            body.append("\n");
            state = FormatterState.BODY;
        } else if (state == FormatterState.P && qName.equals("br")) {
        } else if (state == FormatterState.SUBJECT || state == FormatterState.BODY || state == FormatterState.P) {
            body.append(XmlUtils.makeTag("/" + qName));
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (state != FormatterState.SUBJECT && state != FormatterState.P) {
            return;
        }
        String data = new String(ch, start, length);
        data = data
            .replace('\r', ' ')
            .replace('\n', ' ')
            .replace("&nbsp;", " ")
            .replaceAll("\\s+", " ");
        if (data.isEmpty()) {
            return;
        }
        if (current.isEmpty() && data.charAt(0) == ' ') {
            current.append(data.substring(1));
        } else {
            current.append(data);
        }
    }

}
