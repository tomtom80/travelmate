#!/usr/bin/env bash

set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this script as root." >&2
  exit 1
fi

if [[ -z "${DEMO_DOMAIN:-}" || -z "${DEMO_AUTH_DOMAIN:-}" || -z "${ACME_EMAIL:-}" ]]; then
  echo "Required environment variables: DEMO_DOMAIN, DEMO_AUTH_DOMAIN, ACME_EMAIL" >&2
  exit 1
fi

DEMO_USER="${DEMO_USER:-travelmate}"
DEMO_GROUP="${DEMO_GROUP:-${DEMO_USER}}"
DEMO_HOME="${DEMO_HOME:-/opt/travelmate-demo}"
INSTALL_CADDY="${INSTALL_CADDY:-true}"
SETUP_UFW="${SETUP_UFW:-true}"

apt-get update
apt-get install -y --no-install-recommends \
  ca-certificates \
  curl \
  git \
  gnupg \
  lsb-release \
  ufw

if ! command -v docker >/dev/null 2>&1; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  . /etc/os-release
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu ${VERSION_CODENAME} stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update
  apt-get install -y --no-install-recommends \
    docker-ce \
    docker-ce-cli \
    containerd.io \
    docker-buildx-plugin \
    docker-compose-plugin
fi

if ! id -u "${DEMO_USER}" >/dev/null 2>&1; then
  useradd --create-home --home-dir "${DEMO_HOME}" --shell /bin/bash "${DEMO_USER}"
else
  usermod -d "${DEMO_HOME}" "${DEMO_USER}" || true
fi

install -d -o "${DEMO_USER}" -g "${DEMO_GROUP}" -m 0755 \
  "${DEMO_HOME}" \
  "${DEMO_HOME}/docker" \
  "${DEMO_HOME}/docker/keycloak" \
  "${DEMO_HOME}/docker/keycloak/themes" \
  "${DEMO_HOME}/scripts"

usermod -aG docker "${DEMO_USER}"

if [[ "${INSTALL_CADDY}" == "true" ]] && ! command -v caddy >/dev/null 2>&1; then
  apt-get install -y --no-install-recommends debian-keyring debian-archive-keyring apt-transport-https
  curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
  curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' \
    | tee /etc/apt/sources.list.d/caddy-stable.list >/dev/null
  apt-get update
  apt-get install -y --no-install-recommends caddy
fi

if [[ "${INSTALL_CADDY}" == "true" ]]; then
  cat > /etc/caddy/Caddyfile <<EOF
{
	email ${ACME_EMAIL}
}

${DEMO_DOMAIN} {
	encode zstd gzip
	reverse_proxy 127.0.0.1:8080
}

${DEMO_AUTH_DOMAIN} {
	encode zstd gzip
	reverse_proxy 127.0.0.1:7082
}
EOF
  systemctl enable caddy
  systemctl restart caddy
fi

if [[ "${SETUP_UFW}" == "true" ]]; then
  ufw allow OpenSSH
  ufw allow 80/tcp
  ufw allow 443/tcp
  ufw --force enable
fi

systemctl enable docker
systemctl restart docker

cat <<EOF
Bootstrap complete.

Demo user: ${DEMO_USER}
Demo home: ${DEMO_HOME}
Domains:
  app:  ${DEMO_DOMAIN}
  auth: ${DEMO_AUTH_DOMAIN}

Next steps:
1. Copy .env.demo to ${DEMO_HOME}/.env.demo
2. Ensure DNS for both domains points to this host
3. Add the GitHub Actions secrets described in docs/operations/2026-03-26-demo-betriebskonzept.md
4. Run the demo deploy workflow
EOF
