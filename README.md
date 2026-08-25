# Task Center Backend

O'zbek tilida yozilgan Spring Boot asosidagi autentifikatsiya API. Java juniorlar uchun sodda va tushunarli qilib tayyorlangan.

## Loyiha haqida

`task_center_backend` — foydalanuvchilarni ro'yxatdan o'tkazish (`register`) va tizimga kirish (`login`) uchun JWT token beruvchi backend. Swagger orqali osongina test qilish mumkin.

Asosiy imkoniyatlar:
- `POST /api/auth/register` — yangi foydalanuvchi yaratish
- `POST /api/auth/login` — tizimga kirish va JWT olish
- PostgreSQL bilan ishlaydi
- Swagger UI (springdoc-openapi) mavjud
- JWT (jjwt 0.11.5) bilan himoyalangan

## Texnologiyalar

| Texnologiya | Versiya |
|---|---|
| Java | 17 (Temurin 17.0.19+) |
| Spring Boot | 3.2.5 |
| Spring Security + JPA | 6.2.4 / Hibernate 6.4.4 |
| PostgreSQL | 16/17 |
| JWT | jjwt 0.11.5 |
| Swagger | springdoc-openapi 2.5.0 |
| Build | Maven (mvnw) |

## Talablar

- Java 17+ (`java -version` bilan tekshir)
- Maven (loyihada `mvnw` bor, alohida o'rnatish shart emas)
- PostgreSQL 17 (`localhost:5432`)
- IntelliJ IDEA 2026.2+ (yoki istalgan IDE)

## Tez boshlash (IntelliJ — Usul A, tavsiya)

Bu usulni sen tanlading — eng oson va juniorlarga mos.

1. **Loyihani ochish**
   ```
   File -> Open -> C:\Users\Bekmurod\Desktop\task_center_backend -> pom.xml ni tanla -> Open as Project
   ```
   O'ng pastdagi Maven import tugashini kut.

2. **PostgreSQL ni yoqish**

   Windows da Service o'chiq bo'lsa:
   - `Windows` tugmasi -> `PowerShell` -> o'ng click -> **Run as administrator**
   - Yoz:
     ```
     net start postgresql-x64-17
     ```
     `started successfully` chiqsa bo'ldi.

   Yoki qo'lda yaratilgan klaster ishlayotgan bo'lsa (bu loyihada `C:\Users\Bekmurod\pgdata_test` ishlatilgan), u avtomatik `5432` da turadi.

3. **Database yaratilganini tekshirish**

   Intellij da Terminal ochib:
   ```
   psql -h localhost -U postgres -p 5432 -l
   ```
   Ro'yxatda `taskcenter` bo'lishi kerak. Yo'q bo'lsa:
   ```
   psql -h localhost -U postgres -p 5432 -c "CREATE DATABASE taskcenter;"
   ```

4. **IntelliJ da Run qilish**

   `src/main/java/com/taskcenter/BackendApplication.java` ni och -> `main` yonidagi yashil **▶** ni bos -> **Run 'BackendApplication'**

   Logda:
   ```
   HikariPool-1 - Added connection
   Tomcat started on port 8080
   Started BackendApplication in 4.3 seconds
   ```
   chiqsa backend tayyor.

5. **Swagger ni ochish**

   ```
   http://localhost:8080/swagger-ui/index.html
   http://localhost:8080/v3/api-docs
   ```

   Swagger yuqorisidagi **Servers** dropdown da `http://localhost:8080 - Local` tanlangan bo'lsin. Agar `ngrok` tanlangan bo'lsa, `Failed to fetch` beradi — Local ga o'tkaz.

## Database ni IntelliJ da ulash

`application.yml` dagi sozlama:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskcenter
    username: postgres
    password: password
```

IntelliJ da ko'rish uchun:

1. O'ng/pastdagi **Database** panelini och (View > Tool Windows > Database)
2. `+` -> **Data Source -> PostgreSQL**
3. `Host: localhost`, `Port: 5432`, `Database: taskcenter`, `User: postgres`, `Password: password`
4. **Download** (driver) -> **Test Connection** -> `Succeeded` -> **OK**
5. `taskcenter -> Schemas -> public -> Tables -> users` ichida Swagger orqali yaratilgan userlarni ko'rasan.

## Swagger da test qilish

### Register

`POST /api/auth/register` -> `Try it out`:

```json
{
  "name": "Otabek",
  "email": "otabek@test.uz",
  "password": "Test1234!"
}
```

Javob **201**:
```json
{
  "success": true,
  "message": "Muvaffaqiyatli ro'yxatdan o'tdingiz",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": { "id": 1, "name": "Otabek", "email": "otabek@test.uz" }
  }
}
```

### Login

`POST /api/auth/login` -> `Try it out`:

```json
{
  "email": "otabek@test.uz",
  "password": "Test1234!"
}
```

Javob **200**:
```json
{
  "success": true,
  "message": "Tizimga muvaffaqiyatli kirdingiz",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": { "id": 1, "name": "Otabek", "email": "otabek@test.uz" }
  }
}
```

Token 7 kun amal qiladi (`jwt.expiration: 604800000`). Keyingi himoyalangan so'rovlarda Swagger dagi **Authorize** 🔓 tugmasini bosib `Bearer <token>` ni qo'y.

> Eslatma: `email` da `@` bo'lishi kerak (`strin.gmail.com` emas, `strin@gmail.com`). `AuthService.java:33` da email unique — takrorlansa `Email is already in use!` qaytadi.

### Curl bilan test

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"test","email":"test@test.uz","password":"12345678"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.uz","password":"12345678"}'
```

