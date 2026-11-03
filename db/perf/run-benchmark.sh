#!/usr/bin/env bash
set -euo pipefail
PERF_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DB_DIR="$(dirname "$PERF_DIR")"
DB_HOST="${DB_HOST:-localhost}"; DB_PORT="${DB_PORT:-1433}"; DB_ADMIN_USER="${DB_ADMIN_USER:-sa}"
: "${DB_ADMIN_PASSWORD:?DB_ADMIN_PASSWORD is required}"
OUT_DIR="${OUT_DIR:-$PERF_DIR/results}"

SQLCMD="${SQLCMD:-}"
for candidate in "$SQLCMD" /opt/mssql-tools18/bin/sqlcmd /opt/mssql-tools/bin/sqlcmd "$(command -v sqlcmd || true)"; do
  if [[ -n "$candidate" && -x "$candidate" ]]; then SQLCMD="$candidate"; break; fi
done
export SQLCMDPASSWORD="$DB_ADMIN_PASSWORD"
sql() { "$SQLCMD" -S "${DB_HOST},${DB_PORT}" -U "$DB_ADMIN_USER" -C -b -I "$@"; }

echo ">> Recreating HRM_PERF"
sql -Q "IF DB_ID('HRM_PERF') IS NOT NULL BEGIN ALTER DATABASE HRM_PERF SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE HRM_PERF; END;
        CREATE DATABASE HRM_PERF COLLATE Latin1_General_100_CI_AI;"
for f in schema.sql functions.sql views.sql procedures.sql; do
  echo ">> $f"; sql -d HRM_PERF -i "$DB_DIR/$f" >/dev/null
done
echo ">> Loading synthetic data (may take ~30 s)"
time sql -d HRM_PERF -i "$PERF_DIR/perf-data.sql"
mkdir -p "$OUT_DIR"
echo ">> Running benchmark"
sql -d HRM_PERF -W -i "$PERF_DIR/benchmark.sql" > "$OUT_DIR/benchmark-output.txt" 2>&1
echo ">> Done: $OUT_DIR/benchmark-output.txt"
