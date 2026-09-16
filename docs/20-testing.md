# 20. Testlash

Stek: JUnit 5, Mockito (`@ExtendWith(MockitoExtension.class)`, AssertJ), MockMvc
integratsiya testlar, PostgreSQL compatibility rejimidagi H2
(`jdbc:h2:mem:testdb;MODE=PostgreSQL`). Testlarda Flyway o'chirilgan,
`ddl-auto: create-drop`, rate limit o'chirilgan, `jwt.secret` va `expiration`
fiksirlangan.

Jami 98 ta test, hammasi o'tadi (`./mvnw test`).

| Test klass | Tip | Soni | Nima tekshiradi |
|---|---|---|---|
| `AuthControllerTest` | MockMvc | 9 | register/login/me, 400/401/409 yo'llar |
| `BoardControllerTest` | MockMvc | 12 | column'lar, board, task CRUD/move/delete, labellar, a'zolar |
| `UserControllerTest` | MockMvc | 3 | `/api/me/stats` nol/o'sish/authsiz |
| `WorkspaceControllerTest` | MockMvc | 7 | workspace CRUD, 404'lar |
| `WorkspaceQueriesTest` | DataJpa | 5 | owner/member workspace so'rovlar |
| `JwtTokenProviderTest` | unit | 5 | token yasash/tekshirish/parse |
| `SecurityTest` | MockMvc | 15 | public/private matritsa, gradient bgColor qabul, oversize rad |
| `AuthServiceTest` | Mockito | 4 | register/login, dublikat, noto'g'ri parol |
| `BoardServiceTest` | Mockito | 9 | publicId counter, `findByIdForUpdate` lock, default'lar, eventlar, publicId o'zgarmasligi |
| `WebSocketEventPublisherTest` | Mockito | 2 | topic formati, JSON payload |
| `WorkspaceAuthorizationServiceTest` | Mockito | 11 | rollar, tekshiruvlar, xabarlar |
| `WorkspaceKeyPrefixTest` | Mockito | 4 | `WR` dan `WR2` ga, `WR3` ga, bitta so'z prefiksi |
| `WorkspaceServiceTest` | Mockito | 12 | CRUD, a'zolar, prefix default, paging |

Test yozish odatlari: AAA pattern, konkret stub qiymatlar (masalan `"TC"`,
`"ws-1"`), Mockito strict stub'lar. Integratsiya testlar uchun seed data
`DataSeeder` dan keladi (demo workspace `6a451…`, 4 column/label/task, userlar
`elshod`/`xusan`, parol `password123`).

Bundan tashqari `swagger_smoke_test.ps1` jonli serverda 39 ga yaqin endpoint'ni
uchidan-uchiga tekshirib, vaqt belgisili log yozadi.
