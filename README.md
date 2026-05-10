# taxi-system

Микросервисный сервис заказа такси.

---

## Компоненты

- user-service (8081) - регистрация, авторизация, профили пассажиров и водителей
- trip-service (8082) - создание поездок, статусы, рейтинг, статистика
- notification-worker-service (8083) - отправка уведомлений

Инфраструктура:
- PostgreSQL (5433)
- Redis (6379)
- RabbitMQ (5672, UI:15672)

---

## Запуск

docker-compose up --build

---

## API

### User Service (localhost:8081)

Регистрация и авторизация:
POST /auth/register - создание пользователя (типы: PASSENGER, DRIVER)
POST /auth/login - вход

Пассажиры:
GET /passengers/{id}
PATCH /passengers/{id}

Водители:
GET /drivers/{id}
PATCH /drivers/{id}
PATCH /drivers/{id}/status

Внутренние эндпоинты (между сервисами):
GET /internal/passengers/{id}/exists
GET /internal/drivers/available
POST /internal/drivers/assign
PATCH /internal/drivers/{id}/status

### Trip Service (localhost:8082)

POST /trips - создание поездки (водитель назначается автоматически)
GET /trips/{id} - детали поездки
GET /trips?passenger_id={id} - история поездок пассажира
PATCH /trips/{id}/status - обновление статуса
POST /trips/{id}/rate - оценка поездки (1-5)
GET /trips/stats - статистика (поездок за день, средняя цена)

### Notification Worker Service (localhost:8083)

POST /notifications - создание уведомления
GET /notifications?trip_id={id} - получение уведомлений по поездке

---

## Тестирование

cd tests
python3 tests.py

---
## Примечания

- При создании поездки водитель назначается через атомарный запрос к user-service с использованием pessimistic lock
- Статус водителя меняется на BUSY после назначения поездки
- После завершения поездки статус водителя возвращается в AVAILABLE
