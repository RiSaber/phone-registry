package io.github.risaber.phoneregistry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.net.URI;
import java.util.Objects;

/**
 * Поставщик: идентификатор, базовый URL API и API-ключ.
 * Лишние поля в json игнорируются независимо от настроек ObjectMapper.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Provider(String id, String baseUrl, String apiKey) {

    public Provider {
        id = requireNotBlank(id, "id");
        baseUrl = normalizeBaseUrl(requireNotBlank(baseUrl, "baseUrl"));
        apiKey = requireNotBlank(apiKey, "apiKey");
    }

    private static String requireNotBlank(String value, String field) {
        Objects.requireNonNull(value, field + " не задан");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " пустой");
        }
        return value.trim();
    }

    /** Проверяет, что это абсолютный http(s)-адрес, и убирает завершающие слэши. */
    private static String normalizeBaseUrl(String baseUrl) {
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        URI uri;
        try {
            uri = URI.create(baseUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("baseUrl не является URL: " + baseUrl, e);
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme) || uri.getHost() == null) {
            throw new IllegalArgumentException("baseUrl должен быть http(s)-адресом: " + baseUrl);
        }
        return baseUrl;
    }

    /** Ключ не выводим в логи целиком. */
    @Override
    public String toString() {
        return "Provider[id=" + id + ", baseUrl=" + baseUrl + ", apiKey=***]";
    }
}
