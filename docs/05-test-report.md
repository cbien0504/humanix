# 05 – Báo cáo kiểm thử

> JUnit 5.11 · Mockito 5.14 · AssertJ 3.26 · JaCoCo 0.8.12 · Maven Surefire/Failsafe 3.5.2 · SQL Server 2022
> Chạy ngày 03/11/2026 trên commit của Bước 7. Kết quả: **331 unit test + 8 integration test – 0 lỗi**,
> độ phủ dòng tầng service **94,9 %** (ngưỡng bắt buộc 60 %, build tự fail nếu thấp hơn).

## 1. Chiến lược

```text
                 ┌───────────────────────────────┐
                 │ Kiểm thử thủ công / E2E        │  curl + Playwright (Bước 5–6): luồng trên trình duyệt,
                 │                                │  phân quyền thật, file Excel/CSV tải về
              ┌──┴───────────────────────────────┴──┐
              │ Integration test (*IT, 8)            │  SQL Server thật qua DAO thật, mọi thay đổi
              │                                      │  được ROLLBACK – không làm bẩn dữ liệu
           ┌──┴──────────────────────────────────────┴──┐
           │ Unit test DAO (30)                          │  JDBC giả lập: câu SQL, tham số bind,
           │                                             │  transaction, dịch lỗi SQL, mất kết nối
        ┌──┴─────────────────────────────────────────────┴──┐
        │ Unit test service & tiện ích (301)                 │  quy tắc nghiệp vụ với DAO giả lập (Mockito),
        │                                                    │  công thức thuần (lương, điểm, ngày công)
        └────────────────────────────────────────────────────┘
```

| Nguyên tắc | Áp dụng |
|---|---|
| Thời gian cố định | `Fixtures.clock()` = thứ Hai 02/11/2026 09:00 (Asia/Ho_Chi_Minh) – mọi quy tắc ngày tháng (đi muộn, phép quá hạn, tháng tương lai) cho kết quả xác định |
| Không mock thứ đang kiểm thử | Import nhân viên dùng **`EmployeeService` thật** + DAO giả lập, nên kiểm tra đúng các quy tắc mà màn hình nhập tay dùng |
| Transaction giả có ngữ nghĩa | `DirectTransactions` đếm commit/rollback như transaction thật → kiểm chứng được "tất cả hoặc không" |
| Mockito strict stubs | Stub thừa hoặc gọi sai tham số làm test đỏ → test không "xanh nhờ may mắn" |
| Giá trị kỳ vọng tính tay | Mọi con số lương trong test được tính tay từ tham số 2026 (ghi chú ngay trong test), không copy từ output của code |
| Integration test không phá dữ liệu | `ItDatabase.rollbackAfter(...)` chạy trong transaction và luôn rollback; tự bỏ qua khi không có `HRM_IT_DB_URL` |

## 2. Cách chạy

```bash
./mvnw test                      # 331 unit test, báo cáo: target/surefire-reports
./mvnw verify                    # + integration test + cổng coverage JaCoCo (≥ 60 % dòng của com.hrm.service*)
open target/site/jacoco/index.html

# Integration test trên SQL Server thật (docker-compose/CI đặt sẵn các biến này)
export HRM_IT_DB_URL="jdbc:sqlserver://localhost:1433;databaseName=HRM;encrypt=true;trustServerCertificate=true"
export HRM_IT_DB_USER=hrm_app HRM_IT_DB_PASSWORD='…'
./mvnw verify                    # thêm 8 test *IT, báo cáo: target/failsafe-reports
```

Không có `HRM_IT_DB_URL`, hai lớp `*IT` bị bỏ qua bằng `Assumptions.assumeTrue` – build vẫn xanh trên máy không có SQL Server.

## 3. Kết quả

### 3.1 Theo lớp test

