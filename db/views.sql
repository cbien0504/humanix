SET NOCOUNT ON;
GO

CREATE OR ALTER VIEW dbo.vw_EmployeeDetails
AS
SELECT e.EmployeeId,
       e.EmployeeCode,
       e.FullName,
       e.Gender,
       CASE e.Gender WHEN 'M' THEN N'Nam' WHEN 'F' THEN N'Nữ' ELSE N'Khác' END AS GenderName,
       e.DateOfBirth,
       CASE WHEN e.DateOfBirth IS NULL THEN NULL
            ELSE DATEDIFF(YEAR, e.DateOfBirth, t.Today)
                 - CASE WHEN DATEADD(YEAR, DATEDIFF(YEAR, e.DateOfBirth, t.Today), e.DateOfBirth) > t.Today THEN 1 ELSE 0 END
       END AS Age,
       e.Email,
       e.Phone,
       e.Address,
       e.NationalId,
       e.TaxCode,
       e.BankAccount,
       e.BankName,
       e.DepartmentId,
       d.DepartmentCode,
       d.DepartmentName,
       e.PositionId,
       p.PositionCode,
       p.PositionName,
       p.JobLevel,
       e.ManagerId,
       m.EmployeeCode AS ManagerCode,
       m.FullName     AS ManagerName,
       e.HireDate,
       e.TerminationDate,
       CAST(DATEDIFF(MONTH, e.HireDate, COALESCE(e.TerminationDate, t.Today)) / 12.0 AS DECIMAL(4,1)) AS YearsOfService,
       e.Status,
       CASE e.Status WHEN 'PROBATION' THEN N'Thử việc' WHEN 'ACTIVE' THEN N'Chính thức'
                     WHEN 'ON_LEAVE' THEN N'Nghỉ dài hạn' ELSE N'Đã nghỉ việc' END AS StatusName,
       e.BaseSalary,
       e.Allowance,
       e.Dependents,
       u.UserId,
       u.Username,
       CAST(CASE WHEN u.UserId IS NOT NULL AND u.IsActive = 1 THEN 1 ELSE 0 END AS BIT) AS HasActiveAccount,
       e.CreatedAt,
       e.UpdatedAt
FROM dbo.Employees AS e
JOIN dbo.Departments AS d ON d.DepartmentId = e.DepartmentId
JOIN dbo.Positions   AS p ON p.PositionId   = e.PositionId
LEFT JOIN dbo.Employees AS m ON m.EmployeeId = e.ManagerId
LEFT JOIN dbo.Users     AS u ON u.EmployeeId = e.EmployeeId
CROSS JOIN (SELECT CAST(SYSDATETIME() AS DATE) AS Today) AS t;
GO

