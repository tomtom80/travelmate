#!/usr/bin/env bash
# E2E smoke tests against the locally running Docker Compose stack.
# Usage: ./e2e-test.sh
set -euo pipefail

PASS=0
FAIL=0
ERRORS=""

check() {
  local name="$1"
  local expected="$2"
  local actual="$3"
  if [[ "$actual" == *"$expected"* ]]; then
    echo "  PASS  $name"
    PASS=$((PASS + 1))
  else
    echo "  FAIL  $name (expected: $expected, got: $actual)"
    FAIL=$((FAIL + 1))
    ERRORS="$ERRORS\n  - $name: expected '$expected', got '$actual'"
  fi
}

echo "=== Infrastructure Health ==="

check "PostgreSQL IAM" "accepting connections" \
  "$(docker compose exec -T postgres-iam pg_isready -U travelmate -d travelmate_iam 2>&1 || echo 'UNREACHABLE')"

check "PostgreSQL Trips" "accepting connections" \
  "$(docker compose exec -T postgres-trips pg_isready -U travelmate -d travelmate_trips 2>&1 || echo 'UNREACHABLE')"

check "PostgreSQL Expense" "accepting connections" \
  "$(docker compose exec -T postgres-expense pg_isready -U travelmate -d travelmate_expense 2>&1 || echo 'UNREACHABLE')"

check "RabbitMQ Management UI" "200" \
  "$(curl -s -o /dev/null -w '%{http_code}' http://localhost:15672/ 2>/dev/null || echo 'UNREACHABLE')"

check "Keycloak reachable" "200" \
  "$(curl -s -o /dev/null -w '%{http_code}' http://localhost:7082/realms/travelmate 2>/dev/null || echo 'UNREACHABLE')"

echo ""
echo "=== Application Health Endpoints ==="

check "Gateway /actuator/health" "UP" \
  "$(curl -sf http://localhost:8080/actuator/health 2>/dev/null || echo 'UNREACHABLE')"

check "IAM /iam/actuator/health" "UP" \
  "$(curl -sf http://localhost:8081/iam/actuator/health 2>/dev/null || echo 'UNREACHABLE')"

check "Trips /trips/actuator/health" "UP" \
  "$(curl -sf http://localhost:8082/trips/actuator/health 2>/dev/null || echo 'UNREACHABLE')"

check "Expense /expense/actuator/health" "UP" \
  "$(curl -sf http://localhost:8083/expense/actuator/health 2>/dev/null || echo 'UNREACHABLE')"

echo ""
echo "=== Keycloak Token Endpoint ==="

TOKEN_RESPONSE=$(curl -s -X POST http://localhost:7082/realms/travelmate/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=travelmate-gateway" \
  -d "client_secret=travelmate-gateway-secret" \
  -d "username=testuser" \
  -d "password=testpassword" \
  -d "scope=openid" 2>/dev/null || echo '{}')

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null || echo "")

if [[ -n "$ACCESS_TOKEN" && "$ACCESS_TOKEN" != "None" ]]; then
  echo "  PASS  Keycloak issues access token"
  PASS=$((PASS + 1))
else
  echo "  FAIL  Keycloak issues access token (response: $(echo "$TOKEN_RESPONSE" | head -c 200))"
  FAIL=$((FAIL + 1))
  ERRORS="$ERRORS\n  - Keycloak token: no access_token in response"
fi

echo ""
echo "=== SCS Direct Access (with JWT) ==="

if [[ -n "$ACCESS_TOKEN" && "$ACCESS_TOKEN" != "None" ]]; then
  IAM_CODE=$(curl -s -o /tmp/e2e_iam.html -w '%{http_code}' -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8081/iam/ 2>/dev/null || echo '000')
  check "IAM direct (HTTP 200)" "200" "$IAM_CODE"
  if [[ "$IAM_CODE" == "200" ]]; then
    check "IAM returns HTML" "Travelmate" "$(head -20 /tmp/e2e_iam.html)"
  fi

  TRIPS_CODE=$(curl -s -o /tmp/e2e_trips.html -w '%{http_code}' -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8082/trips/ 2>/dev/null || echo '000')
  check "Trips direct (HTTP 200)" "200" "$TRIPS_CODE"
  if [[ "$TRIPS_CODE" == "200" ]]; then
    check "Trips returns HTML" "Travelmate" "$(head -20 /tmp/e2e_trips.html)"
  fi

  EXPENSE_CODE=$(curl -s -o /tmp/e2e_expense.html -w '%{http_code}' -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8083/expense/ 2>/dev/null || echo '000')
  check "Expense direct (HTTP 200)" "200" "$EXPENSE_CODE"
  if [[ "$EXPENSE_CODE" == "200" ]]; then
    check "Expense returns HTML" "Travelmate" "$(head -20 /tmp/e2e_expense.html)"
  fi
