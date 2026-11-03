# 06 – Nhật ký xử lý sự cố & logging

> Dành cho: người vận hành và lập trình viên bảo trì hệ thống.
> Mọi số liệu, log và thông báo lỗi dưới đây là **kết quả chạy thật** trên môi trường dev
> (Tomcat 10.1 + SQL Server 2022 trong Docker), ngày 02–03/11/2026.

## 1. Logging & giám sát – tìm nguyên nhân ở đâu

| Nguồn | Vị trí | Nội dung |
|---|---|---|
| `hrm.log` | `$CATALINA_BASE/logs/` (đổi bằng `HRM_LOG_DIR`), xoay vòng mỗi ngày hoặc 20 MB, giữ 30 ngày | log ứng dụng; logger `com.hrm.web.access` ghi mỗi request: phương thức, đường dẫn, mã trả về, thời gian |
| `audit.log` + bảng `AuditLogs` | cùng thư mục; màn hình **Quản trị → Nhật ký hệ thống** | ai làm gì, lúc nào, từ IP nào (đăng nhập, sửa lương, duyệt phép, import…) |
| `GET /health` | Docker HEALTHCHECK, load balancer | `200 UP` / `503 DOWN` kèm độ trễ DB; chỉ ghi log khi trạng thái **đổi** |
| `sp_RunDataValidation` | màn hình **Quản trị → Kiểm tra dữ liệu** | 16 kiểm tra toàn vẹn (trùng mã, lương âm, phép chồng lấn, bảng lương sai công thức…) |

Mỗi dòng log có dạng `thời gian MỨC [request-id] [user] logger - nội dung`. **Request id** cũng nằm trong header
`X-Request-Id`, trong JSON lỗi (`"requestId"`) và trên trang lỗi ("Mã tra cứu") – người dùng chỉ cần đọc mã đó cho IT:

```text
$ grep d1ef7bcf-3f58 logs/hrm.log
2026-11-03 09:19:57.948 INFO  [d1ef7bcf-3f58] [api-client] com.hrm.web.ErrorHandler - POST /api/employees -> 400 VALIDATION_FAILED: Lương cơ bản không được âm.
2026-11-03 09:19:57.956 INFO  [d1ef7bcf-3f58] [api-client] com.hrm.web.access - POST /api/employees -> 400 (61 ms)
```

| Mức | Khi nào | Mẫu dòng log |
|---|---|---|
| ERROR | lỗi hệ thống (5xx), DB không kết nối được | `GET /api/employees -> 503 DATABASE_UNAVAILABLE: SQLServerException: The TCP/IP connection…` |
| WARN | 401/403, câu SQL chậm hơn `db.slowQueryMs` (mặc định 500 ms, biến `HRM_DB_SLOW_QUERY_MS`), rò rỉ connection (Hikari leak detection) | `Slow SQL (<ms> ms): <câu SQL>` |
| INFO | lỗi nghiệp vụ 4xx, access log, audit | `POST /api/employees -> 409 DATA_INTEGRITY_VIOLATION: Mã nhân viên đã tồn tại.` |
| DEBUG | bật bằng `HRM_LOG_LEVEL=DEBUG` | `SQL (<ms> ms): <câu SQL>`, `SQL rejected [<mã lỗi>] <câu SQL>: <thông báo SQL Server>` |

Người dùng **không bao giờ** thấy câu SQL, stack trace hay tên bảng: `SqlErrorTranslator` đổi lỗi SQL Server thành thông báo
tiếng Việt (2627/2601 → 409 có tên trường, 547 → 409, 51xxx → thông báo của trigger/SP, 08xxx → 503).

## 2. Ba kịch bản lỗi có chủ đích

### 2.1 Nhập lương âm

Lương âm bị chặn ở **bốn lớp** độc lập – lớp sau vẫn đứng vững khi lớp trước bị vượt qua:

