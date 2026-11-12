package io.github.risaber.phoneregistry;

import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяем настройки соединения без сетевого вызова:
 * openConnection() только создаёт объект, подключение не выполняется.
 */
class ProviderHttpRequestFactoryTest {

    @Test
    void setsTimeoutsAndDisablesRedirects() throws Exception {
        var factory = new ProviderHttpRequestFactory(Duration.ofSeconds(2), Duration.ofSeconds(5));
        var connection = (HttpURLConnection) URI.create("http://provider.invalid/subscribers/1")
                .toURL().openConnection();

        factory.prepareConnection(connection, "GET");

        assertThat(connection.getConnectTimeout()).isEqualTo(2000);
        assertThat(connection.getReadTimeout()).isEqualTo(5000);
        assertThat(connection.getInstanceFollowRedirects()).isFalse();
    }
}
