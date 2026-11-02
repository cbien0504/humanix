# 03 – Stored procedure, view và tối ưu truy vấn

> Tất cả số liệu dưới đây là **kết quả đo thực tế** bằng [`db/perf/run-benchmark.sh`](../db/perf/run-benchmark.sh);
> output gốc lưu tại [`db/perf/results/benchmark-output.txt`](../db/perf/results/benchmark-output.txt).

## 1. Đối tượng lập trình CSDL

### 1.1 Stored procedure ([`db/procedures.sql`](../db/procedures.sql))

| Procedure | Chức năng | Kỹ thuật chính |
|-----------|-----------|----------------|
| `sp_CalculateMonthlyPayroll @PeriodYear, @PeriodMonth, @CalculatedBy` | Tính lương cả công ty cho 1 tháng | 1 câu `INSERT … SELECT` set-based qua chuỗi CTE; `TRY/CATCH` + transaction; `UPDLOCK, HOLDLOCK` chống chạy song song cùng kỳ; `THROW 5110x` cho lỗi nghiệp vụ |
| `sp_GetAttendanceReport @FromDate, @ToDate, @DepartmentId = NULL` | Báo cáo chấm công: ngày công kỳ vọng, đi làm, muộn, nửa ngày, nghỉ phép, vắng, tỷ lệ chuyên cần | Lọc khoảng ngày sargable; biến bảng ngày làm việc; `OPTION (RECOMPILE)` cho tham số tùy chọn |
| `sp_GetDepartmentStatistics @AsOfDate = NULL` | Headcount, cơ cấu giới tính, lương bình quân, quỹ lương, thâm niên, tuyển mới/nghỉ việc YTD, tỷ lệ nghỉ việc | Tổng hợp có điều kiện (`SUM(CASE…)`) trong 1 lượt đọc |
| `sp_InitLeaveBalances @Year` | Khởi tạo/cập nhật số dư phép năm (BR-23) | Idempotent: `UPDATE` + `INSERT … WHERE NOT EXISTS` trong 1 transaction (tránh các lỗi đã biết của `MERGE`) |
| `sp_RunDataValidation` | 16 kiểm tra chất lượng dữ liệu, trả PASS/FAIL/WARN + mẫu vi phạm | CTE đệ quy phát hiện vòng lặp quản lý, `STRING_AGG` |

### 1.2 View ([`db/views.sql`](../db/views.sql))

| View | Mục đích |
|------|----------|
| `vw_EmployeeDetails` | Hồ sơ NV đã giải mã phòng ban, chức danh, quản lý, tài khoản; tính tuổi, thâm niên, tên trạng thái tiếng Việt |
| `vw_MonthlyAttendanceSummary` | Tổng hợp chấm công + ngày nghỉ phép (tách theo tháng) cho từng NV/tháng |
| `vw_PayrollDetails` | Phiếu lương kèm thông tin NV, phòng ban, kỳ lương – dùng cho màn hình và xuất Excel |

### 1.3 Function & script kiểm tra dữ liệu

- `fn_WorkingDays` – inline TVF (được optimizer "mở" vào câu truy vấn gọi nó, không có chi phí gọi hàm từng dòng như scalar UDF).
- [`db/data-validation.sql`](../db/data-validation.sql) – chạy `sp_RunDataValidation`, liệt kê chi tiết 3 quy tắc cốt lõi
  (trùng mã NV, lương âm, nghỉ phép chồng lấn) và **tự kiểm thử**: trong transaction bị rollback, tạm tắt ràng buộc/trigger,
  chèn dữ liệu xấu → các kiểm tra phải báo `FAIL`.

Kết quả chạy trên dữ liệu mẫu:

```
CheckCode CheckName                                      Severity ViolationCount Result
DV01      Mã nhân viên trùng lặp                          ERROR    0              PASS
DV04      Lương cơ bản / phụ cấp âm                       ERROR    0              PASS
DV06      Đơn nghỉ phép chồng lấn ngày                    ERROR    0              PASS
... (16/16 PASS)
-- Part 3 (self-test, rollback):
DV01      Mã nhân viên trùng lặp                          ERROR    1              FAIL   EMP0001
DV04      Lương cơ bản / phụ cấp âm                       ERROR    1              FAIL   EMP0010
DV06      Đơn nghỉ phép chồng lấn ngày                    ERROR    1              FAIL   EMP0016:#18/#21
-- Sau rollback: CK_Employees_Salary is_not_trusted = 0, index & trigger enabled
```