| Lớp | Cách thử | Kết quả |
|---|---|---|
| Trình duyệt | nhập `-5000000` trên form nhân viên | ô `min="0"` báo lỗi ngay, không gửi request |
| Service (API) | `POST /api/employees` với `"baseSalary": -5000000` | `400 VALIDATION_FAILED`, `fieldErrors.baseSalary = "Lương cơ bản không được âm."` (log ở trên) |
| Import file | dòng có `base_salary = -5000000` | báo lỗi đúng dòng/cột, dòng không được ghi (kể cả chế độ "Import dòng hợp lệ") |
| CSDL | `INSERT` thẳng bằng login `hrm_app`, bỏ qua ứng dụng | `Msg 547 … CHECK constraint "CK_Employees_Salary"` – xem bên dưới |

```text
$ sqlcmd -U hrm_app -d HRM -I -Q "INSERT INTO dbo.Employees (…, BaseSalary, …) VALUES (…, -5000000, …)"
Msg 547, Level 16, State 1
The INSERT statement conflicted with the CHECK constraint "CK_Employees_Salary". The conflict occurred in database "HRM", table "dbo.Employees".
```

Phát hiện phụ khi thử lớp CSDL: chạy `sqlcmd` **không có `-I`** thì INSERT bị chặn sớm hơn bởi `Msg 1934 … 'QUOTED_IDENTIFIER'`
(xem sự cố #4) – script thủ công phải luôn dùng `sqlcmd -I`.
Kiểm tra định kỳ: `EXEC dbo.sp_RunDataValidation` → `DV04 Lương cơ bản / phụ cấp âm … 0 PASS`.

Test giữ hành vi: `EmployeeServiceTest.negativeSalaryIsRejectedWithAClearMessage`,
`DatabaseRulesIT.negativeSalaryIsStoppedByTheCheckConstraintEvenWhenTheServiceIsBypassed`.

### 2.2 Trùng mã nhân viên

```text
POST /api/employees {"employeeCode":"EMP0001", …}
→ 409 {"error":"DATA_INTEGRITY_VIOLATION","message":"Mã nhân viên đã tồn tại.","fieldErrors":{"employeeCode":"Mã nhân viên đã tồn tại."},"requestId":"ba6a3d0f-87d3"}
```

Điểm quan trọng là **tranh chấp đồng thời**: kiểm tra "SELECT xem mã có chưa rồi mới INSERT" ở tầng ứng dụng không an toàn khi hai
người cùng bấm Lưu. Thử 6 request song song cùng tạo `EMP0191`:

```text
      5    409 DATA_INTEGRITY_VIOLATION {'employeeCode': 'Mã nhân viên đã tồn tại.'}
      1    201 EMP0191
SELECT COUNT(*) FROM dbo.Employees WHERE EmployeeCode = 'EMP0191'  →  1
```

Vì vậy `EmployeeService` **không** tự SELECT kiểm tra trùng; ràng buộc `UQ_Employees_EmployeeCode` là nguồn sự thật duy nhất,
`SqlErrorTranslator` ánh xạ tên ràng buộc → trường `employeeCode` để form tô đỏ đúng ô. (Màn hình import thì có kiểm tra trùng
trước, nhưng chỉ để báo lỗi sớm theo dòng – lần ghi cuối vẫn dựa vào ràng buộc.)

Test: `SqlErrorTranslatorTest`, `EmployeeServiceTest.duplicateCodeFromTheUniqueIndexReachesTheCallerWithItsField`,
`DatabaseRulesIT.duplicateEmployeeCodeIsStoppedByTheUniqueKey`.

### 2.3 Mất kết nối cơ sở dữ liệu

Diễn biến khi dừng container SQL Server giữa lúc ứng dụng đang chạy (`docker stop hrm-dev-db`):

| Thời điểm | Quan sát |
|---|---|
| t = 0 | DB dừng; Hikari phát hiện connection chết (`Failed to validate connection … The connection is closed`) |
| t + 5,0 s | `/health` → `503 {"status":"DOWN","database":{"error":"Hệ thống tạm thời không kết nối được…","latencyMs":5004}}` |
| | API → `503 DATABASE_UNAVAILABLE` (JSON có request id); trang web → trang lỗi tiếng Việt; đăng nhập mới → 503 |
| | 20 request đồng thời: cả 20 trả 503 sau **5,0 s tổng** – chờ song song tối đa `connectionTimeout`, không treo luồng Tomcat |
| DB khởi động lại | `/health` về `200` sau **6,7 s** (thời gian SQL Server khởi động), **không cần restart Tomcat**, phiên đăng nhập cũ vẫn dùng được |

Các quyết định thiết kế làm nên hành vi trên: `initializationFailTimeout = -1` (ứng dụng vẫn khởi động khi DB chưa sẵn sàng),
`connectionTimeout = 5 s` thay vì 30 s mặc định của Hikari, lỗi kết nối (SQLState `08xxx`, `SQLTransientConnectionException`)
được dịch thành `DatabaseUnavailableException` → 503 thay vì 500 có stack trace.

Cải tiến rút ra từ chính lần diễn tập này – xem sự cố #24.

## 3. Nhật ký sự cố trong quá trình phát triển

| # | Bước | Triệu chứng | Nguyên nhân gốc | Khắc phục · phòng ngừa |
|---|---|---|---|---|
| 1 | 2 | Seed lỗi: CHECK `CK_Users_Username` từ chối tài khoản `hr` | ràng buộc độ dài tối thiểu 3 quá chặt so với dữ liệu thật | nới thành `LEN ≥ 2`, khớp regex phía Java (`Validator.USERNAME`) |
| 2 | 2 | Seed lỗi: phiếu đánh giá `COMPLETED` vi phạm `CK_PerformanceReviews_Done` | chèn trạng thái cuối trước khi có điểm | seed theo đúng luồng: tạo `PENDING_SELF` → điểm → cập nhật trạng thái |
| 3 | 2 | `Msg 1046` khi in tổng kết trong `schema.sql` | `PRINT` không nhận subquery | gán vào biến rồi `PRINT` |
| **4** | 2 | `Msg 1934 … SET options have incorrect settings: 'QUOTED_IDENTIFIER'` khi tạo index lọc và khi ghi bảng có index lọc bằng `sqlcmd` | `sqlcmd` mặc định `QUOTED_IDENTIFIER OFF`; index lọc/cột computed được index bắt buộc `ON` lúc tạo **và** lúc ghi | `init-db.sh` luôn chạy `sqlcmd -I`; JDBC mặc định `ON`. Tái hiện lại ở mục 2.1 khi INSERT thủ công |
| 5 | 3 | `Msg 8124` khi tính thuế lũy tiến trong SP | `SUM` trên biểu thức tham chiếu cột ngoài (outer reference) | tính phần thuế từng bậc trong bảng dẫn xuất rồi mới `SUM` |
| 6 | 3 | Index lọc `IX_LeaveRequests_Pending` không được dùng (scan) | thiếu cột lọc trong `INCLUDE`; predicate dạng tham số | `INCLUDE (Status)` + giữ literal `'PENDING'` – đo trước/sau ở `03-query-optimization.md` (ca 4) |
| 7 | 3 | Sau khi restart SQL Server, mã identity nhảy lên 1002 | `IDENTITY_CACHE` của SQL Server (bỏ cache 1000 giá trị khi tắt đột ngột) – không phải lỗi | chấp nhận khoảng trống (không dùng identity làm số thứ tự nghiệp vụ); mã NV dùng `nextCode` riêng |
| 8 | 5 | API danh sách trả 500 khi không có `?sort=` | `Map.of(...).get(null)` ném `NullPointerException` | `SqlOrder` kiểm tra null, `PageRequest` chuẩn hóa sort trống · `SqlBuildersTest.missingSortFallsBack…` |
| 9 | 5 | Trang JSP lỗi biên dịch | đặt tên biến vòng lặp `lt` – từ khóa của EL | đổi tên biến |
| 10 | 5 | AJAX nhận trang HTML 401/404 mặc định của Tomcat | lỗi do container/filter sinh ra không qua `ErrorHandler` | `<error-page>` mặc định → `ErrorPageServlet` trả JSON cho API/AJAX, HTML cho trình duyệt |
| 11 | 5 | Sau khi deploy, trình duyệt vẫn dùng CSS/JS cũ | cache tĩnh | URL tài nguyên có `?v=${assetVersion}` đổi theo mỗi lần khởi động |
| 12 | 5 | File `.xlsx` tải về bị gắn `;charset=UTF-8` | filter mã hóa ép charset cho mọi response, kể cả nhị phân | filter chỉ đặt encoding cho request; response văn bản tự khai báo |
| 13 | 5 | `Msg 130` trong truy vấn tóm tắt nhóm của dashboard | `SUM` trên biểu thức chứa `EXISTS` | `LEFT JOIN` + `COUNT(a.AttendanceId)` |
| 14 | 5 | Biểu đồ quỹ lương tụt mạnh ở tháng hiện tại | tháng đang chạy đã được tính thử (mới vài ngày công) | dashboard chỉ hiện tháng đã qua hoặc đã duyệt/chi · `ReportingServicesTest.hrGetsCompanyFigures…` |
| 15 | 6 | Upload CSV: `ClassNotFoundException UnsynchronizedBufferedReader` | Maven "gần nhất/khai báo trước thắng": POI kéo commons-io 2.16.1, commons-csv 1.12 cần 2.17 | ghim commons-io 2.17.0 + `maven-enforcer` `requireUpperBoundDeps` (đã kiểm: bỏ ghim là build fail, chỉ rõ 2 đường phụ thuộc) |
| 16 | 6 | Import chấp nhận ngày sinh `31/02/1990` (thành 28/02) | `DateTimeFormatter` mặc định `ResolverStyle.SMART` | `d/M/uuuu` + `STRICT` |
| 17 | 6 | `1,500,000.50` đọc thành 150.000.050 | xóa mọi dấu `.` `,` trước khi parse | bộ đọc tiền: 3 chữ số sau dấu = phân nhóm, còn lại = thập phân, mơ hồ thì báo lỗi · 14 ca trong `EmployeeImportServiceTest` |
| 18 | 6 | `?status=FOO` trả 500 ở 8 API | `Enum.valueOf` ném `IllegalArgumentException` | `WebContext.enumParam` → 400 liệt kê giá trị hợp lệ |
| 19 | 6 | Gợi ý lỗi màu đỏ hiện cả dưới ô hợp lệ sau khi bấm Lưu | jQuery `.show()` ghi `display:block` inline, đè CSS `:invalid ~ .invalid-feedback` của Bootstrap | xóa style inline (`.css('display','')`) |
| 20 | 6 | Hệ thống tích hợp gọi API không kèm key nhận "Phiên đăng nhập đã hết hạn" | một thông báo dùng chung cho mọi 401 | phân biệt: không có phiên → "Chưa xác thực: đăng nhập hoặc gửi header X-API-Key." |
| 21 | 7 | Trên JVM locale `tr-TR`, email `ADMIN@…` lưu thành `admın@…` | `toLowerCase()` không chỉ định locale | `Locale.ROOT` · `ValidatorTest` (đỏ trước khi sửa) |
| 22 | 7 | Lỗi import báo "dòng 3" cho bản ghi ở dòng 5 | số dòng lấy từ bộ đếm bản ghi CSV (ô nhiều dòng, dòng trống làm lệch) | tính dòng từ vị trí ký tự của bản ghi · `TabularFileReaderTest` |
| 23 | 7 | Hash mật khẩu bị cụt làm đăng nhập lỗi 500 | jBCrypt ném `StringIndexOutOfBoundsException` | coi như sai mật khẩu · `PasswordAndTokenTest` |
| 24 | 8 | Khi DB sập, log ngập: 26 request → **49 dòng ERROR** + cảnh báo "Slow SQL (5000 ms)" | mỗi request bị ghi ERROR ở `JdbcExecutor` **và** `ErrorHandler`; thời gian chờ pool bị tính là "SQL chậm"; mỗi lần thăm dò `/health` ghi một ERROR | `JdbcExecutor` không ghi ERROR/"Slow SQL" khi không lấy được kết nối; `/health` chỉ ghi khi đổi trạng thái. Diễn tập lại: **21 dòng ERROR** = 20 request lỗi (mỗi request 1 dòng, có request id) + 1 dòng "Database unreachable"; khi hồi phục: 1 dòng INFO "Database reachable again" · `JdbcExecutorTest.Logging` |
| 25 | vận hành | Tài khoản `emp0010` không đăng nhập được sau buổi thử khóa tài khoản | cơ chế khóa 15 phút sau 5 lần sai hoạt động đúng | chờ 15 phút hoặc Quản trị → Tài khoản → **Mở khóa** (ghi audit `USER_UNLOCKED`) |
| 26 | test | Integration test lương đỏ ở lần chạy đầu: "Kỳ lương đã được duyệt… không thể tính lại" | test giả định kỳ 10/2026 còn ở trạng thái "Đã tính" | test tự mở khóa kỳ **trong** transaction rồi rollback; không giả định trạng thái dữ liệu |

### 3.1 Phân tích chi tiết: #15 – xung đột phiên bản thư viện

1. **Hiện tượng**: upload file CSV trả 500; log: `Caused by: java.lang.ClassNotFoundException: org.apache.commons.io.input.UnsynchronizedBufferedReader`.
2. **Khoanh vùng**: lớp thuộc commons-io; `WEB-INF/lib` chứa `commons-io-2.16.1.jar` – liệt kê jar cho thấy 2.16.1 không có lớp này.
3. **Nguyên nhân**: commons-csv 1.12.0 khai báo commons-io **2.17.0**, poi-ooxml 5.3.0 khai báo **2.16.1**; cùng độ sâu nên Maven chọn
   bản của dependency khai báo trước (POI). Xuất CSV không lỗi vì `CSVPrinter` không dùng lớp đó – lỗi "ngủ" từ Bước 5.
4. **Khắc phục**: khai báo trực tiếp commons-io 2.17.0.
5. **Phòng ngừa**: `maven-enforcer-plugin` với `requireUpperBoundDeps`. Kiểm chứng bằng cách bỏ dòng ghim: build fail với thông báo chỉ rõ
   `poi-ooxml:5.3.0 → commons-io:2.16.1` và `commons-csv:1.12.0 → commons-io:2.17.0`.

### 3.2 Phân tích chi tiết: #24 – log khi mất cơ sở dữ liệu

Khi xảy ra sự cố, log là thứ người trực đọc đầu tiên; 49 dòng ERROR cho 26 request khiến khó thấy đâu là nguyên nhân và dễ gây
"lờn cảnh báo". Nguyên tắc sau khi sửa: **mỗi request lỗi đúng một dòng ERROR có request id** (ở `ErrorHandler`, kèm nguyên nhân gốc
`SQLServerException: The TCP/IP connection to the host … has failed`), **mỗi lần đổi trạng thái DB đúng một dòng** (ở `/health`).
Chi tiết kỹ thuật vẫn xem được khi cần với `HRM_LOG_LEVEL=DEBUG` (`No database connection for SELECT …`).

## 4. Xử lý nhanh (runbook)

| Tình huống | Làm gì |
|---|---|
| Người dùng báo lỗi kèm "Mã tra cứu" | `grep <mã> logs/hrm.log` – thấy toàn bộ request: người dùng, đường dẫn, mã lỗi, nguyên nhân |
| `/health` trả 503 | kiểm tra SQL Server (container/dịch vụ), `HRM_DB_URL`, mật khẩu `hrm_app`; khi DB về, ứng dụng **tự** kết nối lại – không cần restart |
| Request chậm | tìm `Slow SQL` trong log (ngưỡng `db.slowQueryMs`), đối chiếu execution plan theo `03-query-optimization.md` |
| Tài khoản bị khóa | chờ 15 phút hoặc Quản trị → Tài khoản → Mở khóa |
| Không tính lại được lương | kỳ đã duyệt/đã chi là bất biến theo thiết kế (BR-30); sửa sai bằng kỳ điều chỉnh, không mở khóa tùy tiện |
| Import báo nhiều lỗi | chạy chế độ **Chỉ kiểm tra**, tải báo cáo lỗi CSV; với Excel, định dạng cột SĐT/CCCD là Text để giữ số 0 đầu |
| Chạy script SQL thủ công | luôn `sqlcmd -I` (sự cố #4), dùng login có quyền phù hợp, chạy `sp_RunDataValidation` sau khi sửa dữ liệu |
| Giao diện cũ sau khi deploy | tải lại trang (`Ctrl+F5`); URL tài nguyên đã có tham số phiên bản nên thường không cần |
| Hệ thống tích hợp nhận 401/403 | 401: thiếu/sai `X-API-Key`; 403: key đúng nhưng gọi API ngoài phạm vi tích hợp (`docs/api.md` mục 2) |
