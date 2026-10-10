#!/usr/bin/env bash
# OpenAPI JSON spetsifikatsiyasini docs/openapi.json fayliga eksport qilish
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/.."

echo "[OpenAPI Export] Test orqali /v3/api-docs spetsifikatsiyasini docs/openapi.json ga eksport qilish boshlanmoqda..."
"$ROOT_DIR/mvnw" test -Dtest=OpenApiDocumentationTest
echo "[OpenAPI Export] Muvaffaqiyatli yakunlandi: docs/openapi.json"
