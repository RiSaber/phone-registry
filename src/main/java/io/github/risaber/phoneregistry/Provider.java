package io.github.risaber.phoneregistry;

import java.util.Objects;

/**
 * Поставщик: идентификатор, базовый URL API и API-ключ.
 */
public record Provider(String id, String baseUrl, String apiKey) {

    public Provider {
        id = requireNotBlank(id, "id");
        baseUrl = requireNotBlank(baseUrl, "baseUrl");
        apiKey = requireNotBlank(apiKey, "apiKey");
        // Убираем завершающий слэш, чтобы не получить "//subscribers"
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
    }

    private static String requireNotBlank(String value, String field) {
        Objects.requireNonNull(value, field + " не задан");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " пустой");
        }
        return value.trim();
    }

    /** Ключ не выводим в логи целиком. */
    @Override
    public String toString() {
        return "Provider[id=" + id + ", baseUrl=" + baseUrl + ", apiKey=***]";
    }
}
