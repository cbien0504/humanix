/* =============================================================================
   HRM System – query optimisation benchmark (run against HRM_PERF only).
   For each case: estimated plan (SHOWPLAN_TEXT) + logical reads / CPU / elapsed
   (STATISTICS IO, TIME). Indexes are dropped/re-created to compare before vs
   after; the final section restores the indexes defined in schema.sql.
   Results are summarised in docs/03-query-optimization.md.
   ============================================================================= */
SET NOCOUNT ON;
GO
-- warm the buffer pool once so timings compare logical work, not disk I/O
SELECT COUNT_BIG(*) FROM dbo.Attendance WITH (INDEX(1));
SELECT COUNT_BIG(*) FROM dbo.LeaveRequests WITH (INDEX(1));
GO

/* ---------------------------------------------------------------------------
   CASE 1 – monthly attendance aggregate for ALL employees (core of payroll and
   attendance report). 1A: no index on WorkDate · 1B: plain index · 1C: covering
   --------------------------------------------------------------------------- */
DROP INDEX IF EXISTS IX_Attendance_WorkDate ON dbo.Attendance;
GO
PRINT '### CASE 1A - no index on WorkDate';
GO
SET SHOWPLAN_TEXT ON;
GO
SELECT COUNT(*) AS Employees, SUM(x.Days) AS Days, SUM(x.Hours) AS Hours, SUM(x.LateMin) AS LateMin FROM (
    SELECT a.EmployeeId, COUNT(*) AS Days, SUM(a.WorkHours) AS Hours, SUM(CAST(a.LateMinutes AS INT)) AS LateMin
    FROM dbo.Attendance AS a
    WHERE a.WorkDate >= '2026-10-01' AND a.WorkDate <= '2026-10-31'
    GROUP BY a.EmployeeId) AS x OPTION (MAXDOP 1);
GO
SET SHOWPLAN_TEXT OFF;
GO
SET STATISTICS IO, TIME ON;
SELECT COUNT(*) AS Employees, SUM(x.Days) AS Days, SUM(x.Hours) AS Hours, SUM(x.LateMin) AS LateMin FROM (
    SELECT a.EmployeeId, COUNT(*) AS Days, SUM(a.WorkHours) AS Hours, SUM(CAST(a.LateMinutes AS INT)) AS LateMin
    FROM dbo.Attendance AS a
    WHERE a.WorkDate >= '2026-10-01' AND a.WorkDate <= '2026-10-31'
    GROUP BY a.EmployeeId) AS x OPTION (MAXDOP 1);
SET STATISTICS IO, TIME OFF;
GO

CREATE INDEX IX_Attendance_WorkDate ON dbo.Attendance (WorkDate);
GO
PRINT '### CASE 1B - non-covering index on WorkDate';
GO
SET SHOWPLAN_TEXT ON;
GO
SELECT COUNT(*) AS Employees, SUM(x.Days) AS Days, SUM(x.Hours) AS Hours, SUM(x.LateMin) AS LateMin FROM (
    SELECT a.EmployeeId, COUNT(*) AS Days, SUM(a.WorkHours) AS Hours, SUM(CAST(a.LateMinutes AS INT)) AS LateMin
    FROM dbo.Attendance AS a
    WHERE a.WorkDate >= '2026-10-01' AND a.WorkDate <= '2026-10-31'
    GROUP BY a.EmployeeId) AS x OPTION (MAXDOP 1);
GO
SET SHOWPLAN_TEXT OFF;
GO
SET STATISTICS IO, TIME ON;
SELECT COUNT(*) AS Employees, SUM(x.Days) AS Days, SUM(x.Hours) AS Hours, SUM(x.LateMin) AS LateMin FROM (
    SELECT a.EmployeeId, COUNT(*) AS Days, SUM(a.WorkHours) AS Hours, SUM(CAST(a.LateMinutes AS INT)) AS LateMin
    FROM dbo.Attendance AS a
    WHERE a.WorkDate >= '2026-10-01' AND a.WorkDate <= '2026-10-31'
    GROUP BY a.EmployeeId) AS x OPTION (MAXDOP 1);
-- force the index to show the cost of key lookups the optimiser avoided
SELECT COUNT(*) AS Employees, SUM(x.Days) AS Days, SUM(x.Hours) AS Hours, SUM(x.LateMin) AS LateMin FROM (
    SELECT a.EmployeeId, COUNT(*) AS Days, SUM(a.WorkHours) AS Hours, SUM(CAST(a.LateMinutes AS INT)) AS LateMin
    FROM dbo.Attendance AS a WITH (INDEX(IX_Attendance_WorkDate))
    WHERE a.WorkDate >= '2026-10-01' AND a.WorkDate <= '2026-10-31'
    GROUP BY a.EmployeeId) AS x OPTION (MAXDOP 1);
