/* =============================================================================
   HRM System – stored procedures
   Error numbers raised with THROW (translated to business errors in Java):
     511xx payroll · 512xx attendance report · 513xx leave balances
   All procedures are set-based (no cursors) and run their writes in one
   transaction with SET XACT_ABORT ON.
   ============================================================================= */
SET NOCOUNT ON;
GO

/* -----------------------------------------------------------------------------
   sp_CalculateMonthlyPayroll – calculates the payroll of a month for every
   employee employed during that month (BR-30 … BR-34).

     Standard days   = working days of the month (fn_WorkingDays)
     Paid days       = min(standard, worked days + approved paid-leave days)
     Salary/allowance= amount × paid days / standard days
     Insurance       = 8% + 1.5% of min(base, cap × reference salary)
                     + 1% of min(base, cap × regional minimum wage)
                       – none if unpaid days ≥ threshold or insurance > gross
     Taxable income  = max(0, gross − insurance − personal − dependants × deduction)
     PIT             = progressive brackets (TaxBrackets)
     Net             = gross − insurance − PIT
   Every amount is rounded to whole VND. The Java class PayrollCalculator
   implements the same formula (gross→net simulator) and an integration test
   asserts both produce identical payslips.
   Re-runnable while the period is OPEN/CALCULATED; APPROVED/PAID are frozen.
   ----------------------------------------------------------------------------- */
