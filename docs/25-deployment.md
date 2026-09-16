# 25. Deploy (Render)

`render.yaml` blueprint: Docker web servis `task-center-backend` va free
`taskcenter-db` Postgres, Ohio region, branch `master`, `autoDeploy: false`.

## Build va runtime

- `Dockerfile` bo'yicha Docker build, health check path `/v3/api-docs`.
- Render `PORT=10000` beradi, `${PORT:8080}` orqali o'qiladi.
  `forward-headers-strategy` proxy'ni to'g'rilaydi. Free instans bo'sh tursa
  uxlaydi (50 soniyadan ko'p sovuq start).
- DB credential'lar `SPRING_DATASOURCE_URL` (connection string),
  `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` sifatida keladi.
  `JWT_SECRET` generatsiya qilinadi, `JWT_EXPIRATION=604800000`.

## Deploy'da baza

Flyway startup'da migrate qiladi (hozir `validate-on-migrate: false`, sababi
22-bo'limda). Sxema V10 da. Seed data bor bo'lsa o'tkazib yuboriladi
(`"Seed data already exists, skip"`).

## Deploy oqimi

1. `main` ga push qilinadi, keyin `master` fast-forward qilinadi
   (`git push origin main:master`).
2. Render `master` commitni avtomatik build qiladi, Deploys bo'limida `Live`
   kutiladi.
3. Tekshiriladi: `/actuator/health` dagi `{"status":"UP"}`, keyin
   `.\swagger_smoke_test.ps1` yuguriladi (default Render URL).
4. Xato chiqsa Logs ochiladi: compile xatolarda fayl/qator ko'rinadi, Flyway
   xatolarda migratsiya versiyasi ko'rinadi, runtime 500 lar
   `RequestLoggingFilter` dagi `requestId` qatorlari bilan solishtiriladi.
