package org.moera.node.event;

import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

public class SeenHeader {

    public static class Details {

        Long queueStartedAt;
        Integer lastEvent;

    }

    private static final String HEADER_NAME = "seen";

    public static Details parse(StompHeaderAccessor accessor) {
        String seen = String.valueOf(accessor.getFirstNativeHeader(HEADER_NAME));
        Details details = new Details();
        if (seen != null) {
            String[] parts = seen.split("\\s*,\\s*");
            if (parts.length > 0) {
                try {
                    details.queueStartedAt = Long.parseLong(parts[0]);
                } catch (NumberFormatException e) {
                }
            }
            if (parts.length > 1) {
                try {
                    details.lastEvent = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                }
            }
        }
        return details;
    }

}
