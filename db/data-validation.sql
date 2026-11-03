SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

PRINT N'=== Part 1 – validation summary ===';
EXEC dbo.sp_RunDataValidation;
GO

PRINT N'=== Part 2a – duplicate employee codes (normalised: trim + upper) ===';
SELECT UPPER(LTRIM(RTRIM(EmployeeCode))) AS NormalisedCode, COUNT(*) AS Occurrences,
       STRING_AGG(CONCAT(EmployeeId, ':', FullName), N'; ') AS Employees
FROM dbo.Employees
GROUP BY UPPER(LTRIM(RTRIM(EmployeeCode)))
HAVING COUNT(*) > 1;

PRINT N'=== Part 2b – negative salary / allowance ===';
SELECT EmployeeId, EmployeeCode, FullName, BaseSalary, Allowance
FROM dbo.Employees
WHERE BaseSalary < 0 OR Allowance < 0;

SELECT p.PayrollId, e.EmployeeCode, p.GrossSalary, p.NetSalary
FROM dbo.Payroll AS p JOIN dbo.Employees AS e ON e.EmployeeId = p.EmployeeId
WHERE p.GrossSalary < 0 OR p.NetSalary < 0;

PRINT N'=== Part 2c – overlapping leave requests (PENDING/APPROVED) ===';
SELECT e.EmployeeCode, e.FullName,
       a.LeaveRequestId AS RequestA, a.StartDate AS StartA, a.EndDate AS EndA, a.Status AS StatusA,
       b.LeaveRequestId AS RequestB, b.StartDate AS StartB, b.EndDate AS EndB, b.Status AS StatusB
FROM dbo.LeaveRequests AS a
JOIN dbo.LeaveRequests AS b
  ON b.EmployeeId = a.EmployeeId AND b.LeaveRequestId > a.LeaveRequestId
 AND b.StartDate <= a.EndDate AND b.EndDate >= a.StartDate
JOIN dbo.Employees AS e ON e.EmployeeId = a.EmployeeId
WHERE a.Status IN ('PENDING', 'APPROVED') AND b.Status IN ('PENDING', 'APPROVED');
GO

PRINT N'=== Part 3 – self-test (all changes are rolled back) ===';
BEGIN TRANSACTION;

    ALTER INDEX UQ_Employees_EmployeeCode ON dbo.Employees DISABLE;
    INSERT INTO dbo.Employees (EmployeeCode, FullName, Gender, Email, DepartmentId, PositionId, HireDate, BaseSalary, Status)
    SELECT 'EMP0001', N'Bản ghi trùng mã (self-test)', 'M', 'selftest.dup@example.com', DepartmentId, PositionId,
           '2026-01-05', 15000000, 'ACTIVE'
    FROM dbo.Employees WHERE EmployeeCode = 'EMP0016';

    ALTER TABLE dbo.Employees NOCHECK CONSTRAINT CK_Employees_Salary;
    UPDATE dbo.Employees SET BaseSalary = -5000000 WHERE EmployeeCode = 'EMP0010';

    DISABLE TRIGGER dbo.trg_LeaveRequests_PreventOverlap ON dbo.LeaveRequests;
    INSERT INTO dbo.LeaveRequests (EmployeeId, LeaveTypeId, StartDate, EndDate, TotalDays, Reason, Status)
    SELECT e.EmployeeId, lt.LeaveTypeId, '2026-11-17', '2026-11-19', 3, N'Self-test overlap', 'PENDING'
    FROM dbo.Employees AS e CROSS JOIN dbo.LeaveTypes AS lt
    WHERE e.EmployeeCode = 'EMP0016' AND lt.LeaveTypeCode = 'UNPAID';

    PRINT N'Expected FAIL on DV01, DV04, DV06:';
    EXEC dbo.sp_RunDataValidation;

ROLLBACK TRANSACTION;
GO

PRINT N'=== After rollback – constraints trusted again? ===';
SELECT name, is_disabled, is_not_trusted FROM sys.check_constraints WHERE name = 'CK_Employees_Salary';
SELECT name, is_disabled FROM sys.indexes WHERE name = 'UQ_Employees_EmployeeCode';
SELECT name, is_disabled FROM sys.triggers WHERE name = 'trg_LeaveRequests_PreventOverlap';
GO
