$ErrorActionPreference = "Stop"

$environmentFile = ".env"
if (-not (Test-Path -LiteralPath $environmentFile)) {
    throw ".env dosyası bulunamadı. Önce .env.example dosyasını .env olarak kopyalayıp yerel secret değerlerini girin."
}

$settings = @{}
foreach ($line in Get-Content -LiteralPath $environmentFile) {
    if ($line -match '^\s*([^#][^=]*)=(.*)$') {
        $settings[$matches[1].Trim()] = $matches[2].Trim()
    }
}

$env:SSO_ENABLED = "true"
$env:KEYCLOAK_ADMIN_SYNC_ENABLED = "true"
$env:KEYCLOAK_SERVER_URL = "http://localhost:8180"
$env:KEYCLOAK_ISSUER_URI = "http://localhost:8180/realms/announcement-tracker-realm"
$env:KEYCLOAK_CLIENT_AUTH_METHOD = "none"
$env:KEYCLOAK_ADMIN_CLIENT_ID = $settings["KEYCLOAK_ADMIN_CLIENT_ID"]
$env:KEYCLOAK_ADMIN_CLIENT_SECRET = $settings["KEYCLOAK_ADMIN_CLIENT_SECRET"]

if ([string]::IsNullOrWhiteSpace($env:KEYCLOAK_ADMIN_CLIENT_SECRET)) {
    throw "KEYCLOAK_ADMIN_CLIENT_SECRET .env dosyasında tanımlı değil."
}

& .\mvnw.cmd spring-boot:run
