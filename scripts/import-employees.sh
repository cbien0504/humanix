#!/usr/bin/env bash
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
