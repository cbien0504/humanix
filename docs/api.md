# REST API & tích hợp dữ liệu

> Dành cho: lập trình viên của hệ thống khác (máy chấm công, ERP/kế toán, cổng tuyển dụng…) cần đọc/ghi
> dữ liệu nhân sự, và người làm chuyển đổi dữ liệu (data migration) từ hệ thống cũ.
> Mọi ví dụ dưới đây đã chạy thật trên môi trường dev (`http://localhost:8088`) – kết quả được rút gọn.

## 1. Tổng quan

| Nhóm | Đường dẫn | Dùng cho |
|---|---|---|
| Nhân viên | `/api/employees` | đồng bộ hồ sơ nhân viên sang hệ thống khác, tạo/cập nhật hồ sơ |
| Chấm công | `/api/attendance` | máy chấm công / hệ thống vào–ra đẩy dữ liệu, tra cứu công |
| Danh mục | `/api/departments`, `/api/positions` | tra mã phòng ban, chức danh, khung lương |
| Import file | `POST /import/employees` (web) · `scripts/import-employees.sh` (CLI) | chuyển đổi dữ liệu nhân viên từ CSV/Excel |

API dùng **cùng tầng service với giao diện web**: mọi quy tắc nghiệp vụ (mã/email/CCCD duy nhất, khung lương,
vòng lặp quản lý, quy tắc chấm công…) được áp dụng giống hệt khi nhập tay. Không có "đường tắt" ghi thẳng DB.

## 2. Xác thực

### 2.1 API key – cho hệ thống tích hợp

```http
GET /api/employees?size=20 HTTP/1.1
X-API-Key: <giá trị biến môi trường HRM_API_KEY>
```

- Key cấu hình qua biến môi trường `HRM_API_KEY` (không nằm trong mã nguồn hay file cấu hình được commit).
  Để trống ⇒ API key bị **tắt**, mọi request mang `X-API-Key` đều nhận 401.
- So sánh hằng thời gian (`MessageDigest.isEqual`) – không dò được key qua thời gian phản hồi.
- Request dùng key chạy dưới danh tính kỹ thuật `api-client` (quyền HR) nhưng **chỉ** được vào
  `/api/employees`, `/api/attendance`, `/api/departments`, `/api/positions`. Các API khác (lương, đánh giá,
  tuyển dụng, quản trị) trả **403** dù key đúng.
- Không cần CSRF token (không dùng cookie). Mọi thao tác ghi được ghi audit log với người dùng `api-client`.
- Đổi key: đặt giá trị mới cho `HRM_API_KEY`, khởi động lại ứng dụng, cập nhật phía hệ thống tích hợp.

### 2.2 Phiên đăng nhập – cho giao diện web

Trình duyệt dùng cookie phiên `JSESSIONID` (HttpOnly, SameSite=Lax) sau khi đăng nhập ở `/login`.
Mọi request ghi (POST/PUT/DELETE) phải gửi kèm header `X-CSRF-Token` lấy từ thẻ
`<meta name="csrf-token">` của trang (jQuery trong `app.js` tự gắn). Phạm vi dữ liệu theo vai trò:
quản lý chỉ thấy nhân viên phòng mình và không thấy lương/CCCD; nhân viên chỉ thấy dữ liệu của chính mình.

## 3. Quy ước chung

| Mục | Quy ước |
|---|---|
| Định dạng | JSON UTF-8 (`Content-Type: application/json`) |
| Ngày | `yyyy-MM-dd` (ISO-8601), ví dụ `2026-11-16` |
| Giờ | `HH:mm` hoặc `HH:mm:ss` khi gửi; ngày-giờ trả về dạng `2026-10-30T08:50:00` theo giờ Việt Nam (Asia/Ho_Chi_Minh) |
| Thời điểm hệ thống | `createdAt`, `updatedAt`: UTC, có hậu tố `Z` |
| Tiền | số thập phân VND, không định dạng (`17000000.00`) |
| Enum | tên hằng, không phân biệt hoa thường ở query (`status=active`) – ví dụ `PROBATION`, `ACTIVE`, `ON_LEAVE`, `TERMINATED` |
| Phân trang | `page` (từ 1), `size` (1–100, mặc định 10/20), `sort`, `dir=asc\|desc`; giá trị ngoài phạm vi được kẹp lại |
| Kết quả trang | `{"items":[…], "page":1, "size":20, "totalItems":44, "totalPages":3}` |
| Trường lạ | bị bỏ qua (thêm trường mới vào API không làm hỏng client cũ) |

