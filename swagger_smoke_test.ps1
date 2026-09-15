# Swagger'dagi barcha API larni ketma-ket tekshirib log yozadi.
# Ishlatish:
#   .\swagger_smoke_test.ps1
#   .\swagger_smoke_test.ps1 -BaseUrl "http://localhost:8080"
param(
    [string]$BaseUrl = "https://task-centr-backend.onrender.com"
)

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$LogFile = Join-Path $PSScriptRoot "swagger-smoke-$stamp.log"
$script:pass = 0
$script:fail = 0

function Write-Log {
    param([string]$Text, [string]$Color = "White")
    $line = "$(Get-Date -Format 'HH:mm:ss') $Text"
    Write-Host $line -ForegroundColor $Color
    Add-Content -LiteralPath $LogFile -Value $line
}

function Invoke-TestApi {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = "",
        [string]$Desc = "",
        [int]$Expect = 200
    )
    $url = "$BaseUrl$Path"
    $expected = @($Expect)
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token -ne "") { $headers["Authorization"] = "Bearer $Token" }
    $params = @{ Uri = $url; Method = $Method; Headers = $headers; TimeoutSec = 120 }
    if ($Body -ne $null) { $params["Body"] = ($Body | ConvertTo-Json -Depth 5) }
    try {
        $res = Invoke-RestMethod @params
        $status = 200
        if ($res -is [string]) { $preview = $res.Substring(0, [Math]::Min(200, $res.Length)) }
        else { $preview = ($res | ConvertTo-Json -Depth 3 -Compress).Substring(0, [Math]::Min(200, ($res | ConvertTo-Json -Depth 3 -Compress).Length)) }
        $ok = $expected -contains 200
    } catch {
        $status = [int]$_.Exception.Response.StatusCode
        $preview = $_.Exception.Message
        $ok = $expected -contains $status
    }
    if ($ok) { $script:pass++; $c = "Green"; $r = "PASS" }
    else { $script:fail++; $c = "Red"; $r = "FAIL" }
    Write-Log "[$r] $Method $Path | status=$status (kutilgan=$($expected -join '/')) | $Desc" $c
    Write-Log "      -> $preview" Gray
    Start-Sleep -Milliseconds 300
    return $res
}

Write-Log "=== SMOKE TEST BOSHLANDI: $BaseUrl ===" Cyan
Write-Log "Log fayl: $LogFile" Cyan

# 0. Health (authsiz)
Invoke-TestApi -Method Get -Path "/actuator/health" -Desc "Actuator health" | Out-Null

# 1. Auth: register (random user) -> login -> token
$user = "smoke$(Get-Random -Maximum 999999)"
$reg = Invoke-TestApi -Method Post -Path "/api/auth/register" -Body @{ name = $user; password = "password123" } -Desc "Register $user" -Expect @(200, 201)
$login = Invoke-TestApi -Method Post -Path "/api/auth/login" -Body @{ name = $user; password = "password123" } -Desc "Login $user"
$token = $login.data.token
$myId = $login.data.user.id
if ([string]::IsNullOrEmpty($token)) { Write-Log "Token olinmadi, test to'xtatildi!" Red; exit 1 }
Write-Log "Token olindi, userId=$myId" Yellow

# 2. Me
Invoke-TestApi -Method Get -Path "/api/me" -Token $token -Desc "Get me" | Out-Null
Invoke-TestApi -Method Get -Path "/api/auth/me" -Token $token -Desc "Get auth/me" | Out-Null
Invoke-TestApi -Method Get -Path "/api/users" -Token $token -Desc "Get users (oddiy user uchun 403 to'g'ri)" -Expect 403 | Out-Null

# 3. Workspaces
$ws = Invoke-TestApi -Method Post -Path "/api/workspaces" -Token $token -Body @{ title = "Smoke WS"; bgColor = "#ff0000"; description = "smoke test" } -Desc "Create workspace"
$wsId = $ws.data.id
Invoke-TestApi -Method Get -Path "/api/workspaces?page=0&size=20" -Token $token -Desc "List workspaces" | Out-Null
Invoke-TestApi -Method Get -Path "/api/workspaces/$wsId" -Token $token -Desc "Get workspace" | Out-Null
Invoke-TestApi -Method Put -Path "/api/workspaces/$wsId" -Token $token -Body @{ title = "Smoke WS 2" } -Desc "Update workspace" | Out-Null
Invoke-TestApi -Method Get -Path "/api/workspaces/does-not-exist" -Token $token -Desc "Get 404 tekshiruvi" -Expect 404 | Out-Null

