package com.barboya.files;

import com.barboya.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

/**
 * Archivos en disco cifrados con AES-256-GCM. Cada uno queda atado a su clave:
 * alterarlo o intercambiarlo con otro hace fallar la lectura.
 */
@Component
public class EncryptedFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(EncryptedFileStorage.class);
    private static final int IV_BYTES = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Path directory;
    private final SecretKeySpec key;

    public EncryptedFileStorage(AppProperties properties, Environment environment) {
        this.directory = Path.of(properties.files().directory());
        this.key = new SecretKeySpec(keyBytes(properties.files().key(), environment), "AES");
    }

    private static byte[] keyBytes(String configured, Environment environment) {
        if (!configured.isBlank()) {
            byte[] bytes = Base64.getDecoder().decode(configured.trim());
            if (bytes.length == 32) {
                return bytes;
            }
        } else if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            log.warn("FILES_KEY sin definir: los archivos subidos no se podran leer despues de reiniciar");
            return bytes;
        }
        throw new IllegalStateException("Define FILES_KEY con 32 bytes aleatorios en base64");
    }

    @Override
    public String store(byte[] content) {
        String name = UUID.randomUUID().toString();
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        byte[] encrypted = crypt(Cipher.ENCRYPT_MODE, name, iv, content);
        byte[] file = Arrays.copyOf(iv, IV_BYTES + encrypted.length);
        System.arraycopy(encrypted, 0, file, IV_BYTES, encrypted.length);
        try {
            Files.createDirectories(directory);
            // Se escribe aparte y se renombra: nunca queda un archivo a medias
            Path temporary = Files.createTempFile(directory, "carga-", ".tmp");
            Files.write(temporary, file);
            Files.move(temporary, path(name), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return name;
    }

    @Override
    public byte[] read(String name) {
        try {
            byte[] file = Files.readAllBytes(path(name));
            return crypt(Cipher.DECRYPT_MODE, name, Arrays.copyOf(file, IV_BYTES),
                    Arrays.copyOfRange(file, IV_BYTES, file.length));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void delete(String name) {
        try {
            Files.deleteIfExists(path(name));
        } catch (IOException ex) {
            log.error("No se pudo borrar el archivo {}", name, ex);
        }
    }

    /** Solo claves generadas aqui: impide salir de la carpeta. */
    Path path(String name) {
        return directory.resolve(UUID.fromString(name) + ".bin");
    }

    private byte[] crypt(int mode, String name, byte[] iv, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, key, new GCMParameterSpec(128, iv));
            cipher.updateAAD(name.getBytes(StandardCharsets.US_ASCII));
            return cipher.doFinal(input);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo procesar el archivo cifrado", ex);
        }
    }
}
