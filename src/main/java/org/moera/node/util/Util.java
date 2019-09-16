package org.moera.node.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.github.jknack.handlebars.Handlebars.SafeString;
import com.ibm.icu.util.Calendar;
import org.springframework.web.util.HtmlUtils;

public class Util extends org.moera.commons.util.Util {

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

    public static void copyToCalendar(LocalDateTime dateTime, Calendar calendar) {
        calendar.set(dateTime.getYear(), dateTime.getMonthValue() - 1, dateTime.getDayOfMonth(),
                dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond());
    }

    public static Set<String> setParam(String value) {
        return value != null ? new HashSet<>(Arrays.asList(value.split(","))) : Collections.emptySet();
    }

}
