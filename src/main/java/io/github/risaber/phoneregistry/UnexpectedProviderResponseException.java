package io.github.risaber.phoneregistry;

/**
 * Поставщик ответил 200, но тело не соответствует контракту.
 */
public class UnexpectedProviderResponseException extends RuntimeException {

    public UnexpectedProviderResponseException(String providerId, String details) {
        super("Неожиданный ответ поставщика '" + providerId + "': " + details);
    }
}