### 1.4 Kiểm chứng công thức lương trên dữ liệu mẫu (tháng 10/2026, 22 ngày công chuẩn)

| NV | Lương CB | NPT | Ngày công | Gross | Bảo hiểm | TN tính thuế | Thuế TNCN | Net |
|----|---------:|:---:|:---------:|------:|---------:|-------------:|----------:|----:|
| EMP0001 (CEO) | 120.000.000 | 2 | 22 | 130.000.000 | 5.508.000 ¹ | 96.592.000 | 23.957.200 | 100.534.800 |
| EMP0016 | 22.000.000 | 0 | 22 | 23.500.000 | 2.310.000 | 5.690.000 | 319.000 | 20.871.000 |
| EMP0017 | 18.000.000 | 0 | 17 ² | 14.681.818 | 1.890.000 | 0 | 0 | 12.791.818 |
| EMP0029 | 16.000.000 | 2 | 0 ³ | 0 | 0 | 0 | 0 | 0 |

¹ BHXH 8% và BHYT 1,5% trên trần 46.800.000 (20 × 2.340.000); BHTN 1% trên trần 106.200.000 (20 × 5.310.000).
² 5 ngày nghỉ không lương → lương theo tỷ lệ 17/22 nhưng vẫn đóng BH vì số ngày không lương < 14.
³ Nghỉ thai sản (BHXH chi trả) → không hưởng lương công ty, không đóng BH.
Thuế EMP0001: 250.000 + 500.000 + 1.200.000 + 2.800.000 + 5.000.000 + 8.400.000 + 35% × 16.592.000 = **23.957.200** ✓

## 2. Phương pháp đo

| Thông số | Giá trị |
|----------|---------|
| Máy chủ | SQL Server 2022 Developer CU27 (container Linux, giới hạn 2 GB RAM) |
| Database | `HRM_PERF` riêng biệt, cùng schema/function/view/procedure với `HRM` |
| Dữ liệu | 3.000 NV · **731.764** dòng `Attendance` (1 năm) · 29.990 đơn nghỉ (300 đang chờ) |
| Chỉ số | `SET STATISTICS IO` (logical reads – ổn định, không phụ thuộc cache), `STATISTICS TIME`, plan từ `SET SHOWPLAN_TEXT` |
| Điều kiện | Buffer pool đã "làm ấm"; case 1–2 dùng `OPTION (MAXDOP 1)` để so sánh công bằng |

Kích thước các index liên quan (trang 8 KB):

| Index | Số dòng | Trang |
|-------|--------:|------:|
| `PK_Attendance` (clustered) | 731.764 | 6.551 |
| `UQ_Attendance_EmployeeDate` (EmployeeId, WorkDate) | 731.764 | 1.911 |
| `IX_Attendance_WorkDate` (covering) | 731.764 | 3.538 |
| `IX_LeaveRequests_Employee_Dates` | 29.990 | 155 |
| `IX_LeaveRequests_Pending` (filtered) | **300** | **4** |

## 3. Các tình huống tối ưu

### Case 1 – Tổng hợp chấm công tháng cho toàn công ty (lõi của tính lương & báo cáo)

```sql
SELECT a.EmployeeId, COUNT(*), SUM(a.WorkHours), SUM(CAST(a.LateMinutes AS INT))
FROM dbo.Attendance AS a
WHERE a.WorkDate >= '2026-10-01' AND a.WorkDate <= '2026-10-31'
GROUP BY a.EmployeeId;
```

**Trước** – chưa có index trên `WorkDate`:

```
Hash Match(Aggregate, HASH:([a].[EmployeeId]))
  |--Clustered Index Scan(Attendance.PK_Attendance, WHERE:([WorkDate]>='2026-10-01' AND [WorkDate]<='2026-10-31'))
```

