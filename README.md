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
| 7 | Kiểm thử | ⏳ |
| 8 | Xử lý sự cố & logging | ⏳ |
| 9 | Triển khai Docker & CI | ⏳ |
| 10 | Tài liệu hoàn thiện | ⏳ |

## Tài liệu

- [01 – Phân tích yêu cầu](docs/01-requirements.md): actor, use case, ma trận phân quyền, quy trình nghiệp vụ, business rules.
- [02 – Thiết kế CSDL](docs/02-database-design.md): ERD (Mermaid), từ điển dữ liệu, ràng buộc toàn vẹn, index, trigger, dữ liệu mẫu.
- [03 – Stored procedure, view & tối ưu truy vấn](docs/03-query-optimization.md): execution plan trước/sau index đo trên 731.764 dòng chấm công.
- [04 – Kiến trúc ứng dụng](docs/04-architecture.md): phân lớp model → dao → service → controller → view, SOLID, cấu hình, xử lý lỗi, logging, bảo mật.
- [REST API & import dữ liệu](docs/api.md): xác thực API key, quy ước JSON, mã lỗi, endpoint nhân viên/chấm công/danh mục, import CSV/Excel.

## Khởi tạo cơ sở dữ liệu

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
