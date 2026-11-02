#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Employee data migration from CSV/XLSX (same rules as the web import screen).
#   export HRM_DB_URL="jdbc:sqlserver://localhost:1433;databaseName=HRM;encrypt=true;trustServerCertificate=true"
#   export HRM_DB_USER=hrm_app HRM_DB_PASSWORD='...'
#   scripts/import-employees.sh data/import/employees-sample.csv validate
#   scripts/import-employees.sh data/import/employees-sample.csv valid-only errors.csv
# Modes: validate (default, dry run) | valid-only | all-or-nothing
# Requires Java 17+ and a build (./mvnw package). Exit code 0 = no errors, 1 = row errors.
# -----------------------------------------------------------------------------
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WEBAPP="${HRM_WEBAPP_DIR:-$ROOT/target/hrm}"
if [[ ! -d "$WEBAPP/WEB-INF/classes" ]]; then
  echo "Chưa build ứng dụng – chạy ./mvnw package trước." >&2
  exit 2
fi
[[ $# -ge 1 ]] || { echo "Cách dùng: $0 <file.csv|file.xlsx> [validate|valid-only|all-or-nothing] [report.csv]" >&2; exit 2; }
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
major=$("$JAVA" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)
if [[ -z "$major" || "$major" -lt 17 ]]; then
  echo "Cần Java 17+ (đang có: ${major:-không tìm thấy}). Đặt JAVA_HOME tới JDK 17." >&2
  exit 2
fi
args=(--file "$1" --mode "${2:-validate}")
[[ -n "${3:-}" ]] && args+=(--report "$3")
exec "$JAVA" -Dfile.encoding=UTF-8 -DHRM_CONSOLE_LEVEL="${HRM_CONSOLE_LEVEL:-WARN}" -cp "$WEBAPP/WEB-INF/classes:$WEBAPP/WEB-INF/lib/*" com.hrm.tools.DataImportTool "${args[@]}"
