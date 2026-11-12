package io.github.risaber.phoneregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;

/**
 * Spring-конфигурация: реестр заполняется из json-файла,
 * путь к которому задан свойством {@code providers.file}.
 */
@Configuration
public class RegistryConfig {

    @Bean
    public ProviderSource providerSource(@Value("${providers.file}") Path file, ObjectMapper objectMapper) {
        return new JsonFileProviderSource(file, objectMapper);
    }

    /** RestClient.Builder настраивает Spring Boot (Jackson, таймауты из свойств). */
    @Bean
    public ProviderRegistry providerRegistry(RestClient.Builder restClientBuilder, ProviderSource providerSource) {
        ProviderRegistry registry = new ProviderRegistry(restClientBuilder);
        registry.replaceAll(providerSource);
        return registry;
    }
}
