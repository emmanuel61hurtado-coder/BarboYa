package com.barboya.support;

import com.barboya.legal.LegalDocument;
import com.barboya.users.Role;
import com.barboya.users.User;
import com.barboya.users.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Aplicacion completa contra un PostgreSQL real que arranca con las pruebas.
 * Solo el envio de correo se sustituye, para leer los codigos que saldrian.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTest.ClockConfig.class)
public abstract class IntegrationTest {

    protected static final String PASSWORD = "Segura12345";
    private static final EmbeddedPostgres POSTGRES = startPostgres();
    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() % 1_000_000);

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected MutableClock clock;
    @Autowired protected UserRepository users;
    @Autowired protected PasswordEncoder encoder;
    @MockBean protected JavaMailSender mailSender;

    public record Account(UUID id, String email, String phone, String accessToken, Cookie refresh) {
        public String bearer() {
            return "Bearer " + accessToken;
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @BeforeEach
    void resetClock() {
        clock.reset();
    }

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.builder()
                    .setLocaleConfig("locale", "C")
                    .setLocaleConfig("encoding", "UTF-8")
                    .start();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo iniciar PostgreSQL para las pruebas", ex);
        }
    }

    protected String uniqueEmail() {
        return "persona" + SEQUENCE.incrementAndGet() + "@barboya.test";
    }

    protected String uniquePhone() {
        return "+57300%07d".formatted(SEQUENCE.incrementAndGet() % 10_000_000);
    }

    protected MockHttpServletRequestBuilder postJson(String url, Object body) {
        try {
            return post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    protected JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    protected Account account(MvcResult result) throws Exception {
        JsonNode node = body(result);
        JsonNode user = node.get("user");
        return new Account(UUID.fromString(user.get("id").asText()), user.get("email").asText(),
                user.get("phone").asText(), node.get("accessToken").asText(),
                result.getResponse().getCookie("barboya_refresh"));
    }

    /** Registro publico a traves de la API. */
    protected Account register(Role role) throws Exception {
        MvcResult result = mvc.perform(postJson("/api/v1/auth/register", Map.of(
                "fullName", "Persona de Prueba",
                "email", uniqueEmail(),
                "phone", uniquePhone(),
                "password", PASSWORD,
                "role", role.name(),
                "adult", true,
                "termsVersion", LegalDocument.TERMS.version(),
                "privacyVersion", LegalDocument.PRIVACY.version()))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        return account(result);
    }

    protected MvcResult login(String identifier, String password, String userAgent) throws Exception {
        return mvc.perform(postJson("/api/v1/auth/login",
                Map.of("identifier", identifier, "password", password))
                .header("User-Agent", userAgent)).andReturn();
    }

    /** Cuenta de cualquier rol, incluidos los internos que no admiten registro publico. */
    protected Account accountWithRole(Role role) throws Exception {
        if (!role.isStaff()) {
            return register(role);
        }
        String email = uniqueEmail();
        users.save(User.create("Equipo BarboYa", email, uniquePhone(), encoder.encode(PASSWORD), role,
                Instant.now()));
        return account(login(email, PASSWORD, "pruebas"));
    }

    protected int statusOfMe(Account account) throws Exception {
        return mvc.perform(get("/api/v1/users/me").header("Authorization", account.bearer()))
                .andReturn().getResponse().getStatus();
    }

    /** Correos enviados a una direccion; espera porque el envio es asincrono. */
    protected List<SimpleMailMessage> mailsTo(String email) {
        verify(mailSender, after(600).atLeast(0)).send(any(SimpleMailMessage.class));
        return mockingDetails(mailSender).getInvocations().stream()
                .flatMap(invocation -> Arrays.stream(invocation.getArguments()))
                .filter(SimpleMailMessage.class::isInstance)
                .map(SimpleMailMessage.class::cast)
                .filter(m -> m.getTo() != null && m.getTo()[0].equals(email))
                .toList();
    }

    protected String lastCodeSentTo(String email) {
        List<SimpleMailMessage> mails = mailsTo(email);
        assertThat(mails).isNotEmpty();
        Matcher matcher = Pattern.compile("\\b\\d{6}\\b").matcher(mails.getLast().getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }
}