## 4. Lỗi

Mọi lỗi – kể cả 401/403/404 do filter hay container sinh ra – có cùng một dạng JSON:

```json
{
  "status": 400,
  "error": "VALIDATION_FAILED",
  "message": "Dữ liệu không hợp lệ, vui lòng kiểm tra lại.",
  "fieldErrors": {
    "fullName": "Vui lòng nhập họ tên.",
    "email": "Email không hợp lệ.",
    "baseSalary": "Lương cơ bản không được âm."
  },
  "requestId": "32381566-c7c1",
  "timestamp": "2026-11-02T09:27:11.630787735Z"
}
```

| HTTP | `error` | Khi nào | Client nên |
|---|---|---|---|
| 400 | `VALIDATION_FAILED` | dữ liệu sai; `fieldErrors` chỉ đúng trường – kể cả lỗi kiểu JSON: `"[0].workDate": "Trường \"[0].workDate\" không đúng định dạng (yyyy-MM-dd)."` | sửa dữ liệu, không gửi lại nguyên trạng |
| 401 | `HTTP_401` | không gửi gì: *"Chưa xác thực: đăng nhập hoặc gửi header X-API-Key."*; sai key: *"API key không hợp lệ."*; cookie phiên đã hết hạn | kiểm tra cấu hình key |
| 403 | `HTTP_403` / `FORBIDDEN` | không có quyền với đường dẫn hoặc bản ghi | không thử lại |
| 404 | `NOT_FOUND` | không có bản ghi | — |
| 405 | `METHOD_NOT_ALLOWED` | sai phương thức HTTP | — |
| 409 | `DATA_INTEGRITY_VIOLATION` | trùng mã/email/CCCD (có `fieldErrors`) | đổi giá trị hoặc dùng PUT |
| 409 | `BUSINESS_RULE_VIOLATION` | vi phạm quy tắc nghiệp vụ (xóa nhân viên đã có dữ liệu, chấm công ngoài thời gian làm việc…) | đọc `message` |
| 500 | `INTERNAL_ERROR` | lỗi không lường trước – chi tiết chỉ nằm trong log | báo IT kèm `requestId` |
| 503 | `DATABASE_UNAVAILABLE` | mất kết nối CSDL (tự phục hồi khi DB trở lại) | thử lại sau, có backoff |

`requestId` cũng có trong header `X-Request-Id` và trên mọi dòng log của request đó – đủ để tra lỗi.

## 5. Nhân viên – `/api/employees`

| Phương thức | Đường dẫn | Mô tả |
|---|---|---|
| GET | `/api/employees?keyword=&departmentId=&positionId=&status=&page=&size=&sort=&dir=` | tìm kiếm có phân trang. `keyword` tìm theo mã, tên (không dấu cũng được), email, SĐT. `sort`: `code`, `name`, `department`, `position`, `hireDate`, `status`, `salary` |
| GET | `/api/employees/{id}` | chi tiết |
| GET | `/api/employees/by-code/{code}` | chi tiết theo mã nhân viên – tiện cho hệ thống ngoài chỉ biết mã |
| GET | `/api/employees/{id}/salary-history` | lịch sử thay đổi lương (do trigger ghi) |
| GET | `/api/employees/options` | danh sách rút gọn (id, mã, tên) cho dropdown |
| GET | `/api/employees/next-code` | mã gợi ý tiếp theo, ví dụ `{"employeeCode":"EMP0121"}` |
| POST | `/api/employees` | tạo mới → **201** + header `Location` |
| PUT | `/api/employees/{id}` | cập nhật toàn bộ hồ sơ (gửi đủ trường) |
| DELETE | `/api/employees/{id}` | **204**; **409** nếu đã có chấm công/nghỉ phép/lương/đánh giá – khi đó chuyển trạng thái `TERMINATED` |

