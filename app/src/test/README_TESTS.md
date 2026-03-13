# Unit-тесты RWBOT Android

## Одна точка входа

Запуск **всех** тестов из одной точки:

- **В Android Studio:** откройте `AllTestsSuite.kt` → зелёный треугольник рядом с классом → **Run 'AllTestsSuite'**.
- **Из командной строки:**
  ```bash
  ./gradlew :app:testDebugUnitTest
  ```
  (на Windows: `gradlew.bat`)

Оба способа выполняют один и тот же набор тестов.

---

## Что покрыто тестами

| Модуль | Класс теста | Что проверяется |
|--------|-------------|------------------|
| **Domain** | `ReviewClassifierTest` | Классификация: blacklist, благодарность, жалоба, сложность |
| | `DecisionEngineTest` | Решение AUTO_SEND / MODERATE по порогам и blacklist |
| | `ReviewPipelineTest` | Пайплайн: уже обработан, ошибка Yandex, модерация, автоотправка, ошибка WB |
| **Data — API** | `WildberriesApiTest` | GET/POST WB: путь, query, парсинг, 401 |
| | `YandexApiTest` | POST Yandex completion: путь, парсинг ответа |
| **Data — репозитории** | `ReviewRepositoryImplTest` | Синхронизация с WB, 401, отправка ответа, счётчик модерации |
| | `YandexRepositoryTest` | Успех, пустой ответ, 401 |
| **UI — ViewModels** | `ReviewsViewModelTest` | Список, фильтр, синхронизация, сообщения |
| | `ReviewDetailViewModelTest` | Загрузка, обработка, одобрение, отклонение |
| | `StatsViewModelTest` | Счётчики по статусам, перезагрузка |
| | `SettingsViewModelTest` | Загрузка настроек, обновление полей, сохранение в SecureSettings |

---

## Зависимости тестов

- **JUnit 4** — прогон тестов
- **MockK** — моки для Kotlin
- **kotlinx-coroutines-test** — `runTest`, `UnconfinedTestDispatcher`, `advanceUntilIdle`
- **MockWebServer** (OkHttp) — эмуляция WB и Yandex API
- **MainCoroutineRule** — подмена `Dispatchers.Main` для ViewModel-тестов