**Sau** – `CREATE INDEX IX_Attendance_WorkDate ON Attendance (WorkDate) INCLUDE (EmployeeId, Status, LateMinutes, WorkHours)`:

```
Hash Match(Aggregate, HASH:([a].[EmployeeId]))
  |--Index Seek(Attendance.IX_Attendance_WorkDate, SEEK:([WorkDate] >= '2026-10-01' AND [WorkDate] <= '2026-10-31') ORDERED FORWARD)
```

| Biến thể | Toán tử truy cập | Logical reads | CPU (ms) | Elapsed (ms) |
|----------|------------------|--------------:|---------:|-------------:|
| 1A. Không index | Clustered Index Scan | 6.551 | 47 | 46 |
| 1B. Index `(WorkDate)` không INCLUDE – optimizer tự chọn | Clustered Index Scan (bỏ qua index) | 6.551 | 47 | 46 |
| 1B'. Ép dùng index đó bằng hint | Index Seek + **Key Lookup** ×64.562 | **197.870** | 79 | 79 |
| **1C. Covering index** | **Index Seek** | **314** | **18** | **17** |

**Nhận xét**

- Covering index giảm **95%** logical reads (6.551 → 314) và ~2,7 lần thời gian.
- Index **không phủ** là vô dụng ở đây: truy vấn lấy ~9% bảng (64.562/731.764 dòng), vượt "tipping point" nên optimizer quay về scan.
  Ép dùng index còn tệ gấp 30 lần do mỗi dòng phải key lookup về clustered index. Bài học: với truy vấn khoảng (range) trả về nhiều dòng,
  phải `INCLUDE` đủ cột mà truy vấn cần.
- Quan sát phụ trong lần đo đầu: khi câu bao ngoài chỉ dùng `COUNT`, optimizer **tự loại bỏ cột không dùng** và quét index hẹp
  `UQ_Attendance_EmployeeDate` (1.911 trang) – nên khi benchmark phải dùng đúng các cột của truy vấn thật, nếu không sẽ kết luận sai.

### Case 2 – SARGability: hàm trên cột làm mất khả năng seek

| Điều kiện | Toán tử | Logical reads | CPU (ms) |
|-----------|---------|--------------:|---------:|
| `WHERE YEAR(WorkDate) = 2026 AND MONTH(WorkDate) = 10` | Index **Scan** toàn bộ, tính `datepart()` cho 731.764 dòng | 1.911 | 89 |
| `WHERE WorkDate >= '2026-10-01' AND WorkDate < '2026-11-01'` | Index **Seek** | 314 | 3 |

**Nhận xét:** cùng kết quả (64.562 dòng) nhưng nhanh hơn **~30 lần CPU**. Quy ước trong dự án: không bao giờ bọc cột trong hàm ở
mệnh đề `WHERE`/`JOIN`; DAO Java luôn truyền khoảng `[đầu tháng, đầu tháng sau)`. Trigger đồng bộ số dư phép cũng viết
`StartDate >= DATEFROMPARTS(@y,1,1) AND StartDate < DATEFROMPARTS(@y+1,1,1)` thay vì `YEAR(StartDate) = @y`.

### Case 3 – Kiểm tra chồng lấn đơn nghỉ (chạy ở mỗi lần nộp đơn và trong trigger)

```sql
SELECT TOP (1) LeaveRequestId FROM dbo.LeaveRequests
WHERE EmployeeId = @e AND Status IN ('PENDING','APPROVED') AND StartDate <= @end AND EndDate >= @start;
```

| | Toán tử | Logical reads |
|-|---------|--------------:|
| Không index | Clustered Index Scan (29.990 dòng) | 325 |
| `IX_LeaveRequests_Employee_Dates (EmployeeId, StartDate, EndDate) INCLUDE (Status, …)` | Index Seek `EmployeeId = @e AND StartDate <= @end` | **2** |

**Nhận xét:** giảm 160 lần. Quan trọng hơn: chi phí cũ **tăng tuyến tính theo tổng số đơn của cả công ty**, chi phí mới chỉ phụ thuộc số đơn
của 1 nhân viên → hệ thống không chậm dần theo thời gian. Thứ tự cột: cột so sánh bằng (`EmployeeId`) trước, cột khoảng sau.

