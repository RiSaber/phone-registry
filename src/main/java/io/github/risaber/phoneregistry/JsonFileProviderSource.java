package io.github.risaber.phoneregistry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Поставщики из json-файла на диске. Формат:
 * <pre>
 * [ {"id": "alpha", "baseUrl": "https://alpha.example", "apiKey": "..."} ]
 * </pre>
 * Файл читается при каждом вызове {@link #load()}, поэтому повторная загрузка
 * подхватывает изменения на диске.
 */
public class JsonFileProviderSource implements ProviderSource {

    private static final TypeReference<List<Provider>> PROVIDER_LIST = new TypeReference<>() { };

    private final Path file;
    private final ObjectMapper objectMapper;

    public JsonFileProviderSource(Path file, ObjectMapper objectMapper) {
        this.file = Objects.requireNonNull(file, "file");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public List<Provider> load() {
        try (var in = Files.newInputStream(file)) {
            List<Provider> providers = objectMapper.readValue(in, PROVIDER_LIST);
            if (providers == null) {
                return List.of();
            }
            if (providers.contains(null)) {
                throw new IllegalArgumentException("Пустой элемент (null) в списке поставщиков: " + file);
            }
            return List.copyOf(providers);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать поставщиков из " + file, e);
        }
    }
}