CREATE OR ALTER VIEW dbo.vw_MonthlyAttendanceSummary
AS
WITH Att AS (
    SELECT a.EmployeeId,
           DATEFROMPARTS(YEAR(a.WorkDate), MONTH(a.WorkDate), 1) AS MonthStart,
           COUNT(*)                                               AS RecordedDays,
           SUM(CASE WHEN a.Status = 'PRESENT'  THEN 1 ELSE 0 END) AS PresentDays,
           SUM(CASE WHEN a.Status = 'LATE'     THEN 1 ELSE 0 END) AS LateDays,
           SUM(CASE WHEN a.Status = 'HALF_DAY' THEN 1 ELSE 0 END) AS HalfDays,
           SUM(CASE WHEN a.Status = 'ABSENT'   THEN 1 ELSE 0 END) AS AbsentDays,
           SUM(CASE a.Status WHEN 'PRESENT' THEN 1.0 WHEN 'LATE' THEN 1.0 WHEN 'HALF_DAY' THEN 0.5 ELSE 0 END) AS WorkedDays,
           SUM(CAST(a.LateMinutes AS INT))                        AS TotalLateMinutes,
           SUM(a.WorkHours)                                       AS TotalWorkHours
    FROM dbo.Attendance AS a
    GROUP BY a.EmployeeId, DATEFROMPARTS(YEAR(a.WorkDate), MONTH(a.WorkDate), 1)
), Lv AS (
    SELECT lr.EmployeeId,
           DATEFROMPARTS(YEAR(wd.WorkDate), MONTH(wd.WorkDate), 1) AS MonthStart,
           SUM(CASE WHEN lt.IsPaid = 1 THEN 1 ELSE 0 END)          AS PaidLeaveDays,
           SUM(CASE WHEN lt.IsPaid = 0 THEN 1 ELSE 0 END)          AS UnpaidLeaveDays
    FROM dbo.LeaveRequests AS lr
    JOIN dbo.LeaveTypes AS lt ON lt.LeaveTypeId = lr.LeaveTypeId
    CROSS APPLY dbo.fn_WorkingDays(lr.StartDate, lr.EndDate) AS wd
    WHERE lr.Status = 'APPROVED'
    GROUP BY lr.EmployeeId, DATEFROMPARTS(YEAR(wd.WorkDate), MONTH(wd.WorkDate), 1)
)
SELECT e.EmployeeId,
       e.EmployeeCode,
       e.FullName,
       e.DepartmentId,
       d.DepartmentName,
       k.MonthStart,
       YEAR(k.MonthStart)                  AS WorkYear,
       MONTH(k.MonthStart)                 AS WorkMonth,
       ISNULL(a.RecordedDays, 0)           AS RecordedDays,
       ISNULL(a.PresentDays, 0)            AS PresentDays,
       ISNULL(a.LateDays, 0)               AS LateDays,
       ISNULL(a.HalfDays, 0)               AS HalfDays,
       ISNULL(a.AbsentDays, 0)             AS AbsentDays,
       CAST(ISNULL(a.WorkedDays, 0) AS DECIMAL(4,1)) AS WorkedDays,
       ISNULL(l.PaidLeaveDays, 0)          AS PaidLeaveDays,
       ISNULL(l.UnpaidLeaveDays, 0)        AS UnpaidLeaveDays,
       ISNULL(a.TotalLateMinutes, 0)       AS TotalLateMinutes,
       CAST(ISNULL(a.TotalWorkHours, 0) AS DECIMAL(7,2)) AS TotalWorkHours
FROM Att AS a
FULL OUTER JOIN Lv AS l ON l.EmployeeId = a.EmployeeId AND l.MonthStart = a.MonthStart
CROSS APPLY (SELECT COALESCE(a.EmployeeId, l.EmployeeId) AS EmployeeId,
                    COALESCE(a.MonthStart, l.MonthStart) AS MonthStart) AS k
JOIN dbo.Employees   AS e ON e.EmployeeId   = k.EmployeeId
JOIN dbo.Departments AS d ON d.DepartmentId = e.DepartmentId;
GO

CREATE OR ALTER VIEW dbo.vw_PayrollDetails
AS
SELECT pr.PayrollId,
       pp.PeriodId,
       pp.PeriodYear,
       pp.PeriodMonth,
       pp.Status AS PeriodStatus,
       pr.EmployeeId,
       e.EmployeeCode,
       e.FullName,
       pr.DepartmentId,
       d.DepartmentName,
       p.PositionName,
       e.BankAccount,
       e.BankName,
       pr.BaseSalary,
       pr.Allowance,
       pr.Dependents,
       pr.StandardDays,
       pr.WorkedDays,
       pr.PaidLeaveDays,
       pr.PaidDays,
       pr.UnpaidDays,
       pr.SalaryAmount,
       pr.AllowanceAmount,
       pr.GrossSalary,
       pr.SocialInsurance,
       pr.HealthInsurance,
       pr.UnemploymentInsurance,
       pr.TotalInsurance,
       pr.PersonalDeduction,
       pr.DependentDeduction,
       pr.TaxableIncome,
       pr.PersonalIncomeTax,
       pr.NetSalary
FROM dbo.Payroll AS pr
JOIN dbo.PayrollPeriods AS pp ON pp.PeriodId = pr.PeriodId
JOIN dbo.Employees      AS e  ON e.EmployeeId = pr.EmployeeId
JOIN dbo.Positions      AS p  ON p.PositionId = e.PositionId
JOIN dbo.Departments    AS d  ON d.DepartmentId = pr.DepartmentId;
GO
