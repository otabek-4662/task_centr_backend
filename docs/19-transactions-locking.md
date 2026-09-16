# 19. Tranzaksiyalar va bloklash

## Tranzaksiya chegaralari

- `BoardService` class darajasida `@Transactional`: har bir public metod
  (column'lar, tasklar, labellar, a'zolar) bitta tranzaksiyada ishlaydi.
- `WorkspaceService` va `AuthService` da `@Transactional` yo'q, har bir repository
  chaqiruv o'zicha commit bo'ladi. Bir qatorli metodlar uchun shu yetadi.
  Ko'p qadamli `createWorkspace` atomiklikka emas, fail-fast tartibga tayanadi.
- Testlar rollback izolatsiya uchun `@Transactional`.

## Pessimistic locking (task counter)

`Workspace.taskCounter` ni kodda faqat `BoardService.createTask` yozadi:

1. `workspaceRepository.findByIdForUpdate(workspaceId)` chaqiriladi.
   `@Lock(PESSIMISTIC_WRITE)` va `SELECT w … WHERE w.id = :id` qator lock'ni
   tranzaksiya oxirigacha ushlaydi. Servis metodi tranzaksiyali bo'lgani uchun shu
   ishlaydi, bo'lmasa `FOR UPDATE` darhol bo'shab ketardi.
2. `taskCounter` bittaga oshirilib saqlanadi, `publicId = keyPrefix + "-" + counter`
   yasaladi.

Bitta workspace'da parallel create'lar navbatga turadi, shuning uchun dublikat
public ID chiqmaydi. Bitta so'rovda faqat bitta workspace locklanadi, shuning
uchun hozircha deadlock tartib muammosi yo'q.

## Nima qilinmagan

- Entity'larda `@Version` optimistic locking yo'q. V6 dagi `version` ustunlari
  sxemada bor, lekin JPA ularni o'qimaydi ham yozmaydi ham.
- `resolveUniqueKeyPrefix` lock'siz tekshir-tanla qiladi. Himoya partial unique
  index (V10): yutqazgan race 409 beradi va chaqiruvchi qayta urinishi kerak
  (kodda avtomatik retry yo'q).
