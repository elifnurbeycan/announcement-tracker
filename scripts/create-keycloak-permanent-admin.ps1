param(
    [string]$ServerUrl = "http://localhost:8180",
    [string]$EnvironmentFile = ".env"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $EnvironmentFile)) {
    throw "Environment file not found: $EnvironmentFile"
}

$settings = @{}
foreach ($line in Get-Content -LiteralPath $EnvironmentFile) {
    if ($line -match '^\s*([^#][^=]*)=(.*)$') {
        $settings[$matches[1].Trim()] = $matches[2].Trim()
    }
}

$bootstrapUsername = $settings["KEYCLOAK_BOOTSTRAP_ADMIN_USERNAME"]
$bootstrapPassword = $settings["KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD"]
$consoleUsername = $settings["KEYCLOAK_CONSOLE_ADMIN_USERNAME"]
$consolePassword = $settings["KEYCLOAK_CONSOLE_ADMIN_PASSWORD"]

if (@($bootstrapUsername, $bootstrapPassword, $consoleUsername, $consolePassword) |
        Where-Object { [string]::IsNullOrWhiteSpace($_) }) {
    throw "Bootstrap and permanent console admin settings must exist in $EnvironmentFile"
}

$masterToken = Invoke-RestMethod -Method Post `
    -Uri "$ServerUrl/realms/master/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
        grant_type = "password"
        client_id = "admin-cli"
        username = $bootstrapUsername
        password = $bootstrapPassword
    }

$headers = @{ Authorization = "Bearer $($masterToken.access_token)" }
$users = @(Invoke-RestMethod -Method Get `
    -Uri "$ServerUrl/admin/realms/master/users?username=$consoleUsername&exact=true" `
    -Headers $headers)
if ($users.Count -eq 1 -and $users[0] -is [System.Array]) {
    $users = $users[0]
}

if ($users.Count -eq 0) {
    Invoke-RestMethod -Method Post `
        -Uri "$ServerUrl/admin/realms/master/users" `
        -Headers $headers `
        -ContentType "application/json" `
        -Body (@{
            username = $consoleUsername
            enabled = $true
            emailVerified = $true
        } | ConvertTo-Json)
    $users = @(Invoke-RestMethod -Method Get `
        -Uri "$ServerUrl/admin/realms/master/users?username=$consoleUsername&exact=true" `
        -Headers $headers)
    if ($users.Count -eq 1 -and $users[0] -is [System.Array]) {
        $users = $users[0]
    }
}

$userId = [string]$users[0].id
if ([string]::IsNullOrWhiteSpace($userId)) {
    throw "Permanent Keycloak administrator was created but its id could not be resolved."
}
Invoke-RestMethod -Method Put `
    -Uri "$ServerUrl/admin/realms/master/users/$userId/reset-password" `
    -Headers $headers `
    -ContentType "application/json" `
    -Body (@{ type = "password"; value = $consolePassword; temporary = $false } | ConvertTo-Json)

$adminRole = Invoke-RestMethod -Method Get `
    -Uri "$ServerUrl/admin/realms/master/roles/admin" `
    -Headers $headers
Invoke-RestMethod -Method Post `
    -Uri "$ServerUrl/admin/realms/master/users/$userId/role-mappings/realm" `
    -Headers $headers `
    -ContentType "application/json" `
    -Body (ConvertTo-Json -InputObject @($adminRole) -Depth 5)

Write-Output "Permanent Keycloak console administrator is ready: $consoleUsername"
