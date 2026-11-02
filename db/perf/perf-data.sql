/* =============================================================================
   HRM System – large synthetic dataset for query-optimisation experiments.
   Run in a DEDICATED database (HRM_PERF) after schema/functions/views/procedures
   – never in the real HRM database. See db/perf/run-benchmark.sh.
     3,000 employees · 20 departments · 1 year of attendance (~735,000 rows)
     30,000 leave requests (≈1% pending)
   ============================================================================= */
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

INSERT INTO dbo.Holidays (HolidayDate, HolidayName) VALUES
 ('2026-01-01', N'Tết Dương lịch'), ('2026-02-16', N'Tết'), ('2026-02-17', N'Tết'), ('2026-02-18', N'Tết'),
 ('2026-02-19', N'Tết'), ('2026-02-20', N'Tết'), ('2026-04-27', N'Giỗ Tổ'), ('2026-04-30', N'30/4'),
 ('2026-05-01', N'1/5'), ('2026-09-01', N'Quốc khánh'), ('2026-09-02', N'Quốc khánh');

INSERT INTO dbo.LeaveTypes (LeaveTypeCode, LeaveTypeName, IsPaid, RequiresBalance) VALUES
 ('ANNUAL', N'Nghỉ phép năm', 1, 1), ('UNPAID', N'Nghỉ không lương', 0, 0);

INSERT INTO dbo.PayrollSettings (SettingKey, SettingValue, Description) VALUES
 ('SOCIAL_INSURANCE_RATE', 0.08, N'BHXH'), ('HEALTH_INSURANCE_RATE', 0.015, N'BHYT'),
 ('UNEMPLOYMENT_INSURANCE_RATE', 0.01, N'BHTN'), ('BASE_SALARY_REFERENCE', 2340000, N'Lương cơ sở'),
 ('REGIONAL_MINIMUM_WAGE', 5310000, N'Lương tối thiểu vùng'), ('INSURANCE_CAP_MULTIPLIER', 20, N'Hệ số trần'),
 ('PERSONAL_DEDUCTION', 15500000, N'Giảm trừ bản thân'), ('DEPENDENT_DEDUCTION', 6200000, N'Giảm trừ NPT'),
 ('UNPAID_DAYS_NO_INSURANCE', 14, N'Ngưỡng không đóng BH');
INSERT INTO dbo.TaxBrackets (BracketNo, LowerBound, UpperBound, Rate) VALUES
 (1, 0, 5000000, 0.05), (2, 5000000, 10000000, 0.10), (3, 10000000, 18000000, 0.15), (4, 18000000, 32000000, 0.20),
 (5, 32000000, 52000000, 0.25), (6, 52000000, 80000000, 0.30), (7, 80000000, NULL, 0.35);

INSERT INTO dbo.Positions (PositionCode, PositionName, JobLevel, MinSalary, MaxSalary) VALUES
 ('STAFF', N'Nhân viên', 3, 5000000, 60000000);

WITH n AS (SELECT TOP (20) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS i FROM sys.all_objects)
INSERT INTO dbo.Departments (DepartmentCode, DepartmentName)
SELECT CONCAT('D', FORMAT(i, '00')), CONCAT(N'Phòng ', i) FROM n;
GO

-- 3,000 employees with realistic Vietnamese names built from name parts
DECLARE @Last  TABLE (i INT IDENTITY(0,1), v NVARCHAR(20));
DECLARE @Mid   TABLE (i INT IDENTITY(0,1), v NVARCHAR(20));
DECLARE @First TABLE (i INT IDENTITY(0,1), v NVARCHAR(20));
INSERT INTO @Last  (v) VALUES (N'Nguyễn'),(N'Trần'),(N'Lê'),(N'Phạm'),(N'Hoàng'),(N'Vũ'),(N'Đặng'),(N'Bùi'),(N'Đỗ'),(N'Hồ'),(N'Ngô'),(N'Dương');
INSERT INTO @Mid   (v) VALUES (N'Văn'),(N'Thị'),(N'Minh'),(N'Hoàng'),(N'Ngọc'),(N'Thanh'),(N'Quốc'),(N'Thu');
INSERT INTO @First (v) VALUES (N'Hùng'),(N'Lan'),(N'Tuấn'),(N'Mai'),(N'Long'),(N'Hà'),(N'Nam'),(N'Linh'),(N'Sơn'),(N'Trang'),
                              (N'Phúc'),(N'Thảo'),(N'Khoa'),(N'Yến'),(N'Bảo'),(N'Chi'),(N'Đức'),(N'Hương'),(N'Việt'),(N'Ánh');