SET STATISTICS IO, TIME OFF;
GO

DROP INDEX IX_Attendance_WorkDate ON dbo.Attendance;
CREATE INDEX IX_Attendance_WorkDate ON dbo.Attendance (WorkDate) INCLUDE (EmployeeId, Status, LateMinutes, WorkHours);
GO
PRINT '### CASE 1C - covering index (WorkDate) INCLUDE (EmployeeId, Status, LateMinutes, WorkHours)';
GO
SET SHOWPLAN_TEXT ON;
GO
SELECT COUNT(*) AS Employees, SUM(x.Days) AS Days, SUM(x.Hours) AS Hours, SUM(x.LateMin) AS LateMin FROM (
    SELECT a.EmployeeId, COUNT(*) AS Days, SUM(a.WorkHours) AS Hours, SUM(CAST(a.LateMinutes AS INT)) AS LateMin
    FROM dbo.Attendance AS a
    WHERE a.WorkDate >= '2026-10-01' AND a.WorkDate <= '2026-10-31'
    GROUP BY a.EmployeeId) AS x OPTION (MAXDOP 1);
GO
SET SHOWPLAN_TEXT OFF;
GO
SET STATISTICS IO, TIME ON;
SELECT COUNT(*) AS Employees, SUM(x.Days) AS Days, SUM(x.Hours) AS Hours, SUM(x.LateMin) AS LateMin FROM (
    SELECT a.EmployeeId, COUNT(*) AS Days, SUM(a.WorkHours) AS Hours, SUM(CAST(a.LateMinutes AS INT)) AS LateMin
    FROM dbo.Attendance AS a
    WHERE a.WorkDate >= '2026-10-01' AND a.WorkDate <= '2026-10-31'
    GROUP BY a.EmployeeId) AS x OPTION (MAXDOP 1);
SET STATISTICS IO, TIME OFF;
GO

/* ---------------------------------------------------------------------------
   CASE 2 – SARGability: YEAR()/MONTH() on the column vs. a date range
   --------------------------------------------------------------------------- */
PRINT '### CASE 2A - non-sargable YEAR(WorkDate) = 2026 AND MONTH(WorkDate) = 10';
GO
SET SHOWPLAN_TEXT ON;
GO
SELECT COUNT(*) FROM dbo.Attendance AS a
WHERE YEAR(a.WorkDate) = 2026 AND MONTH(a.WorkDate) = 10 OPTION (MAXDOP 1);
GO
SET SHOWPLAN_TEXT OFF;
GO
SET STATISTICS IO, TIME ON;
SELECT COUNT(*) FROM dbo.Attendance AS a
WHERE YEAR(a.WorkDate) = 2026 AND MONTH(a.WorkDate) = 10 OPTION (MAXDOP 1);
SET STATISTICS IO, TIME OFF;
GO
PRINT '### CASE 2B - sargable WorkDate >= 2026-10-01 AND WorkDate < 2026-11-01';
GO
SET SHOWPLAN_TEXT ON;
GO
SELECT COUNT(*) FROM dbo.Attendance AS a
WHERE a.WorkDate >= '2026-10-01' AND a.WorkDate < '2026-11-01' OPTION (MAXDOP 1);
GO
SET SHOWPLAN_TEXT OFF;
GO
SET STATISTICS IO, TIME ON;
SELECT COUNT(*) FROM dbo.Attendance AS a
WHERE a.WorkDate >= '2026-10-01' AND a.WorkDate < '2026-11-01' OPTION (MAXDOP 1);
SET STATISTICS IO, TIME OFF;
GO

/* ---------------------------------------------------------------------------
   CASE 3 – leave overlap check (runs on every leave submission)
   --------------------------------------------------------------------------- */
DROP INDEX IF EXISTS IX_LeaveRequests_Employee_Dates ON dbo.LeaveRequests;
GO
PRINT '### CASE 3A - overlap check without index';
GO
SET SHOWPLAN_TEXT ON;
GO
SELECT TOP (1) lr.LeaveRequestId FROM dbo.LeaveRequests AS lr
WHERE lr.EmployeeId = 1500 AND lr.Status IN ('PENDING', 'APPROVED')
  AND lr.StartDate <= '2026-03-10' AND lr.EndDate >= '2026-03-09';
GO
SET SHOWPLAN_TEXT OFF;
GO
SET STATISTICS IO, TIME ON;
SELECT TOP (1) lr.LeaveRequestId FROM dbo.LeaveRequests AS lr
WHERE lr.EmployeeId = 1500 AND lr.Status IN ('PENDING', 'APPROVED')
  AND lr.StartDate <= '2026-03-10' AND lr.EndDate >= '2026-03-09';