Trường của `POST`/`PUT` (bắt buộc in đậm): **`employeeCode`**, **`fullName`**, **`gender`** (`M`/`F`/`O`),
`dateOfBirth`, **`email`**, `phone`, `address`, `nationalId` (12 số), `taxCode`, `bankAccount`, `bankName`,
**`departmentId`**, **`positionId`**, `managerId`, **`hireDate`**, `terminationDate` (bắt buộc khi `TERMINATED`),
`status` (mặc định `PROBATION`), **`baseSalary`** (trong khung lương của chức danh), `allowance`, `dependents`.

```bash
curl -s -X POST http://localhost:8088/api/employees \
  -H "X-API-Key: $HRM_API_KEY" -H "Content-Type: application/json" \
  -d '{"employeeCode":"EMP0120","fullName":"Hoàng Minh Tú","gender":"M","dateOfBirth":"1996-06-12",
       "email":"tu.hm@example.com","phone":"0915000120","departmentId":4,"positionId":6,"managerId":13,
       "hireDate":"2026-11-16","status":"PROBATION","baseSalary":17000000,"allowance":1000000,"dependents":0}'
```

```http
HTTP/1.1 201
Location: /api/employees/47

{"employeeId":47,"employeeCode":"EMP0120","fullName":"Hoàng Minh Tú","gender":"M","dateOfBirth":"1996-06-12","age":30,
 "email":"tu.hm@example.com","departmentId":4,"departmentCode":"IT","departmentName":"Phòng Công nghệ thông tin",
 "positionId":6,"positionCode":"DEV","positionName":"Lập trình viên","managerId":13,"managerCode":"EMP0013",
 "managerName":"Lý Hoàng Nam","hireDate":"2026-11-16","status":"PROBATION","baseSalary":17000000.00, …}
```

Gửi lại cùng mã → `409 DATA_INTEGRITY_VIOLATION`, `fieldErrors: {"employeeCode":"Mã nhân viên đã tồn tại."}`.
Lương ngoài khung → `400`, `"baseSalary": "Lương cơ bản phải trong khung 12.000.000 – 28.000.000 của chức danh Lập trình viên."`.

## 6. Chấm công – `/api/attendance`

| Phương thức | Đường dẫn | Mô tả |
|---|---|---|
| GET | `/api/attendance?from=&to=&departmentId=&employeeId=&status=&keyword=&page=&size=&sort=&dir=` | tra cứu. `status`: `PRESENT`, `LATE`, `HALF_DAY`, `ABSENT`. `sort`: `date`, `employee`, `code`, `status`, `late`, `hours` |
| POST | `/api/attendance` | ghi/sửa công **một** ngày của một nhân viên |
| POST | `/api/attendance/bulk` | ghi tối đa **1000** bản ghi một lần, kết quả từng phần tử |
| DELETE | `/api/attendance/{id}` | xóa bản ghi công → 204 |
| GET/POST | `/api/attendance/today`, `/check-in`, `/check-out`, `/my` | chỉ dành cho người dùng đăng nhập (chấm công của chính mình) |

Bản ghi gửi lên: `employeeCode` **hoặc** `employeeId`, **`workDate`**, `checkIn`, `checkOut`, `status`
(chỉ cần khi `ABSENT`), `note` (≤ 500 ký tự).

Quy tắc (giống màn hình HR):
- Không ghi cho ngày tương lai, ngày trước ngày vào làm hoặc sau ngày nghỉ việc; `checkOut` phải sau `checkIn`.
- Trạng thái và số phút muộn **do hệ thống tính**: vào sau 08:30 + 5 phút ân hạn ⇒ `LATE`; làm dưới 4 giờ ⇒ `HALF_DAY`
  (cấu hình trong `app.properties`). Client không tự gán `LATE`.
- Mỗi nhân viên một bản ghi/ngày: gửi lại cùng `employeeCode` + `workDate` sẽ **ghi đè** (idempotent) –
  máy chấm công có thể gửi lại an toàn khi mất mạng.
- Bản ghi từ API có `source = "API"` để phân biệt với `WEB` (tự chấm) và `MANUAL` (HR sửa).

