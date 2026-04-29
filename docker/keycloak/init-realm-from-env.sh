#!/bin/bash
# Render the Keycloak realm-export template by substituting ${APP_BASE_URL}
# with the value from the environment. Must run BEFORE Keycloak starts so
# the resulting realm-export.json is picked up by `kc.sh start --import-realm`.

set -euo pipefail

APP_BASE_URL_VALUE="${APP_BASE_URL:-http://localhost:8080}"

SOURCE="/opt/keycloak/data/realm-export.template.json"
TARGET="/opt/keycloak/data/import/realm-export.json"

if [ ! -f "$SOURCE" ]; then
  echo "[init-realm] template not found at $SOURCE" >&2
  exit 1
fi

mkdir -p "$(dirname "$TARGET")"

sed "s|\${APP_BASE_URL}|${APP_BASE_URL_VALUE}|g" "$SOURCE" > "$TARGET"

echo "[init-realm] rendered $TARGET with APP_BASE_URL=${APP_BASE_URL_VALUE}"
