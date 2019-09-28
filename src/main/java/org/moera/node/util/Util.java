package org.moera.node.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    public static String fromNow(LocalDateTime dateTime) {
        long diff = dateTime.until(LocalDateTime.now(), ChronoUnit.SECONDS);
        if (diff < 60) {
            return "few seconds ago";
        }
        diff /= 60;
        if (diff == 1) {
            return "a minute ago";
        }
        if (diff < 60) {
            return String.format("%d minutes ago", diff);
        }
        diff /= 60;
        if (diff == 1) {
            return "an hour ago";
        }
        if (diff < 24) {
            return String.format("%d hours ago", diff);
        }
        diff /= 24;
        if (diff == 1) {
            return "yesterday";
        }
        if (diff < 30) {
            return String.format("%d days ago", diff);
        }
        if (diff < 60) {
            return "a month ago";
        }
        if (diff < 330) {
            return String.format("%d months ago", diff / 30);
        }
        diff /= 365;
        if (diff <= 1) {
            return "a year ago";
        }
        return String.format("%d years ago", diff);
    }

}