# 4. Columns
$c1 = Invoke-TestApi -Method Post -Path "/api/workspaces/$wsId/columns" -Token $token -Body @{ title = "To Do" } -Desc "Create column 1"
$c2 = Invoke-TestApi -Method Post -Path "/api/workspaces/$wsId/columns" -Token $token -Body @{ title = "Done" } -Desc "Create column 2"
$col1 = $c1.data.id; $col2 = $c2.data.id
Invoke-TestApi -Method Get -Path "/api/workspaces/$wsId/columns" -Token $token -Desc "List columns" | Out-Null
Invoke-TestApi -Method Get -Path "/api/workspaces/$wsId/board" -Token $token -Desc "Get board" | Out-Null
Invoke-TestApi -Method Put -Path "/api/workspaces/$wsId/columns/$col1" -Token $token -Body @{ title = "To Do v2" } -Desc "Update column" | Out-Null
Invoke-TestApi -Method Patch -Path "/api/workspaces/$wsId/columns/$col1" -Token $token -Body @{ title = "To Do v3" } -Desc "Patch column" | Out-Null
Invoke-TestApi -Method Patch -Path "/api/workspaces/$wsId/columns" -Token $token -Body @(@{ id = $col1; order = 2 }, @{ id = $col2; order = 1 }) -Desc "Reorder columns" | Out-Null

# 5. Labels
$lb = Invoke-TestApi -Method Post -Path "/api/workspaces/$wsId/labels" -Token $token -Body @{ name = "bug"; color = "red" } -Desc "Create label"
$labelId = $lb.data.id
Invoke-TestApi -Method Get -Path "/api/workspaces/$wsId/labels" -Token $token -Desc "List labels" | Out-Null

# 6. Tasks (CRUD to'liq)
$due = (Get-Date).AddDays(30).ToString("yyyy-MM-ddTHH:mm:ss")
$t = Invoke-TestApi -Method Post -Path "/api/workspaces/$wsId/tasks" -Token $token -Body @{ columnId = $col1; title = "Smoke task"; description = "desc"; priority = "MEDIUM"; dueDate = $due } -Desc "Create task"
$taskId = $t.data.id
Invoke-TestApi -Method Get -Path "/api/workspaces/$wsId/tasks?page=0&size=50" -Token $token -Desc "List tasks" | Out-Null
Invoke-TestApi -Method Get -Path "/api/workspaces/$wsId/tasks/$taskId" -Token $token -Desc "Get task" | Out-Null
Invoke-TestApi -Method Put -Path "/api/workspaces/$wsId/tasks/$taskId" -Token $token -Body @{ title = "Smoke task v2"; priority = "HIGH" } -Desc "Update task (PUT)" | Out-Null
Invoke-TestApi -Method Patch -Path "/api/workspaces/$wsId/tasks/$taskId" -Token $token -Body @{ description = "patched" } -Desc "Patch task" | Out-Null
Invoke-TestApi -Method Patch -Path "/api/workspaces/$wsId/tasks/$taskId/column/$col2" -Token $token -Desc "Move task to column 2" | Out-Null
Invoke-TestApi -Method Put -Path "/api/workspaces/$wsId/tasks/$taskId/assignee/$myId" -Token $token -Desc "Assign task to self" | Out-Null
Invoke-TestApi -Method Post -Path "/api/workspaces/$wsId/tasks/$taskId/labels/$labelId" -Token $token -Desc "Add label to task" | Out-Null
Invoke-TestApi -Method Delete -Path "/api/workspaces/$wsId/tasks/$taskId/labels/$labelId" -Token $token -Desc "Remove label from task" | Out-Null
Invoke-TestApi -Method Delete -Path "/api/workspaces/$wsId/tasks/$taskId" -Token $token -Desc "Delete task" | Out-Null
Invoke-TestApi -Method Get -Path "/api/workspaces/$wsId/tasks/$taskId" -Token $token -Desc "Get deleted task (404 kutiladi)" -Expect 404 | Out-Null

# 7. Members
Invoke-TestApi -Method Get -Path "/api/workspaces/$wsId/members" -Token $token -Desc "List members" | Out-Null

# 8. Tozalash (teskari tartibda)
Invoke-TestApi -Method Delete -Path "/api/workspaces/$wsId/labels/$labelId" -Token $token -Desc "Delete label" | Out-Null
Invoke-TestApi -Method Delete -Path "/api/workspaces/$wsId/columns/$col1" -Token $token -Desc "Delete column 1" | Out-Null
Invoke-TestApi -Method Delete -Path "/api/workspaces/$wsId/columns/$col2" -Token $token -Desc "Delete column 2" | Out-Null
Invoke-TestApi -Method Delete -Path "/api/workspaces/$wsId" -Token $token -Desc "Delete workspace" | Out-Null
Invoke-TestApi -Method Get -Path "/api/workspaces/$wsId" -Token $token -Desc "Get deleted workspace (404 kutiladi)" -Expect 404 | Out-Null

# 9. Authsiz so'rov rad etilishi kerak
Invoke-TestApi -Method Get -Path "/api/workspaces" -Desc "Authsiz (403 kutiladi)" -Expect 403 | Out-Null

Write-Log "=== YAKUN: PASS=$($script:pass) FAIL=$($script:fail) ===" Cyan
Write-Log "Log fayl: $LogFile" Cyan
if ($script:fail -gt 0) { exit 1 }
