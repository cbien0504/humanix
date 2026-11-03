SET NOCOUNT ON;
GO

CREATE OR ALTER TRIGGER dbo.trg_Employees_AuditSalary
ON dbo.Employees
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF NOT (UPDATE(BaseSalary) OR UPDATE(Allowance)) RETURN;

    INSERT INTO dbo.SalaryHistory (EmployeeId, OldBaseSalary, NewBaseSalary, OldAllowance, NewAllowance, ChangedBy)
    SELECT i.EmployeeId, d.BaseSalary, i.BaseSalary, d.Allowance, i.Allowance,
           COALESCE(CAST(SESSION_CONTEXT(N'app_user') AS VARCHAR(50)), LEFT(SUSER_SNAME(), 50))
    FROM inserted AS i
    LEFT JOIN deleted AS d ON d.EmployeeId = i.EmployeeId
    WHERE d.EmployeeId IS NULL
       OR d.BaseSalary <> i.BaseSalary
       OR d.Allowance  <> i.Allowance;
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_Employees_DeactivateUser
ON dbo.Employees
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF NOT UPDATE(Status) RETURN;

    UPDATE u
       SET IsActive = 0, UpdatedAt = SYSUTCDATETIME()
    FROM dbo.Users AS u
    JOIN inserted AS i ON i.EmployeeId = u.EmployeeId
    JOIN deleted  AS d ON d.EmployeeId = i.EmployeeId
    WHERE i.Status = 'TERMINATED' AND d.Status <> 'TERMINATED' AND u.IsActive = 1;
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_LeaveRequests_PreventOverlap
ON dbo.LeaveRequests
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1
        FROM inserted AS i
        JOIN dbo.LeaveRequests AS lr
          ON lr.EmployeeId      = i.EmployeeId
         AND lr.LeaveRequestId <> i.LeaveRequestId
         AND lr.Status IN ('PENDING', 'APPROVED')
         AND lr.StartDate <= i.EndDate
         AND lr.EndDate   >= i.StartDate
        WHERE i.Status IN ('PENDING', 'APPROVED')
    )
        THROW 51001, N'Đơn nghỉ phép bị chồng lấn với một đơn khác đang chờ duyệt hoặc đã được duyệt.', 1;
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_LeaveRequests_SyncBalance
ON dbo.LeaveRequests
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    WITH Affected AS (
        SELECT EmployeeId, LeaveTypeId, YEAR(StartDate) AS BalanceYear FROM inserted
        UNION
        SELECT EmployeeId, LeaveTypeId, YEAR(StartDate) FROM deleted
    )
    UPDATE lb
       SET UsedDays  = ISNULL(used.Days, 0),
           UpdatedAt = SYSUTCDATETIME()
    FROM dbo.LeaveBalances AS lb
    JOIN Affected AS a
      ON a.EmployeeId = lb.EmployeeId AND a.LeaveTypeId = lb.LeaveTypeId AND a.BalanceYear = lb.BalanceYear
    OUTER APPLY (
        SELECT SUM(lr.TotalDays) AS Days
        FROM dbo.LeaveRequests AS lr
        WHERE lr.EmployeeId  = lb.EmployeeId
          AND lr.LeaveTypeId = lb.LeaveTypeId
          AND lr.Status      = 'APPROVED'
          AND lr.StartDate  >= DATEFROMPARTS(lb.BalanceYear, 1, 1)
          AND lr.StartDate  <  DATEFROMPARTS(lb.BalanceYear + 1, 1, 1)
    ) AS used
    WHERE lb.UsedDays <> ISNULL(used.Days, 0);
END;
GO

CREATE OR ALTER TRIGGER dbo.trg_Candidates_StageHistory
ON dbo.Candidates
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF NOT UPDATE(Stage) RETURN;

    INSERT INTO dbo.CandidateStageHistory (CandidateId, FromStage, ToStage, ChangedBy)
    SELECT i.CandidateId, d.Stage, i.Stage,
           COALESCE(CAST(SESSION_CONTEXT(N'app_user') AS VARCHAR(50)), LEFT(SUSER_SNAME(), 50))
    FROM inserted AS i
    LEFT JOIN deleted AS d ON d.CandidateId = i.CandidateId
    WHERE d.CandidateId IS NULL OR d.Stage <> i.Stage;
END;
GO
