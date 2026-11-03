# 08 – Hạn chế hiện tại & kế hoạch cải tiến

> Dành cho: người đánh giá dự án và nhóm phát triển tiếp theo.
> Mỗi mục xuất phát từ một hạn chế **đã quan sát được** khi xây dựng, kiểm thử hoặc diễn tập sự cố – kèm bằng chứng và đề xuất.
> Công sức: S ≈ 1–3 ngày · M ≈ 1–2 tuần · L > 2 tuần.

## 1. Hạn chế hiện tại

| # | Hạn chế | Bằng chứng | Ảnh hưởng |
|---|---|---|---|
| H1 | Tham số lương và biểu thuế **không có ngày hiệu lực** (`PayrollSettings`, `TaxBrackets` chỉ lưu giá trị hiện hành) | `db/schema.sql` | tính lại một tháng cũ sau khi đổi tham số sẽ dùng tham số mới; hiện được chặn gián tiếp vì kỳ đã duyệt/đã chi bị khóa |
| H2 | Công thức lương tồn tại **hai nơi**: `sp_CalculateMonthlyPayroll` (chạy thật) và `PayrollCalculator` (mô phỏng) | `PayrollProcedureParityIT` phải đối chiếu từng phiếu | mỗi thay đổi luật phải sửa hai chỗ; mới được bảo vệ bằng test, chưa loại bỏ được trùng lặp |
| H3 | Khi mất DB, **mỗi request vẫn chờ đủ 5 giây** (connection timeout) rồi mới trả 503 | diễn tập ở `06-troubleshooting-log.md` mục 2.3 | người dùng chờ lâu; tải dồn lên pool khi DB chập chờn |
| H4 | **Một API key** dùng chung, quyền cố định, **chưa giới hạn tần suất** | `AuthenticationFilter`, `AccessPolicy` | không phân biệt/thu hồi riêng từng hệ thống tích hợp; dễ bị gọi dồn dập |
| H5 | Phiên đăng nhập nằm trong bộ nhớ Tomcat | `META-INF/context.xml` (`<Manager pathname=""/>`) | restart/deploy là mọi người phải đăng nhập lại; chưa chạy được nhiều instance sau load balancer |
| H6 | jBCrypt 0.4 không đọc hash `$2b$`/`$2y$` (dù ràng buộc DB cho phép) | `PasswordAndTokenTest`, sự cố #23 | không nhập được mật khẩu đã băm từ hệ thống cũ viết bằng PHP/Python |
| H7 | Tầng web (controller, filter, JSP) và JavaScript **chưa có test tự động**; JaCoCo chỉ tính unit test | `05-test-report.md` mục 3.2 | lỗi giao diện chỉ phát hiện bằng kiểm thử thủ công (như sự cố #19) |
| H8 | Giờ làm = giờ ra − giờ vào, **chưa trừ nghỉ trưa**; chưa có tăng ca, ca làm việc | cột tính toán `Attendance.WorkHours` trong `db/schema.sql`, ảnh chấm công (07:51–18:06 = 10,25 giờ) | số giờ làm cao hơn thực tế; công ty làm ca không dùng được |
| H9 | Ngày lễ chỉ nhập bằng SQL/seed, **chưa có màn hình quản lý** | không có view/controller cho `Holidays` | mỗi năm phải nhờ IT chạy script |
| H10 | Chưa có **thông báo** (email/in-app) cho đơn chờ duyệt, phiếu lương mới, hạn đánh giá | — | người dùng phải tự vào kiểm tra |
| H11 | CV ứng viên chỉ lưu **đường dẫn** (http/https), chưa lưu file | `RecruitmentService` | phụ thuộc nơi lưu bên ngoài |
| H12 | Workflow CI mới được kiểm chứng bằng cách **chạy lại thủ công các bước tương đương** (build image, `docker compose`, `./mvnw verify` kèm integration test trên DB mới khởi tạo) | `.github/workflows/ci.yml`, `05-test-report.md` | cần theo dõi lần chạy đầu trên GitHub Actions (runner, cache Maven/Docker, thời gian khởi động SQL Server) |

## 2. Lộ trình đề xuất

### Ngắn hạn – ổn định & vận hành (1 sprint)

| Ưu tiên | Đề xuất | Giải quyết | Công sức |
|---|---|---|---|
| 1 | **Fail-fast khi DB sập**: trạng thái DB do `/health` theo dõi (đã ghi log khi đổi trạng thái) được dùng làm "cầu dao" – trong lúc DOWN trả 503 ngay, thử lại định kỳ | H3 | S |
| 2 | Chạy CI thật trên GitHub, thêm **quét lỗ hổng phụ thuộc** (OWASP Dependency-Check/Dependabot) và **quét image** (Trivy), đẩy image lên GHCR khi merge `main` | H12 | S |
| 3 | Chuyển sang thư viện BCrypt hỗ trợ `$2a/$2b/$2y` (ví dụ `at.favre.lib:bcrypt`), giữ nguyên cost 10 | H6 | S |
| 4 | Test tầng web: `WebContext`/`ErrorHandler`/filter với request giả lập; 5–10 kịch bản Playwright (đăng nhập, chấm công, gửi/duyệt phép, tính lương, import) chạy trong CI trên stack compose | H7 | M |
| 5 | Log dạng JSON + metrics (Micrometer/Prometheus: request, lỗi, pool Hikari, thời gian SQL) và dashboard Grafana | vận hành | M |

### Trung hạn – nghiệp vụ (1–2 quý)

| Ưu tiên | Đề xuất | Giải quyết | Công sức |
|---|---|---|---|
| 6 | **Tham số lương có hiệu lực theo thời gian** (`ValidFrom`/`ValidTo` cho `PayrollSettings`, `TaxBrackets`); kỳ lương lưu ảnh chụp tham số đã dùng | H1 | M |
| 7 | **Một nguồn công thức lương**: tính trong Java (đã có `PayrollCalculator` đã kiểm chứng) và ghi hàng loạt bằng batch JDBC, hoặc mô phỏng bằng chính SP với cờ `@DryRun`; giữ test đối chiếu cho giai đoạn chuyển | H2 | M |
| 8 | Chấm công: nghỉ trưa cấu hình được, tăng ca (OT) có duyệt, ca làm việc; nhận dữ liệu máy chấm công qua API bulk sẵn có | H8 | L |
| 9 | Màn hình quản lý ngày lễ, cộng dồn phép năm chưa dùng, đơn nửa ngày, duyệt nhiều cấp | H9 | M |
| 10 | Thông báo email/in-app (hàng đợi gửi thư, mẫu tiếng Việt, tùy chọn tắt) | H10 | M |
| 11 | Nhiều API key theo hệ thống tích hợp: phạm vi quyền, ngày hết hạn, thu hồi, ghi audit theo key, **rate limiting** | H4 | M |
| 12 | Lưu file CV (kho tệp có kiểm tra định dạng/kích thước, quét mã độc) | H11 | M |

### Dài hạn – mở rộng

| Đề xuất | Giải quyết | Công sức |
|---|---|---|
| Đăng nhập một lần (OIDC/SAML với Microsoft Entra ID, Google Workspace) và **MFA** bắt buộc cho HR/Admin | bảo mật | M |
| Phiên đăng nhập ngoài Tomcat (Redis/JDBC session store) → chạy nhiều instance sau load balancer, deploy không gián đoạn | H5 | M |
| Tuân thủ **Nghị định 13/2023/NĐ-CP** về bảo vệ dữ liệu cá nhân: phân loại dữ liệu nhạy cảm, mã hóa cột (CCCD, tài khoản ngân hàng), thời hạn lưu trữ, xuất/xóa dữ liệu theo yêu cầu | pháp lý | L |
| Tìm kiếm toàn văn (SQL Server Full-Text) cho hồ sơ và ứng viên khi dữ liệu lớn; tách báo cáo sang bản sao chỉ đọc | hiệu năng | M |
| Ứng dụng di động/PWA chấm công kèm vị trí (geofence) | trải nghiệm | L |
| Đa ngôn ngữ (vi/en), đa công ty | mở rộng | L |

## 3. Nguyên tắc khi thực hiện

- Mỗi cải tiến đi kèm **test chứng minh** (như các lỗi ở `05-test-report.md` mục 5 và `06-troubleshooting-log.md`) và cập nhật tài liệu tương ứng.
- Không phá vỡ tích hợp: thay đổi API theo hướng bổ sung (trường mới bị bỏ qua ở client cũ), thông báo trước khi bỏ endpoint.
- Thay đổi CSDL bằng script migration có thứ tự (gợi ý: Flyway) thay cho chạy lại `init-db.sh`, vì dữ liệu thật không được tạo lại.
