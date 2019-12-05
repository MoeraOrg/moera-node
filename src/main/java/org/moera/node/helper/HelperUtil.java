package org.moera.node.helper;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.github.jknack.handlebars.Options;
import org.moera.node.util.Util;

public class HelperUtil {

    public static void safeAppend(StringBuilder buf, Object s) {
        buf.append(Util.he(s));
    }

    public static <T> T mandatoryHash(String name, Options options) {
        T value = options.hash(name);
        if (value == null) {
            throw new MissingArgumentException(name);
        }
        return value;
    }

    public static Long integerArg(String paramName, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Short) {
            return ((Short) value).longValue();
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Double) {
            return ((Double) value).longValue();
        }
        String valueS = value.toString();
        if (valueS.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(valueS);
        } catch (NumberFormatException e) {
            throw new TypeMismatchException(paramName, "integer", valueS);
        }
    }

    public static long intArg(String paramName, Object value) {
        Long intValue = integerArg(paramName, value);
        return intValue != null ? intValue : 0;
    }

    public static Long integerArg(int paramN, Object value) {
        return integerArg(Integer.toString(paramN), value);
    }

    public static long intArg(int paramN, Object value) {
        return intArg(Integer.toString(paramN), value);
    }

    public static Boolean booleanArg(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return ((Integer) value) != 0;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String valueS = value.toString();
        if (valueS.isEmpty()) {
            return false;
        }
        if (valueS.equals("0")) {
            return false;
        }
        if (valueS.equals("1")) {
            return true;
        }
        return Boolean.parseBoolean(valueS);
    }

    public static boolean boolArg(Object value) {
        Boolean valueB = booleanArg(value);
        return valueB != null ? valueB : false;
    }

    public static String boolResult(boolean value) {
        return value ? "true" : "";
    }

    public static LocalDateTime timestampArg(String paramName, Object value) {
        return timestampArg(paramName, value, true);
    }

    public static LocalDateTime timestampArg(String paramName, Object value, boolean defaultNow) {
        if (value == null) {
            return defaultNow ? LocalDateTime.now() : null;
        }
        if (value instanceof Instant) {
            return ((Instant) value).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        if (value instanceof LocalDate) {
            return ((LocalDate) value).atStartOfDay();
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof ZonedDateTime) {
            return ((ZonedDateTime) value).toLocalDateTime();
        }
        if (value instanceof Date) {
            return ((Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        return Instant.ofEpochMilli(HelperUtil.intArg(paramName, value))
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public static void appendAttr(StringBuilder buf, String attrName, Object value) {
        if (value != null) {
            buf.append(' ');
            buf.append(attrName);
            buf.append("=\"");
            safeAppend(buf, value);
            buf.append('"');
        }
    }

    public static void appendAttr(StringBuilder buf, String attrName, boolean value) {
        if (value) {
            buf.append(' ');
            buf.append(attrName);
        }
    }

}
