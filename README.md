# HRM System – Hệ thống Quản lý Nhân sự

Ứng dụng web quản lý nhân sự viết bằng **Java 17 · Servlet/JSP · JDBC · Microsoft SQL Server**, gồm các module:
thông tin nhân viên, chấm công & nghỉ phép, tính lương, tuyển dụng, đánh giá hiệu suất và phân quyền theo vai trò.

## Tiến độ thực hiện

| Bước | Nội dung | Trạng thái |
|------|----------|:----------:|
| 1 | Phân tích yêu cầu – [docs/01-requirements.md](docs/01-requirements.md) | ✅ |
| 2 | Thiết kế CSDL – [docs/02-database-design.md](docs/02-database-design.md), [db/](db) | ✅ |
| 3 | Stored procedure, view, tối ưu truy vấn, kiểm tra dữ liệu – [docs/03-query-optimization.md](docs/03-query-optimization.md) | ✅ |
| 4 | Kiến trúc ứng dụng – [docs/04-architecture.md](docs/04-architecture.md) | ✅ |
| 5 | Phát triển tính năng (6 module + dashboard, báo cáo) | ✅ |
| 6 | REST API & import dữ liệu – [docs/api.md](docs/api.md) | ✅ |
| 7 | Kiểm thử – [docs/05-test-report.md](docs/05-test-report.md) | ✅ |
| 8 | Xử lý sự cố & logging – [docs/06-troubleshooting-log.md](docs/06-troubleshooting-log.md) | ✅ |
| 9 | Triển khai Docker & CI – [Dockerfile](Dockerfile), [docker-compose.yml](docker-compose.yml), [CI](.github/workflows/ci.yml) | ✅ |
| 10 | Tài liệu hoàn thiện | ⏳ |

## Tài liệu

- [01 – Phân tích yêu cầu](docs/01-requirements.md): actor, use case, ma trận phân quyền, quy trình nghiệp vụ, business rules.
- [02 – Thiết kế CSDL](docs/02-database-design.md): ERD (Mermaid), từ điển dữ liệu, ràng buộc toàn vẹn, index, trigger, dữ liệu mẫu.
- [03 – Stored procedure, view & tối ưu truy vấn](docs/03-query-optimization.md): execution plan trước/sau index đo trên 731.764 dòng chấm công.
- [04 – Kiến trúc ứng dụng](docs/04-architecture.md): phân lớp model → dao → service → controller → view, SOLID, cấu hình, xử lý lỗi, logging, bảo mật.
- [05 – Báo cáo kiểm thử](docs/05-test-report.md): 331 unit test + 8 integration test, độ phủ tầng service 94,9 %, lỗi thật tìm được nhờ test.
- [06 – Nhật ký xử lý sự cố & logging](docs/06-troubleshooting-log.md): diễn tập lương âm, trùng mã (kể cả 6 request song song), mất kết nối DB; 26 sự cố thực tế; runbook.
- [REST API & import dữ liệu](docs/api.md): xác thực API key, quy ước JSON, mã lỗi, endpoint nhân viên/chấm công/danh mục, import CSV/Excel.

## Chạy nhanh bằng Docker (khuyến nghị)

Yêu cầu: Docker 24+ với Compose v2, ~3 GB RAM trống.

```bash
cp .env.example .env            # đổi MSSQL_SA_PASSWORD, APP_DB_PASSWORD, HRM_API_KEY
docker compose up -d --build    # lần đầu vài phút: tải image, build, khởi tạo DB và dữ liệu mẫu
docker compose ps               # chờ app ở trạng thái (healthy)
# → http://localhost:8080  ·  tài khoản demo: admin / hr / manager / employee, mật khẩu Demo@123
```

| Service | Vai trò |
|---|---|
| `db` | SQL Server 2022 (Developer), dữ liệu trong volume `mssql-data`, healthcheck bằng `sqlcmd` |
| `db-init` | chạy một lần `db/init-db.sh`: schema, view, SP, trigger, dữ liệu mẫu, login `hrm_app` quyền tối thiểu; chạy lại thì bỏ qua phần đã có |
| `app` | image `hrm-system` (multi-stage: Maven build → Tomcat 10.1/JRE 17), chạy bằng user không phải root, log trong volume `app-logs`, healthcheck `/health` |

