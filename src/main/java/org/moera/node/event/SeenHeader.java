package org.moera.node.event;

import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

public class SeenHeader {

    public record Details(
        Long queueStartedAt,
        Integer lastEvent
    ) {
    }

    private static final String HEADER_NAME = "seen";

    public static Details parse(StompHeaderAccessor accessor) {
        String seen = String.valueOf(accessor.getFirstNativeHeader(HEADER_NAME));
        String[] parts = seen.split("\\s*,\\s*");
        return new Details(parseLong(parts, 0), parseInteger(parts, 1));
    }

    private static Long parseLong(String[] parts, int index) {
        if (parts.length > index) {
            try {
                return Long.parseLong(parts[index]);
            } catch (NumberFormatException e) {
            }
        }
        return null;
    }

    private static Integer parseInteger(String[] parts, int index) {
        if (parts.length > index) {
            try {
                return Integer.parseInt(parts[index]);
            } catch (NumberFormatException e) {
            }
        }
        return null;
    }

}
