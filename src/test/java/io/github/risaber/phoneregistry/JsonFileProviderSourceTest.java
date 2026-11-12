package io.github.risaber.phoneregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonFileProviderSourceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsProvidersFromFile(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("providers.json");
        Files.writeString(file, """
                [
                  {"id": "alpha", "baseUrl": "https://alpha.example", "apiKey": "alpha-key"},
                  {"id": "beta",  "baseUrl": "https://beta.example/v1/", "apiKey": "beta-key"}
                ]
                """);

        var providers = new JsonFileProviderSource(file, objectMapper).load();

        assertThat(providers).containsExactly(
                new Provider("alpha", "https://alpha.example", "alpha-key"),
                new Provider("beta", "https://beta.example/v1", "beta-key"));
    }

    @Test
    void unknownFieldsAreIgnored(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("providers.json");
        Files.writeString(file, """
                [{"id": "alpha", "baseUrl": "https://alpha.example", "apiKey": "k", "comment": "лишнее поле"}]
                """);

        assertThat(new JsonFileProviderSource(file, objectMapper).load())
                .containsExactly(new Provider("alpha", "https://alpha.example", "k"));
    }

    @Test
    void nullElementFailsWithFileName(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("providers.json");
        Files.writeString(file, "[null]");

        assertThatThrownBy(new JsonFileProviderSource(file, objectMapper)::load)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("providers.json");
    }

    @Test
    void missingFileFails(@TempDir Path dir) {
        var source = new JsonFileProviderSource(dir.resolve("absent.json"), objectMapper);

        assertThatThrownBy(source::load).isInstanceOf(UncheckedIOException.class);
    }
}