CREATE OR ALTER PROCEDURE dbo.sp_CalculateMonthlyPayroll
    @PeriodYear   SMALLINT,
    @PeriodMonth  TINYINT,
    @CalculatedBy VARCHAR(50) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @PeriodYear IS NULL OR @PeriodMonth IS NULL OR @PeriodMonth NOT BETWEEN 1 AND 12
       OR @PeriodYear NOT BETWEEN 2000 AND 2100
        THROW 51101, N'Kỳ lương không hợp lệ (tháng 1–12, năm 2000–2100).', 1;

    DECLARE @MonthStart   DATE = DATEFROMPARTS(@PeriodYear, @PeriodMonth, 1);
    DECLARE @MonthEnd     DATE = EOMONTH(@MonthStart);
    DECLARE @StandardDays DECIMAL(4,1) = (SELECT COUNT(*) FROM dbo.fn_WorkingDays(@MonthStart, @MonthEnd));

    DECLARE @SiRate DECIMAL(18,4), @HiRate DECIMAL(18,4), @UiRate DECIMAL(18,4),
            @ReferenceSalary DECIMAL(18,4), @RegionalMinWage DECIMAL(18,4), @CapMultiplier DECIMAL(18,4),
            @PersonalDeduction DECIMAL(18,4), @DependentDeduction DECIMAL(18,4), @NoInsuranceDays DECIMAL(18,4);

    -- one lookup per key on the tiny PK table (avoids NULL-elimination warnings of MAX(CASE ...))
    SELECT @SiRate             = (SELECT SettingValue FROM dbo.PayrollSettings WHERE SettingKey = 'SOCIAL_INSURANCE_RATE'),
           @HiRate             = (SELECT SettingValue FROM dbo.PayrollSettings WHERE SettingKey = 'HEALTH_INSURANCE_RATE'),
           @UiRate             = (SELECT SettingValue FROM dbo.PayrollSettings WHERE SettingKey = 'UNEMPLOYMENT_INSURANCE_RATE'),
           @ReferenceSalary    = (SELECT SettingValue FROM dbo.PayrollSettings WHERE SettingKey = 'BASE_SALARY_REFERENCE'),
           @RegionalMinWage    = (SELECT SettingValue FROM dbo.PayrollSettings WHERE SettingKey = 'REGIONAL_MINIMUM_WAGE'),
           @CapMultiplier      = (SELECT SettingValue FROM dbo.PayrollSettings WHERE SettingKey = 'INSURANCE_CAP_MULTIPLIER'),
           @PersonalDeduction  = (SELECT SettingValue FROM dbo.PayrollSettings WHERE SettingKey = 'PERSONAL_DEDUCTION'),
           @DependentDeduction = (SELECT SettingValue FROM dbo.PayrollSettings WHERE SettingKey = 'DEPENDENT_DEDUCTION'),
           @NoInsuranceDays    = (SELECT SettingValue FROM dbo.PayrollSettings WHERE SettingKey = 'UNPAID_DAYS_NO_INSURANCE');

    IF @SiRate IS NULL OR @HiRate IS NULL OR @UiRate IS NULL OR @ReferenceSalary IS NULL OR @RegionalMinWage IS NULL
       OR @CapMultiplier IS NULL OR @PersonalDeduction IS NULL OR @DependentDeduction IS NULL OR @NoInsuranceDays IS NULL
        THROW 51102, N'Thiếu tham số lương trong bảng PayrollSettings.', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.TaxBrackets)
        THROW 51104, N'Chưa cấu hình biểu thuế TNCN (TaxBrackets).', 1;

    DECLARE @SiHiCap DECIMAL(18,2) = @CapMultiplier * @ReferenceSalary;
    DECLARE @UiCap   DECIMAL(18,2) = @CapMultiplier * @RegionalMinWage;
    DECLARE @PeriodId INT, @Status VARCHAR(20);

    BEGIN TRY
        BEGIN TRANSACTION;

        -- Serialise concurrent runs for the same period (UPDLOCK + HOLDLOCK = key-range lock)
        SELECT @PeriodId = PeriodId, @Status = Status
        FROM dbo.PayrollPeriods WITH (UPDLOCK, HOLDLOCK)
        WHERE PeriodYear = @PeriodYear AND PeriodMonth = @PeriodMonth;

        IF @PeriodId IS NULL
        BEGIN
            INSERT INTO dbo.PayrollPeriods (PeriodYear, PeriodMonth, Status) VALUES (@PeriodYear, @PeriodMonth, 'OPEN');
            SET @PeriodId = SCOPE_IDENTITY();
        END
        ELSE IF @Status IN ('APPROVED', 'PAID')
            THROW 51103, N'Kỳ lương đã được duyệt hoặc đã chi trả – không thể tính lại.', 1;

        DELETE FROM dbo.Payroll WHERE PeriodId = @PeriodId;

        WITH Emp AS (
            SELECT e.EmployeeId, e.DepartmentId, e.BaseSalary, e.Allowance, e.Dependents
            FROM dbo.Employees AS e
            WHERE e.HireDate <= @MonthEnd
              AND (e.TerminationDate IS NULL OR e.TerminationDate >= @MonthStart)
        ), Worked AS (
            SELECT a.EmployeeId,
                   SUM(CASE a.Status WHEN 'PRESENT' THEN 1.0 WHEN 'LATE' THEN 1.0 WHEN 'HALF_DAY' THEN 0.5 ELSE 0 END) AS Days
            FROM dbo.Attendance AS a
            WHERE a.WorkDate >= @MonthStart AND a.WorkDate <= @MonthEnd       -- sargable range
            GROUP BY a.EmployeeId
        ), PaidLeave AS (
            SELECT lr.EmployeeId, COUNT(*) AS Days
            FROM dbo.LeaveRequests AS lr
            JOIN dbo.LeaveTypes AS lt ON lt.LeaveTypeId = lr.LeaveTypeId AND lt.IsPaid = 1
            CROSS APPLY dbo.fn_WorkingDays(CASE WHEN lr.StartDate > @MonthStart THEN lr.StartDate ELSE @MonthStart END,
                                           CASE WHEN lr.EndDate   < @MonthEnd   THEN lr.EndDate   ELSE @MonthEnd   END) AS wd
            WHERE lr.Status = 'APPROVED' AND lr.StartDate <= @MonthEnd AND lr.EndDate >= @MonthStart
            GROUP BY lr.EmployeeId
        ), Days AS (
            SELECT e.*, ISNULL(w.Days, 0) AS WorkedDays, ISNULL(pl.Days, 0) AS PaidLeaveDays,
                   CASE WHEN ISNULL(w.Days, 0) + ISNULL(pl.Days, 0) > @StandardDays THEN @StandardDays
                        ELSE ISNULL(w.Days, 0) + ISNULL(pl.Days, 0) END AS PaidDays
            FROM Emp AS e
            LEFT JOIN Worked    AS w  ON w.EmployeeId  = e.EmployeeId
            LEFT JOIN PaidLeave AS pl ON pl.EmployeeId = e.EmployeeId
        ), Gross AS (
            SELECT d.*,
                   @StandardDays - d.PaidDays AS UnpaidDays,
                   CASE WHEN @StandardDays = 0 THEN 0 ELSE ROUND(d.BaseSalary * d.PaidDays / @StandardDays, 0) END AS SalaryAmount,
                   CASE WHEN @StandardDays = 0 THEN 0 ELSE ROUND(d.Allowance  * d.PaidDays / @StandardDays, 0) END AS AllowanceAmount,
                   ROUND(CASE WHEN d.BaseSalary < @SiHiCap THEN d.BaseSalary ELSE @SiHiCap END * @SiRate, 0) AS SiRaw,
                   ROUND(CASE WHEN d.BaseSalary < @SiHiCap THEN d.BaseSalary ELSE @SiHiCap END * @HiRate, 0) AS HiRaw,
                   ROUND(CASE WHEN d.BaseSalary < @UiCap   THEN d.BaseSalary ELSE @UiCap   END * @UiRate, 0) AS UiRaw
            FROM Days AS d
        ), Ins AS (
            SELECT g.*, g.SalaryAmount + g.AllowanceAmount AS GrossSalary, x.Insured
            FROM Gross AS g
            CROSS APPLY (SELECT CASE WHEN g.UnpaidDays >= @NoInsuranceDays
                                       OR g.SiRaw + g.HiRaw + g.UiRaw > g.SalaryAmount + g.AllowanceAmount
                                     THEN 0 ELSE 1 END AS Insured) AS x
        ), Taxable AS (
            SELECT i.*,
                   i.SiRaw * i.Insured AS SocialInsurance,
                   i.HiRaw * i.Insured AS HealthInsurance,
                   i.UiRaw * i.Insured AS UnemploymentInsurance,
                   CAST(@PersonalDeduction AS DECIMAL(18,2))                AS PersonalDeduction,
                   CAST(i.Dependents * @DependentDeduction AS DECIMAL(18,2)) AS DependentDeduction,
                   CASE WHEN t.Amount > 0 THEN t.Amount ELSE 0 END          AS TaxableIncome
            FROM Ins AS i
            CROSS APPLY (SELECT i.GrossSalary - (i.SiRaw + i.HiRaw + i.UiRaw) * i.Insured
                                - @PersonalDeduction - i.Dependents * @DependentDeduction AS Amount) AS t
        )
        INSERT INTO dbo.Payroll (PeriodId, EmployeeId, DepartmentId, BaseSalary, Allowance, Dependents,
                                 StandardDays, WorkedDays, PaidLeaveDays, PaidDays, UnpaidDays,
                                 SalaryAmount, AllowanceAmount, GrossSalary,
                                 SocialInsurance, HealthInsurance, UnemploymentInsurance,
                                 PersonalDeduction, DependentDeduction, TaxableIncome, PersonalIncomeTax, NetSalary)
        SELECT @PeriodId, t.EmployeeId, t.DepartmentId, t.BaseSalary, t.Allowance, t.Dependents,
               @StandardDays, t.WorkedDays, t.PaidLeaveDays, t.PaidDays, t.UnpaidDays,
               t.SalaryAmount, t.AllowanceAmount, t.GrossSalary,
               t.SocialInsurance, t.HealthInsurance, t.UnemploymentInsurance,
               t.PersonalDeduction, t.DependentDeduction, t.TaxableIncome, pit.Tax,
               t.GrossSalary - t.SocialInsurance - t.HealthInsurance - t.UnemploymentInsurance - pit.Tax
        FROM Taxable AS t
        CROSS APPLY (
            -- tax of each bracket the income reaches; derived table first because an aggregate
            -- cannot mix an outer reference with inner columns (error 8124)
            SELECT ISNULL(ROUND(SUM(b.Amount), 0), 0) AS Tax
            FROM (SELECT (CASE WHEN tb.UpperBound IS NULL OR t.TaxableIncome < tb.UpperBound
                               THEN t.TaxableIncome ELSE tb.UpperBound END - tb.LowerBound) * tb.Rate AS Amount
                  FROM dbo.TaxBrackets AS tb
                  WHERE t.TaxableIncome > tb.LowerBound) AS b
        ) AS pit;

        UPDATE pp
           SET Status           = 'CALCULATED',
               StandardWorkDays = @StandardDays,
               EmployeeCount    = s.Cnt,
               TotalGross       = s.Gross,
               TotalNet         = s.Net,
               CalculatedAt     = SYSUTCDATETIME(),
               CalculatedBy     = @CalculatedBy
        FROM dbo.PayrollPeriods AS pp
        CROSS APPLY (SELECT COUNT(*) AS Cnt, ISNULL(SUM(GrossSalary), 0) AS Gross, ISNULL(SUM(NetSalary), 0) AS Net
                     FROM dbo.Payroll WHERE PeriodId = @PeriodId) AS s
        WHERE pp.PeriodId = @PeriodId;

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;

    SELECT PeriodId, PeriodYear, PeriodMonth, Status, StandardWorkDays, EmployeeCount, TotalGross, TotalNet, CalculatedAt
    FROM dbo.PayrollPeriods
    WHERE PeriodId = @PeriodId;
