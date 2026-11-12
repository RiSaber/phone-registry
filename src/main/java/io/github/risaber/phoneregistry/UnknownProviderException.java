package io.github.risaber.phoneregistry;

/**
 * Поставщика с таким идентификатором нет в реестре.
 */
public class UnknownProviderException extends RuntimeException {

    public UnknownProviderException(String providerId) {
        super("Неизвестный поставщик: " + providerId);
    }
}
