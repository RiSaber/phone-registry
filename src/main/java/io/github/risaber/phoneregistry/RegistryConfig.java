package io.github.risaber.phoneregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.time.Duration;

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

    /**
     * RestClient.Builder создаёт Spring Boot (новый экземпляр на каждую точку внедрения),
     * транспорт с таймаутами задаём здесь.
     */
    @Bean
    public ProviderRegistry providerRegistry(
            RestClient.Builder restClientBuilder,
            ProviderSource providerSource,
            @Value("${providers.connect-timeout:2s}") Duration connectTimeout,
            @Value("${providers.read-timeout:5s}") Duration readTimeout) {
        restClientBuilder.requestFactory(new ProviderHttpRequestFactory(connectTimeout, readTimeout));
        ProviderRegistry registry = new ProviderRegistry(restClientBuilder);
        registry.replaceAll(providerSource);
        return registry;
    }
}