Avtomatik script ham bor: `swagger_auth_test2.ps1`

```powershell
powershell -ExecutionPolicy Bypass -File swagger_auth_test2.ps1
```

## Loyiha tuzilishi

```
src/main/java/com/taskcenter/
  BackendApplication.java       # Main (run shu yerdan)
  config/
    SecurityConfig.java         # /api/auth/** va /swagger-ui/** ochiq, qolganlari JWT
    SwaggerConfig.java          # OpenAPI serverlar: localhost:8080 va ngrok
  controller/
    AuthController.java         # POST /api/auth/register, /login
    GlobalExceptionHandler.java
  dto/
    RegisterRequest.java        # name, email, password
    LoginRequest.java           # email, password
    AuthResponse.java           # token + user
    ApiResponse.java            # success, message, data
  model/
    User.java                   # JPA Entity, UserDetails
  repository/
    UserRepository.java
  security/
    JwtTokenProvider.java       # token generatsiya
    JwtAuthenticationFilter.java
    CustomUserDetailsService.java
src/main/resources/
  application.yml               # DB va jwt sozlamalari
```

## Muhit o'zgaruvchilari

`application.yml` da:

```yaml
jwt:
  secret: super-secret-key-that-needs-to-be-at-least-256-bits-long...
  expiration: 604800000 # 7 kun
```

Docker da:

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/taskcenter
SPRING_DATASOURCE_USERNAME: postgres
SPRING_DATASOURCE_PASSWORD: password
```

## Docker bilan ishga tushirish

```bash
docker compose up -d
# yoki
docker-compose up -d
```

`docker-compose.yml` da `postgres:16` va `backend` (eclipse-temurin:17) bor. `app.jar` ni avval `mvnw package -DskipTests` bilan build qilish kerak.

## Build

```bash
./mvnw package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
# yoki
./start.bat  # Windows
```

## Git

```bash
git clone https://github.com/otabek-4662/task_centr_backend.git
git add .
git commit -m "xabar"
git push origin master
```

## Muammolar va yechim

| Xato | Sabab | Yechim |
|---|---|---|
| `localhost refused the connection` | Backend o'chiq | `BackendApplication` ni run qil |
| `5432 CLOSED` | Postgres o'chiq | `net start postgresql-x64-17` (admin) |
| `Failed to fetch` Swagger da | Server ngrok tanlangan | Swagger Servers -> `http://localhost:8080` ni tanla |
| `Email is already in use!` | Email takror | Boshqa email bilan register qil |
| `Port 8080 already in use` | Eski jar turibdi | IntelliJ da eski run ni Stop (qizil ■) qil |

## Muallif

Otabek Sotimov — Java junior. Savollar bo'lsa Swagger dagi `Try it out` bilan test qilib, `users` jadvalidan tekshirib bor.

