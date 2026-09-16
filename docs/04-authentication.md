# 04. Autentifikatsiya

Sessiyasiz JWT Bearer autentifikatsiya. Sessiya, cookie va OAuth provayderlar yo'q.

## Ro'yxatdan o'tish (`AuthService.register`)

1. Band `name` bilan urinish `ConflictException` bilan qaytariladi, 409
   (`"Bu nom allaqachon ishlatilmoqda!"`).
2. `User(name, fullName=name, BCrypt(password), role=USER)` yaratiladi. `id` UUID
   (`prePersist`), `email` default `user_<id>@taskcenter.local`.
3. `AuthenticationManager` orqali autentifikatsiya qilinib, natija
   `SecurityContextHolder` ga qo'yiladi.
4. HTTP 201 bilan `AuthResponse{token, user{id,name,fullName}}` qaytadi.
5. `users` keshdagi shu nomdagi yozuv tozalanadi.

Validatsiya qoidalari: `name` bo'sh bo'lmasin, `password` bo'sh bo'lmasin va kamida
6 belgidan iborat bo'lsin.

## Kirish (`AuthService.login`)

1. `AuthenticationManager.authenticate(UsernamePasswordAuthenticationToken(name, password))`
   chaqiriladi. Ma'lumot noto'g'ri bo'lsa `BadCredentialsException` chiqadi, 401
   (`"Nom yoki parol xato"`).
2. User yuklanadi (`getUserByName`, `users` keshidan) va HTTP 200 bilan `AuthResponse`
   qaytadi.

## Parollar

Faqat BCrypt (`SecurityConfig` dagi `BCryptPasswordEncoder` bean). Parol hech qaysi
DTO'da tashqariga chiqmaydi.

## JWT yaratish (`JwtTokenProvider`, HS256)

Subject qilib user `name` yoziladi, claim'larga `id` va `role` qo'shiladi, berilgan
vaqt va amal qilish muddati qo'yiladi (`jwt.expiration`, default 604800000 ms, ya'ni
7 kun). `generateToken(Authentication)` principal'ni `User` ga cast qiladi.
Yordamchi metodlar: `getUserNameFromJWT` (subject'ni o'qiydi), `getUserIdFromToken`
(`id` claim'ni o'qiydi), `validateToken` (har qanday `JwtException` yoki
`IllegalArgumentException` da false qaytaradi).

## Har so'rovda JWT tekshiruv (`JwtAuthenticationFilter`)

`Authorization: Bearer <token>` header'ni o'qiydigan `OncePerRequestFilter`. Token
bor va valid bo'lsa, `CustomUserDetailsService` orqali `UserDetails` yuklanadi
(`UserRepository.findByName`) va `SecurityContextHolder` ga
`UsernamePasswordAuthenticationToken` qo'yiladi. Xato bo'lsa yutib yuboriladi va
so'rov autentifikatsiyasiz davom etadi, keyin avtorizatsiyadan yiqiladi (odatda 403).

## Qaysi yo'llar ochiq

`POST /api/auth/register`, `POST /api/auth/login`, `/swagger-ui/**`,
`/v3/api-docs/**` va `/actuator/health|metrics|prometheus` dan boshqa hamma yo'l
valid token talab qiladi (`anyRequest().authenticated()`).

```mermaid
sequenceDiagram
    participant C as Mijoz
    participant A as AuthController
    participant S as AuthService
    participant M as AuthenticationManager
    participant J as JwtTokenProvider
    C->>A: POST /api/auth/login {name, password}
    A->>S: login()
    S->>M: authenticate()
    M-->>S: Authentication(User)
    S->>J: generateToken()
    J-->>S: JWT (sub=name, id, role)
    S-->>C: 200 {token, user}
    Note over C: Keyingi so'rovlar:<br/>Authorization: Bearer &lt;token&gt;<br/>→ JwtAuthenticationFilter → SecurityContext
```

## Refresh tokenlar

Yo'q. Refresh endpoint ham, rotatsiya ham, chaqirib olish ro'yxati ham mavjud
emas. Token muddati o'tsa, mijoz qayta login qiladi.
