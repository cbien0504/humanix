#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-1433}"
DB_NAME="${DB_NAME:-HRM}"
DB_ADMIN_USER="${DB_ADMIN_USER:-sa}"
: "${DB_ADMIN_PASSWORD:?DB_ADMIN_PASSWORD is required}"
FORCE_RESET="${FORCE_RESET:-false}"

SQLCMD="${SQLCMD:-}"
for candidate in "$SQLCMD" /opt/mssql-tools18/bin/sqlcmd /opt/mssql-tools/bin/sqlcmd "$(command -v sqlcmd || true)"; do
  if [[ -n "$candidate" && -x "$candidate" ]]; then SQLCMD="$candidate"; break; fi
done
[[ -x "$SQLCMD" ]] || { echo "sqlcmd not found" >&2; exit 1; }

export SQLCMDPASSWORD="$DB_ADMIN_PASSWORD"
sql() { "$SQLCMD" -S "${DB_HOST},${DB_PORT}" -U "$DB_ADMIN_USER" -C -b -I "$@"; }

echo ">> Waiting for SQL Server at ${DB_HOST}:${DB_PORT} ..."
for i in $(seq 1 60); do
  if sql -Q "SELECT 1" >/dev/null 2>&1; then break; fi
  [[ $i -eq 60 ]] && { echo "SQL Server is not reachable" >&2; exit 1; }
  sleep 2
done

echo ">> create-database.sql"
sql -i "$SCRIPT_DIR/create-database.sql"

has_schema=$(sql -d "$DB_NAME" -h -1 -W -Q "SET NOCOUNT ON; SELECT CASE WHEN OBJECT_ID('dbo.Employees') IS NULL THEN 0 ELSE 1 END" | tr -d '[:space:]')
if [[ "$has_schema" == "1" && "$FORCE_RESET" != "true" ]]; then
  echo ">> Schema already present – skipping schema/seed (set FORCE_RESET=true to rebuild)."
else
  for f in schema.sql functions.sql views.sql procedures.sql triggers.sql seed.sql; do
    if [[ -f "$SCRIPT_DIR/$f" ]]; then
      echo ">> $f"
      sql -d "$DB_NAME" -i "$SCRIPT_DIR/$f"
    fi
  done
fi

if [[ -n "${APP_DB_PASSWORD:-}" && -f "$SCRIPT_DIR/security.sql" ]]; then
  echo ">> security.sql (application login hrm_app)"
  sql -d "$DB_NAME" -v APP_DB_PASSWORD="$APP_DB_PASSWORD" -i "$SCRIPT_DIR/security.sql"
fi

echo ">> Database ${DB_NAME} is ready."
