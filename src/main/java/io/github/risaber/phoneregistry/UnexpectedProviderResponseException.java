package io.github.risaber.phoneregistry;

/**
 * Ответ поставщика не соответствует контракту: код не 200/404
 * или тело ответа 200 не {@code {"status":"ok"}}.
 */
public class UnexpectedProviderResponseException extends RuntimeException {

    public UnexpectedProviderResponseException(String providerId, String details) {
        super(message(providerId, details));
    }

    public UnexpectedProviderResponseException(String providerId, String details, Throwable cause) {
        super(message(providerId, details), cause);
    }

    private static String message(String providerId, String details) {
        return "Неожиданный ответ поставщика '" + providerId + "': " + details;
    }
}
