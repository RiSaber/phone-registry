package io.github.risaber.phoneregistry;

import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Реестр HTTP-клиентов поставщиков.
 * <p>
 * Набор поставщиков заменяется целиком: новая неизменяемая карта собирается
 * отдельно и подставляется одной записью в volatile-поле. Читающие потоки
 * видят либо старый набор, либо новый, но никогда не смесь.
 */
public class ProviderRegistry {

    private final RestClient.Builder restClientBuilder;

    private volatile Map<String, ProviderClient> clients = Map.of();

    public ProviderRegistry(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = Objects.requireNonNull(restClientBuilder, "restClientBuilder");
    }

    /** Заменить набор поставщиков данными из источника (список, файл). */
    public void replaceAll(ProviderSource source) {
        replaceAll(source.load());
    }

    /** Заменить набор поставщиков целиком. */
    public void replaceAll(Collection<Provider> providers) {
        Map<String, ProviderClient> next = new HashMap<>();
        for (Provider provider : providers) {
            ProviderClient previous = next.put(provider.id(), new ProviderClient(provider, restClientBuilder));
            if (previous != null) {
                throw new IllegalArgumentException("Повторяющийся id поставщика: " + provider.id());
            }
        }
        // При ошибке выше старый набор остаётся нетронутым
        clients = Map.copyOf(next);
    }

    /**
     * Доступен ли номер у поставщика.
     *
     * @throws UnknownProviderException если поставщика нет в реестре
     */
    public boolean isAvailable(String providerId, String phone) {
        return client(providerId).isAvailable(phone);
    }

    public ProviderClient client(String providerId) {
        ProviderClient client = clients.get(providerId);
        if (client == null) {
            throw new UnknownProviderException(providerId);
        }
        return client;
    }

    public Set<String> providerIds() {
        return clients.keySet();
    }
}
