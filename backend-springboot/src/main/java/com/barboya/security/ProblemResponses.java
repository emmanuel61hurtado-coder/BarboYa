package com.barboya.security;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Errores emitidos por los filtros, con el mismo formato que los controladores. */
final class ProblemResponses {

    private ProblemResponses() {
    }

    /** Los textos son constantes del codigo: no requieren escapado. */
    static void write(HttpServletResponse response, int status, String code, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"status\":%d,\"code\":\"%s\",\"detail\":\"%s\"}".formatted(status, code, detail));
    }
}
