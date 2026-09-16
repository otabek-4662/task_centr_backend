# 16. Validatsiya va xatolar

## Bean Validation (Jakarta)

Quyidagi body'larda `@Valid` bor: auth, workspace-create va task/column-create.

| DTO | Qoidalar |
|---|---|
| `RegisterRequest` | `name` bo'sh emas, `password` bo'sh emas va kamida 6 belgi |
| `LoginRequest` | `name` va `password` bo'sh emas |
| `WorkspaceCreateRequest` | `title` bo'sh emas va 255 belgigacha, `bgColor` 255 belgigacha (ixtiyoriy string, gradient ham bo'ladi), `description` 5000 belgigacha |
| `ColumnCreateRequest` | `title` bo'sh emas va 255 belgigacha |
| `TaskCreateRequest` | `columnId` va `title` bo'sh emas, `title` 255 belgigacha, `description` 2000 belgigacha, `dueDate` kelasi yoki hozirgi sana (`@FutureOrPresent`, custom) |
| `TaskUpdateRequest` | `title` 255 belgigacha, `description` 2000 belgigacha, `dueDate` kelasi yoki hozirgi sana (hammasi ixtiyoriy) |

## Xato formati

Hamma xato `ApiResponse.error(message)` ko'rinishida:

```json
{ "success": false, "message": "Nom yoki parol xato", "data": null, "timestamp": "2026-09-15T21:00:00" }
```

Bitta istisno bor: `RateLimitFilter` yozgan xom 429 body'da `timestamp` bo'lmaydi.

## `GlobalExceptionHandler` xaritasi

| Exception | Status | Xabar |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | `"Validatsiya xatosi"` (field xarita tuziladi, lekin javobga qo'shilmaydi) |
| `IllegalArgumentException` | 400 | exception xabari |
| `BadCredentialsException` | 401 | `"Nom yoki parol xato"` |
| `ForbiddenException` | 403 | exception xabari (yoki bo'sh) |
| `AccessDeniedException` | 403 | `"Sizga ruxsat yo'q"` |
| `EntityNotFoundException` | 404 | exception xabari |
| `UsernameNotFoundException` | 404 | exception xabari |
| `ConflictException` | 409 | masalan `"Bu nom allaqachon ishlatilmoqda!"` |
| `DataIntegrityViolationException` | 409 | `"Ma'lumotlar bazasida xatolik: dublikat yoki bog'lanish xatosi"` |
| `DataAccessException` | 500 | umumiy `"Serverda xatolik yuz berdi…"` (sabab faqat server logida) |
| boshqa `RuntimeException` | 500 | umumiy xabar, stack loglanadi |
| Rate limit oshishi | 429 | `{"success":false,"message":"Too many requests. Please try again later.","data":null}` |

Biznes exception'lar uchta: `EntityNotFoundException(resource, id)`,
`ForbiddenException([msg])`, `ConflictException(msg)`. Validatsiya xabarlari
o'zbekcha, rate-limit va actuator xabarlari inglizcha.
