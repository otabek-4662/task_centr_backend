# 22. Konfiguratsiya

Hammasi `src/main/resources/application.yml` da. Secret'larni commit qilmang,
`.env` ishlating (`.env.example` dan nusxa olinadi, git-ignored).

| O'zgaruvchi | Maqsad | Majburiy | Misol |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL | ha | `jdbc:postgresql://localhost:5432/taskcenter` |
| `SPRING_DATASOURCE_USERNAME` | DB user | ha | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | DB parol | ha | `<your-password>` |
| `JWT_SECRET` | HS256 imzo kaliti (256 bitdan kam bo'lmasin) | ha | `<your-secret>` |
| `JWT_EXPIRATION` | Token TTL, ms (default 7 kun) | yo'q | `604800000` |
| `PORT` | HTTP port (Render `10000` beradi) | yo'q | `8080` |
| `ratelimit.auth.enabled` | Auth 10/daq limiter | yo'q | `true` |
| `ratelimit.api.enabled` | API 100/daq limiter | yo'q | `true` |

## Boshqa muhim sozlamalar

- JPA `ddl-auto: validate`, `open-in-view: false`. Flyway yoqilgan,
  `baseline-on-migrate: true`, locations `classpath:db/migration`,
  `validate-on-migrate: false`. Oxirgisi ataylab o'chirilgan: prod'ga bir marta
  qo'lda boshqa checksum'li `V6` applied qilingan edi, u yerda `flyway repair`
  qilingach `true` ga qaytarish mumkin.
- Jackson timestamp'lar, batch size 20, `allow_jdbc_metadata_access: false`.
- Server compression yoqilgan, `forward-headers-strategy: framework` (Render proxy
  uchun).
- Testlar `application-test.yml` bilan override qiladi: H2 (`MODE=PostgreSQL`),
  Flyway o'chiq, rate limit o'chiq, JWT secret fiksirlangan.
