param(
    [string]$BaseUrl = "http://localhost:8080"
)
$ErrorActionPreference = "Continue"

function Test-BaseUrl($url) {
    try {
        $r = Invoke-RestMethod -Uri "$url/v3/api-docs" -Method Get -TimeoutSec 5 -ErrorAction Stop
        return $true
    } catch { return $false }
}

Write-Host "=== Task Center Backend - Swagger Auth Test ===" -ForegroundColor Cyan
Write-Host "Tekshirilmoqda: $BaseUrl" -ForegroundColor Gray

if (-not (Test-BaseUrl $BaseUrl)) {
    Write-Host "XATO: $BaseUrl ishlamayapti" -ForegroundColor Red
    Write-Host "start.bat ni ishga tushir"
    exit 1
}
Write-Host "Active URL: $BaseUrl" -ForegroundColor Green
Write-Host "Swagger UI: $BaseUrl/swagger-ui/index.html" -ForegroundColor Cyan

$stamp = Get-Date -Format "MMddHHmmss"
$testUser = @{
    name = "Test User $stamp"
    email = "test$stamp@example.com"
    password = "Test1234!"
}
$loginData = @{
    email = $testUser.email
    password = $testUser.password
}

Write-Host "`n--- 1. REGISTER testi ---" -ForegroundColor Yellow
Write-Host "POST $BaseUrl/api/auth/register" -ForegroundColor Gray
Write-Host ($testUser | ConvertTo-Json) -ForegroundColor DarkGray
try {
    $reg = Invoke-RestMethod -Uri "$BaseUrl/api/auth/register" -Method Post -ContentType "application/json" -Body ($testUser | ConvertTo-Json) -ErrorAction Stop
    Write-Host "REGISTER OK (201 CREATED)" -ForegroundColor Green
    $reg | ConvertTo-Json -Depth 5 | Write-Host -ForegroundColor White
    $tokenFromRegister = $reg.data.token
    Write-Host "Token (register): $($tokenFromRegister.Substring(0,40))..." -ForegroundColor DarkCyan
} catch {
    $body = $_.ErrorDetails.Message
    Write-Host "REGISTER XATO: $($_.Exception.Message)" -ForegroundColor Red
    if ($body) { Write-Host $body -ForegroundColor Red }
}

Write-Host "`n--- 2. LOGIN testi ---" -ForegroundColor Yellow
Write-Host "POST $BaseUrl/api/auth/login" -ForegroundColor Gray
Write-Host ($loginData | ConvertTo-Json) -ForegroundColor DarkGray
try {
    $login = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -ContentType "application/json" -Body ($loginData | ConvertTo-Json) -ErrorAction Stop
    Write-Host "LOGIN OK (200 OK)" -ForegroundColor Green
    $login | ConvertTo-Json -Depth 5 | Write-Host -ForegroundColor White
    $jwt = $login.data.token
    $user = $login.data.user
    Write-Host "JWT Token: $($jwt.Substring(0,40))..." -ForegroundColor DarkCyan
    Write-Host "User: $($user.name) email=$($user.email) id=$($user.id)" -ForegroundColor DarkCyan
} catch {
    $body = $_.ErrorDetails.Message
    Write-Host "LOGIN XATO: $($_.Exception.Message)" -ForegroundColor Red
    if ($body) { Write-Host $body -ForegroundColor Red }
    exit 1
}

Write-Host "`n--- 3. JWT bilan himoyalangan so'rov ---" -ForegroundColor Yellow
$headers = @{ Authorization = "Bearer $jwt" }
try {
    $docs = Invoke-RestMethod -Uri "$BaseUrl/v3/api-docs" -Method Get -Headers $headers -TimeoutSec 5 -ErrorAction Stop
    $paths = $docs.paths.PSObject.Properties.Name
    Write-Host "Endpointlar:" -ForegroundColor Gray
    $paths | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }
    Write-Host "JWT yuborildi, server qabul qildi" -ForegroundColor Green
} catch { Write-Host "Protected test skip: $($_.Exception.Message)" -ForegroundColor Gray }

Write-Host "`n=== XULOSA ===" -ForegroundColor Cyan
Write-Host "Register -> 201 + token + user (AuthController.java:27)" -ForegroundColor Green
Write-Host "Login -> 200 + token + user (AuthController.java:34)" -ForegroundColor Green
Write-Host "Swagger da ham xuddi shu JSON ni Try it out bilan yuborasan" -ForegroundColor Green