### Bulk – ví dụ

```bash
curl -s -X POST http://localhost:8088/api/attendance/bulk \
  -H "X-API-Key: $HRM_API_KEY" -H "Content-Type: application/json" \
  -d '[{"employeeCode":"EMP0016","workDate":"2026-10-30","checkIn":"08:20","checkOut":"17:40","note":"máy chấm công cổng A"},
       {"employeeCode":"EMP0017","workDate":"2026-10-30","checkIn":"08:50","checkOut":"17:35"},
       {"employeeCode":"EMP9999","workDate":"2026-10-30","checkIn":"08:00","checkOut":"17:00"},
       {"employeeCode":"EMP0018","workDate":"2026-10-30","checkIn":"18:00","checkOut":"08:00"}]'
```

```json
{"received":4,"saved":2,"failed":2,"results":[
  {"index":0,"employeeCode":"EMP0016","workDate":"2026-10-30","ok":true,"status":"PRESENT","message":null},
  {"index":1,"employeeCode":"EMP0017","workDate":"2026-10-30","ok":true,"status":"LATE","message":null},
  {"index":2,"employeeCode":"EMP9999","workDate":"2026-10-30","ok":false,"status":null,"message":"Không tìm thấy nhân viên EMP9999."},
  {"index":3,"employeeCode":"EMP0018","workDate":"2026-10-30","ok":false,"status":null,"message":"Giờ check-out phải sau giờ check-in."}]}
```

Các phần tử **độc lập** (không phải một transaction): một bản ghi sai không chặn các bản ghi khác. HTTP 200
nghĩa là *đã xử lý lô*; client phải đọc `failed`/`results[].ok`. Lỗi ở cấp lô (JSON hỏng, rỗng, > 1000 phần tử,
sai kiểu dữ liệu như `"workDate":"30/10/2026"`) trả 400 và **không** ghi gì. Mỗi lô ghi một dòng audit
`ATTENDANCE_IMPORTED`.

## 7. Danh mục – `/api/departments`, `/api/positions`

| Phương thức | Đường dẫn | Mô tả |
|---|---|---|
| GET | `/api/departments` · `/api/departments/{id}` | danh sách kèm `employeeCount`, trưởng phòng |
| POST · PUT · DELETE | `/api/departments[/{id}]` | quản lý phòng ban (DELETE bị chặn khi còn nhân viên) |
| GET | `/api/positions` · `/api/positions/{id}` | chức danh kèm khung lương `minSalary`–`maxSalary` |
| POST · PUT · DELETE | `/api/positions[/{id}]` | quản lý chức danh |

```json
{"positionId":6,"positionCode":"DEV","positionName":"Lập trình viên","jobLevel":4,"minSalary":12000000.00,
 "maxSalary":28000000.00,"description":null,"active":true,"employeeCount":7}
```

## 8. Import nhân viên từ CSV / Excel (data migration)

Có hai cách, **cùng một bộ kiểm tra** (`EmployeeImportService`):

| | Web (`/import`, HR/Admin) | Dòng lệnh |
|---|---|---|
| Dùng khi | HR tự import vài trăm dòng | chuyển đổi dữ liệu hàng loạt, chạy trong script |
| Lệnh | chọn file + chế độ → *Thực hiện* | `scripts/import-employees.sh <file> [mode] [report.csv]` |
| Kết quả | bảng lỗi theo dòng/cột, tải báo cáo lỗi CSV | in bảng lỗi, ghi `report.csv`; exit code 0 = sạch, 1 = có dòng lỗi, 2 = lỗi tham số/hệ thống |

**File**: `.csv` (UTF-8, có/không BOM, phân cách `,` hoặc `;`) hoặc `.xlsx` (sheet đầu tiên), tối đa 10.000 dòng
(upload qua web: tối đa 5 MB). Dòng 1 là tiêu đề – không phân biệt hoa thường, khoảng trắng (`Employee Code` = `employee_code`).
Tải file mẫu tại `/import/template.csv`; ví dụ trong repo: `data/import/employees-sample.csv` (hợp lệ) và
`data/import/employees-with-errors.csv` (cố ý chứa mỗi dòng một loại lỗi).