else
  echo "  SKIP  (no token available)"
fi

echo ""
echo "=== Gateway Routing (unauthenticated) ==="

GW_IAM_CODE=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/iam/ 2>/dev/null || echo '000')
check "Gateway /iam/ redirects to login (302)" "302" "$GW_IAM_CODE"

GW_TRIPS_CODE=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/trips/ 2>/dev/null || echo '000')
check "Gateway /trips/ redirects to login (302)" "302" "$GW_TRIPS_CODE"

echo ""
echo "=== Gateway OAuth2 Login Flow ==="

# Step 1: Hit gateway, capture session cookie and redirect
rm -f /tmp/e2e_cookies.txt
GW_LOGIN_REDIRECT=$(curl -s -D- -c /tmp/e2e_cookies.txt http://localhost:8080/trips/ 2>/dev/null | grep -i '^location:' | tr -d '\r' | awk '{print $2}')
check "Gateway redirects to /oauth2/authorization" "/oauth2/authorization/keycloak" "$GW_LOGIN_REDIRECT"

# Step 2: Follow to Keycloak (full redirect chain)
KC_LOGIN_PAGE=$(curl -s -L -c /tmp/e2e_cookies.txt -b /tmp/e2e_cookies.txt http://localhost:8080/oauth2/authorization/keycloak 2>/dev/null)
KC_ACTION=$(echo "$KC_LOGIN_PAGE" | grep -o 'action="[^"]*"' | head -1 | sed 's/action="//;s/"//' | python3 -c "import html,sys; print(html.unescape(sys.stdin.read().strip()))" 2>/dev/null || echo "")

if [[ -n "$KC_ACTION" ]]; then
  echo "  PASS  Keycloak login page loaded"
  PASS=$((PASS + 1))
else
  echo "  FAIL  Keycloak login page loaded (no form action found)"
  FAIL=$((FAIL + 1))
  ERRORS="$ERRORS\n  - Keycloak login page: no form action"
fi

# Step 3: Submit credentials to Keycloak
if [[ -n "$KC_ACTION" ]]; then
  KC_CALLBACK=$(curl -s -D- -c /tmp/e2e_cookies.txt -b /tmp/e2e_cookies.txt \
    -d "username=testuser" -d "password=testpassword" \
    "$KC_ACTION" 2>/dev/null | grep -i '^location:' | tr -d '\r' | awk '{print $2}')
  check "Keycloak redirects back with auth code" "login/oauth2/code/keycloak" "$KC_CALLBACK"

  # Step 4: Follow callback to gateway (code exchange)
  if [[ -n "$KC_CALLBACK" ]]; then
    GW_AFTER_LOGIN=$(curl -s -D- -c /tmp/e2e_cookies.txt -b /tmp/e2e_cookies.txt \
      "$KC_CALLBACK" 2>/dev/null)
    GW_AFTER_CODE=$(echo "$GW_AFTER_LOGIN" | grep -i '^http/' | head -1 | awk '{print $2}')
    GW_AFTER_LOCATION=$(echo "$GW_AFTER_LOGIN" | grep -i '^location:' | head -1 | tr -d '\r' | awk '{print $2}')
    echo "  INFO  After code exchange: HTTP $GW_AFTER_CODE -> Location: $GW_AFTER_LOCATION"

    # Step 5: Follow post-login redirect
    if [[ -n "$GW_AFTER_LOCATION" ]]; then
      FINAL_RESP=$(curl -s -D /tmp/e2e_final_headers.txt -c /tmp/e2e_cookies.txt -b /tmp/e2e_cookies.txt \
        -o /tmp/e2e_final.html \
        -w '%{http_code}' \
        "http://localhost:8080${GW_AFTER_LOCATION}" 2>/dev/null || echo '000')
      check "Post-login page loads (HTTP 200)" "200" "$FINAL_RESP"
      if [[ "$FINAL_RESP" == "200" ]]; then
        check "Post-login page is HTML" "Travelmate" "$(head -20 /tmp/e2e_final.html)"
      else
        echo "  INFO  Response headers:"
        cat /tmp/e2e_final_headers.txt 2>/dev/null | head -10
        echo "  INFO  Response body:"
        head -10 /tmp/e2e_final.html 2>/dev/null
      fi
    fi
  fi
fi

echo ""
echo "========================================"
echo "  Results: $PASS passed, $FAIL failed"
echo "========================================"
if [[ $FAIL -gt 0 ]]; then
  echo -e "\nFailures:$ERRORS"
  exit 1
fi
