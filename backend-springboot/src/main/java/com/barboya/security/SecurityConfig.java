package com.barboya.security;

import com.barboya.auth.AuthSessionRepository;
import com.barboya.config.AppProperties;
import com.barboya.users.UserRepository;
import com.barboya.web.SpaRoutes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Clock;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String CONTENT_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
            "font-src 'self' data: https://fonts.gstatic.com",
            "img-src 'self' data: blob: https://tile.openstreetmap.org",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt, AuthSessionRepository sessions,
                                    UserRepository users, AppProperties properties, Clock clock) throws Exception {
        var jwtFilter = new JwtAuthenticationFilter(jwt, sessions, users, clock);
        var rateLimit = new AuthRateLimitFilter(
                new RateLimiter(properties.security().authRequestsPerMinute(), clock));

        http
                // La API autentica con el encabezado Authorization. La unica cookie (refresco)
                // es SameSite=Strict, asi que otros sitios no pueden enviarla.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsSource(properties)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h
                        .frameOptions(f -> f.deny())
                        // Solo recursos propios, las fuentes de Google y las teselas del mapa
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_POLICY))
                        // El servidor de mapas exige saber desde que sitio se piden las teselas
                        .referrerPolicy(r -> r.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) -> ProblemResponses.write(
                                response, 401, "UNAUTHENTICATED", "Inicia sesion para continuar"))
                        .accessDeniedHandler((request, response, ex) -> ProblemResponses.write(
                                response, 403, "FORBIDDEN", "No tienes permiso para esta operacion")))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/password/forgot",
                                "/api/v1/auth/password/reset").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        // Fotos del catalogo: las pide el navegador sin encabezado de sesion
                        .requestMatchers(HttpMethod.GET, "/api/v1/photos/*").permitAll()
                        // Interfaz publica: los datos se piden despues a la API, con sesion
                        .requestMatchers(HttpMethod.GET, SpaRoutes.PAGES).permitAll()
                        .requestMatchers(HttpMethod.GET, SpaRoutes.FILES).permitAll()
                        // Solo existen con el perfil dev
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPERADMIN")
                        .requestMatchers("/api/v1/merchant/**").hasAnyRole("RESTAURANT", "STORE")
                        .requestMatchers("/api/v1/courier/**").hasRole("COURIER")
                        .requestMatchers("/api/v1/finance/**").hasAnyRole("RESTAURANT", "STORE", "COURIER")
                        .requestMatchers("/api/v1/orders/**").hasRole("CUSTOMER")
                        // El WebSocket se autentica con su primer mensaje
                        .requestMatchers(HttpMethod.GET, "/ws/v1/events").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        // Todo lo no declarado queda cerrado
                        .anyRequest().denyAll())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimit, JwtAuthenticationFilter.class);
        return http.build();
    }

    private static CorsConfigurationSource corsSource(AppProperties properties) {
        List<String> origins = properties.security().corsAllowedOrigins().stream()
                .map(String::trim)
                .filter(origin -> !origin.isEmpty() && !origin.contains("*"))
                .toList();
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