| Cột | Bắt buộc | Ghi chú |
|---|---|---|
| `employee_code` | ✔ | duy nhất trong file và hệ thống |
| `full_name` | ✔ | |
| `gender` | ✔ | `M`/`F`/`O` hoặc `Nam`/`Nữ`/`Khác` |
| `date_of_birth` | | `yyyy-MM-dd` hoặc `dd/MM/yyyy` (kiểm tra chặt: `31/02/1990` bị từ chối); đủ 15 tuổi khi vào làm |
| `email` | ✔ | duy nhất |
| `phone`, `national_id` | | trong Excel nên định dạng cột là **Text** để giữ số 0 đầu (hệ thống nhắc khi phát hiện) |
| `tax_code`, `address`, `bank_name`, `bank_account` | | |
| `department_code`, `position_code` | ✔ | mã đang hoạt động (xem `/api/departments`, `/api/positions`) |
| `manager_code` | | nhân viên đã có **hoặc** nằm ở dòng phía trên trong cùng file |
| `hire_date` | ✔ | |
| `status` | | `PROBATION` (mặc định) / `ACTIVE` / `ON_LEAVE` hoặc nhãn tiếng Việt; không import `TERMINATED` |
| `base_salary` | ✔ | trong khung lương chức danh; chấp nhận `12.000.000`, `12,000,000`, `12000000 ₫` |
| `allowance`, `dependents` | | |

**Chế độ** (`mode`):

| Chế độ | Ý nghĩa |
|---|---|
| `validate` (mặc định) | chỉ kiểm tra, không ghi – luôn chạy bước này trước |
| `valid-only` | ghi các dòng hợp lệ (mỗi dòng một transaction), báo cáo dòng lỗi |
| `all-or-nothing` | chỉ ghi khi **mọi** dòng hợp lệ, trong **một** transaction – lỗi ở dòng nào cũng rollback toàn bộ |

```bash
export HRM_DB_URL="jdbc:sqlserver://localhost:1433;databaseName=HRM;encrypt=true;trustServerCertificate=true"
export HRM_DB_USER=hrm_app HRM_DB_PASSWORD='…'
./mvnw -q -DskipTests package
scripts/import-employees.sh data/import/employees-with-errors.csv validate target/import-errors.csv
```

```text
File: employees-with-errors.csv | chế độ: VALIDATE_ONLY
Dòng dữ liệu: 12 | hợp lệ: 1 | dòng lỗi: 11 | đã import: 0
  dòng 3    employee_code    EMP0201                  Mã nhân viên bị trùng trong file.
  dòng 4    employee_code    EMP0001                  Mã nhân viên đã tồn tại trong hệ thống.
  dòng 5    email            abc@                     Email không hợp lệ.
  dòng 5    base_salary      -5000000                 Lương cơ bản không được âm.
  dòng 7    base_salary      90000000                 Lương cơ bản phải trong khung 12.000.000 – 28.000.000 …
  dòng 8    date_of_birth    31/02/1990               Ngày không hợp lệ (yyyy-MM-dd hoặc dd/MM/yyyy).
  …
Báo cáo lỗi: target/import-errors.csv
```

Số dòng là **số dòng trong file** (dòng tiêu đề là dòng 1) để người dùng mở Excel sửa đúng chỗ. Báo cáo lỗi CSV
có BOM (mở đúng tiếng Việt trong Excel) và vô hiệu hóa giá trị bắt đầu bằng `= + - @` (chống CSV injection).

## 9. Lưu ý vận hành & bảo mật

- Production: chỉ mở API qua HTTPS (đặt `HRM_COOKIE_SECURE=true` sau reverse proxy TLS); không đưa key vào URL.
- Mỗi hệ thống tích hợp nên có key riêng – hiện tại hệ thống hỗ trợ **một** key; nhiều key/phân quyền theo client
  và giới hạn tần suất (rate limiting) nằm trong kế hoạch cải tiến (`docs/08-improvement-plan.md`).
- Request/response không chứa mật khẩu; `Cache-Control: no-store` trên mọi response động.
