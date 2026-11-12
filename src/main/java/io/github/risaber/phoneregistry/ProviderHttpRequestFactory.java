package io.github.risaber.phoneregistry;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Duration;

/**
 * Транспорт для запросов к поставщикам:
 * <ul>
 *     <li>таймауты соединения и чтения — зависший поставщик не подвешивает
 *     вызов навсегда, а даёт {@code ResourceAccessException};</li>
 *     <li>редиректы не выполняются — API-ключ не уйдёт на чужой хост,
 *     а 3xx вернётся как неожиданный ответ.</li>
 * </ul>
 */
public class ProviderHttpRequestFactory extends SimpleClientHttpRequestFactory {

    public ProviderHttpRequestFactory(Duration connectTimeout, Duration readTimeout) {
        setConnectTimeout(connectTimeout);
        setReadTimeout(readTimeout);
    }

    @Override
    protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
        super.prepareConnection(connection, httpMethod);
        connection.setInstanceFollowRedirects(false);
    }
}