END;
GO

/* -----------------------------------------------------------------------------
   sp_GetAttendanceReport – attendance per employee over a date range
   (working days only), optionally for one department.
     ExpectedDays = working days while employed in the range
     AbsentDays   = expected − days with attendance − approved leave days
   ----------------------------------------------------------------------------- */
CREATE OR ALTER PROCEDURE dbo.sp_GetAttendanceReport
    @FromDate     DATE,
    @ToDate       DATE,
    @DepartmentId INT = NULL
AS
BEGIN
    SET NOCOUNT ON;

    IF @FromDate IS NULL OR @ToDate IS NULL OR @ToDate < @FromDate
        THROW 51201, N'Khoảng thời gian báo cáo không hợp lệ.', 1;
    IF DATEDIFF(DAY, @FromDate, @ToDate) > 366
        THROW 51202, N'Khoảng thời gian báo cáo tối đa là 1 năm.', 1;

    DECLARE @WorkingDays TABLE (WorkDate DATE NOT NULL PRIMARY KEY);
    INSERT INTO @WorkingDays (WorkDate) SELECT WorkDate FROM dbo.fn_WorkingDays(@FromDate, @ToDate);
    DECLARE @TotalWorkingDays INT = @@ROWCOUNT;

    WITH Emp AS (
        SELECT e.EmployeeId, e.EmployeeCode, e.FullName, e.DepartmentId, d.DepartmentName,
               p.PositionName, e.HireDate, e.TerminationDate
        FROM dbo.Employees AS e
        JOIN dbo.Departments AS d ON d.DepartmentId = e.DepartmentId
        JOIN dbo.Positions   AS p ON p.PositionId   = e.PositionId
        WHERE (@DepartmentId IS NULL OR e.DepartmentId = @DepartmentId)
          AND e.HireDate <= @ToDate
          AND (e.TerminationDate IS NULL OR e.TerminationDate >= @FromDate)
    ), Expected AS (
        -- employed for the whole range (the vast majority): no per-employee lookup needed
        SELECT e.EmployeeId,
               CASE WHEN e.HireDate <= @FromDate AND (e.TerminationDate IS NULL OR e.TerminationDate >= @ToDate)
                    THEN @TotalWorkingDays
                    ELSE (SELECT COUNT(*) FROM @WorkingDays AS w
                          WHERE w.WorkDate >= e.HireDate AND (e.TerminationDate IS NULL OR w.WorkDate <= e.TerminationDate))
               END AS Days
        FROM Emp AS e
    ), Att AS (
        SELECT a.EmployeeId,
               SUM(CASE WHEN a.Status = 'PRESENT'  THEN 1 ELSE 0 END) AS PresentDays,
               SUM(CASE WHEN a.Status = 'LATE'     THEN 1 ELSE 0 END) AS LateDays,
               SUM(CASE WHEN a.Status = 'HALF_DAY' THEN 1 ELSE 0 END) AS HalfDays,
               SUM(CASE WHEN a.Status <> 'ABSENT'  THEN 1 ELSE 0 END) AS AttendedDays,
               SUM(CASE a.Status WHEN 'PRESENT' THEN 1.0 WHEN 'LATE' THEN 1.0 WHEN 'HALF_DAY' THEN 0.5 ELSE 0 END) AS WorkedDays,
               SUM(CAST(a.LateMinutes AS INT)) AS LateMinutes,
               SUM(a.WorkHours) AS WorkHours
        FROM dbo.Attendance AS a
        JOIN @WorkingDays AS w ON w.WorkDate = a.WorkDate
        WHERE a.WorkDate >= @FromDate AND a.WorkDate <= @ToDate             -- sargable: seek on IX_Attendance_WorkDate
        GROUP BY a.EmployeeId
    ), Lv AS (
        SELECT lr.EmployeeId,
               SUM(CASE WHEN lt.IsPaid = 1 THEN 1 ELSE 0 END) AS PaidLeaveDays,
               SUM(CASE WHEN lt.IsPaid = 0 THEN 1 ELSE 0 END) AS UnpaidLeaveDays
        FROM dbo.LeaveRequests AS lr
        JOIN dbo.LeaveTypes AS lt ON lt.LeaveTypeId = lr.LeaveTypeId
        JOIN @WorkingDays AS w ON w.WorkDate BETWEEN lr.StartDate AND lr.EndDate
        WHERE lr.Status = 'APPROVED' AND lr.StartDate <= @ToDate AND lr.EndDate >= @FromDate
        GROUP BY lr.EmployeeId
    )
    SELECT e.EmployeeId, e.EmployeeCode, e.FullName, e.DepartmentId, e.DepartmentName, e.PositionName,
           ISNULL(x.Days, 0)            AS ExpectedDays,
           ISNULL(a.PresentDays, 0)     AS PresentDays,
           ISNULL(a.LateDays, 0)        AS LateDays,
           ISNULL(a.HalfDays, 0)        AS HalfDays,
           ISNULL(l.PaidLeaveDays, 0)   AS PaidLeaveDays,
           ISNULL(l.UnpaidLeaveDays, 0) AS UnpaidLeaveDays,
           CASE WHEN ISNULL(x.Days, 0) - ISNULL(a.AttendedDays, 0) - ISNULL(l.PaidLeaveDays, 0) - ISNULL(l.UnpaidLeaveDays, 0) > 0
                THEN ISNULL(x.Days, 0) - ISNULL(a.AttendedDays, 0) - ISNULL(l.PaidLeaveDays, 0) - ISNULL(l.UnpaidLeaveDays, 0)
                ELSE 0 END              AS AbsentDays,
           CAST(ISNULL(a.WorkedDays, 0) AS DECIMAL(5,1)) AS WorkedDays,
           ISNULL(a.LateMinutes, 0)     AS LateMinutes,
           CAST(ISNULL(a.WorkHours, 0) AS DECIMAL(8,2))  AS WorkHours,
           CAST(CASE WHEN ISNULL(x.Days, 0) = 0 THEN 0
                     ELSE 100.0 * (ISNULL(a.WorkedDays, 0) + ISNULL(l.PaidLeaveDays, 0)) / x.Days END AS DECIMAL(5,1)) AS AttendanceRate
    FROM Emp AS e
    LEFT JOIN Expected AS x ON x.EmployeeId = e.EmployeeId
    LEFT JOIN Att      AS a ON a.EmployeeId = e.EmployeeId
    LEFT JOIN Lv       AS l ON l.EmployeeId = e.EmployeeId
    ORDER BY e.DepartmentName, e.EmployeeCode
    OPTION (RECOMPILE);   -- optional @DepartmentId: compile a plan for the actual value (no parameter sniffing)
