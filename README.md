# phone-registry

Сервис-реестр HTTP-клиентов нескольких поставщиков. По идентификатору поставщика и номеру телефона сервис узнаёт, доступен ли номер.

Стек: Java 17, Spring Boot 3.3 (без веб-сервера), `RestClient`, Jackson, JUnit 5, AssertJ.

## Как это работает

```java
ProviderRegistry registry = new ProviderRegistry(RestClient.builder());

// Поставщики готовым списком
registry.replaceAll(new ListProviderSource(List.of(
        new Provider("alpha", "https://api.alpha.example", "alpha-key"))));

// ...или из json-файла на диске (набор заменяется целиком)
registry.replaceAll(new JsonFileProviderSource(Path.of("providers.json"), new ObjectMapper()));

boolean available = registry.isAvailable("alpha", "77001234567");
```

Запрос к поставщику: `GET {baseUrl}/subscribers/{phone}`.

| Ответ поставщика | Результат |
|---|---|
| `200` и `{"status":"ok"}` | `true`, номер доступен |
| `404` | `false`, номер недоступен |
| другие HTTP-ошибки | исключение `RestClientResponseException` (`HttpClientErrorException` / `HttpServerErrorException`) |
| сетевые ошибки | исключение `ResourceAccessException` |

## Структура

| Класс | Назначение |
|---|---|
| `Provider` | record: `id`, `baseUrl`, `apiKey` (с проверкой на пустые значения, ключ скрыт в `toString`) |
| `ProviderSource` | интерфейс источника поставщиков |
| `ListProviderSource` | поставщики готовым списком |
| `JsonFileProviderSource` | поставщики из json-файла |
| `ProviderClient` | HTTP-клиент одного поставщика |
| `ProviderRegistry` | реестр клиентов: `replaceAll(...)`, `isAvailable(providerId, phone)` |
| `RegistryConfig` | Spring-конфигурация: реестр из файла, путь в свойстве `providers.file` |

Формат файла (см. `providers.example.json`):

```json
[
  {"id": "alpha", "baseUrl": "https://api.alpha.example", "apiKey": "alpha-secret-key"}
]
```

## Принятые допущения

- **API-ключ** передаётся в заголовке `X-API-Key`: в задании способ не указан. Заголовок задан константой `ProviderClient.API_KEY_HEADER`.
- **200 с телом, отличным от `{"status":"ok"}`**: бросается `UnexpectedProviderResponseException`. Непонятный ответ не стоит молча выдавать за «недоступен».
- **Неизвестный id поставщика**: бросается `UnknownProviderException`.
- **Замена набора** атомарна: новая неизменяемая карта собирается отдельно и подставляется одной записью в `volatile`-поле. При ошибке (например, повторяющийся `id`) старый набор остаётся.
- Номер подставляется через URI-шаблон, поэтому спецсимволы кодируются.

## Тесты

Сетевой вызов подменяется `MockRestServiceServer`, реального HTTP-запроса нет.

- `ProviderClientTest`: успешный ответ, 404, а также 500, 401, сетевая ошибка, 200 с неожиданным телом; проверяются URL, метод и заголовок с ключом.
- `ProviderRegistryTest`: маршрутизация по id, неизвестный поставщик, замена набора целиком, откат при дублях.
- `JsonFileProviderSourceTest`: чтение файла, отсутствующий файл.
- `RegistryConfigTest`: Spring-контекст собирает реестр из json-файла.

Запуск (Maven ставить не нужно, wrapper скачает его сам):

```bash
./mvnw test        # Linux / macOS
mvnw.cmd test      # Windows
```