| Lớp test | Số test | Nội dung chính |
|---|---:|---|
| `PayrollCalculatorTest` | 20 | công thức lương: 7 bậc thuế (9 mốc biên), trần BHXH/BHYT (46,8 tr) và BHTN (106,2 tr), ngày công, ≥ 14 ngày không lương, làm tròn HALF_UP |
| `PayrollServiceTest` | 19 | quy trình kỳ lương OPEN→CALCULATED→APPROVED→PAID, tháng tương lai, duyệt trùng (optimistic), bảo mật phiếu lương, mô phỏng, tham số |
| `LeaveServiceTest` | 29 | ngày công (cuối tuần, ngày lễ), số dư đã trừ đơn chờ, chồng lấn, giới hạn loại phép, quá 30 ngày, qua năm, tự duyệt, sai quản lý, duyệt trùng, hủy |
| `AttendancePolicyTest` | 12 | 08:35 vẫn đúng giờ, 08:36 muộn 6 phút, đúng 4 giờ không phải nửa ngày, ngày công tháng 11/2026 = 21 |
| `AttendanceServiceTest` | 23 | check-in theo giờ server, trùng check-in, đang nghỉ phép, check-out quyết định trạng thái, nhập tay, **API bulk** (phần tử độc lập, > 1000) |
| `EmployeeServiceTest` | 24 | **lương âm**, khung lương, **trùng mã** (409 có `field`), đủ 15 tuổi, vòng lặp quản lý, xóa khi đã có dữ liệu, phạm vi dữ liệu theo vai trò |
| `EmployeeImportServiceTest` | 29 | 3 chế độ import, quản lý ở dòng trên, rollback khi DB từ chối dòng sau, trùng trong file/DB, định dạng tiền thật tế, gợi ý mất số 0 |
| `TabularFileReaderTest` | 8 | BOM, dấu `;`, tiêu đề thân thiện, ô nhiều dòng, dòng trống, XLSX (ngày, số, công thức), giới hạn 10.000 dòng |
| `AuthServiceTest` | 10 | khóa sau 5 lần sai, không kiểm mật khẩu khi đang khóa, cân bằng thời gian khi user không tồn tại, đổi mật khẩu |
| `UserServiceTest` | 7 | mật khẩu tạm đạt chính sách và chỉ lưu hash, admin không tự khóa/tự hạ quyền |
| `RecruitmentServiceTest` | 21 | pipeline không nhảy bước, tiếp nhận = tạo NV thử việc + đóng tin, link CV chỉ http(s), quyền quản lý |
| `PerformanceServiceTest` | 25 | điểm có trọng số 4,10; điểm cuối 0,3×4,35+0,7×4,10 = 4,18 (B); 9 mốc xếp loại; mở kỳ khi trọng số ≠ 100 % |
| `OrganisationServicesTest` | 11 | phòng ban/chức danh, audit không bao giờ làm hỏng nghiệp vụ, kiểm tra chất lượng dữ liệu |
| `ReportingServicesTest` | 7 | báo cáo lọc theo phòng của quản lý, dashboard theo vai trò, ẩn tháng lương đang chạy |
| `ValidatorTest` | 11 | số điện thoại VN, giá trị tùy chọn, **locale Thổ Nhĩ Kỳ** |
| `AccessPolicyTest` | 40 | ma trận phân quyền URL × 5 loại tài khoản (kể cả API key), so khớp theo đoạn đường dẫn |
| `PasswordAndTokenTest` | 5 | BCrypt có salt, hash hỏng không khớp, chính sách mật khẩu, so sánh CSRF |
| `SqlErrorTranslatorTest` | 10 | 2627/2601/547/51xxx/1205/08xxx → 409/409/409/409/500/503 với thông báo thân thiện |
| `SqlBuildersTest` | 6 | WHERE động, escape `% _ [`, ORDER BY chỉ cột trong whitelist, kẹp phân trang |
| `JdbcExecutorTest` | 11 | bind từng kiểu dữ liệu, `SESSION_CONTEXT` trước khi ghi, transaction lồng nhau, rollback, đóng tài nguyên |
| `JdbcEmployeeDaoTest` | 2 | **SQL injection**: từ khóa và tham số sắp xếp độc hại không bao giờ nằm trong câu SQL |
| `DatabaseOutageTest` | 1 | pool Hikari thật tới cổng đóng → **503 sau < 1 s**, không treo luồng Tomcat |
| **Unit – tổng** | **331** | 0 failure · 0 error · 0 skipped |
| `PayrollProcedureParityIT` | 3 | `sp_CalculateMonthlyPayroll` và `PayrollCalculator` cho kết quả **giống hệt** từng phiếu, 10 khoản × ~35 người × tháng 8, 9, 10/2026 |
| `DatabaseRulesIT` | 5 | CHECK lương âm, UNIQUE mã NV, trigger chồng lấn nghỉ phép (51001), trigger lịch sử lương ghi đúng người sửa, kỳ lương đã chi không tính lại (51103) |
| **Integration – tổng** | **8** | chạy trên SQL Server 2022; dữ liệu sau test không đổi – đã đối chiếu số dòng và `CHECKSUM_AGG` của 6 bảng (Payroll, PayrollPeriods, Employees, SalaryHistory, LeaveRequests, LeaveBalances) trước/sau |

### 3.2 Độ phủ (JaCoCo, chỉ tính unit test)

| Package | Dòng được phủ | % dòng | % nhánh |
|---|---:|---:|---:|
| `com.hrm.service` | 1405/1480 | **94,9 %** | **81,2 %** |
| `com.hrm.util` | 32/39 | 82,1 % | 93,5 % |
| `com.hrm.security` | 132/150 | 88,0 % | 79,7 % |
| `com.hrm.exception` | 36/40 | 90,0 % | 50,0 % |
| `com.hrm.dao.jdbc` | 225/950 | 23,7 % | 48,2 % |
| `com.hrm.model` | 369/644 | 57,3 % | 51,7 % |
| `com.hrm.web.*`, `com.hrm.config`, `com.hrm.tools`, `com.hrm.report` | – | 0–19 % | – |
| **Toàn bộ** | 2268/4582 | 49,5 % | 56,9 % |

