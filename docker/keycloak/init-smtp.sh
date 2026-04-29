#!/bin/bash
# Wait for Keycloak to be ready, then configure SMTP for the travelmate realm.
# This script runs as a background process alongside the Keycloak server.

until /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user "$KEYCLOAK_ADMIN" \
  --password "$KEYCLOAK_ADMIN_PASSWORD" \
  --client admin-cli 2>/dev/null; do
  sleep 2
done

# Disable SSL requirement on master realm so admin API works over HTTP in Docker
/opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE

/opt/keycloak/bin/kcadm.sh update realms/travelmate \
  -s "smtpServer.host=mailpit" \
  -s "smtpServer.port=1025" \
  -s "smtpServer.from=noreply@travelmate.local" \
  -s "smtpServer.fromDisplayName=Travelmate" \
  -s "smtpServer.ssl=false" \
  -s "smtpServer.starttls=false" \
  -s "smtpServer.auth=false"

echo "SMTP configured for travelmate realm"

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
