# taxi-service

Сервис такси на Spring Boot.

## Services

- `user-service` (`:8081`) - пассажиры, водители, auth
- `trip-service` (`:8082`) - поездки, статусы, рейтинг, статистика
- `notification-worker-service` (`:8083`) - задачи уведомлений и воркеры
- Infra: PostgreSQL (`:5433`), Redis (`:6379`), RabbitMQ (`:5672`, UI `:15672`)

## Run

```bash
docker-compose up --build
```

### User Service (`http://localhost:8081`)

- `POST /auth/register` - регистрация пользователя (PASSENGER/DRIVER)
- `POST /auth/login` - логин
- `GET /passengers/{id}` - получить пассажира
- `PATCH /passengers/{id}` - обновить профиль пассажира
- `GET /drivers/{id}` - получить водителя
- `PATCH /drivers/{id}` - обновить профиль водителя
- `PATCH /drivers/{id}/status` - обновить статус водителя (AVAILABLE/BUSY/OFFLINE)

Internal endpoints (межсервисные):

- `GET /internal/passengers/{id}/exists` - проверка существования пассажира
- `GET /internal/drivers/available` - список свободных водителей
- `POST /internal/drivers/assign` - атомарно назначить свободного водителя (pessimistic lock + LIMIT 1)
- `PATCH /internal/drivers/{id}/status` - обновить статус водителя

### Trip Service (`http://localhost:8082`)

- `POST /trips` - создать поездку (автоматически назначает водителя через user-service)
- `GET /trips/{id}` - получить поездку
- `GET /trips?passenger_id={id}` - история поездок пассажира
- `PATCH /trips/{id}/status` - обновить статус поездки
- `POST /trips/{id}/rate` - оценить поездку (1-5)
- `GET /trips/stats` - статистика поездок (за день, средняя цена)

### Notification Worker Service (`http://localhost:8083`)

- `POST /notifications` - создать задачу уведомления
- `GET /notifications?trip_id={id}` - получить уведомления по поездке
