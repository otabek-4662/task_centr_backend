# 15. Xavfsizlik

`SecurityConfig` (`@EnableWebSecurity`): sessiyalar stateless
(`SessionCreationPolicy.STATELESS`), CSRF o'chirilgan (cookie'siz token API).

## Filtrlar qanday tartibda ishlaydi

`RateLimitFilter`, keyin `JwtAuthenticationFilter`, keyin
`UsernamePasswordAuthenticationFilter` (ikkala custom filtr ham undan oldin
qo'yilgan). Parollar `BCryptPasswordEncoder` bilan tekshiriladi.

## Qaysi yo'llar ochiq

| Matcher | Ruxsat |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | permitAll |
| `/swagger-ui/**`, `/v3/api-docs/**` | permitAll |
| `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` | permitAll |
| `GET /api/users`, `DELETE /api/users/**` | `ADMIN` ilova roli |
| `/api/workspaces/**` | `USER` yoki `ADMIN` |
| qolgan hamma yo'l | autentifikatsiyalangan user |

Parol noto'g'ri bo'lsa 401, ruxsat bo'lmasa yoki anonim bo'lsa 403 chiqadi.

## CORS

`CorsConfigurationSource` quyidagilarga ruxsat beradi:
`https://task-manager-frontend.vercel.app`,
`https://task-center-frontend.onrender.com`,
`http://localhost:3000|5173|5174`. Metodlar `GET,POST,PUT,DELETE,PATCH,OPTIONS`,
hamma header, credential'lar ruxsat.

## Parollar, tokenlar, header'lar

- Parollar faqat BCrypt hash ko'rinishida saqlanadi va hech qaysi DTO'da
  tashqariga chiqmaydi.
- JWT `Authorization: Bearer <token>` orqali yuboriladi. WebSocket handshake xuddi
  shu header yoki `?token=` query param qabul qiladi.
- Refresh tokenlar yo'q, serverda token chaqirib olish ro'yxati yo'q (04-bo'limga qarang).
- Xavfsizlik javob header'lari Spring default'lari (`X-Content-Type-Options`,
  `X-Frame-Options: DENY` ... test javoblarida ko'rinadi). Maxsus CSP/HSTS yo'q.
