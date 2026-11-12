package io.github.risaber.phoneregistry;

import java.util.List;

/**
 * Поставщики, переданные готовым списком.
 */
public class ListProviderSource implements ProviderSource {

    private final List<Provider> providers;

    public ListProviderSource(List<Provider> providers) {
        // Копия защищает от изменений исходного списка снаружи
        this.providers = List.copyOf(providers);
    }

    @Override
    public List<Provider> load() {
        return providers;
    }
}