END;
GO

/* -----------------------------------------------------------------------------
   sp_GetDepartmentStatistics – headcount & HR KPIs per department as of a date
   (current status columns reflect today's status).
   ----------------------------------------------------------------------------- */
CREATE OR ALTER PROCEDURE dbo.sp_GetDepartmentStatistics
    @AsOfDate DATE = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET @AsOfDate = ISNULL(@AsOfDate, CAST(SYSDATETIME() AS DATE));
    DECLARE @YearStart DATE = DATEFROMPARTS(YEAR(@AsOfDate), 1, 1);

    WITH Headcount AS (
        SELECT e.DepartmentId,
               COUNT(*) AS Headcount,
               SUM(CASE WHEN e.Status = 'ACTIVE'    THEN 1 ELSE 0 END) AS ActiveCount,
               SUM(CASE WHEN e.Status = 'PROBATION' THEN 1 ELSE 0 END) AS ProbationCount,
               SUM(CASE WHEN e.Status = 'ON_LEAVE'  THEN 1 ELSE 0 END) AS OnLeaveCount,
               SUM(CASE WHEN e.Gender = 'M' THEN 1 ELSE 0 END)         AS MaleCount,
               SUM(CASE WHEN e.Gender = 'F' THEN 1 ELSE 0 END)         AS FemaleCount,
               AVG(e.BaseSalary)                                      AS AvgBaseSalary,
               SUM(e.BaseSalary + e.Allowance)                        AS MonthlySalaryFund,
               AVG(DATEDIFF(MONTH, e.HireDate, @AsOfDate) / 12.0)     AS AvgYearsOfService,
               AVG(CASE WHEN e.DateOfBirth IS NULL THEN NULL ELSE DATEDIFF(DAY, e.DateOfBirth, @AsOfDate) / 365.25 END) AS AvgAge
        FROM dbo.Employees AS e
        WHERE e.HireDate <= @AsOfDate AND (e.TerminationDate IS NULL OR e.TerminationDate > @AsOfDate)
        GROUP BY e.DepartmentId
    ), Movement AS (
        SELECT e.DepartmentId,
               SUM(CASE WHEN e.HireDate BETWEEN @YearStart AND @AsOfDate THEN 1 ELSE 0 END)        AS NewHiresYtd,
               SUM(CASE WHEN e.TerminationDate BETWEEN @YearStart AND @AsOfDate THEN 1 ELSE 0 END) AS TerminationsYtd
        FROM dbo.Employees AS e
        GROUP BY e.DepartmentId
    )
    SELECT d.DepartmentId, d.DepartmentCode, d.DepartmentName, m.FullName AS ManagerName,
           ISNULL(h.Headcount, 0)       AS Headcount,
           ISNULL(h.ActiveCount, 0)     AS ActiveCount,
           ISNULL(h.ProbationCount, 0)  AS ProbationCount,
           ISNULL(h.OnLeaveCount, 0)    AS OnLeaveCount,
           ISNULL(h.MaleCount, 0)       AS MaleCount,
           ISNULL(h.FemaleCount, 0)     AS FemaleCount,
           CAST(ISNULL(h.AvgBaseSalary, 0) AS DECIMAL(18,0))     AS AvgBaseSalary,
           CAST(ISNULL(h.MonthlySalaryFund, 0) AS DECIMAL(18,0)) AS MonthlySalaryFund,
           CAST(ISNULL(h.AvgYearsOfService, 0) AS DECIMAL(4,1))  AS AvgYearsOfService,
           CAST(ISNULL(h.AvgAge, 0) AS DECIMAL(4,1))             AS AvgAge,
           ISNULL(mv.NewHiresYtd, 0)     AS NewHiresYtd,
           ISNULL(mv.TerminationsYtd, 0) AS TerminationsYtd,
           -- turnover ≈ leavers / (current headcount + leavers) for the year to date
           CAST(CASE WHEN ISNULL(h.Headcount, 0) + ISNULL(mv.TerminationsYtd, 0) = 0 THEN 0
                     ELSE 100.0 * ISNULL(mv.TerminationsYtd, 0) / (ISNULL(h.Headcount, 0) + ISNULL(mv.TerminationsYtd, 0))
                END AS DECIMAL(5,1)) AS TurnoverRate
    FROM dbo.Departments AS d
    LEFT JOIN dbo.Employees AS m ON m.EmployeeId = d.ManagerId
    LEFT JOIN Headcount     AS h ON h.DepartmentId = d.DepartmentId
    LEFT JOIN Movement      AS mv ON mv.DepartmentId = d.DepartmentId
    WHERE d.IsActive = 1
    ORDER BY d.DepartmentName;
