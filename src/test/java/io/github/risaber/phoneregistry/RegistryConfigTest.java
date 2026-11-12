package io.github.risaber.phoneregistry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверка Spring-контекста: реестр заполняется из json-файла.
 * HTTP-запросы здесь не выполняются.
 */
@SpringBootTest(properties = "providers.file=src/test/resources/providers-test.json")
class RegistryConfigTest {

    @Autowired
    private ProviderRegistry registry;

    @Test
    void registryIsLoadedFromJsonFile() {
        assertThat(registry.providerIds()).containsExactlyInAnyOrder("alpha", "beta");
    }
}
