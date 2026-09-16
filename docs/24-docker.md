# 24. Docker

## Dockerfile (multi-stage)

1. `build` bosqichi: `maven:3.9.9-eclipse-temurin-17`. Avval `pom.xml` nusxalanadi
   (layer cache uchun `dependency:go-offline`), keyin `src`, `mvnw`, `.mvn` va
   `mvn package -DskipTests -B` yuguradi.
2. `runtime` bosqichi: `eclipse-temurin:17-jre-alpine` va `curl` (healthcheck uchun).
   Build'dagi repackaged jar `app.jar` sifatida olinadi, `EXPOSE 8080`,
   `ENTRYPOINT ["java", "-jar", "app.jar"]`.

```bash
docker build -t taskcenter-backend .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/taskcenter \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=password \
  -e JWT_SECRET=<your-secret> \
  taskcenter-backend
```

## docker-compose.yml (lokal dev)

- `postgres:16` (`taskcenter-db`), login/parol `postgres/password`, 5432 port,
  data `pgdata` da saqlanadi, `pg_isready` healthcheck.
- `backend` (eclipse-temurin:17-jdk) oldindan build qilingan `./app.jar` ni `:ro`
  mount qiladi, sog'lom Postgres'ni kutib keyin ko'tariladi, datasource env'larni
  va `SPRING_JPA_HIBERNATE_DDL_AUTO=update` ni oladi, 8080 portda turadi,
  `restart: unless-stopped`.

```bash
./mvnw package -DskipTests   # ./target/...jar ni ./app.jar ga nusxalash kerak
docker compose up -d postgres
docker compose up backend
```
