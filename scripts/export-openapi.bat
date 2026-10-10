@echo off
REM OpenAPI JSON spetsifikatsiyasini docs/openapi.json fayliga eksport qilish
echo [OpenAPI Export] Test orqali /v3/api-docs spetsifikatsiyasini docs/openapi.json ga eksport qilish boshlanmoqda...
call "%~dp0..\mvnw.cmd" test -Dtest=OpenApiDocumentationTest
if %ERRORLEVEL% EQU 0 (
    echo [OpenAPI Export] Muvaffaqiyatli yakunlandi: docs/openapi.json
) else (
    echo [OpenAPI Export] Xatolik yuz berdi!
    exit /b %ERRORLEVEL%
)
