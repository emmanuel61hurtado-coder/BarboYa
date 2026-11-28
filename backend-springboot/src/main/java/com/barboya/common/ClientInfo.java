package com.barboya.common;

import jakarta.servlet.http.HttpServletRequest;

/** Origen de la peticion, para sesiones y auditoria. */
public record ClientInfo(String ip, String userAgent) {

    public static ClientInfo from(HttpServletRequest request) {
        return new ClientInfo(
                truncate(request.getRemoteAddr(), 64),
                truncate(request.getHeader("User-Agent"), 255));
    }

    public static ClientInfo system() {
        return new ClientInfo(null, null);
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
