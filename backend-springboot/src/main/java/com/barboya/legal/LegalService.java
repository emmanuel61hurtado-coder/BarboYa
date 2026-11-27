package com.barboya.legal;

import com.barboya.common.ApiException;
import com.barboya.common.ClientInfo;
import com.barboya.config.FeatureProperties;
import com.barboya.config.LegalProperties;
import com.barboya.users.UserRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class LegalService {

    private static final String ADULT = "ADULT";

    /** {@code reviewed} es falso mientras falten datos del responsable o la aprobacion de un abogado. */
    public record Info(
            String termsVersion, String privacyVersion, boolean reviewed,
            boolean pharmacy, boolean driverRegistration) {
    }

    public record Text(String version, boolean reviewed, String body) {
    }

    private final LegalAcceptanceRepository acceptances;
    private final UserRepository users;
    private final LegalProperties operator;
    private final FeatureProperties features;
    private final Clock clock;

    public LegalService(LegalAcceptanceRepository acceptances, UserRepository users, LegalProperties operator,
                        FeatureProperties features, Clock clock) {
        this.acceptances = acceptances;
        this.users = users;
        this.operator = operator;
        this.features = features;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Info info() {
        return new Info(LegalDocument.TERMS.version(), LegalDocument.PRIVACY.version(), reviewed(),
                features.pharmacy(), features.driverRegistration());
    }

    @Transactional(readOnly = true)
    public Text text(String slug) {
        LegalDocument document = Arrays.stream(LegalDocument.values())
                .filter(d -> d.slug().equals(slug))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND",
                        "Documento no encontrado"));
        String body = read(document)
                .replace("{{version}}", document.version())
                .replace("{{responsable}}", orPending(operator.operatorName(), "nombre del responsable"))
                .replace("{{identificacion}}", orPending(operator.operatorId(), "documento o NIT"))
                .replace("{{direccion}}", orPending(operator.operatorAddress(), "dirección"))
                .replace("{{telefono}}", orPending(operator.operatorPhone(), "teléfono"))
                .replace("{{correo}}", orPending(operator.operatorEmail(), "correo"));
        return new Text(document.version(), reviewed(), body);
    }

    /** La persona debe haber visto las versiones vigentes y marcado cada casilla. */
    public void requireCurrent(String termsVersion, String privacyVersion) {
        if (!LegalDocument.TERMS.version().equals(termsVersion)
                || !LegalDocument.PRIVACY.version().equals(privacyVersion)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CONSENT_REQUIRED",
                    "Acepta los terminos y autoriza el tratamiento de tus datos en su version vigente");
        }
    }

    public void recordRegistration(UUID userId, ClientInfo client) {
        record(userId, ADULT, "18+", client);
        for (LegalDocument document : LegalDocument.values()) {
            record(userId, document.name(), document.version(), client);
        }
    }

    /** Documentos cuya version vigente la persona todavia no acepto. */
    @Transactional(readOnly = true)
    public List<LegalDocument> pending(UUID userId) {
        Set<String> accepted = accepted(userId);
        return Arrays.stream(LegalDocument.values())
                .filter(d -> !accepted.contains(d.name() + "|" + d.version()))
                .toList();
    }

    public void accept(UUID userId, String termsVersion, String privacyVersion, ClientInfo client) {
        requireCurrent(termsVersion, privacyVersion);
        // Bloquear a la persona evita registrar dos veces la misma aceptacion
        users.lockById(userId).orElseThrow();
        for (LegalDocument document : pending(userId)) {
            record(userId, document.name(), document.version(), client);
        }
    }

    private Set<String> accepted(UUID userId) {
        return acceptances.findByUserId(userId).stream()
                .map(a -> a.getDocument() + "|" + a.getVersion())
                .collect(Collectors.toSet());
    }

    private void record(UUID userId, String document, String version, ClientInfo client) {
        acceptances.save(new LegalAcceptance(userId, document, version, clock.instant(), client.ip(),
                client.userAgent()));
    }

    private boolean reviewed() {
        return operator.reviewed() && operator.operatorComplete();
    }

    private static String orPending(String value, String what) {
        return value.isBlank() ? "[PENDIENTE: " + what + "]" : value;
    }

    private static String read(LegalDocument document) {
        try {
            return new ClassPathResource("legal/" + document.slug() + ".md")
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
