# Task Center Backend - hujjatlar

Spring Boot 3.2.5, Java 17, PostgreSQL asosidagi Task Center Backend uchun
dasturchi hujjati. Hamma ma'lumot repozitoriy kodidan olingan, tegishli sinf
va metod nomlari matn ichida ko'rsatilgan.

## Mundarija

| # | Hujjat | Mazmuni |
|---|---|---|
| 01 | [Umumiy ko'rinish](01-overview.md) | Tizim nima, tushunchalar, so'rov oqimi |
| 02 | [Arxitektura](02-architecture.md) | Qatlamlar, mas'uliyatlar, diagrammalar |
| 03 | [Loyiha strukturasi](03-project-structure.md) | Real paketlar va asosiy sinflar |
| 04 | [Autentifikatsiya](04-authentication.md) | Register, login, JWT, filtr (refresh token yo'q) |
| 05 | [Avtorizatsiya](05-authorization.md) | Rollar, a'zolik, ruxsat jadvali |
| 06 | [Foydalanuvchilar](06-users.md) | User endpoint'lari |
| 07 | [Workspace'lar](07-workspaces.md) | Workspace'lar, prefikslar, a'zolar |
| 08 | [Boardlar](08-boards.md) | Board saqlanadigan entity emas, o'qish view'i |
| 09 | [Column'lar](09-columns.md) | Column CRUD va tartiblash |
| 10 | [Tasklar](10-tasks.md) | Task lifecycle batafsil |
| 11 | [Label'lar](11-labels.md) | Label'lar va task bog'lanishi |
| 12 | [REST API](12-rest-api.md) | To'liq endpoint ma'lumotnomasi |
| 13 | [WebSocket](13-websocket.md) | STOMP endpoint, eventlar, auth |
| 14 | [Baza](14-database.md) | Jadvallar, cheklovlar, ER diagramma |
| 15 | [Xavfsizlik](15-security.md) | Spring Security, CORS, CSRF, public endpoint'lar |
| 16 | [Validatsiya va xatolar](16-validation-errors.md) | Bean Validation, xato o'rami |
| 17 | [Keshlash](17-caching.md) | Caffeine keshlari va tozalash |
| 18 | [Rate limiting](18-rate-limiting.md) | Bucket4j limitlar (auth 10/min, API 100/min) |
| 19 | [Tranzaksiyalar va bloklash](19-transactions-locking.md) | Task yaratishda pessimistic lock |
| 20 | [Testlash](20-testing.md) | 98 ta tekshirilgan test, H2 setup, smoke skript |
| 21 | [Monitoring](21-monitoring.md) | Actuator, Prometheus, request loglar |
| 22 | [Konfiguratsiya](22-configuration.md) | Muhit o'zgaruvchilari |
| 23 | [Lokal ishga tushirish](23-local-development.md) | Setup, run, migrate, Swagger |
| 24 | [Docker](24-docker.md) | Dockerfile va docker-compose |
| 25 | [Deploy](25-deployment.md) | Render blueprint va oqim |

Hamma API javobi `ApiResponse` o'ramida qaytadi
(`success`, `message`, `data`, `timestamp`).