```bash
docker compose logs -f app                       # log ứng dụng
FORCE_RESET=true docker compose up db-init       # tạo lại dữ liệu mẫu từ đầu
docker compose down                              # dừng (giữ dữ liệu) · down -v: xóa luôn dữ liệu
```

Image không chứa mật khẩu nào: mọi cấu hình đi qua biến môi trường (`HRM_DB_URL`, `HRM_DB_USER`, `HRM_DB_PASSWORD`, `HRM_API_KEY`…).

## Khởi tạo cơ sở dữ liệu (không dùng Docker Compose)

```bash
docker run -d --name hrm-db -e ACCEPT_EULA=Y -e MSSQL_SA_PASSWORD='<StrongPassword>' \
  -e TZ=Asia/Ho_Chi_Minh -p 1433:1433 mcr.microsoft.com/mssql/server:2022-latest
docker cp db hrm-db:/db
docker exec -e DB_ADMIN_PASSWORD='<StrongPassword>' hrm-db bash /db/init-db.sh
```

Script tạo database `HRM` (24 bảng, ràng buộc PK/FK/UNIQUE/CHECK, index, trigger, 5 stored procedure, 3 view) và nạp dữ liệu mẫu:
6 phòng ban, 36 nhân viên, 33 tài khoản (mật khẩu `Demo@123`), ~2.000 bản ghi chấm công, đơn nghỉ phép, bảng lương 3 tháng,
tuyển dụng, đánh giá. Truyền thêm `-e APP_DB_PASSWORD=...` để tạo login quyền tối thiểu `hrm_app` cho ứng dụng.

| Script | Nội dung |
|--------|----------|
| `db/procedures.sql` | `sp_CalculateMonthlyPayroll`, `sp_GetAttendanceReport`, `sp_GetDepartmentStatistics`, `sp_InitLeaveBalances`, `sp_RunDataValidation` |
| `db/views.sql` | `vw_EmployeeDetails`, `vw_MonthlyAttendanceSummary`, `vw_PayrollDetails` |
| `db/data-validation.sql` | 16 kiểm tra toàn vẹn (trùng mã NV, lương âm, nghỉ phép chồng lấn…) + tự kiểm thử |
| `db/perf/run-benchmark.sh` | Sinh 731.764 dòng chấm công vào DB riêng `HRM_PERF` và đo execution plan |

## Tính năng chính

| Module | Chức năng |
|--------|-----------|
| Xác thực & phân quyền | Đăng nhập BCrypt, khóa 15 phút sau 5 lần sai, đổi mật khẩu, quản trị tài khoản & vai trò, CSRF, phân quyền URL + phạm vi dữ liệu, nhật ký audit |
| Thông tin nhân viên | Phòng ban, chức danh & khung lương, hồ sơ nhân viên (tìm kiếm không dấu, lọc, sắp xếp, phân trang), lịch sử lương |
| Chấm công & nghỉ phép | Check-in/out theo giờ server, quy tắc đi muộn/nửa ngày, HR điều chỉnh; đơn nghỉ phép, số dư, luồng duyệt của quản lý |
| Tính lương | Kỳ lương OPEN→CALCULATED→APPROVED→PAID, SP tính lương set-based, phiếu lương, mô phỏng Gross→Net, xuất Excel/CSV |
| Tuyển dụng | Tin tuyển dụng, pipeline ứng viên dạng Kanban, lịch sử bước, tiếp nhận thành nhân viên thử việc |
| Đánh giá hiệu suất | Kỳ đánh giá, tiêu chí có trọng số, tự đánh giá → quản lý chấm, xếp loại A–E |
| Báo cáo & dashboard | Dashboard theo vai trò (Chart.js), thống kê phòng ban, báo cáo chấm công, xuất Excel/CSV |
| Tích hợp | REST API JSON (`/api/employees`, `/api/attendance` kể cả nhận lô 1000 bản ghi từ máy chấm công, danh mục) xác thực bằng `X-API-Key`; import nhân viên từ CSV/Excel (web + dòng lệnh) với 3 chế độ và báo lỗi theo từng dòng/cột |

