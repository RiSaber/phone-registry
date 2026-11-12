package io.github.risaber.phoneregistry;

import java.util.List;

/**
 * Источник набора поставщиков (готовый список, файл и т.п.).
 */
@FunctionalInterface
public interface ProviderSource {

    List<Provider> load();
}
