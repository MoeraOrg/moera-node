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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.github.jknack.handlebars.Handlebars.SafeString;
import com.ibm.icu.util.Calendar;
import org.springframework.web.util.HtmlUtils;

public class Util {

    public static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    public static Timestamp farFuture() {
        return Timestamp.from(Instant.now().plus(3650, ChronoUnit.DAYS));
    }

    public static Timestamp farPast() {
        return Timestamp.from(Instant.EPOCH);
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

    public static Duration mulPow2(Duration duration, int power) {
        if (power <= 0) {
            throw new IllegalArgumentException("Power must be positive");
        }
        Duration result = duration;
        for (int i = 2; i <= power; i++) {
            result = result.plus(result);
        }
        return result;
    }

    public static String ue(Object s) {
        return URLEncoder.encode(s.toString(), StandardCharsets.UTF_8);
    }

    public static SafeString he(Object s) {
        if (s == null) {
            return new SafeString("");
        }
        return s instanceof SafeString ss ? ss : new SafeString(HtmlUtils.htmlEscape(s.toString()));
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
            return "now";
        }
        diff /= 60;
        if (diff < 60) {
            return "%d\u00a0min".formatted(diff);
        }
        diff /= 60;
        if (diff < 24) {
            return "%d\u00a0hr".formatted(diff);
        }
        diff /= 24;
        if (diff < 14) {
            return "%d\u00a0d".formatted(diff);
        }
        if (diff < 35) {
            return "%d\u00a0wk".formatted(diff / 7);
        }
        if (diff < 365) {
            return "%d\u00a0mo".formatted(diff / 30);
        }
        diff /= 365;
        return "%d\u00a0yr".formatted(diff);
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
        throw new IllegalArgumentException("\"%s\" is not a valid value for boolean".formatted(value));
    }

    public static Duration toDuration(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        ChronoUnit unit = switch (s.charAt(s.length() - 1)) {
            case 's' -> ChronoUnit.SECONDS;
            case 'm' -> ChronoUnit.MINUTES;
            case 'h' -> ChronoUnit.HOURS;
            case 'd' -> ChronoUnit.DAYS;
            default -> throw new DurationFormatException(s);
        };
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
        return bytes != null ? Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) : null;
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
            if (!buf.isEmpty()) {
                buf.append(' ');
            }
            buf.append(hexByte(b));
        }
        return buf.toString();
    }

    public static String hexByte(byte b) {
        return "%02X".formatted(b >= 0 ? b : 256 + (int) b);
    }

    public static Optional<UUID> uuid(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static <X extends Throwable> UUID uuidOrNull(
        String value, Supplier<? extends X> exceptionSupplier
    ) throws X {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw exceptionSupplier.get();
        }

    }

}
