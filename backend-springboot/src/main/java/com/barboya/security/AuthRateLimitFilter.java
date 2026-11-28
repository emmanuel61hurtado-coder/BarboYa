package com.barboya.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Limita por IP las operaciones donde se adivinan secretos cortos. Renovar o
 * cerrar sesion queda fuera: usa un token aleatorio largo y ocurre en cada
 * recarga de la pagina, tambien en redes moviles que comparten IP.
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED = Set.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/password/forgot",
            "/api/v1/auth/password/reset",
            "/api/v1/auth/email/verification",
            "/api/v1/auth/email/verify");

    private final RateLimiter limiter;

    public AuthRateLimitFilter(RateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
                || !LIMITED.contains(request.getRequestURI().substring(request.getContextPath().length()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!limiter.allow(request.getRemoteAddr())) {
            response.setHeader("Retry-After", "60");
            ProblemResponses.write(response, 429, "TOO_MANY_REQUESTS", "Demasiados intentos; espera un minuto");
            return;
        }
        chain.doFilter(request, response);
    }
}
