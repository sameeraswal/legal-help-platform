#!/usr/bin/env bash
#
# One-off script to create (or reset) an admin account directly in the dev
# database. There is no in-app bootstrap mechanism for admins — the public
# /api/auth/register endpoint rejects Role.ADMIN by design — so this is the
# supported way to get the first admin account into a local dev environment.
#
# Requires: the dev docker-compose stack running
#   (docker compose -f deploy/docker-compose.dev.yml up)
# and the `htpasswd` CLI (part of Apache httpd tools; ships with macOS).
#
# Usage:
#   ./create-admin.sh <email> <password> [name]
#
# Safe to re-run: matches on email (unique) and updates the password/role
# instead of failing.

set -euo pipefail

if [ $# -lt 2 ]; then
  echo "Usage: $0 <email> <password> [name]" >&2
  exit 1
fi

EMAIL="$1"
PASSWORD="$2"
NAME="${3:-Admin}"

if ! command -v htpasswd >/dev/null 2>&1; then
  echo "error: htpasswd not found (install Apache httpd tools, or hash the bcrypt password another way)" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/../../deploy/docker-compose.dev.yml"

# Cost factor 10 matches Spring Security's default `new BCryptPasswordEncoder()`.
HASH="$(htpasswd -bnBC 10 "" "$PASSWORD" | cut -d: -f2)"

# Escape single quotes for the inline SQL literal.
ESCAPED_EMAIL="${EMAIL//\'/\'\'}"
ESCAPED_NAME="${NAME//\'/\'\'}"

docker compose -f "$COMPOSE_FILE" exec -T mysql \
  mysql -u"${DB_USER:-legalhelp}" -p"${DB_PASSWORD:-legalhelp}" "${DB_NAME:-legalhelp}" <<SQL
INSERT INTO users (role, name, email, password_hash, status, created_at)
VALUES ('ADMIN', '${ESCAPED_NAME}', '${ESCAPED_EMAIL}', '${HASH}', 'ACTIVE', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), role = 'ADMIN', status = 'ACTIVE';
SQL

echo "Admin user ready: ${EMAIL}"