SET STATISTICS IO, TIME OFF;
GO
CREATE INDEX IX_LeaveRequests_Employee_Dates ON dbo.LeaveRequests (EmployeeId, StartDate, EndDate)
    INCLUDE (Status, LeaveTypeId, TotalDays);
GO
PRINT '### CASE 3B - overlap check with IX_LeaveRequests_Employee_Dates';
GO
SET SHOWPLAN_TEXT ON;
GO
SELECT TOP (1) lr.LeaveRequestId FROM dbo.LeaveRequests AS lr
WHERE lr.EmployeeId = 1500 AND lr.Status IN ('PENDING', 'APPROVED')
  AND lr.StartDate <= '2026-03-10' AND lr.EndDate >= '2026-03-09';
GO
SET SHOWPLAN_TEXT OFF;
GO
SET STATISTICS IO, TIME ON;
SELECT TOP (1) lr.LeaveRequestId FROM dbo.LeaveRequests AS lr
WHERE lr.EmployeeId = 1500 AND lr.Status IN ('PENDING', 'APPROVED')
  AND lr.StartDate <= '2026-03-10' AND lr.EndDate >= '2026-03-09';
SET STATISTICS IO, TIME OFF;
GO

/* ---------------------------------------------------------------------------
   CASE 4 – approval queue: filtered index on PENDING rows only (300 of 30k rows)
   4A no filtered index
   4B filtered index WITHOUT the filter column  → optimiser ignores it
   4C filtered index INCLUDE (Status)            → chosen, also without RECOMPILE
   4D value passed in a variable                 → cannot use a filtered index
   --------------------------------------------------------------------------- */
DROP INDEX IF EXISTS IX_LeaveRequests_Pending ON dbo.LeaveRequests;
GO
PRINT '### CASE 4A - pending queue without filtered index';
GO
SET STATISTICS IO, TIME ON;
SELECT COUNT(*) AS Pending, MIN(lr.StartDate) AS FirstStart FROM dbo.LeaveRequests AS lr WHERE lr.Status = 'PENDING';
SET STATISTICS IO, TIME OFF;
GO
CREATE INDEX IX_LeaveRequests_Pending ON dbo.LeaveRequests (StartDate) INCLUDE (EmployeeId, LeaveTypeId)
    WHERE Status = 'PENDING';
GO
PRINT '### CASE 4B - filtered index without the filter column (Status)';
GO
SET STATISTICS IO, TIME ON;
SELECT COUNT(*) AS Pending, MIN(lr.StartDate) AS FirstStart FROM dbo.LeaveRequests AS lr WHERE lr.Status = 'PENDING' OPTION (RECOMPILE);
SELECT COUNT(*) AS Pending, MIN(lr.StartDate) AS FirstStart FROM dbo.LeaveRequests AS lr WITH (INDEX(IX_LeaveRequests_Pending))
WHERE lr.Status = 'PENDING' OPTION (RECOMPILE);
SET STATISTICS IO, TIME OFF;
GO
DROP INDEX IX_LeaveRequests_Pending ON dbo.LeaveRequests;
CREATE INDEX IX_LeaveRequests_Pending ON dbo.LeaveRequests (StartDate) INCLUDE (EmployeeId, LeaveTypeId, Status)
    WHERE Status = 'PENDING';
GO
PRINT '### CASE 4C - filtered index INCLUDE (Status), literal predicate, joined like the application query';
GO
SET SHOWPLAN_TEXT ON;
GO
SELECT lr.LeaveRequestId, e.FullName, lr.StartDate FROM dbo.LeaveRequests AS lr
JOIN dbo.Employees AS e ON e.EmployeeId = lr.EmployeeId
WHERE lr.Status = 'PENDING' ORDER BY lr.StartDate;
GO
SET SHOWPLAN_TEXT OFF;
GO
SET STATISTICS IO, TIME ON;
SELECT COUNT(*) AS Pending, MIN(lr.StartDate) AS FirstStart FROM dbo.LeaveRequests AS lr WHERE lr.Status = 'PENDING';
SELECT COUNT(*) AS Pending FROM dbo.LeaveRequests AS lr JOIN dbo.Employees AS e ON e.EmployeeId = lr.EmployeeId
WHERE lr.Status = 'PENDING';
SET STATISTICS IO, TIME OFF;
GO
PRINT '### CASE 4D - value in a variable (plan must be valid for any value)';
GO
SET STATISTICS IO, TIME ON;
DECLARE @status VARCHAR(20) = 'PENDING';
SELECT COUNT(*) AS Pending, MIN(lr.StartDate) AS FirstStart FROM dbo.LeaveRequests AS lr WHERE lr.Status = @status;
SET STATISTICS IO, TIME OFF;
GO
SELECT i.name AS IndexName, SUM(ps.used_page_count) AS Pages, SUM(ps.row_count) AS IndexRows
FROM sys.dm_db_partition_stats AS ps JOIN sys.indexes AS i ON i.object_id = ps.object_id AND i.index_id = ps.index_id
WHERE ps.object_id IN (OBJECT_ID('dbo.LeaveRequests'), OBJECT_ID('dbo.Attendance')) GROUP BY i.name ORDER BY i.name;
GO

