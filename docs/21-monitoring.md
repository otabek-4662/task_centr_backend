# 21. Monitoring

## Actuator

`spring-boot-starter-actuator` va `micrometer-registry-prometheus` ulangan.
`/actuator` ostida auth'siz uchta endpoint ochiq: `health`, `metrics`,
`prometheus`. Health tafsilot rejimi `when-authorized`.

## Baza health check

`DatabaseHealthIndicator` `JdbcTemplate` orqali `SELECT 1` yuguradi. Baza
tirik bo'lsa `UP` va `database=PostgreSQL` detail qaytadi, yiqilsa exception
bilan `DOWN` qaytadi. Birinchi yiqilishda stderr ga bir martalik `DB DOWN ALERT`
yoziladi, tiklanishda `DB UP` yoziladi.

## So'rovlar logi

`RequestLoggingFilter` (`@Order(1)`) har bir so'rovda javobga 8 belgili
`X-Request-ID` header qo'yadi va bitta qator log yozadi: 2xx/3xx uchun `INFO`,
4xx uchun `WARN`, 500+ yoki 1000 ms dan sekin so'rovlar uchun `SLOW`/`ERROR`
darajada, metod, URI, status va davomiylik bilan. Bular Render dashboard va test
output'da ko'rinadigan log qatorlari.

## Prometheus metrikalari

Micrometer'da HikariCP, JVM va cache meterlar yoqilgan
(`metrics.enable.hikaricp/jvm/cache`), `/actuator/prometheus` dan scrape qilinadi.