### Case 4 – Hàng đợi duyệt nghỉ phép: filtered index

| Biến thể | Logical reads | Ghi chú |
|----------|--------------:|---------|
| 4A. Không filtered index | 155 | Scan `IX_LeaveRequests_Employee_Dates` để lọc `Status` |
| 4B. Filtered index `WHERE Status = 'PENDING'` **không** chứa cột `Status` | 155 | Optimizer **bỏ qua** index (ép hint thì chỉ 2 trang) |
| **4C. Filtered index `INCLUDE (…, Status)`** | **4** | Được chọn tự nhiên, kể cả trong truy vấn JOIN như ứng dụng và không cần `RECOMPILE` |
| 4D. Giá trị lọc truyền qua biến `@status` | 155 | Plan phải đúng với mọi giá trị → không thể dùng filtered index |

**Nhận xét – bài học thực tế:**

1. Filtered index chỉ chứa 300/29.990 dòng (4 trang thay vì 155) – rất rẻ để duy trì, vì đơn rời khỏi trạng thái `PENDING` sẽ tự ra khỏi index.
2. Phải đưa **cột của biểu thức lọc** vào index (`INCLUDE (Status)`), nếu không optimizer có thể không khớp index (đã đo ở 4B).
   Đã cập nhật `schema.sql` theo kết quả này.
3. Giá trị lọc phải là **literal** trong câu SQL: DAO viết `WHERE lr.Status = 'PENDING'` (hằng số do lập trình viên viết, không phải
   dữ liệu người dùng → không có rủi ro SQL injection), không bind thành tham số `?`.

### Case 5 – Tìm kiếm nhân viên theo tên

| Điều kiện | Toán tử | Logical reads |
|-----------|---------|--------------:|
| `FullName LIKE N'%hung%'` (ký tự đại diện ở đầu) | Clustered Index Scan | 17 |
| `FullName LIKE N'nguyen van%'` (tiền tố) | Index Seek trên `IX_Employees_FullName` | 3 |

**Nhận xét:** nhờ collation `Latin1_General_100_CI_AI`, seek theo tiền tố vẫn khớp "Nguyễn Văn…" khi gõ không dấu.
Ô tìm kiếm chung của ứng dụng cần tìm "chứa" (`%x%`) nên chấp nhận scan – với vài nghìn NV chỉ 17 trang (~2 ms).
Khi dữ liệu lớn hơn nhiều: dùng Full-Text Search hoặc tìm theo tiền tố trên từng từ (ghi trong kế hoạch cải tiến).

### Case 6 – View tiện lợi vs. stored procedure cho báo cáo tháng

| Cách truy vấn | Logical reads (tổng) | CPU (ms) | Elapsed (ms) |
|---------------|---------------------:|---------:|-------------:|
| `SELECT … FROM vw_MonthlyAttendanceSummary WHERE WorkYear = 2026 AND WorkMonth = 10` | ~47.900 (3.704 Attendance + 43.604 worktable) | **~600** (plan song song) | ~75 |
| `EXEC sp_GetAttendanceReport '2026-10-01', '2026-10-31'` | ~7.200 (400 Attendance) | **~73** | ~74 |

**Nhận xét:** thời gian trả về tương đương nhưng view tốn **gấp 8 lần CPU**: điều kiện lọc nằm trên cột *dẫn xuất* (tháng tính từ `WorkDate`)
nên SQL Server phải tổng hợp **tất cả các tháng** của mọi NV rồi mới lọc, và dùng nhiều luồng để bù. Khi nhiều người cùng xem báo cáo,
CPU mới là tài nguyên khan hiếm. Vì vậy: view dùng cho truy vấn theo **một nhân viên** (lọc `EmployeeId` được đẩy xuống seek) và cho ad-hoc;
màn hình báo cáo toàn công ty gọi stored procedure với khoảng ngày sargable.

