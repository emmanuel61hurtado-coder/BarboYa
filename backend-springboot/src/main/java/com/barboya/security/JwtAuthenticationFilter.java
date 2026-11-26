package com.barboya.security;

import com.barboya.auth.AuthSessionRepository;
import com.barboya.users.User;
import com.barboya.users.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.List;

/**
 * Valida el token y ademas la sesion y la cuenta en la base: cerrar una sesion o
 * suspender a alguien surte efecto de inmediato, y el rol siempre es el actual.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final AuthSessionRepository sessions;
    private final UserRepository users;
    private final Clock clock;

    public JwtAuthenticationFilter(JwtService jwt, AuthSessionRepository sessions, UserRepository users, Clock clock) {
        this.jwt = jwt;
        this.sessions = sessions;
        this.users = users;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            jwt.parse(header.substring(7))
                    .filter(claims -> sessions.findById(claims.sessionId())
                            .filter(s -> s.getUserId().equals(claims.userId()) && s.isActive(clock.instant()))
                            .isPresent())
                    .ifPresent(claims -> users.findById(claims.userId())
                            .filter(user -> !user.isSuspended())
                            .ifPresent(user -> authenticate(user, new AuthUser(
                                    user.getId(), claims.sessionId(), user.getRole()))));
        }
        chain.doFilter(request, response);
    }

    private static void authenticate(User user, AuthUser principal) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
        SecurityContextHolder.setContext(context);
    }
}