END;
GO

/* -----------------------------------------------------------------------------
   sp_InitLeaveBalances – create/refresh balances of every leave type that
   requires one (annual leave) for a year. Idempotent: existing rows keep
   UsedDays; EntitledDays is refreshed when it changed and stays >= UsedDays.
   BR-23: 12 days + 1 day per 5 full years of service at 1 January;
          joiners during the year: 12 × months worked / 12 (hire month counts
          if hired on or before the 15th), fraction >= 0.5 rounds up.
   ----------------------------------------------------------------------------- */
CREATE OR ALTER PROCEDURE dbo.sp_InitLeaveBalances
    @Year SMALLINT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @Year IS NULL OR @Year NOT BETWEEN 2000 AND 2100
        THROW 51301, N'Năm không hợp lệ.', 1;

    DECLARE @YearStart DATE = DATEFROMPARTS(@Year, 1, 1), @YearEnd DATE = DATEFROMPARTS(@Year, 12, 31);
    DECLARE @Source TABLE (EmployeeId INT, LeaveTypeId INT, EntitledDays DECIMAL(5,1), UsedDays DECIMAL(5,1),
                           PRIMARY KEY (EmployeeId, LeaveTypeId));

    INSERT INTO @Source (EmployeeId, LeaveTypeId, EntitledDays, UsedDays)
    SELECT e.EmployeeId, lt.LeaveTypeId,
           CASE WHEN e.HireDate < @YearStart
                THEN 12 + svc.FullYears / 5
                ELSE ROUND(12.0 * (12 - MONTH(e.HireDate) + CASE WHEN DAY(e.HireDate) <= 15 THEN 1 ELSE 0 END) / 12, 0)
           END,
           ISNULL(used.Days, 0)
    FROM dbo.Employees AS e
    CROSS JOIN dbo.LeaveTypes AS lt
    CROSS APPLY (SELECT DATEDIFF(YEAR, e.HireDate, @YearStart)
                        - CASE WHEN DATEADD(YEAR, DATEDIFF(YEAR, e.HireDate, @YearStart), e.HireDate) > @YearStart
                               THEN 1 ELSE 0 END AS FullYears) AS svc
    OUTER APPLY (SELECT SUM(lr.TotalDays) AS Days
                 FROM dbo.LeaveRequests AS lr
                 WHERE lr.EmployeeId = e.EmployeeId AND lr.LeaveTypeId = lt.LeaveTypeId AND lr.Status = 'APPROVED'
                   AND lr.StartDate >= @YearStart AND lr.StartDate <= @YearEnd) AS used
    WHERE lt.RequiresBalance = 1 AND lt.IsActive = 1
      AND e.HireDate <= @YearEnd
      AND (e.TerminationDate IS NULL OR e.TerminationDate >= @YearStart);

    DECLARE @Inserted INT, @Updated INT;
    BEGIN TRANSACTION;

    UPDATE lb
       SET EntitledDays = s.EntitledDays, UpdatedAt = SYSUTCDATETIME()
    FROM dbo.LeaveBalances AS lb
    JOIN @Source AS s ON s.EmployeeId = lb.EmployeeId AND s.LeaveTypeId = lb.LeaveTypeId
    WHERE lb.BalanceYear = @Year AND lb.EntitledDays <> s.EntitledDays AND s.EntitledDays >= lb.UsedDays;
    SET @Updated = @@ROWCOUNT;

    INSERT INTO dbo.LeaveBalances (EmployeeId, LeaveTypeId, BalanceYear, EntitledDays, UsedDays)
    SELECT s.EmployeeId, s.LeaveTypeId, @Year, s.EntitledDays,
           CASE WHEN s.UsedDays > s.EntitledDays THEN s.EntitledDays ELSE s.UsedDays END
    FROM @Source AS s
    WHERE NOT EXISTS (SELECT 1 FROM dbo.LeaveBalances AS lb
                      WHERE lb.EmployeeId = s.EmployeeId AND lb.LeaveTypeId = s.LeaveTypeId AND lb.BalanceYear = @Year);
    SET @Inserted = @@ROWCOUNT;

    COMMIT TRANSACTION;

    SELECT @Year AS BalanceYear, @Inserted AS InsertedCount, @Updated AS UpdatedCount;
