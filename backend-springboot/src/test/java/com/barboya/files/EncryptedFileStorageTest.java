package com.barboya.files;

import com.barboya.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptedFileStorageTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @TempDir Path directory;

    private EncryptedFileStorage storage(String key, String profile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        return new EncryptedFileStorage(new AppProperties(null, null, null, null,
                new AppProperties.Files(directory.toString(), key)), environment);
    }

    @Test
    void readsBackWhatItStoredEvenAfterARestart() {
        String name = storage(KEY, "prod").store("cedula".getBytes());

        assertThat(storage(KEY, "prod").read(name)).isEqualTo("cedula".getBytes());
    }

    @Test
    void detectsTamperingAndSwappedFiles() throws Exception {
        EncryptedFileStorage storage = storage(KEY, "prod");
        String first = storage.store("uno".getBytes());
        String second = storage.store("dos".getBytes());

        Files.copy(storage.path(first), storage.path(second), StandardCopyOption.REPLACE_EXISTING);
        assertThatThrownBy(() -> storage.read(second)).isInstanceOf(IllegalStateException.class);

        byte[] bytes = Files.readAllBytes(storage.path(first));
        bytes[bytes.length - 1] ^= 1;
        Files.write(storage.path(first), bytes);
        assertThatThrownBy(() -> storage.read(first)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesPathsOutsideItsFolder() {
        assertThatThrownBy(() -> storage(KEY, "prod").read("../../secreto"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void productionNeedsAKey() {
        assertThatThrownBy(() -> storage("", "prod")).hasMessageContaining("FILES_KEY");
        assertThatThrownBy(() -> storage("Y29ydGE=", "dev")).hasMessageContaining("FILES_KEY");
    }
}
