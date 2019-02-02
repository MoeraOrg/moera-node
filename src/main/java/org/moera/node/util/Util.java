package org.moera.node.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.github.jknack.handlebars.Handlebars.SafeString;
import org.springframework.web.util.HtmlUtils;

public class Util {

    public static String ue(Object s) {
        try {
            return URLEncoder.encode(s.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "ue:" + e.getMessage();
        }
    }

    public static SafeString he(Object s) {
        if (s == null) {
            return new SafeString("");
        }
        return s instanceof SafeString ? (SafeString) s : new SafeString(HtmlUtils.htmlEscape(s.toString()));
    }

    public static int random(int min, int max) {
        return (int) (Math.random() * (max - min)) + min;
    }

}
