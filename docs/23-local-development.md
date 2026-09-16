# 23. Lokal ishga tushirish

## 1. Talablar

Java 17+, PostgreSQL 14+, Maven wrapper repo ichida (`mvnw`/`mvnw.cmd`, alohida
o'rnatish shart emas). Windows'da `start.bat` ni bossangiz `mvnw spring-boot:run`
yuguradi.

## 2. Clone

```bash
git clone https://github.com/otabek-4662/task_centr_backend.git
cd task_centr_backend
```

## 3. Muhit o'zgaruvchilari

`.env.example` ni `.env` ga nusxalang va to'ldiring (22-bo'limga qarang).
Default qiymatlar lokal Postgres (`taskcenter`/`postgres`/`password`) bilan ishlaydi.

## 4. PostgreSQL + baza

```bash
docker compose up -d postgres   # postgres:16, taskcenter DB, 5432 port
# yoki lokal serverda: CREATE DATABASE taskcenter;
```

## 5. Migratsiyalar

Flyway startup'da avtomatik ishlaydi (V1-V10). Bo'sh bazaga demo seed data
(workspace, column'lar, labellar, tasklar) `DataSeeder` orqali tushadi.

## 6. Run

```bash
./mvnw spring-boot:run        # Linux/macOS
mvnw.cmd spring-boot:run      # Windows
```

## 7. Testlar

```bash
./mvnw test                    # 98 test, H2 in-memory
.\swagger_smoke_test.ps1       # live smoke test (~39 endpoint)
.\swagger_smoke_test.ps1 -BaseUrl "http://localhost:8080"
```

## 8. Swagger va health

- Swagger: `http://localhost:8080/swagger-ui/index.html` (register qilib, Authorize
  ga `Bearer <token>` qo'yiladi)
- Health: `http://localhost:8080/actuator/health`
