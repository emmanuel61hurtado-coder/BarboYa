package com.barboya.security;

import com.barboya.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Claves derivadas de JWT_SECRET. Fuera de los perfiles dev y test la
 * aplicacion no arranca sin un secreto propio.
 */
@Component
public class SecretKeys {

    private static final Logger log = LoggerFactory.getLogger(SecretKeys.class);
    private static final int MIN_LENGTH = 32;

    private final byte[] master;

    public SecretKeys(AppProperties properties, Environment environment) {
        String secret = properties.security().jwtSecret();
        if (secret.length() >= MIN_LENGTH && !secret.startsWith("cambia")) {
            this.master = secret.getBytes(StandardCharsets.UTF_8);
        } else if (secret.isEmpty() && environment.acceptsProfiles(Profiles.of("dev", "test"))) {
            this.master = new byte[48];
            new SecureRandom().nextBytes(this.master);
            log.warn("JWT_SECRET sin definir: se usa uno temporal y las sesiones se invalidan al reiniciar");
        } else {
            throw new IllegalStateException(
                    "Define JWT_SECRET con un valor propio de al menos " + MIN_LENGTH + " caracteres");
        }
    }

    public SecretKey jwtKey() {
        return new SecretKeySpec(hmac("jwt-signing"), "HmacSHA256");
    }

    /** Huella con clave para codigos cortos, que sin ella serian faciles de adivinar. */
    public String codeFingerprint(String value) {
        return HexFormat.of().formatHex(hmac("code:" + value));
    }

    /** Huella de tokens aleatorios largos. */
    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private byte[] hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(master, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
