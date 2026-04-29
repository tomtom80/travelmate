#!/bin/bash
# Wait for Keycloak to be ready, then configure SMTP for the travelmate realm.
# This variant reads SMTP settings from environment variables so it can be used
# for hosted demo environments without Mailpit.

set -euo pipefail

until /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user "$KEYCLOAK_ADMIN" \
  --password "$KEYCLOAK_ADMIN_PASSWORD" \
  --client admin-cli 2>/dev/null; do
  sleep 2
done

/opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE

SMTP_HOST_VALUE="${SMTP_HOST:-}"
SMTP_PORT_VALUE="${SMTP_PORT:-1025}"
SMTP_FROM_VALUE="${SMTP_FROM:-noreply@travelmate.local}"
SMTP_FROM_DISPLAY_NAME_VALUE="${SMTP_FROM_DISPLAY_NAME:-Travelmate}"
SMTP_SSL_VALUE="${SMTP_SSL:-false}"
SMTP_STARTTLS_VALUE="${SMTP_STARTTLS:-false}"
SMTP_AUTH_VALUE="${SMTP_AUTH:-false}"

declare -a args
args=(
  -s "smtpServer.host=${SMTP_HOST_VALUE}"
  -s "smtpServer.port=${SMTP_PORT_VALUE}"
  -s "smtpServer.from=${SMTP_FROM_VALUE}"
  -s "smtpServer.fromDisplayName=${SMTP_FROM_DISPLAY_NAME_VALUE}"
  -s "smtpServer.ssl=${SMTP_SSL_VALUE}"
  -s "smtpServer.starttls=${SMTP_STARTTLS_VALUE}"
  -s "smtpServer.auth=${SMTP_AUTH_VALUE}"
)

if [ -n "${SMTP_USER:-}" ]; then
  args+=(-s "smtpServer.user=${SMTP_USER}")
fi

if [ -n "${SMTP_PASSWORD:-}" ]; then
  args+=(-s "smtpServer.password=${SMTP_PASSWORD}")
fi

/opt/keycloak/bin/kcadm.sh update realms/travelmate "${args[@]}"

echo "SMTP configured for travelmate realm via environment variables"

APP_BASE_URL_VALUE="${APP_BASE_URL:-http://localhost:8080}"

CLIENT_UUID=$(/opt/keycloak/bin/kcadm.sh get clients -r travelmate -q "clientId=travelmate-gateway" --fields id 2>/dev/null \
  | awk -F'"' '/"id"/ {print $4; exit}')

if [ -n "$CLIENT_UUID" ]; then
  /opt/keycloak/bin/kcadm.sh update "clients/${CLIENT_UUID}" -r travelmate \
    -s "baseUrl=${APP_BASE_URL_VALUE}/iam/" \
    -s "redirectUris=[\"${APP_BASE_URL_VALUE}/login/oauth2/code/keycloak\",\"${APP_BASE_URL_VALUE}/*\"]" \
    -s "webOrigins=[\"${APP_BASE_URL_VALUE}\"]"
  echo "Client travelmate-gateway URLs updated to ${APP_BASE_URL_VALUE}"
fi

/opt/keycloak/bin/kcadm.sh update realms/travelmate \
  -s "attributes.appBaseUrl=${APP_BASE_URL_VALUE}"
echo "Realm appBaseUrl attribute set to ${APP_BASE_URL_VALUE}"