END;
GO

/* -----------------------------------------------------------------------------
   sp_RunDataValidation – data quality checks on live data. Constraints and
   triggers prevent most of these issues, but data loaded with constraints
   disabled, legacy imports or direct SQL fixes can still break them.
   Result: one row per check with PASS/FAIL/WARN and up to 10 sample keys.
   ----------------------------------------------------------------------------- */
CREATE OR ALTER PROCEDURE dbo.sp_RunDataValidation
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @Today DATE = CAST(SYSDATETIME() AS DATE);
    DECLARE @r TABLE (CheckCode VARCHAR(10) PRIMARY KEY, CheckName NVARCHAR(200), Severity VARCHAR(10),
                      ViolationCount INT, SampleKeys NVARCHAR(1000));

    -- DV01: duplicate employee codes after normalisation (trim + upper case)
    WITH x AS (SELECT UPPER(LTRIM(RTRIM(EmployeeCode))) AS K FROM dbo.Employees
               GROUP BY UPPER(LTRIM(RTRIM(EmployeeCode))) HAVING COUNT(*) > 1)
    INSERT INTO @r SELECT 'DV01', N'Mã nhân viên trùng lặp', 'ERROR', (SELECT COUNT(*) FROM x),
           (SELECT STRING_AGG(K, ', ') FROM (SELECT TOP 10 K FROM x ORDER BY K) s);

    -- DV02: duplicate e-mails (case/whitespace-insensitive)
    WITH x AS (SELECT LOWER(LTRIM(RTRIM(Email))) AS K FROM dbo.Employees
               GROUP BY LOWER(LTRIM(RTRIM(Email))) HAVING COUNT(*) > 1)
    INSERT INTO @r SELECT 'DV02', N'Email nhân viên trùng lặp', 'ERROR', (SELECT COUNT(*) FROM x),
           (SELECT STRING_AGG(K, ', ') FROM (SELECT TOP 10 K FROM x ORDER BY K) s);

    -- DV03: duplicate national ID numbers
    WITH x AS (SELECT NationalId AS K FROM dbo.Employees WHERE NationalId IS NOT NULL
               GROUP BY NationalId HAVING COUNT(*) > 1)
    INSERT INTO @r SELECT 'DV03', N'Số CCCD trùng lặp', 'ERROR', (SELECT COUNT(*) FROM x),
           (SELECT STRING_AGG(K, ', ') FROM (SELECT TOP 10 K FROM x ORDER BY K) s);

    -- DV04: negative salary or allowance
    WITH x AS (SELECT EmployeeCode AS K FROM dbo.Employees WHERE BaseSalary < 0 OR Allowance < 0)
    INSERT INTO @r SELECT 'DV04', N'Lương cơ bản / phụ cấp âm', 'ERROR', (SELECT COUNT(*) FROM x),
           (SELECT STRING_AGG(K, ', ') FROM (SELECT TOP 10 K FROM x ORDER BY K) s);

    -- DV05: salary outside the position salary band (warning – may be an approved exception)
    WITH x AS (SELECT e.EmployeeCode AS K FROM dbo.Employees AS e JOIN dbo.Positions AS p ON p.PositionId = e.PositionId
               WHERE e.Status <> 'TERMINATED' AND (e.BaseSalary < p.MinSalary OR e.BaseSalary > p.MaxSalary))
    INSERT INTO @r SELECT 'DV05', N'Lương nằm ngoài khung lương chức danh', 'WARNING', (SELECT COUNT(*) FROM x),
           (SELECT STRING_AGG(K, ', ') FROM (SELECT TOP 10 K FROM x ORDER BY K) s);

    -- DV06: overlapping active leave requests of the same employee
    WITH x AS (SELECT CONCAT(e.EmployeeCode, ':#', a.LeaveRequestId, '/#', b.LeaveRequestId) AS K
               FROM dbo.LeaveRequests AS a
               JOIN dbo.LeaveRequests AS b ON b.EmployeeId = a.EmployeeId AND b.LeaveRequestId > a.LeaveRequestId
                                          AND b.StartDate <= a.EndDate AND b.EndDate >= a.StartDate
               JOIN dbo.Employees AS e ON e.EmployeeId = a.EmployeeId
               WHERE a.Status IN ('PENDING', 'APPROVED') AND b.Status IN ('PENDING', 'APPROVED'))
    INSERT INTO @r SELECT 'DV06', N'Đơn nghỉ phép chồng lấn ngày', 'ERROR', (SELECT COUNT(*) FROM x),
           (SELECT STRING_AGG(K, ', ') FROM (SELECT TOP 10 K FROM x ORDER BY K) s);

    -- DV07: TotalDays differs from the actual number of working days (e.g. holiday added later)
    WITH x AS (SELECT CONCAT('#', lr.LeaveRequestId) AS K
               FROM dbo.LeaveRequests AS lr
               CROSS APPLY (SELECT COUNT(*) AS Days FROM dbo.fn_WorkingDays(lr.StartDate, lr.EndDate)) AS wd
               WHERE lr.Status IN ('PENDING', 'APPROVED') AND lr.TotalDays <> wd.Days)
    INSERT INTO @r SELECT 'DV07', N'Số ngày nghỉ không khớp số ngày làm việc thực tế', 'WARNING', (SELECT COUNT(*) FROM x),
           (SELECT STRING_AGG(K, ', ') FROM (SELECT TOP 10 K FROM x ORDER BY K) s);

    -- DV08: leave balance out of sync with approved requests
    WITH x AS (SELECT CONCAT(e.EmployeeCode, '/', lb.BalanceYear) AS K
               FROM dbo.LeaveBalances AS lb
               JOIN dbo.Employees AS e ON e.EmployeeId = lb.EmployeeId
               OUTER APPLY (SELECT SUM(lr.TotalDays) AS Days FROM dbo.LeaveRequests AS lr
                            WHERE lr.EmployeeId = lb.EmployeeId AND lr.LeaveTypeId = lb.LeaveTypeId AND lr.Status = 'APPROVED'
                              AND lr.StartDate >= DATEFROMPARTS(lb.BalanceYear, 1, 1)
                              AND lr.StartDate <  DATEFROMPARTS(lb.BalanceYear + 1, 1, 1)) AS u
               WHERE lb.UsedDays <> ISNULL(u.Days, 0))
    INSERT INTO @r SELECT 'DV08', N'Số ngày phép đã dùng lệch với đơn đã duyệt', 'ERROR', (SELECT COUNT(*) FROM x),
           (SELECT STRING_AGG(K, ', ') FROM (SELECT TOP 10 K FROM x ORDER BY K) s);

    -- DV09: attendance outside the employment period
    WITH x AS (SELECT CONCAT(e.EmployeeCode, '@', CONVERT(CHAR(10), a.WorkDate, 23)) AS K
               FROM dbo.Attendance AS a JOIN dbo.Employees AS e ON e.EmployeeId = a.EmployeeId
               WHERE a.WorkDate < e.HireDate OR (e.TerminationDate IS NOT NULL AND a.WorkDate > e.TerminationDate))
    INSERT INTO @r SELECT 'DV09', N'Chấm công ngoài thời gian làm việc (trước ngày vào / sau ngày nghỉ)', 'ERROR',
           (SELECT COUNT(*) FROM x), (SELECT STRING_AGG(K, ', ') FROM (SELECT TOP 10 K FROM x ORDER BY K) s);

    -- DV10: attendance on a day covered by approved leave
    WITH x AS (SELECT CONCAT(e.EmployeeCode, '@', CONVERT(CHAR(10), a.WorkDate, 23)) AS K
               FROM dbo.Attendance AS a
               JOIN dbo.Employees AS e ON e.EmployeeId = a.EmployeeId
               WHERE a.Status <> 'ABSENT' AND EXISTS (
                     SELECT 1 FROM dbo.LeaveRequests AS lr
                     WHERE lr.EmployeeId = a.EmployeeId AND lr.Status = 'APPROVED'
                       AND a.WorkDate BETWEEN lr.StartDate AND lr.EndDate))
    INSERT INTO @r SELECT 'DV10', N'Có chấm công trong ngày đã được duyệt nghỉ', 'WARNING', (SELECT COUNT(*) FROM x),
           (SELECT STRING_AGG(K, ', ') FROM (SELECT TOP 10 K FROM x ORDER BY K) s);

    -- DV11: missing check-out on past days
    WITH x AS (SELECT CONCAT(e.EmployeeCode, '@', CONVERT(CHAR(10), a.WorkDate, 23)) AS K
               FROM dbo.Attendance AS a JOIN dbo.Employees AS e ON e.EmployeeId = a.EmployeeId
               WHERE a.WorkDate < @Today AND a.CheckIn IS NOT NULL AND a.CheckOut IS NULL)
    INSERT INTO @r SELECT 'DV11', N'Thiếu giờ check-out (ngày đã qua)', 'WARNING', (SELECT COUNT(*) FROM x),
           (SELECT STRING_AGG(K, ', ') FROM (SELECT TOP 10 K FROM x ORDER BY K) s);

    -- DV12: terminated employees with an active login
    WITH x AS (SELECT e.EmployeeCode AS K FROM dbo.Employees AS e JOIN dbo.Users AS u ON u.EmployeeId = e.EmployeeId
               WHERE e.Status = 'TERMINATED' AND u.IsActive = 1)
    INSERT INTO @r SELECT 'DV12', N'Nhân viên đã nghỉ việc nhưng tài khoản còn hoạt động', 'ERROR', (SELECT COUNT(*) FROM x),
           (SELECT STRING_AGG(K, ', ') FROM (SELECT TOP 10 K FROM x ORDER BY K) s);

    -- DV13: management cycles (A manages B manages … A) – recursive CTE with depth guard
    WITH Chain AS (
        SELECT e.EmployeeId AS StartId, e.ManagerId AS CurrentId, 1 AS Depth
        FROM dbo.Employees AS e WHERE e.ManagerId IS NOT NULL
        UNION ALL
        SELECT c.StartId, m.ManagerId, c.Depth + 1
        FROM Chain AS c JOIN dbo.Employees AS m ON m.EmployeeId = c.CurrentId
        WHERE m.ManagerId IS NOT NULL AND c.Depth < 50 AND c.CurrentId <> c.StartId
    ), x AS (SELECT DISTINCT e.EmployeeCode AS K FROM Chain AS c JOIN dbo.Employees AS e ON e.EmployeeId = c.StartId
             WHERE c.CurrentId = c.StartId)
    INSERT INTO @r SELECT 'DV13', N'Vòng lặp trong quan hệ quản lý', 'ERROR', (SELECT COUNT(*) FROM x),
           (SELECT STRING_AGG(K, ', ') FROM (SELECT TOP 10 K FROM x ORDER BY K) s);

    -- DV14: active employees reporting to a terminated manager
    WITH x AS (SELECT e.EmployeeCode AS K FROM dbo.Employees AS e JOIN dbo.Employees AS m ON m.EmployeeId = e.ManagerId
               WHERE e.Status <> 'TERMINATED' AND m.Status = 'TERMINATED')
    INSERT INTO @r SELECT 'DV14', N'Nhân viên có quản lý trực tiếp đã nghỉ việc', 'WARNING', (SELECT COUNT(*) FROM x),
           (SELECT STRING_AGG(K, ', ') FROM (SELECT TOP 10 K FROM x ORDER BY K) s);

    -- DV15: payslip arithmetic (net = gross − insurance − tax, nothing negative)
    WITH x AS (SELECT CONCAT(pp.PeriodYear, '-', FORMAT(pp.PeriodMonth, '00'), '/', e.EmployeeCode) AS K
               FROM dbo.Payroll AS p
               JOIN dbo.PayrollPeriods AS pp ON pp.PeriodId = p.PeriodId
               JOIN dbo.Employees AS e ON e.EmployeeId = p.EmployeeId
               WHERE p.NetSalary < 0 OR p.GrossSalary < 0
                  OR p.NetSalary <> p.GrossSalary - p.TotalInsurance - p.PersonalIncomeTax)
    INSERT INTO @r SELECT 'DV15', N'Bảng lương sai công thức hoặc có số âm', 'ERROR', (SELECT COUNT(*) FROM x),
           (SELECT STRING_AGG(K, ', ') FROM (SELECT TOP 10 K FROM x ORDER BY K) s);

    -- DV16: active review criteria weights must total 100
    DECLARE @Weight DECIMAL(9,2) = (SELECT ISNULL(SUM(Weight), 0) FROM dbo.ReviewCriteria WHERE IsActive = 1);
    INSERT INTO @r SELECT 'DV16', N'Tổng trọng số tiêu chí đánh giá khác 100', 'ERROR',
           CASE WHEN @Weight = 100 THEN 0 ELSE 1 END,
           CASE WHEN @Weight = 100 THEN NULL ELSE CONCAT(N'Tổng = ', @Weight) END;

    SELECT CheckCode, CheckName, Severity, ViolationCount,
           CASE WHEN ViolationCount = 0 THEN 'PASS' WHEN Severity = 'ERROR' THEN 'FAIL' ELSE 'WARN' END AS Result,
           SampleKeys
    FROM @r
    ORDER BY CheckCode;
END;
GO
