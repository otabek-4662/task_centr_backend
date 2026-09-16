# 18. Rate limiting

Bucket4j `RateLimitFilter` da ishlaydi (`OncePerRequestFilter`, JWT filtrdan oldin
turadi). Bucket'lar in-memory `ConcurrentHashMap` da saqlanadi va mijoz IP'si
bo'yicha ajratiladi (`X-Forwarded-For` birinchi qiymat, bo'lmasa `X-Real-IP`,
bo'lmasa remote address).

| Doira | Path'lar | Limit |
|---|---|---|
| Auth | `/api/auth/*` | har bir IP uchun 10 so'rov/daq (greedy refill 10/daq) |
| API | boshqa `/api/*` | har bir IP uchun 100 so'rov/daq (greedy refill 100/daq) |

O'chirish tugmalari (default `true`): `ratelimit.auth.enabled` va
`ratelimit.api.enabled` (testlarda ikkalasi ham `false`).
`RateLimitConfig.authRateLimiter` bean (10/daq) mavjud, lekin filtr har bir kalit
uchun `resolveBucket` da o'z bucket'ini yasaydi, shuning uchun bean amalda
ishlatilmaydi.

Limit oshsa HTTP 429 qaytadi:
`{"success":false,"message":"Too many requests. Please try again later.","data":null}`.
Limitlar instans xotirasida saqlanadi, replikalar o'rtasida umumiy emas.