Ghi chú: các `Jdbc*Dao` còn lại được thực thi bởi integration test trên DB thật (Failsafe không gắn JaCoCo agent nên không cộng vào bảng).
Tầng web (controller, filter, JSP) được kiểm thử thủ công E2E ở Bước 5–6 – xem mục 6.

## 4. Kiểm thử theo yêu cầu đề bài

| Yêu cầu | Bằng chứng |
|---|---|
| Logic tính lương | `PayrollCalculatorTest` (20 ca tính tay) + `PayrollProcedureParityIT` (SP ≡ Java trên dữ liệu thật) |
| Quy trình nghỉ phép | `LeaveServiceTest` (29) + `DatabaseRulesIT.overlappingLeaveIsStoppedByTheTrigger` |
| Validate dữ liệu | `EmployeeServiceTest`, `ValidatorTest`, `EmployeeImportServiceTest`, `AttendanceServiceTest` |
| Lương âm | service: `negativeSalaryIsRejectedWithAClearMessage` · DB: `negativeSalaryIsStoppedByTheCheckConstraint…IT` |
| Trùng mã nhân viên | service: `duplicateCodeFromTheUniqueIndex…` · DAO: `SqlErrorTranslatorTest` · DB: `duplicateEmployeeCodeIsStoppedByTheUniqueKey` |
| Mất kết nối DB | `DatabaseOutageTest`, `JdbcExecutorTest.poolTimeoutBecomesDatabaseUnavailable`, `SqlErrorTranslatorTest.connectionFailuresMean503` |
| Phân quyền | `AccessPolicyTest` (40 ca) + kiểm tra phạm vi dữ liệu trong từng service test |
| Chống SQL injection | `JdbcEmployeeDaoTest`, `SqlBuildersTest` |

## 5. Lỗi thật phát hiện nhờ viết test

Mỗi lỗi được tái hiện bằng một test **đỏ trước**, sửa, rồi test xanh và ở lại làm hồi quy.

| # | Triệu chứng | Nguyên nhân | Sửa | Test giữ lỗi không quay lại |
|---|---|---|---|---|
| 1 | Trên server đặt locale `tr-TR`, `ADMIN@EXAMPLE.COM` được lưu thành `admın@example.com` (chữ ı không chấm), đăng nhập/so trùng email hỏng | `toLowerCase()`/`toUpperCase()` không chỉ định locale dùng locale mặc định của JVM | `Locale.ROOT` trong `Validator.upper/lower`, `RecruitmentService`, `LeaveService` | `ValidatorTest.caseConversionDoesNotDependOnTheServerLocale`, `RecruitmentServiceTest.sourceIsNormalisedIndependentlyOfTheServerLocale` |
| 2 | Import CSV có địa chỉ xuống dòng hoặc dòng trống: lỗi báo "dòng 3" trong khi bản ghi ở dòng 5 – người dùng sửa nhầm dòng | Số dòng lấy từ bộ đếm *bản ghi* của Commons CSV, không phải số dòng của file | Tính dòng từ vị trí ký tự của bản ghi (bỏ qua dòng trống đứng trước) | `TabularFileReaderTest.rowNumbersAreFileLinesDespiteMultiLineValuesAndBlankLines` |
| 3 | Hash mật khẩu bị cụt trong DB (`$2a$10$abc`) làm trang đăng nhập lỗi 500 | jBCrypt 0.4 ném `StringIndexOutOfBoundsException`, code chỉ bắt `IllegalArgumentException` | Bắt cả `IndexOutOfBoundsException` → coi như sai mật khẩu | `PasswordAndTokenTest.malformedOrMissingHashesNeverMatch` |

Các lỗi khác tìm thấy khi kiểm thử Bước 6 (xung đột commons-io, `31/02` bị chấp nhận, đọc sai `1,500,000.50`, enum sai trả 500…)
được ghi trong `docs/06-troubleshooting-log.md`.

Hai bài học về chính bộ test:

- **Field initializer trong lớp `@Nested` chạy trước khi `MockitoExtension` tiêm mock** – service tạo ở đó nhận DAO `null`. Tạo service trong `@BeforeEach`.
- **Integration test không được giả định trạng thái dữ liệu**: lần chạy đầu `PayrollProcedureParityIT` đỏ vì kỳ 10/2026 trên DB dev đã được duyệt (SP từ chối tính lại – đúng nghiệp vụ). Test nay tự mở khóa kỳ *trong* transaction rồi rollback, nên chạy được trên mọi trạng thái.

## 6. Giới hạn và hướng mở rộng

- Controller/filter/JSP chưa có test tự động; đã kiểm thử thủ công E2E (curl cho toàn bộ API, Playwright cho các màn hình chính). Bước tiếp theo hợp lý: test `WebContext`/`ErrorHandler` với `MockHttpServletRequest` và vài kịch bản Playwright chạy trong CI.
- JavaScript phía trình duyệt (`app.js`, các trang) chưa có unit test.
- Kiểm thử hiệu năng nằm ở `docs/03-query-optimization.md` (731.764 dòng chấm công, execution plan trước/sau index).
- Integration test trong CI sẽ dùng SQL Server service container (Bước 9).