## Build & chạy ứng dụng (Tomcat)

Yêu cầu: JDK 17+, Tomcat 10.1 (Jakarta EE 10). Maven không bắt buộc – dùng Maven Wrapper đi kèm.

```bash
./mvnw clean package                 # tạo target/hrm.war
export HRM_DB_URL="jdbc:sqlserver://localhost:1433;databaseName=HRM;encrypt=true;trustServerCertificate=true"
export HRM_DB_USER=hrm_app HRM_DB_PASSWORD='<mật khẩu hrm_app>'
cp target/hrm.war $CATALINA_HOME/webapps/ && $CATALINA_HOME/bin/catalina.sh run
# → http://localhost:8080/hrm/        kiểm tra: http://localhost:8080/hrm/health
```

Cấu hình đọc từ `src/main/resources/db.properties`, `app.properties` với biến môi trường ghi đè (`${HRM_DB_PASSWORD:}`…) –
không có mật khẩu nào trong mã nguồn. Danh sách biến: [docs/04-architecture.md#5](docs/04-architecture.md).

## Kiểm thử

```bash
./mvnw test       # 331 unit test: service (Mockito), DAO (JDBC giả lập), phân quyền, bảo mật
./mvnw verify     # + cổng coverage JaCoCo: build fail nếu tầng service < 60 % dòng (hiện 94,9 %)
# + 8 integration test trên SQL Server thật (tự bỏ qua nếu không đặt biến):
HRM_IT_DB_URL="jdbc:sqlserver://localhost:1433;databaseName=HRM;encrypt=true;trustServerCertificate=true" \
HRM_IT_DB_USER=hrm_app HRM_IT_DB_PASSWORD='…' ./mvnw verify
```

Điểm nhấn: stored procedure tính lương được đối chiếu **từng phiếu** với công thức Java trên dữ liệu thật; lương âm / trùng mã
được chặn ở cả service lẫn ràng buộc DB; mất kết nối DB trả 503 trong < 1 giây. Chi tiết: [docs/05-test-report.md](docs/05-test-report.md).

## CI – GitHub Actions

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) chạy khi push lên `main` và với mọi pull request:

| Job | Nội dung |
|---|---|
| `build` | `./mvnw verify`: biên dịch, enforcer, 333 unit test, cổng coverage JaCoCo; lưu báo cáo test/coverage và file WAR |
| `integration` | khởi động SQL Server 2022 với mật khẩu sinh ngẫu nhiên mỗi lần chạy, chạy `db/init-db.sh`, rồi `./mvnw verify` kèm 8 integration test |
| `docker` | build image từ `Dockerfile` (có cache), không push |

## Tích hợp hệ thống & chuyển đổi dữ liệu

```bash
export HRM_API_KEY='<chuỗi ngẫu nhiên dài>'        # bật API key cho hệ thống ngoài (để trống = tắt)
curl -H "X-API-Key: $HRM_API_KEY" "http://localhost:8080/hrm/api/employees/by-code/EMP0016"
curl -H "X-API-Key: $HRM_API_KEY" -H "Content-Type: application/json" \
     -d '[{"employeeCode":"EMP0016","workDate":"2026-10-30","checkIn":"08:20","checkOut":"17:40"}]' \
     http://localhost:8080/hrm/api/attendance/bulk

# Import nhân viên từ file (cùng biến HRM_DB_* như ứng dụng): kiểm tra trước, rồi mới ghi
scripts/import-employees.sh data/import/employees-with-errors.csv validate target/import-errors.csv
scripts/import-employees.sh data/import/employees-sample.csv all-or-nothing
```

Hoặc dùng màn hình **Import dữ liệu** (`/import`, quyền HR/Admin). Chi tiết: [docs/api.md](docs/api.md).