/* ---------------------------------------------------------------------------
   CASE 5 – employee name search: leading wildcard vs prefix
   --------------------------------------------------------------------------- */
PRINT '### CASE 5A - LIKE N''%hung%'' (leading wildcard)';
GO
SET SHOWPLAN_TEXT ON;
GO
SELECT EmployeeId, EmployeeCode, FullName FROM dbo.Employees WHERE FullName LIKE N'%hung%';
GO
SET SHOWPLAN_TEXT OFF;
GO
SET STATISTICS IO, TIME ON;
SELECT COUNT(*) FROM dbo.Employees WHERE FullName LIKE N'%hung%';
SET STATISTICS IO, TIME OFF;
GO
PRINT '### CASE 5B - LIKE N''nguyen van%'' (prefix, accent-insensitive collation)';
GO
SET SHOWPLAN_TEXT ON;
GO
SELECT EmployeeId, EmployeeCode, FullName FROM dbo.Employees WHERE FullName LIKE N'nguyen van%';
GO
SET SHOWPLAN_TEXT OFF;
GO
SET STATISTICS IO, TIME ON;
SELECT COUNT(*) FROM dbo.Employees WHERE FullName LIKE N'nguyen van%';
SET STATISTICS IO, TIME OFF;
GO

/* ---------------------------------------------------------------------------
   CASE 6 – view filtered on derived month vs procedure with a date range
   --------------------------------------------------------------------------- */
PRINT '### CASE 6A - vw_MonthlyAttendanceSummary WHERE WorkYear = 2026 AND WorkMonth = 10';
GO
SET STATISTICS IO, TIME ON;
SELECT COUNT(*), SUM(WorkedDays) FROM dbo.vw_MonthlyAttendanceSummary WHERE WorkYear = 2026 AND WorkMonth = 10;
SET STATISTICS IO, TIME OFF;
GO
PRINT '### CASE 6B - sp_GetAttendanceReport 2026-10-01..2026-10-31';
GO
DECLARE @t0 DATETIME2 = SYSDATETIME();
DECLARE @r TABLE (EmployeeId INT, EmployeeCode VARCHAR(20), FullName NVARCHAR(150), DepartmentId INT, DepartmentName NVARCHAR(150),
                  PositionName NVARCHAR(150), ExpectedDays INT, PresentDays INT, LateDays INT, HalfDays INT, PaidLeaveDays INT,
                  UnpaidLeaveDays INT, AbsentDays INT, WorkedDays DECIMAL(5,1), LateMinutes INT, WorkHours DECIMAL(8,2),
                  AttendanceRate DECIMAL(5,1));
SET STATISTICS IO ON;
INSERT INTO @r EXEC dbo.sp_GetAttendanceReport @FromDate = '2026-10-01', @ToDate = '2026-10-31';
SET STATISTICS IO OFF;
SELECT COUNT(*) AS Rows, SUM(WorkedDays) AS WorkedDays, DATEDIFF(MILLISECOND, @t0, SYSDATETIME()) AS ElapsedMs FROM @r;
GO

/* ---------------------------------------------------------------------------
   CASE 7 – set-based payroll for 3,000 employees
   --------------------------------------------------------------------------- */
PRINT '### CASE 7 - sp_CalculateMonthlyPayroll 2026-10 (3,000 employees)';
GO
DECLARE @t0 DATETIME2 = SYSDATETIME();
DECLARE @s TABLE (PeriodId INT, PeriodYear SMALLINT, PeriodMonth TINYINT, Status VARCHAR(20), StandardWorkDays DECIMAL(4,1),
                  EmployeeCount INT, TotalGross DECIMAL(18,2), TotalNet DECIMAL(18,2), CalculatedAt DATETIME2(0));
INSERT INTO @s EXEC dbo.sp_CalculateMonthlyPayroll @PeriodYear = 2026, @PeriodMonth = 10, @CalculatedBy = 'perf';
SELECT EmployeeCount, TotalGross, TotalNet, DATEDIFF(MILLISECOND, @t0, SYSDATETIME()) AS ElapsedMs FROM @s;
GO
