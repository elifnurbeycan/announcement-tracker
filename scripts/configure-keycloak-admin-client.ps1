param(
    [string]$ServerUrl = "http://localhost:8180",
    [string]$Realm = "announcement-tracker-realm",
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

$adminUsername = $settings["KEYCLOAK_BOOTSTRAP_ADMIN_USERNAME"]
$adminPassword = $settings["KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD"]
$clientId = $settings["KEYCLOAK_ADMIN_CLIENT_ID"]
$clientSecret = $settings["KEYCLOAK_ADMIN_CLIENT_SECRET"]

if ([string]::IsNullOrWhiteSpace($adminUsername) -or
    [string]::IsNullOrWhiteSpace($adminPassword) -or
    [string]::IsNullOrWhiteSpace($clientId) -or
    [string]::IsNullOrWhiteSpace($clientSecret)) {
    throw "Required Keycloak admin settings are missing from $EnvironmentFile"
}

$masterToken = Invoke-RestMethod -Method Post `
    -Uri "$ServerUrl/realms/master/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
        grant_type = "password"
        client_id = "admin-cli"
        username = $adminUsername
        password = $adminPassword
    }

$headers = @{ Authorization = "Bearer $($masterToken.access_token)" }

# Ensure the browser/API client emits an explicit audience claim so bearer
# tokens can be validated by the Spring resource server.
$applicationClients = @(Invoke-RestMethod -Method Get `
    -Uri "$ServerUrl/admin/realms/$Realm/clients?clientId=announcement-tracker-app" `
    -Headers $headers)
if ($applicationClients.Count -eq 1 -and $applicationClients[0] -is [System.Array]) {
    $applicationClients = $applicationClients[0]
}
if ($applicationClients.Count -eq 0) {
    throw "Keycloak application client announcement-tracker-app was not found."
}
$applicationClientUuid = [string]$applicationClients[0].id
$protocolMappers = @(Invoke-RestMethod -Method Get `
    -Uri "$ServerUrl/admin/realms/$Realm/clients/$applicationClientUuid/protocol-mappers/models" `
    -Headers $headers)
if ($protocolMappers.Count -eq 1 -and $protocolMappers[0] -is [System.Array]) {
    $protocolMappers = $protocolMappers[0]
}
$audienceMapper = $protocolMappers | Where-Object { $_.name -eq "announcement-tracker-api-audience" }
if (-not $audienceMapper) {
    Invoke-RestMethod -Method Post `
        -Uri "$ServerUrl/admin/realms/$Realm/clients/$applicationClientUuid/protocol-mappers/models" `
        -Headers $headers `
        -ContentType "application/json" `
        -Body (@{
            name = "announcement-tracker-api-audience"
            protocol = "openid-connect"
            protocolMapper = "oidc-audience-mapper"
            consentRequired = $false
            config = @{
                "included.client.audience" = "announcement-tracker-app"
                "id.token.claim" = "false"
                "access.token.claim" = "true"
                "introspection.token.claim" = "true"
            }
        } | ConvertTo-Json -Depth 6)
}

$clients = @(Invoke-RestMethod -Method Get `
    -Uri "$ServerUrl/admin/realms/$Realm/clients?clientId=$clientId" `
    -Headers $headers)
if ($clients.Count -eq 1 -and $clients[0] -is [System.Array]) {
    $clients = $clients[0]
}

if ($clients.Count -gt 0 -and [string]::IsNullOrWhiteSpace([string]$clients[0].id)) {
    throw "Keycloak returned a provisioning client without an internal id."
}

$clientPayload = @{
    clientId = $clientId
    name = "Announcement Tracker User Provisioning Client"
    description = "Backend-only service account for subscriber lifecycle synchronization"
    enabled = $true
    secret = $clientSecret
    publicClient = $false
    bearerOnly = $false
    standardFlowEnabled = $false
    directAccessGrantsEnabled = $false
    implicitFlowEnabled = $false
    serviceAccountsEnabled = $true
    fullScopeAllowed = $true
    protocol = "openid-connect"
}

if ($clients.Count -eq 0) {
    Invoke-RestMethod -Method Post `
        -Uri "$ServerUrl/admin/realms/$Realm/clients" `
        -Headers $headers `
        -ContentType "application/json" `
        -Body ($clientPayload | ConvertTo-Json -Depth 5)
    $clients = @(Invoke-RestMethod -Method Get `
        -Uri "$ServerUrl/admin/realms/$Realm/clients?clientId=$clientId" `
        -Headers $headers)
    if ($clients.Count -eq 1 -and $clients[0] -is [System.Array]) {
        $clients = $clients[0]
    }
} else {
    $clientPayload["id"] = [string]$clients[0].id
    Invoke-RestMethod -Method Put `
        -Uri "$ServerUrl/admin/realms/$Realm/clients/$($clients[0].id)" `
        -Headers $headers `
        -ContentType "application/json" `
        -Body ($clientPayload | ConvertTo-Json -Depth 5)
}

$clientUuid = $clients[0].id
$serviceAccount = Invoke-RestMethod -Method Get `
    -Uri "$ServerUrl/admin/realms/$Realm/clients/$clientUuid/service-account-user" `
    -Headers $headers
$realmManagementClients = @(Invoke-RestMethod -Method Get `
    -Uri "$ServerUrl/admin/realms/$Realm/clients?clientId=realm-management" `
    -Headers $headers)
if ($realmManagementClients.Count -eq 1 -and $realmManagementClients[0] -is [System.Array]) {
    $realmManagementClients = $realmManagementClients[0]
}
$realmManagementClient = $realmManagementClients[0]

$roles = @()
foreach ($roleName in @("manage-users", "query-users", "view-users")) {
    $roles += Invoke-RestMethod -Method Get `
        -Uri "$ServerUrl/admin/realms/$Realm/clients/$($realmManagementClient.id)/roles/$roleName" `
        -Headers $headers
}

Invoke-RestMethod -Method Post `
    -Uri "$ServerUrl/admin/realms/$Realm/users/$($serviceAccount.id)/role-mappings/clients/$($realmManagementClient.id)" `
    -Headers $headers `
    -ContentType "application/json" `
    -Body ($roles | ConvertTo-Json -Depth 5)

$serviceToken = Invoke-RestMethod -Method Post `
    -Uri "$ServerUrl/realms/$Realm/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
        grant_type = "client_credentials"
        client_id = $clientId
        client_secret = $clientSecret
    }

$null = Invoke-RestMethod -Method Get `
    -Uri "$ServerUrl/admin/realms/$Realm/users?max=1" `
    -Headers @{ Authorization = "Bearer $($serviceToken.access_token)" }

Write-Output "Keycloak provisioning client is configured; client_credentials and user-management permission checks succeeded."