**Tối ưu thêm sau khi đọc `STATISTICS IO`:** lần đo đầu, CTE tính "ngày công kỳ vọng" của SP join biến bảng ngày làm việc cho **từng** NV
(`Scan count 3001, logical reads 6002`). Viết lại bằng `CASE`: NV làm việc trọn kỳ (đa số) dùng luôn tổng số ngày làm việc, chỉ NV vào/nghỉ
giữa kỳ mới đếm → còn `Scan count 1, logical reads 2`.

### Case 7 – Tính lương set-based

`EXEC sp_CalculateMonthlyPayroll 2026, 10` cho **3.000 NV: 273 ms** (đọc chấm công, nghỉ phép, tính BH, thuế lũy tiến, ghi 3.000 dòng
và cập nhật tổng kỳ trong 1 transaction). Cách làm "từng nhân viên" (cursor, hoặc vòng lặp Java gọi N câu SQL) cần ≥ 3.000 × vài round-trip;
set-based để SQL Server tối ưu toàn cục và chỉ có 1 round-trip từ ứng dụng.

## 4. Tổng hợp nguyên tắc áp dụng trong dự án

| Nguyên tắc | Áp dụng |
|------------|---------|
| Index cho mọi cột FK và mọi mẫu truy vấn chính | Mục 5 của [02-database-design.md](02-database-design.md) |
| Covering index (`INCLUDE`) cho truy vấn khoảng | `IX_Attendance_WorkDate`, `IX_LeaveRequests_Employee_Dates`, `IX_Payroll_EmployeeId` |
| Filtered index cho tập con nhỏ, hay truy vấn | `IX_LeaveRequests_Pending`, `IX_Employees_ManagerId`, unique `UX_Employees_NationalId` |
| Điều kiện sargable | Khoảng ngày, không dùng hàm trên cột |
| Set-based thay cho cursor/vòng lặp | Tính lương, khởi tạo phép năm, sinh phiếu đánh giá |
| Inline TVF thay cho scalar UDF | `fn_WorkingDays` |
| `OPTION (RECOMPILE)` cho truy vấn có tham số tùy chọn (catch-all) chạy không thường xuyên | `sp_GetAttendanceReport` (`@DepartmentId` có thể NULL) |
| Phân trang phía server `ORDER BY … OFFSET … FETCH` + đếm tổng riêng | DAO danh sách nhân viên, chấm công |
| `READ_COMMITTED_SNAPSHOT ON` | Báo cáo không bị chặn bởi giao dịch ghi đang chạy |
| Connection pool (HikariCP), `PreparedStatement` tái sử dụng plan | Tầng DAO (Bước 4) |

## 5. Bảo trì & giám sát đề xuất

```sql
-- Index phân mảnh > 30% → REBUILD, 5–30% → REORGANIZE
SELECT OBJECT_NAME(ips.object_id) AS TableName, i.name AS IndexName, ips.avg_fragmentation_in_percent, ips.page_count
FROM sys.dm_db_index_physical_stats(DB_ID(), NULL, NULL, NULL, 'LIMITED') AS ips
JOIN sys.indexes AS i ON i.object_id = ips.object_id AND i.index_id = ips.index_id
WHERE ips.page_count > 100 AND ips.avg_fragmentation_in_percent > 5;

-- Truy vấn tốn CPU nhất kể từ lần khởi động
SELECT TOP (10) qs.total_worker_time / qs.execution_count AS AvgCpuUs, qs.execution_count,
       SUBSTRING(t.text, qs.statement_start_offset / 2 + 1, 200) AS StatementText
FROM sys.dm_exec_query_stats AS qs CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) AS t
ORDER BY AvgCpuUs DESC;
```

Bật **Query Store** (`ALTER DATABASE HRM SET QUERY_STORE = ON`) để theo dõi plan thay đổi theo thời gian; ứng dụng ghi log cảnh báo
mọi truy vấn > 500 ms (Bước 4).

## 6. Cách tái lập

```bash
docker cp db hrm-db:/db
docker exec -e DB_ADMIN_PASSWORD='<sa password>' hrm-db bash /db/perf/run-benchmark.sh
docker exec hrm-db cat /db/perf/results/benchmark-output.txt
```