WITH n AS (SELECT TOP (3000) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS i
           FROM sys.all_objects a CROSS JOIN sys.all_objects b)
INSERT INTO dbo.Employees (EmployeeCode, FullName, Gender, DateOfBirth, Email, DepartmentId, PositionId, HireDate,
                           Status, BaseSalary, Allowance, Dependents)
SELECT CONCAT('P', FORMAT(n.i, '00000')),
       CONCAT(l.v, N' ', m.v, N' ', f.v),
       CASE WHEN n.i % 2 = 0 THEN 'F' ELSE 'M' END,
       DATEADD(DAY, -(12000 + n.i * 3 % 7000), '2026-01-01'),
       CONCAT('p', n.i, '@perf.example.com'),
       1 + n.i % 20, 1,
       DATEADD(DAY, -(n.i * 7 % 3000), '2025-10-31'),
       'ACTIVE',
       8000000 + (n.i * 7919 % 40) * 1000000,
       (n.i % 4) * 500000,
       n.i % 3
FROM n
JOIN @Last  AS l ON l.i = n.i % 12
JOIN @Mid   AS m ON m.i = (n.i / 12) % 8
JOIN @First AS f ON f.i = (n.i / 96) % 20;
GO

-- One year of attendance (2025-11-01 → 2026-10-31), ~98% presence
INSERT INTO dbo.Attendance (EmployeeId, WorkDate, CheckIn, CheckOut, Status, LateMinutes, Source)
SELECT e.EmployeeId, d.WorkDate,
       DATEADD(MINUTE, x.InMin, CAST(d.WorkDate AS DATETIME2(0))),
       DATEADD(MINUTE, 1050 + x.R % 60, CAST(d.WorkDate AS DATETIME2(0))),
       CASE WHEN x.InMin > 515 THEN 'LATE' ELSE 'PRESENT' END,
       CASE WHEN x.InMin > 515 THEN x.InMin - 510 ELSE 0 END,
       'WEB'
FROM dbo.Employees AS e
JOIN dbo.fn_WorkingDays('2025-11-01', '2026-10-31') AS d ON d.WorkDate >= e.HireDate
CROSS APPLY (SELECT ABS(CHECKSUM(e.EmployeeId, d.WorkDate) % 100) AS R) AS r0
CROSS APPLY (SELECT r0.R AS R, CASE WHEN r0.R < 10 THEN 520 + r0.R * 3 ELSE 470 + r0.R % 40 END AS InMin) AS x
WHERE r0.R < 98;
GO

-- 10 two-day leave requests per employee, 5 weeks apart (never overlapping)
WITH k AS (SELECT TOP (10) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) - 1 AS k FROM sys.all_objects)
INSERT INTO dbo.LeaveRequests (EmployeeId, LeaveTypeId, StartDate, EndDate, TotalDays, Reason, Status, ApproverId, DecidedAt)
SELECT e.EmployeeId, 1 + (e.EmployeeId + k.k) % 2, s.StartDate, DATEADD(DAY, 1, s.StartDate), 2, N'Perf test',
       st.Status,
       CASE WHEN st.Status IN ('APPROVED', 'REJECTED') THEN 1 END,
       CASE WHEN st.Status IN ('APPROVED', 'REJECTED') THEN SYSUTCDATETIME() END
FROM dbo.Employees AS e
CROSS JOIN k
CROSS APPLY (SELECT DATEADD(DAY, (e.EmployeeId % 3), DATEADD(WEEK, k.k * 5, '2025-11-03')) AS StartDate) AS s
CROSS APPLY (SELECT CASE WHEN (e.EmployeeId * 31 + k.k) % 100 = 0 THEN 'PENDING'
                         WHEN (e.EmployeeId + k.k) % 9 = 0 THEN 'REJECTED' ELSE 'APPROVED' END AS Status) AS st
WHERE e.EmployeeId > 1;   -- employee 1 is the approver
GO

UPDATE STATISTICS dbo.Attendance WITH FULLSCAN;
UPDATE STATISTICS dbo.LeaveRequests WITH FULLSCAN;
UPDATE STATISTICS dbo.Employees WITH FULLSCAN;
SELECT (SELECT COUNT(*) FROM dbo.Employees) AS Employees,
       (SELECT COUNT(*) FROM dbo.Attendance) AS Attendance,
       (SELECT COUNT(*) FROM dbo.LeaveRequests) AS LeaveRequests,
       (SELECT COUNT(*) FROM dbo.LeaveRequests WHERE Status = 'PENDING') AS PendingRequests;
GO
