package io.github.risaber.phoneregistry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderTest {

    @Test
    void trailingSlashesAreRemoved() {
        assertThat(new Provider("a", "https://host/api/v1///", "k").baseUrl())
                .isEqualTo("https://host/api/v1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "не url", "ftp://host", "host.example", "https://"})
    void invalidBaseUrlIsRejected(String baseUrl) {
        assertThatThrownBy(() -> new Provider("a", baseUrl, "k"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void apiKeyIsHiddenInToString() {
        assertThat(new Provider("a", "https://host", "secret-key").toString())
                .doesNotContain("secret-key");
    }
}
