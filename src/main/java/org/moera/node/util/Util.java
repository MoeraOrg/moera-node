package org.moera.node.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.github.jknack.handlebars.Handlebars.SafeString;
import com.ibm.icu.util.Calendar;
import org.moera.commons.util.DurationFormatException;
import org.springframework.web.util.HtmlUtils;

public class Util {

    public static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    public static Timestamp farFuture() {
        return Timestamp.from(Instant.now().plus(3650, ChronoUnit.DAYS));
    }

    public static Long toEpochSecond(Timestamp timestamp) {
        return timestamp != null ? timestamp.getTime() / 1000 : null;
    }

    public static Timestamp toTimestamp(Long epochSecond) {
        return epochSecond != null ? Timestamp.from(Instant.ofEpochSecond(epochSecond)) : null;
    }

    public static String formatTimestamp(long timestamp) {
        return DateTimeFormatter.ISO_DATE_TIME.format(
                LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC));
    }

    public static String ue(Object s) {
        return URLEncoder.encode(s.toString(), StandardCharsets.UTF_8);
    }

    public static SafeString he(Object s) {
        if (s == null) {
            return new SafeString("");
        }
        return s instanceof SafeString ? (SafeString) s : new SafeString(HtmlUtils.htmlEscape(s.toString()));
    }

    public static String le(Object s) {
        return s != null ? s.toString().replace("%", "%%") : null;
    }

    public static String re(Object s) {
        return s != null ? Pattern.quote(s.toString()) : null;
    }

    public static Instant latest(Instant instant1, Instant instant2) {
        return instant1.isBefore(instant2) ? instant2 : instant1;
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

    public static Duration toDuration(String s) {
        if (s == null || s.equals("")) {
            return null;
        }
        ChronoUnit unit;
        switch (s.charAt(s.length() - 1)) {
            case 's':
                unit = ChronoUnit.SECONDS;
                break;
            case 'm':
                unit = ChronoUnit.MINUTES;
                break;
            case 'h':
                unit = ChronoUnit.HOURS;
                break;
            case 'd':
                unit = ChronoUnit.DAYS;
                break;
            default:
                throw new DurationFormatException(s);
        }
        long amount;
        try {
            amount = Long.parseLong(s.substring(0, s.length() - 1));
        } catch (NumberFormatException e) {
            throw new DurationFormatException(s);
        }
        return Duration.of(amount, unit);
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

    public static String base64encode(byte[] bytes) {
        return bytes != null ? Base64.getEncoder().encodeToString(bytes) : null;
    }

    public static byte[] base64decode(String s) {
        return s != null ? Base64.getDecoder().decode(s) : null;
    }

    public static String base64urlencode(byte[] bytes) {
        return bytes != null ? Base64.getUrlEncoder().encodeToString(bytes) : null;
    }

    public static byte[] base64urldecode(String s) {
        return s != null ? Base64.getUrlDecoder().decode(s) : null;
    }

    public static long currentMoment() {
        return Instant.now().getEpochSecond() * 1000;
    }

    public static int random(int min, int max) {
        return (int) (Math.random() * (max - min)) + min;
    }

    public static String dump(byte[] bytes) {
        StringBuilder buf = new StringBuilder();
        for (byte b : bytes) {
            if (buf.length() > 0) {
                buf.append(' ');
            }
            buf.append(hexByte(b));
        }
        return buf.toString();
    }

    public static String hexByte(byte b) {
        return String.format("%02X", b >= 0 ? b : 256 + (int) b);
    }

}
