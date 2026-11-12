package io.github.risaber.phoneregistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
 *     ({@code ResourceAccessException}) пробрасываются вызывающему коду;</li>
 *     <li>любой другой ответ (иной 2xx/3xx, 200 с другим телом или не-JSON) —
 *     {@link UnexpectedProviderResponseException}.</li>
 * </ul>
 */
public class ProviderClient {

    /** Заголовок с API-ключом (в задании способ передачи не указан). */
    public static final String API_KEY_HEADER = "X-API-Key";

    private static final String STATUS_OK = "ok";

    /** Только для разбора тела ответа; потокобезопасен после создания. */
    private static final ObjectMapper JSON = new ObjectMapper();

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

    /**
     * @throws IllegalArgumentException если номер пустой или является
     *                                  служебным сегментом пути ({@code .}, {@code ..})
     */
    public boolean isAvailable(String phone) {
        String checkedPhone = validatePhone(phone);

        ResponseEntity<String> response;
        try {
            // Тело читаем строкой: так ответ разбирается независимо от Content-Type
            response = restClient.get()
                    .uri("/subscribers/{phone}", checkedPhone)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class);
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }

        int status = response.getStatusCode().value();
        if (status != 200) {
            throw new UnexpectedProviderResponseException(provider.id(),
                    "ожидался код 200, получен " + status);
        }
        if (isStatusOk(response.getBody())) {
            return true;
        }
        // 200, но тело не по контракту: не выдаём это за «недоступен»
        throw new UnexpectedProviderResponseException(provider.id(),
                "ответ 200 без {\"status\":\"ok\"}");
    }

    private boolean isStatusOk(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        try {
            JsonNode node = JSON.readTree(body);
            return node != null && node.isObject() && STATUS_OK.equals(node.path("status").textValue());
        } catch (JsonProcessingException e) {
            throw new UnexpectedProviderResponseException(provider.id(), "тело ответа 200 не JSON", e);
        }
    }

    private static String validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Номер телефона не задан");
        }
        String trimmed = phone.trim();
        // «.» и «..» сервер или прокси нормализует в другой путь
        if (trimmed.equals(".") || trimmed.equals("..")) {
            throw new IllegalArgumentException("Некорректный номер телефона: " + trimmed);
        }
        return trimmed;
    }
}
