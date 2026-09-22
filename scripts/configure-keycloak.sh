#!/usr/bin/env bash
set -eu

KCADM="/opt/keycloak/bin/kcadm.sh"
SERVER_URL="${KEYCLOAK_SERVER_URL:-http://keycloak:8080}"
REALM="${KEYCLOAK_REALM:-announcement-tracker-realm}"
APP_CLIENT_ID="${KEYCLOAK_CLIENT_ID:-announcement-tracker-app}"
ADMIN_CLIENT_ID="${KEYCLOAK_ADMIN_CLIENT_ID:-announcement-tracker-admin}"
APP_BASE_URL="${APP_BASE_URL:-http://localhost:8080}"
APP_BASE_URL="${APP_BASE_URL%/}"

login() {
  username="$1"
  password="$2"
  [ -n "$username" ] && [ -n "$password" ] &&
    "$KCADM" config credentials \
      --server "$SERVER_URL" \
      --realm master \
      --user "$username" \
      --password "$password" >/dev/null 2>&1
}

attempt=0
until login "${KEYCLOAK_CONSOLE_ADMIN_USERNAME:-}" "${KEYCLOAK_CONSOLE_ADMIN_PASSWORD:-}" ||
      login "${KEYCLOAK_BOOTSTRAP_ADMIN_USERNAME:-}" "${KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD:-}"; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 60 ]; then
    echo "Keycloak configuration failed: administrator login was not available." >&2
    exit 1
  fi
  sleep 2
done

"$KCADM" update "realms/$REALM" \
  -s enabled=true \
  -s resetPasswordAllowed=true \
  -s loginTheme=announcement-tracker >/dev/null

if [ -n "${SPRING_MAIL_HOST:-}" ] &&
   [ -n "${SPRING_MAIL_USERNAME:-}" ] &&
   [ -n "${SPRING_MAIL_PASSWORD:-}" ] &&
   [ -n "${MAIL_FROM:-}" ]; then
  "$KCADM" update "realms/$REALM" \
    -s "smtpServer.host=${SPRING_MAIL_HOST}" \
    -s "smtpServer.port=${SPRING_MAIL_PORT:-587}" \
    -s "smtpServer.from=${MAIL_FROM}" \
    -s "smtpServer.fromDisplayName=e-Duyuru Takip" \
    -s "smtpServer.auth=true" \
    -s "smtpServer.starttls=true" \
    -s "smtpServer.ssl=false" \
    -s "smtpServer.user=${SPRING_MAIL_USERNAME}" \
    -s "smtpServer.password=${SPRING_MAIL_PASSWORD}" >/dev/null
else
  echo "WARNING: Keycloak SMTP was not configured. Add SPRING_MAIL_* and MAIL_FROM values to .env." >&2
fi

APP_CLIENT_UUID=$("$KCADM" get clients -r "$REALM" -q "clientId=$APP_CLIENT_ID" \
  --fields id --format csv --noquotes 2>/dev/null | tail -n 1 | tr -d '\r')
if [ -z "$APP_CLIENT_UUID" ]; then
  echo "Keycloak configuration failed: application client was not found." >&2
  exit 1
fi

"$KCADM" update "clients/$APP_CLIENT_UUID" -r "$REALM" \
  -s "redirectUris=[\"$APP_BASE_URL/login/oauth2/code/keycloak\",\"$APP_BASE_URL/user-login.html\",\"$APP_BASE_URL/oauth2/authorization/keycloak\"]" \
  -s "webOrigins=[\"$APP_BASE_URL\"]" >/dev/null

ROLE_MAPPER_ID=$("$KCADM" get "clients/$APP_CLIENT_UUID/protocol-mappers/models" -r "$REALM" \
  --fields id,name --format csv --noquotes 2>/dev/null \
  | grep ',announcement-tracker-realm-roles$' \
  | tail -n 1 \
  | cut -d, -f1 \
  | tr -d '\r' || true)
if [ -z "$ROLE_MAPPER_ID" ]; then
  "$KCADM" create "clients/$APP_CLIENT_UUID/protocol-mappers/models" -r "$REALM" \
    -s name=announcement-tracker-realm-roles \
    -s protocol=openid-connect \
    -s protocolMapper=oidc-usermodel-realm-role-mapper \
    -s consentRequired=false \
    -s 'config={"multivalued":"true","userinfo.token.claim":"true","id.token.claim":"true","access.token.claim":"true","claim.name":"realm_access.roles","jsonType.label":"String"}' >/dev/null
fi

for role in manage-users query-users view-users view-realm; do
  "$KCADM" add-roles -r "$REALM" \
    --uusername "service-account-$ADMIN_CLIENT_ID" \
    --cclientid realm-management \
    --rolename "$role" >/dev/null
done

echo "Keycloak realm configuration completed."
