package io.github.risaber.phoneregistry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * HTTP-клиент одного поставщика.
 * <p>
 * Контракт: {@code GET {baseUrl}/subscribers/{phone}}
 * <ul>
 *     <li>200 и {@code {"status":"ok"}} — номер доступен ({@code true});</li>
 *     <li>404 — номер недоступен ({@code false});</li>
 *     <li>прочие HTTP-ошибки ({@code RestClientResponseException}) и сетевые
 *     ({@code ResourceAccessException}) пробрасываются вызывающему коду.</li>
 * </ul>
 */
public class ProviderClient {

    /** Заголовок с API-ключом (в задании способ передачи не указан). */
    public static final String API_KEY_HEADER = "X-API-Key";

    private static final String STATUS_OK = "ok";

    private final Provider provider;
    private final RestClient restClient;

    /**
     * @param builder общий builder; клонируется, чтобы настройки этого
     *                поставщика не попали в клиенты других
     */
    public ProviderClient(Provider provider, RestClient.Builder builder) {
        this.provider = provider;
        this.restClient = builder.clone()
                .baseUrl(provider.baseUrl())
                .defaultHeader(API_KEY_HEADER, provider.apiKey())
                .build();
    }

    public Provider provider() {
        return provider;
    }

    public boolean isAvailable(String phone) {
        SubscriberResponse body;
        try {
            body = restClient.get()
                    .uri("/subscribers/{phone}", phone)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(SubscriberResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }

        if (body != null && STATUS_OK.equals(body.status())) {
            return true;
        }
        // 200, но тело не по контракту: не выдаём это за «недоступен»
        throw new UnexpectedProviderResponseException(provider.id(),
                "ответ 200 без status=ok: " + body);
    }

    /** Тело ответа поставщика. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SubscriberResponse(String status) {
    }
}
