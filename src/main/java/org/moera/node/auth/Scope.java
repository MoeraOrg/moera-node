package org.moera.node.auth;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Scope {

    //CHECKSTYLE:OFF
    ALL                     (0x3fffffff, false),
    OTHER                   (0x00000001, true),
    VIEW_MEDIA              (0x00000002, true),
    VIEW_CONTENT            (0x00000004, true),
    ADD_POST                (0x00000008, true),
    UPDATE_POST             (0x00000010, true),
    ADD_COMMENT             (0x00000020, true),
    UPDATE_COMMENT          (0x00000040, true),
    REACT                   (0x00000080, true),
    DELETE_OWN_CONTENT      (0x00000100, true),
    UNDELETE_OWN_CONTENT    (0x00000200, true),
    DELETE_OTHERS_CONTENT   (0x00000400, true),
    UNDELETE_OTHERS_CONTENT (0x00000800, true),
    VIEW_PEOPLE             (0x00001000, true),
    BLOCK_PEOPLE            (0x00002000, true),
    FRIEND                  (0x00004000, true),
    REMOTE_IDENTIFY         (0x00008000, true),
    VIEW_DRAFTS             (0x00010000, true),
    USE_DRAFTS              (0x00020000, true),
    VIEW_FEEDS              (0x00040000, true),
    UPDATE_FEEDS            (0x00080000, true),
    NAME                    (0x00100000, true),
    PLUGINS                 (0x00200000, true),
    VIEW_PROFILE            (0x00400000, true),
    UPDATE_PROFILE          (0x00800000, true),
    SHERIFF                 (0x01000000, true),
    VIEW_SETTINGS           (0x02000000, true),
    UPDATE_SETTINGS         (0x04000000, true),
    SUBSCRIBE               (0x08000000, true),
    MANAGE_TOKENS           (0x10000000, true),
    USER_LISTS              (0x20000000, true),
    REMOTE_ADD_POST         (0x00008008, false),
    REMOTE_UPDATE_POST      (0x00008010, false),
    REMOTE_ADD_COMMENT      (0x00008020, false),
    REMOTE_UPDATE_COMMENT   (0x00008040, false),
    REMOTE_REACT            (0x00008080, false),
    REMOTE_DELETE_CONTENT   (0x00008100, false);
    //CHECKSTYLE:ON

    private final long mask;
    private final boolean basic;

    Scope(long mask, boolean basic) {
        this.mask = mask;
        this.basic = basic;
    }

    public long getMask() {
        return mask;
    }

    public boolean isBasic() {
        return basic;
    }

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static String toValue(Scope scope) {
        return scope != null ? scope.getValue() : null;
    }

    public static Scope forValue(String value) {
        try {
            return parse(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonCreator
    public static Scope parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

    public static List<String> toValues(long mask) {
        List<String> values = new ArrayList<>();
        for (Scope scope : values()) {
            if (scope.isBasic() && (scope.getMask() & mask) != 0) {
                values.add(scope.getValue());
            }
        }
        return values;
    }

    public static long forValues(List<String> values) {
        long mask = 0;
        for (String value : values) {
            Scope scope = forValue(value);
            if (scope == null) {
                continue;
            }
            mask |= scope.getMask();
        }
        return mask;
    }

}
