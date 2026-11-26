package com.barboya.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Limita por IP las operaciones de autenticacion, objetivo habitual de fuerza bruta. */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter limiter;

    public AuthRateLimitFilter(RateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
                || !request.getRequestURI().startsWith(request.getContextPath() + "/api/v1/auth/");
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
