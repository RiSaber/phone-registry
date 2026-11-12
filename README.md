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
| другие HTTP-ошибки (4xx, 5xx) | исключение `RestClientResponseException` (`HttpClientErrorException` / `HttpServerErrorException`) |
| сетевые ошибки и таймауты | исключение `ResourceAccessException` |
| иной ответ: 2xx/3xx кроме 200, 200 с другим телом или не-JSON | исключение `UnexpectedProviderResponseException` |

## Структура

| Класс | Назначение |
|---|---|
| `Provider` | record: `id`, `baseUrl`, `apiKey` (проверка на пустые значения и http(s)-адрес, ключ скрыт в `toString`) |
| `ProviderSource` | интерфейс источника поставщиков |
| `ListProviderSource` | поставщики готовым списком |
| `JsonFileProviderSource` | поставщики из json-файла |
| `ProviderClient` | HTTP-клиент одного поставщика |
| `ProviderRegistry` | реестр клиентов: `replaceAll(...)`, `isAvailable(providerId, phone)` |
| `ProviderHttpRequestFactory` | транспорт: таймауты соединения и чтения, редиректы отключены |
| `RegistryConfig` | Spring-конфигурация: реестр из файла (`providers.file`), таймауты (`providers.connect-timeout`, `providers.read-timeout`, по умолчанию 2 с и 5 с) |

Формат файла (см. `providers.example.json`):

```json
[
  {"id": "alpha", "baseUrl": "https://api.alpha.example", "apiKey": "alpha-secret-key"}
]
```

## Принятые допущения

- **API-ключ** передаётся в заголовке `X-API-Key`: в задании способ не указан. Заголовок задан константой `ProviderClient.API_KEY_HEADER`.
- **Успех — строго код 200** с телом `{"status":"ok"}`. Тело разбирается независимо от `Content-Type`. Любой другой ответ (201, 204, 3xx, иное тело, не-JSON) — `UnexpectedProviderResponseException`: непонятный ответ не стоит молча выдавать за «недоступен».
- **Таймауты** обязательны: иначе молчащий поставщик подвесил бы вызов навсегда и ошибка не дошла бы до вызывающего кода.
- **Редиректы не выполняются**, чтобы API-ключ не ушёл на другой хост; 3xx возвращается как неожиданный ответ.
- **Номер телефона**: пустой номер, `.` и `..` отклоняются `IllegalArgumentException` без запроса к поставщику. Остальное подставляется через URI-шаблон и кодируется как один сегмент пути (`+` → `%2B`, `/` → `%2F`). Формат номера не проверяется: в задании он не задан.
- **Неизвестный id поставщика** (в том числе `null`): бросается `UnknownProviderException`.
- **Замена набора** атомарна: новая неизменяемая карта собирается отдельно и подставляется одной записью в `volatile`-поле. При ошибке (повторяющийся `id`, битый файл) старый набор остаётся.
- **Json-файл**: лишние поля игнорируются; битый файл или некорректный поставщик дают исключение с путём к файлу. Если файл записан с синтаксической ошибкой, Jackson может процитировать фрагмент файла (в том числе ключ) в тексте причины.

## Тесты

Сетевой вызов подменяется `MockRestServiceServer`, реального HTTP-запроса нет.

- `ProviderClientTest`: успешный ответ, 404, а также 500, 401, сетевая ошибка, 200 с неожиданным/не-JSON телом, 201/204/302, ответ с `text/plain`, некорректный номер, кодирование номера; проверяются URL, метод и заголовок с ключом.
- `ProviderRegistryTest`: маршрутизация по id, неизвестный поставщик, замена набора целиком, откат при дублях.
- `JsonFileProviderSourceTest`: чтение файла, лишние поля, элемент `null`, отсутствующий файл.
- `ProviderTest`: нормализация и проверка `baseUrl`, скрытие ключа.
- `ProviderHttpRequestFactoryTest`: таймауты и отключённые редиректы (без сетевого вызова).
- `RegistryConfigTest`: Spring-контекст собирает реестр из json-файла.

Запуск (Maven ставить не нужно, wrapper скачает его сам):

```bash
./mvnw test        # Linux / macOS
mvnw.cmd test      # Windows
```

`PhoneRegistryApplication` — контейнер Spring-бинов без веб-сервера: стартует, собирает реестр из `providers.example.json` (путь относительно рабочего каталога) и завершается.
