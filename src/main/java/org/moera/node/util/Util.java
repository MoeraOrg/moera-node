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
import org.moera.node.model.RevisionInfo;
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

    public static Boolean toBoolean(String value) {
        if (value == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value) || "0".equals(value)) {
            return false;
        }
        throw new IllegalArgumentException(String.format("\"%s\" is not a valid value for boolean", value));
    }

    public static boolean toBoolean(String value, boolean defaultValue) {
        Boolean boolValue = toBoolean(value);
        return boolValue != null ? boolValue : defaultValue;
    }

    public static void ellipsize(StringBuilder buf, int size) {
        if (buf != null && buf.length() > size) {
            buf.setLength(size);
            buf.append('\u2026');
        }
    }

    public static String ellipsize(String s, int size) {
        if (s != null && s.length() > size) {
            return s.substring(0, size) + '\u2026';
        }
        return s;
    }

    public static <R extends RevisionInfo> R revisionByTimestamp(R[] revisions, Long timestamp) {
        return Arrays.stream(revisions)
                .filter(r -> r.getCreatedAt() <= timestamp)
                .filter(r -> r.getDeletedAt() == null || r.getDeletedAt() > timestamp)
                .findFirst()
                .orElse(null);
    }

}
