/* =============================================================================
   HRM System – schema: tables, keys, constraints, indexes
   Target : Microsoft SQL Server 2019+ (tested on 2022)
   Run in : database HRM   (sqlcmd -d HRM -i db/schema.sql)
   ---------------------------------------------------------------------------
   WARNING: this script DROPS every user object in the current database before
   re-creating the schema. It is meant for a dedicated HRM database only.
   Execution order of the full setup: see db/init-db.sh
   ---------------------------------------------------------------------------
   Conventions
     * PK  : <Table>Id INT/BIGINT IDENTITY, constraint PK_<Table>
     * FK  : FK_<Child>_<Parent>, UNIQUE: UQ_/UX_, CHECK: CK_, DEFAULT: DF_
     * Money: DECIMAL(18,2) VND; dates: DATE; event timestamps: DATETIME2 UTC
       (SYSUTCDATETIME()); attendance check-in/out: local company time.
   ============================================================================= */
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

/* -----------------------------------------------------------------------------
   0. Clean slate – drop views, procedures, functions, FKs, tables
   ----------------------------------------------------------------------------- */
DECLARE @sql NVARCHAR(MAX) = N'';
SELECT @sql += N'DROP VIEW ' + QUOTENAME(SCHEMA_NAME(schema_id)) + N'.' + QUOTENAME(name) + N';'
FROM sys.views WHERE is_ms_shipped = 0;
SELECT @sql += N'DROP PROCEDURE ' + QUOTENAME(SCHEMA_NAME(schema_id)) + N'.' + QUOTENAME(name) + N';'
FROM sys.procedures WHERE is_ms_shipped = 0;
SELECT @sql += N'DROP FUNCTION ' + QUOTENAME(SCHEMA_NAME(schema_id)) + N'.' + QUOTENAME(name) + N';'
FROM sys.objects WHERE type IN ('FN', 'IF', 'TF') AND is_ms_shipped = 0;
SELECT @sql += N'ALTER TABLE ' + QUOTENAME(OBJECT_SCHEMA_NAME(parent_object_id)) + N'.'
             + QUOTENAME(OBJECT_NAME(parent_object_id)) + N' DROP CONSTRAINT ' + QUOTENAME(name) + N';'
FROM sys.foreign_keys;
SELECT @sql += N'DROP TABLE ' + QUOTENAME(SCHEMA_NAME(schema_id)) + N'.' + QUOTENAME(name) + N';'
FROM sys.tables WHERE is_ms_shipped = 0;
EXEC sys.sp_executesql @sql;
GO

/* -----------------------------------------------------------------------------
   1. Security: Roles, Users, UserRoles  (Users.EmployeeId FK added in section 3)
   ----------------------------------------------------------------------------- */
CREATE TABLE dbo.Roles (
    RoleId      INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Roles PRIMARY KEY,
    RoleCode    VARCHAR(20)   NOT NULL CONSTRAINT UQ_Roles_RoleCode UNIQUE,
    RoleName    NVARCHAR(100) NOT NULL,
    Description NVARCHAR(255) NULL
);

CREATE TABLE dbo.Users (
    UserId             INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Users PRIMARY KEY,
    Username           VARCHAR(50)  NOT NULL CONSTRAINT UQ_Users_Username UNIQUE,
    PasswordHash       CHAR(60)     NOT NULL,
    EmployeeId         INT          NULL,
    IsActive           BIT          NOT NULL CONSTRAINT DF_Users_IsActive DEFAULT (1),
    MustChangePassword BIT          NOT NULL CONSTRAINT DF_Users_MustChange DEFAULT (0),
    FailedLoginCount   INT          NOT NULL CONSTRAINT DF_Users_Failed DEFAULT (0),
    LockedUntil        DATETIME2(0) NULL,
    LastLoginAt        DATETIME2(0) NULL,
    CreatedAt          DATETIME2(0) NOT NULL CONSTRAINT DF_Users_CreatedAt DEFAULT (SYSUTCDATETIME()),
    UpdatedAt          DATETIME2(0) NULL,
    CONSTRAINT CK_Users_Username     CHECK (LEN(Username) >= 2 AND Username NOT LIKE '%[^a-zA-Z0-9._-]%'),
    -- Defence in depth: only BCrypt hashes can ever be stored, never plain text.
    CONSTRAINT CK_Users_PasswordHash CHECK (PasswordHash LIKE '$2[aby]$[0-9][0-9]$%'),
    CONSTRAINT CK_Users_FailedLogin  CHECK (FailedLoginCount >= 0)
);

CREATE TABLE dbo.UserRoles (
    UserId     INT          NOT NULL,
    RoleId     INT          NOT NULL,
    AssignedAt DATETIME2(0) NOT NULL CONSTRAINT DF_UserRoles_AssignedAt DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_UserRoles PRIMARY KEY (UserId, RoleId),
    CONSTRAINT FK_UserRoles_Users FOREIGN KEY (UserId) REFERENCES dbo.Users (UserId) ON DELETE CASCADE,
    CONSTRAINT FK_UserRoles_Roles FOREIGN KEY (RoleId) REFERENCES dbo.Roles (RoleId)
);
CREATE INDEX IX_UserRoles_RoleId ON dbo.UserRoles (RoleId);

/* -----------------------------------------------------------------------------
   2. Organisation: Departments, Positions
   ----------------------------------------------------------------------------- */
CREATE TABLE dbo.Departments (
    DepartmentId   INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Departments PRIMARY KEY,
    DepartmentCode VARCHAR(20)   NOT NULL CONSTRAINT UQ_Departments_Code UNIQUE,
    DepartmentName NVARCHAR(150) NOT NULL CONSTRAINT UQ_Departments_Name UNIQUE,
    Description    NVARCHAR(500) NULL,
    ManagerId      INT           NULL,      -- FK to Employees, added after Employees exists
    IsActive       BIT           NOT NULL CONSTRAINT DF_Departments_IsActive DEFAULT (1),
    CreatedAt      DATETIME2(0)  NOT NULL CONSTRAINT DF_Departments_CreatedAt DEFAULT (SYSUTCDATETIME()),
    UpdatedAt      DATETIME2(0)  NULL,
    CONSTRAINT CK_Departments_Code CHECK (LEN(DepartmentCode) >= 2
        AND DepartmentCode COLLATE Latin1_General_BIN2 NOT LIKE '%[^A-Z0-9_-]%')
);

CREATE TABLE dbo.Positions (
    PositionId   INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Positions PRIMARY KEY,
    PositionCode VARCHAR(20)   NOT NULL CONSTRAINT UQ_Positions_Code UNIQUE,
    PositionName NVARCHAR(150) NOT NULL,
    JobLevel     TINYINT       NOT NULL CONSTRAINT DF_Positions_JobLevel DEFAULT (1),
    MinSalary    DECIMAL(18,2) NOT NULL CONSTRAINT DF_Positions_MinSalary DEFAULT (0),
    MaxSalary    DECIMAL(18,2) NOT NULL,
    Description  NVARCHAR(500) NULL,
    IsActive     BIT           NOT NULL CONSTRAINT DF_Positions_IsActive DEFAULT (1),
    CreatedAt    DATETIME2(0)  NOT NULL CONSTRAINT DF_Positions_CreatedAt DEFAULT (SYSUTCDATETIME()),
    UpdatedAt    DATETIME2(0)  NULL,
    CONSTRAINT CK_Positions_Code        CHECK (LEN(PositionCode) >= 2
        AND PositionCode COLLATE Latin1_General_BIN2 NOT LIKE '%[^A-Z0-9_-]%'),
    CONSTRAINT CK_Positions_JobLevel    CHECK (JobLevel BETWEEN 1 AND 10),
    CONSTRAINT CK_Positions_SalaryRange CHECK (MinSalary >= 0 AND MaxSalary >= MinSalary)
);

/* -----------------------------------------------------------------------------
   3. Employees
   ----------------------------------------------------------------------------- */
CREATE TABLE dbo.Employees (
    EmployeeId      INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Employees PRIMARY KEY,
    EmployeeCode    VARCHAR(20)   NOT NULL CONSTRAINT UQ_Employees_EmployeeCode UNIQUE,
    FullName        NVARCHAR(150) NOT NULL,
    Gender          CHAR(1)       NOT NULL,
    DateOfBirth     DATE          NULL,
    Email           VARCHAR(150)  NOT NULL CONSTRAINT UQ_Employees_Email UNIQUE,
    Phone           VARCHAR(20)   NULL,
    Address         NVARCHAR(300) NULL,
    NationalId      VARCHAR(12)   NULL,     -- CCCD, unique when present (filtered index below)
    TaxCode         VARCHAR(14)   NULL,
    BankAccount     VARCHAR(30)   NULL,
    BankName        NVARCHAR(100) NULL,
    DepartmentId    INT           NOT NULL,
    PositionId      INT           NOT NULL,
    ManagerId       INT           NULL,
    HireDate        DATE          NOT NULL,
    TerminationDate DATE          NULL,
    Status          VARCHAR(20)   NOT NULL CONSTRAINT DF_Employees_Status DEFAULT ('PROBATION'),
    BaseSalary      DECIMAL(18,2) NOT NULL,
    Allowance       DECIMAL(18,2) NOT NULL CONSTRAINT DF_Employees_Allowance DEFAULT (0),
    Dependents      TINYINT       NOT NULL CONSTRAINT DF_Employees_Dependents DEFAULT (0),
    CreatedAt       DATETIME2(0)  NOT NULL CONSTRAINT DF_Employees_CreatedAt DEFAULT (SYSUTCDATETIME()),
    UpdatedAt       DATETIME2(0)  NULL,
    CONSTRAINT FK_Employees_Departments FOREIGN KEY (DepartmentId) REFERENCES dbo.Departments (DepartmentId),
    CONSTRAINT FK_Employees_Positions   FOREIGN KEY (PositionId)   REFERENCES dbo.Positions (PositionId),
    CONSTRAINT FK_Employees_Manager     FOREIGN KEY (ManagerId)    REFERENCES dbo.Employees (EmployeeId),
    CONSTRAINT CK_Employees_Code        CHECK (LEN(EmployeeCode) >= 3
        AND EmployeeCode COLLATE Latin1_General_BIN2 NOT LIKE '%[^A-Z0-9-]%'),
    CONSTRAINT CK_Employees_Gender      CHECK (Gender IN ('M', 'F', 'O')),
    CONSTRAINT CK_Employees_Email       CHECK (Email LIKE '%_@_%._%' AND Email NOT LIKE '% %'),
    CONSTRAINT CK_Employees_NationalId  CHECK (NationalId IS NULL OR (LEN(NationalId) = 12 AND NationalId NOT LIKE '%[^0-9]%')),
    CONSTRAINT CK_Employees_Status      CHECK (Status IN ('PROBATION', 'ACTIVE', 'ON_LEAVE', 'TERMINATED')),
    CONSTRAINT CK_Employees_Salary      CHECK (BaseSalary >= 0 AND Allowance >= 0),
    CONSTRAINT CK_Employees_Dependents  CHECK (Dependents <= 20),
    CONSTRAINT CK_Employees_MinAge      CHECK (DateOfBirth IS NULL OR DateOfBirth <= DATEADD(YEAR, -15, HireDate)),
    CONSTRAINT CK_Employees_TermDate    CHECK (TerminationDate IS NULL OR TerminationDate >= HireDate),
    CONSTRAINT CK_Employees_Terminated  CHECK (Status <> 'TERMINATED' OR TerminationDate IS NOT NULL),
    CONSTRAINT CK_Employees_SelfManager CHECK (ManagerId IS NULL OR ManagerId <> EmployeeId)
);
CREATE UNIQUE INDEX UX_Employees_NationalId ON dbo.Employees (NationalId) WHERE NationalId IS NOT NULL;
CREATE INDEX IX_Employees_DepartmentId ON dbo.Employees (DepartmentId) INCLUDE (Status, PositionId);
CREATE INDEX IX_Employees_PositionId   ON dbo.Employees (PositionId);
CREATE INDEX IX_Employees_ManagerId    ON dbo.Employees (ManagerId) WHERE ManagerId IS NOT NULL;
CREATE INDEX IX_Employees_FullName     ON dbo.Employees (FullName);
CREATE INDEX IX_Employees_Status       ON dbo.Employees (Status) INCLUDE (DepartmentId, HireDate, TerminationDate);

ALTER TABLE dbo.Departments ADD CONSTRAINT FK_Departments_Manager
    FOREIGN KEY (ManagerId) REFERENCES dbo.Employees (EmployeeId);
CREATE INDEX IX_Departments_ManagerId ON dbo.Departments (ManagerId) WHERE ManagerId IS NOT NULL;

-- Deleting an employee (only allowed when no business data exists) also removes the login.
ALTER TABLE dbo.Users ADD CONSTRAINT FK_Users_Employees
    FOREIGN KEY (EmployeeId) REFERENCES dbo.Employees (EmployeeId) ON DELETE CASCADE;
CREATE UNIQUE INDEX UX_Users_EmployeeId ON dbo.Users (EmployeeId) WHERE EmployeeId IS NOT NULL;

CREATE TABLE dbo.SalaryHistory (
    SalaryHistoryId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SalaryHistory PRIMARY KEY,
    EmployeeId      INT           NOT NULL,
    OldBaseSalary   DECIMAL(18,2) NULL,
    NewBaseSalary   DECIMAL(18,2) NOT NULL,
    OldAllowance    DECIMAL(18,2) NULL,
    NewAllowance    DECIMAL(18,2) NOT NULL,
    ChangedBy       VARCHAR(50)   NULL,
    ChangedAt       DATETIME2(0)  NOT NULL CONSTRAINT DF_SalaryHistory_ChangedAt DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_SalaryHistory_Employees FOREIGN KEY (EmployeeId) REFERENCES dbo.Employees (EmployeeId) ON DELETE CASCADE
);
CREATE INDEX IX_SalaryHistory_Employee ON dbo.SalaryHistory (EmployeeId, ChangedAt DESC);

/* -----------------------------------------------------------------------------
   4. Attendance & leave
   ----------------------------------------------------------------------------- */
CREATE TABLE dbo.Holidays (
    HolidayDate DATE          NOT NULL CONSTRAINT PK_Holidays PRIMARY KEY,
    HolidayName NVARCHAR(150) NOT NULL
);

CREATE TABLE dbo.Attendance (
    AttendanceId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Attendance PRIMARY KEY,
    EmployeeId   INT           NOT NULL,
    WorkDate     DATE          NOT NULL,
    CheckIn      DATETIME2(0)  NULL,
    CheckOut     DATETIME2(0)  NULL,
    Status       VARCHAR(20)   NOT NULL,
    LateMinutes  SMALLINT      NOT NULL CONSTRAINT DF_Attendance_LateMinutes DEFAULT (0),
    WorkHours    AS CAST(CASE WHEN CheckIn IS NOT NULL AND CheckOut IS NOT NULL
                              THEN DATEDIFF(MINUTE, CheckIn, CheckOut) / 60.0 ELSE 0 END AS DECIMAL(5,2)) PERSISTED,
    Source       VARCHAR(10)   NOT NULL CONSTRAINT DF_Attendance_Source DEFAULT ('WEB'),
    Note         NVARCHAR(500) NULL,
    CreatedAt    DATETIME2(0)  NOT NULL CONSTRAINT DF_Attendance_CreatedAt DEFAULT (SYSUTCDATETIME()),
    UpdatedAt    DATETIME2(0)  NULL,
    CONSTRAINT FK_Attendance_Employees  FOREIGN KEY (EmployeeId) REFERENCES dbo.Employees (EmployeeId),
    CONSTRAINT UQ_Attendance_EmployeeDate UNIQUE (EmployeeId, WorkDate),
    CONSTRAINT CK_Attendance_Status     CHECK (Status IN ('PRESENT', 'LATE', 'HALF_DAY', 'ABSENT')),
    CONSTRAINT CK_Attendance_CheckOrder CHECK (CheckOut IS NULL OR (CheckIn IS NOT NULL AND CheckOut > CheckIn)),
    CONSTRAINT CK_Attendance_SameDay    CHECK (CheckIn IS NULL OR CAST(CheckIn AS DATE) = WorkDate),
    CONSTRAINT CK_Attendance_Late       CHECK (LateMinutes >= 0),
    CONSTRAINT CK_Attendance_Source     CHECK (Source IN ('WEB', 'API', 'MANUAL', 'IMPORT'))
);
-- Covering index for month/date-range reports across all employees (see docs/03-query-optimization.md)
CREATE INDEX IX_Attendance_WorkDate ON dbo.Attendance (WorkDate)
    INCLUDE (EmployeeId, Status, LateMinutes, WorkHours);

CREATE TABLE dbo.LeaveTypes (
    LeaveTypeId       INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_LeaveTypes PRIMARY KEY,
    LeaveTypeCode     VARCHAR(20)   NOT NULL CONSTRAINT UQ_LeaveTypes_Code UNIQUE,
    LeaveTypeName     NVARCHAR(100) NOT NULL,
    IsPaid            BIT           NOT NULL,   -- company pays salary for these days
    RequiresBalance   BIT           NOT NULL CONSTRAINT DF_LeaveTypes_RequiresBalance DEFAULT (0),
    MaxDaysPerRequest DECIMAL(5,1)  NULL,
    IsActive          BIT           NOT NULL CONSTRAINT DF_LeaveTypes_IsActive DEFAULT (1),
    CONSTRAINT CK_LeaveTypes_MaxDays CHECK (MaxDaysPerRequest IS NULL OR MaxDaysPerRequest > 0)
);

CREATE TABLE dbo.LeaveBalances (
    EmployeeId    INT          NOT NULL,
    LeaveTypeId   INT          NOT NULL,
    BalanceYear   SMALLINT     NOT NULL,
    EntitledDays  DECIMAL(5,1) NOT NULL,
    UsedDays      DECIMAL(5,1) NOT NULL CONSTRAINT DF_LeaveBalances_Used DEFAULT (0),
    RemainingDays AS (EntitledDays - UsedDays) PERSISTED,
    UpdatedAt     DATETIME2(0) NULL,
    CONSTRAINT PK_LeaveBalances PRIMARY KEY (EmployeeId, LeaveTypeId, BalanceYear),
    CONSTRAINT FK_LeaveBalances_Employees  FOREIGN KEY (EmployeeId)  REFERENCES dbo.Employees (EmployeeId),
    CONSTRAINT FK_LeaveBalances_LeaveTypes FOREIGN KEY (LeaveTypeId) REFERENCES dbo.LeaveTypes (LeaveTypeId),
    -- Last line of defence: approved leave can never exceed the entitlement.
    CONSTRAINT CK_LeaveBalances_Days CHECK (EntitledDays >= 0 AND UsedDays >= 0 AND UsedDays <= EntitledDays)
);

CREATE TABLE dbo.LeaveRequests (
    LeaveRequestId  INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_LeaveRequests PRIMARY KEY,
    EmployeeId      INT           NOT NULL,
    LeaveTypeId     INT           NOT NULL,
    StartDate       DATE          NOT NULL,
    EndDate         DATE          NOT NULL,
    TotalDays       DECIMAL(5,1)  NOT NULL,   -- working days, computed by the application
    Reason          NVARCHAR(500) NOT NULL,
    Status          VARCHAR(20)   NOT NULL CONSTRAINT DF_LeaveRequests_Status DEFAULT ('PENDING'),
    ApproverId      INT           NULL,
    DecidedAt       DATETIME2(0)  NULL,
    ApproverComment NVARCHAR(500) NULL,
    CreatedAt       DATETIME2(0)  NOT NULL CONSTRAINT DF_LeaveRequests_CreatedAt DEFAULT (SYSUTCDATETIME()),
    UpdatedAt       DATETIME2(0)  NULL,
    CONSTRAINT FK_LeaveRequests_Employees  FOREIGN KEY (EmployeeId)  REFERENCES dbo.Employees (EmployeeId),
    CONSTRAINT FK_LeaveRequests_LeaveTypes FOREIGN KEY (LeaveTypeId) REFERENCES dbo.LeaveTypes (LeaveTypeId),
    CONSTRAINT FK_LeaveRequests_Approver   FOREIGN KEY (ApproverId)  REFERENCES dbo.Employees (EmployeeId),
    CONSTRAINT CK_LeaveRequests_Dates     CHECK (EndDate >= StartDate AND YEAR(EndDate) = YEAR(StartDate)),
    CONSTRAINT CK_LeaveRequests_TotalDays CHECK (TotalDays > 0),
    CONSTRAINT CK_LeaveRequests_Status    CHECK (Status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT CK_LeaveRequests_Decision  CHECK (Status IN ('PENDING', 'CANCELLED')
        OR (ApproverId IS NOT NULL AND DecidedAt IS NOT NULL)),
    CONSTRAINT CK_LeaveRequests_NoSelfApproval CHECK (ApproverId IS NULL OR ApproverId <> EmployeeId)
);
-- Overlap check & "my requests" – seek on employee, range on dates
CREATE INDEX IX_LeaveRequests_Employee_Dates ON dbo.LeaveRequests (EmployeeId, StartDate, EndDate)
    INCLUDE (Status, LeaveTypeId, TotalDays);
-- Approval queue only ever looks at PENDING rows – a small filtered index.
-- The filter column Status is INCLUDEd on purpose: without it the optimiser ignored
-- the index for COUNT/MIN queries (measured in docs/03-query-optimization.md, case 4).
CREATE INDEX IX_LeaveRequests_Pending ON dbo.LeaveRequests (StartDate)
    INCLUDE (EmployeeId, LeaveTypeId, Status) WHERE Status = 'PENDING';
CREATE INDEX IX_LeaveRequests_LeaveTypeId ON dbo.LeaveRequests (LeaveTypeId);

/* -----------------------------------------------------------------------------
   5. Payroll
   ----------------------------------------------------------------------------- */
CREATE TABLE dbo.PayrollSettings (
    SettingKey   VARCHAR(50)   NOT NULL CONSTRAINT PK_PayrollSettings PRIMARY KEY,
    SettingValue DECIMAL(18,4) NOT NULL,
    Description  NVARCHAR(255) NOT NULL,
    UpdatedAt    DATETIME2(0)  NULL,
    CONSTRAINT CK_PayrollSettings_Value CHECK (SettingValue >= 0)
);

CREATE TABLE dbo.TaxBrackets (
    BracketNo  TINYINT       NOT NULL CONSTRAINT PK_TaxBrackets PRIMARY KEY,
    LowerBound DECIMAL(18,2) NOT NULL,
    UpperBound DECIMAL(18,2) NULL,      -- NULL = no upper limit
    Rate       DECIMAL(5,4)  NOT NULL,
    CONSTRAINT CK_TaxBrackets_Bounds CHECK (LowerBound >= 0 AND (UpperBound IS NULL OR UpperBound > LowerBound)),
    CONSTRAINT CK_TaxBrackets_Rate   CHECK (Rate > 0 AND Rate < 1)
);

CREATE TABLE dbo.PayrollPeriods (
    PeriodId         INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_PayrollPeriods PRIMARY KEY,
    PeriodYear       SMALLINT      NOT NULL,
    PeriodMonth      TINYINT       NOT NULL,
    StandardWorkDays DECIMAL(4,1)  NULL,
    Status           VARCHAR(20)   NOT NULL CONSTRAINT DF_PayrollPeriods_Status DEFAULT ('OPEN'),
    EmployeeCount    INT           NULL,
    TotalGross       DECIMAL(18,2) NULL,
    TotalNet         DECIMAL(18,2) NULL,
    CalculatedAt     DATETIME2(0)  NULL,
    CalculatedBy     VARCHAR(50)   NULL,
    ApprovedAt       DATETIME2(0)  NULL,
    ApprovedBy       VARCHAR(50)   NULL,
    PaidAt           DATETIME2(0)  NULL,
    CreatedAt        DATETIME2(0)  NOT NULL CONSTRAINT DF_PayrollPeriods_CreatedAt DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT UQ_PayrollPeriods_YearMonth UNIQUE (PeriodYear, PeriodMonth),
    CONSTRAINT CK_PayrollPeriods_Month  CHECK (PeriodMonth BETWEEN 1 AND 12),
    CONSTRAINT CK_PayrollPeriods_Year   CHECK (PeriodYear BETWEEN 2000 AND 2100),
    CONSTRAINT CK_PayrollPeriods_Status CHECK (Status IN ('OPEN', 'CALCULATED', 'APPROVED', 'PAID'))
);

CREATE TABLE dbo.Payroll (
    PayrollId             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Payroll PRIMARY KEY,
    PeriodId              INT           NOT NULL,
    EmployeeId            INT           NOT NULL,
    DepartmentId          INT           NOT NULL,   -- snapshot at calculation time
    BaseSalary            DECIMAL(18,2) NOT NULL,
    Allowance             DECIMAL(18,2) NOT NULL,
    Dependents            TINYINT       NOT NULL,
    StandardDays          DECIMAL(4,1)  NOT NULL,
    WorkedDays            DECIMAL(4,1)  NOT NULL,
    PaidLeaveDays         DECIMAL(4,1)  NOT NULL,
    PaidDays              DECIMAL(4,1)  NOT NULL,
    UnpaidDays            DECIMAL(4,1)  NOT NULL,
    SalaryAmount          DECIMAL(18,2) NOT NULL,   -- base salary prorated by paid days
    AllowanceAmount       DECIMAL(18,2) NOT NULL,
    GrossSalary           DECIMAL(18,2) NOT NULL,
    SocialInsurance       DECIMAL(18,2) NOT NULL,
    HealthInsurance       DECIMAL(18,2) NOT NULL,
    UnemploymentInsurance DECIMAL(18,2) NOT NULL,
    TotalInsurance        AS (SocialInsurance + HealthInsurance + UnemploymentInsurance) PERSISTED,
    PersonalDeduction     DECIMAL(18,2) NOT NULL,
    DependentDeduction    DECIMAL(18,2) NOT NULL,
    TaxableIncome         DECIMAL(18,2) NOT NULL,
    PersonalIncomeTax     DECIMAL(18,2) NOT NULL,
    NetSalary             DECIMAL(18,2) NOT NULL,
    CreatedAt             DATETIME2(0)  NOT NULL CONSTRAINT DF_Payroll_CreatedAt DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_Payroll_PayrollPeriods FOREIGN KEY (PeriodId) REFERENCES dbo.PayrollPeriods (PeriodId) ON DELETE CASCADE,
    CONSTRAINT FK_Payroll_Employees      FOREIGN KEY (EmployeeId) REFERENCES dbo.Employees (EmployeeId),
    CONSTRAINT FK_Payroll_Departments    FOREIGN KEY (DepartmentId) REFERENCES dbo.Departments (DepartmentId),
    CONSTRAINT UQ_Payroll_PeriodEmployee UNIQUE (PeriodId, EmployeeId),
    CONSTRAINT CK_Payroll_NonNegative CHECK (BaseSalary >= 0 AND Allowance >= 0 AND SalaryAmount >= 0
        AND AllowanceAmount >= 0 AND GrossSalary >= 0 AND SocialInsurance >= 0 AND HealthInsurance >= 0
        AND UnemploymentInsurance >= 0 AND TaxableIncome >= 0 AND PersonalIncomeTax >= 0 AND NetSalary >= 0),
    CONSTRAINT CK_Payroll_Days CHECK (WorkedDays >= 0 AND PaidLeaveDays >= 0 AND UnpaidDays >= 0
        AND PaidDays <= StandardDays),
    CONSTRAINT CK_Payroll_NetLeGross CHECK (NetSalary <= GrossSalary)
);
CREATE INDEX IX_Payroll_EmployeeId ON dbo.Payroll (EmployeeId) INCLUDE (PeriodId, GrossSalary, NetSalary);
CREATE INDEX IX_Payroll_DepartmentId ON dbo.Payroll (DepartmentId);

/* -----------------------------------------------------------------------------
   6. Recruitment
   ----------------------------------------------------------------------------- */
CREATE TABLE dbo.JobPostings (
    JobPostingId   INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_JobPostings PRIMARY KEY,
    JobCode        VARCHAR(20)   NOT NULL CONSTRAINT UQ_JobPostings_JobCode UNIQUE,
    Title          NVARCHAR(200) NOT NULL,
    DepartmentId   INT           NOT NULL,
    PositionId     INT           NOT NULL,
    Openings       SMALLINT      NOT NULL CONSTRAINT DF_JobPostings_Openings DEFAULT (1),
    EmploymentType VARCHAR(20)   NOT NULL CONSTRAINT DF_JobPostings_EmploymentType DEFAULT ('FULL_TIME'),
    Location       NVARCHAR(100) NULL,
    SalaryMin      DECIMAL(18,2) NULL,
    SalaryMax      DECIMAL(18,2) NULL,
    Description    NVARCHAR(MAX) NULL,
    Requirements   NVARCHAR(MAX) NULL,
    Status         VARCHAR(20)   NOT NULL CONSTRAINT DF_JobPostings_Status DEFAULT ('DRAFT'),
    OpenDate       DATE          NULL,
    CloseDate      DATE          NULL,
    CreatedBy      VARCHAR(50)   NULL,
    CreatedAt      DATETIME2(0)  NOT NULL CONSTRAINT DF_JobPostings_CreatedAt DEFAULT (SYSUTCDATETIME()),
    UpdatedAt      DATETIME2(0)  NULL,
    CONSTRAINT FK_JobPostings_Departments FOREIGN KEY (DepartmentId) REFERENCES dbo.Departments (DepartmentId),
    CONSTRAINT FK_JobPostings_Positions   FOREIGN KEY (PositionId)   REFERENCES dbo.Positions (PositionId),
    CONSTRAINT CK_JobPostings_Code     CHECK (JobCode COLLATE Latin1_General_BIN2 NOT LIKE '%[^A-Z0-9-]%'),
    CONSTRAINT CK_JobPostings_Openings CHECK (Openings > 0),
    CONSTRAINT CK_JobPostings_Type     CHECK (EmploymentType IN ('FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERN')),
    CONSTRAINT CK_JobPostings_Status   CHECK (Status IN ('DRAFT', 'OPEN', 'ON_HOLD', 'CLOSED')),
    CONSTRAINT CK_JobPostings_Salary   CHECK ((SalaryMin IS NULL OR SalaryMin >= 0) AND (SalaryMax IS NULL OR SalaryMax >= 0)
        AND (SalaryMin IS NULL OR SalaryMax IS NULL OR SalaryMax >= SalaryMin)),
    CONSTRAINT CK_JobPostings_Dates    CHECK (OpenDate IS NULL OR CloseDate IS NULL OR CloseDate >= OpenDate)
);
CREATE INDEX IX_JobPostings_Status     ON dbo.JobPostings (Status) INCLUDE (DepartmentId);
CREATE INDEX IX_JobPostings_Department ON dbo.JobPostings (DepartmentId);
CREATE INDEX IX_JobPostings_PositionId ON dbo.JobPostings (PositionId);

CREATE TABLE dbo.Candidates (
    CandidateId     INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Candidates PRIMARY KEY,
    JobPostingId    INT            NOT NULL,
    FullName        NVARCHAR(150)  NOT NULL,
    Email           VARCHAR(150)   NOT NULL,
    Phone           VARCHAR(20)    NULL,
    Source          VARCHAR(20)    NOT NULL CONSTRAINT DF_Candidates_Source DEFAULT ('WEBSITE'),
    ResumeUrl       NVARCHAR(500)  NULL,
    ExpectedSalary  DECIMAL(18,2)  NULL,
    Stage           VARCHAR(20)    NOT NULL CONSTRAINT DF_Candidates_Stage DEFAULT ('APPLIED'),
    Rating          TINYINT        NULL,
    InterviewAt     DATETIME2(0)   NULL,
    Notes           NVARCHAR(2000) NULL,
    AppliedDate     DATE           NOT NULL,
    HiredEmployeeId INT            NULL,
    CreatedAt       DATETIME2(0)   NOT NULL CONSTRAINT DF_Candidates_CreatedAt DEFAULT (SYSUTCDATETIME()),
    UpdatedAt       DATETIME2(0)   NULL,
    CONSTRAINT FK_Candidates_JobPostings FOREIGN KEY (JobPostingId) REFERENCES dbo.JobPostings (JobPostingId),
    CONSTRAINT FK_Candidates_Employees   FOREIGN KEY (HiredEmployeeId) REFERENCES dbo.Employees (EmployeeId),
    CONSTRAINT UQ_Candidates_JobEmail    UNIQUE (JobPostingId, Email),
    CONSTRAINT CK_Candidates_Stage  CHECK (Stage IN ('APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'HIRED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT CK_Candidates_Source CHECK (Source IN ('WEBSITE', 'LINKEDIN', 'TOPCV', 'VIETNAMWORKS', 'REFERRAL', 'OTHER')),
    CONSTRAINT CK_Candidates_Rating CHECK (Rating IS NULL OR Rating BETWEEN 1 AND 5),
    CONSTRAINT CK_Candidates_Salary CHECK (ExpectedSalary IS NULL OR ExpectedSalary >= 0),
    CONSTRAINT CK_Candidates_Email  CHECK (Email LIKE '%_@_%._%'),
    CONSTRAINT CK_Candidates_Hired  CHECK (Stage <> 'HIRED' OR HiredEmployeeId IS NOT NULL)
);
CREATE INDEX IX_Candidates_Stage ON dbo.Candidates (Stage) INCLUDE (JobPostingId);
CREATE INDEX IX_Candidates_HiredEmployee ON dbo.Candidates (HiredEmployeeId) WHERE HiredEmployeeId IS NOT NULL;

CREATE TABLE dbo.CandidateStageHistory (
    HistoryId   BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_CandidateStageHistory PRIMARY KEY,
    CandidateId INT          NOT NULL,
    FromStage   VARCHAR(20)  NULL,
    ToStage     VARCHAR(20)  NOT NULL,
    ChangedBy   VARCHAR(50)  NULL,
    ChangedAt   DATETIME2(0) NOT NULL CONSTRAINT DF_CandidateStageHistory_ChangedAt DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_CandidateStageHistory_Candidates FOREIGN KEY (CandidateId)
        REFERENCES dbo.Candidates (CandidateId) ON DELETE CASCADE
);
CREATE INDEX IX_CandidateStageHistory_Candidate ON dbo.CandidateStageHistory (CandidateId, ChangedAt);

/* -----------------------------------------------------------------------------
   7. Performance management
   ----------------------------------------------------------------------------- */
CREATE TABLE dbo.ReviewCycles (
    CycleId     INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ReviewCycles PRIMARY KEY,
    CycleName   NVARCHAR(150) NOT NULL CONSTRAINT UQ_ReviewCycles_Name UNIQUE,
    PeriodStart DATE          NOT NULL,
    PeriodEnd   DATE          NOT NULL,
    DueDate     DATE          NULL,
    SelfWeight  DECIMAL(3,2)  NOT NULL CONSTRAINT DF_ReviewCycles_SelfWeight DEFAULT (0.30),
    Status      VARCHAR(20)   NOT NULL CONSTRAINT DF_ReviewCycles_Status DEFAULT ('DRAFT'),
    CreatedBy   VARCHAR(50)   NULL,
    CreatedAt   DATETIME2(0)  NOT NULL CONSTRAINT DF_ReviewCycles_CreatedAt DEFAULT (SYSUTCDATETIME()),
    UpdatedAt   DATETIME2(0)  NULL,
    CONSTRAINT CK_ReviewCycles_Period CHECK (PeriodEnd > PeriodStart),
    CONSTRAINT CK_ReviewCycles_Due    CHECK (DueDate IS NULL OR DueDate >= PeriodStart),
    CONSTRAINT CK_ReviewCycles_Weight CHECK (SelfWeight BETWEEN 0 AND 1),
    CONSTRAINT CK_ReviewCycles_Status CHECK (Status IN ('DRAFT', 'OPEN', 'CLOSED'))
);

CREATE TABLE dbo.ReviewCriteria (
    CriteriaId   INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ReviewCriteria PRIMARY KEY,
    CriteriaName NVARCHAR(150) NOT NULL CONSTRAINT UQ_ReviewCriteria_Name UNIQUE,
    Description  NVARCHAR(500) NULL,
    Weight       DECIMAL(5,2)  NOT NULL,     -- percent; active criteria sum to 100
    DisplayOrder TINYINT       NOT NULL CONSTRAINT DF_ReviewCriteria_Order DEFAULT (0),
    IsActive     BIT           NOT NULL CONSTRAINT DF_ReviewCriteria_IsActive DEFAULT (1),
    CONSTRAINT CK_ReviewCriteria_Weight CHECK (Weight > 0 AND Weight <= 100)
);

CREATE TABLE dbo.PerformanceReviews (
    ReviewId           INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_PerformanceReviews PRIMARY KEY,
    CycleId            INT            NOT NULL,
    EmployeeId         INT            NOT NULL,
    ReviewerId         INT            NULL,
    Status             VARCHAR(20)    NOT NULL CONSTRAINT DF_PerformanceReviews_Status DEFAULT ('PENDING_SELF'),
    SelfScore          DECIMAL(3,2)   NULL,
    ManagerScore       DECIMAL(3,2)   NULL,
    FinalScore         DECIMAL(3,2)   NULL,
    Rating             CHAR(1)        NULL,
    SelfComment        NVARCHAR(2000) NULL,
    ManagerComment     NVARCHAR(2000) NULL,
    SelfSubmittedAt    DATETIME2(0)   NULL,
    ManagerSubmittedAt DATETIME2(0)   NULL,
    CreatedAt          DATETIME2(0)   NOT NULL CONSTRAINT DF_PerformanceReviews_CreatedAt DEFAULT (SYSUTCDATETIME()),
    UpdatedAt          DATETIME2(0)   NULL,
    CONSTRAINT FK_PerformanceReviews_Cycles    FOREIGN KEY (CycleId)    REFERENCES dbo.ReviewCycles (CycleId) ON DELETE CASCADE,
    CONSTRAINT FK_PerformanceReviews_Employees FOREIGN KEY (EmployeeId) REFERENCES dbo.Employees (EmployeeId),
    CONSTRAINT FK_PerformanceReviews_Reviewer  FOREIGN KEY (ReviewerId) REFERENCES dbo.Employees (EmployeeId),
    CONSTRAINT UQ_PerformanceReviews_CycleEmployee UNIQUE (CycleId, EmployeeId),
    CONSTRAINT CK_PerformanceReviews_Status   CHECK (Status IN ('PENDING_SELF', 'PENDING_MANAGER', 'COMPLETED')),
    CONSTRAINT CK_PerformanceReviews_Scores   CHECK ((SelfScore IS NULL OR SelfScore BETWEEN 1 AND 5)
        AND (ManagerScore IS NULL OR ManagerScore BETWEEN 1 AND 5) AND (FinalScore IS NULL OR FinalScore BETWEEN 1 AND 5)),
    CONSTRAINT CK_PerformanceReviews_Rating   CHECK (Rating IS NULL OR Rating IN ('A', 'B', 'C', 'D', 'E')),
    CONSTRAINT CK_PerformanceReviews_Reviewer CHECK (ReviewerId IS NULL OR ReviewerId <> EmployeeId),
    CONSTRAINT CK_PerformanceReviews_Done     CHECK (Status <> 'COMPLETED' OR (FinalScore IS NOT NULL AND Rating IS NOT NULL))
);
CREATE INDEX IX_PerformanceReviews_Reviewer ON dbo.PerformanceReviews (ReviewerId, Status) INCLUDE (CycleId);
CREATE INDEX IX_PerformanceReviews_Employee ON dbo.PerformanceReviews (EmployeeId);

CREATE TABLE dbo.ReviewScores (
    ReviewId     INT     NOT NULL,
    CriteriaId   INT     NOT NULL,
    SelfScore    TINYINT NULL,
    ManagerScore TINYINT NULL,
    CONSTRAINT PK_ReviewScores PRIMARY KEY (ReviewId, CriteriaId),
    CONSTRAINT FK_ReviewScores_Reviews  FOREIGN KEY (ReviewId)   REFERENCES dbo.PerformanceReviews (ReviewId) ON DELETE CASCADE,
    CONSTRAINT FK_ReviewScores_Criteria FOREIGN KEY (CriteriaId) REFERENCES dbo.ReviewCriteria (CriteriaId),
    CONSTRAINT CK_ReviewScores_Range CHECK ((SelfScore IS NULL OR SelfScore BETWEEN 1 AND 5)
        AND (ManagerScore IS NULL OR ManagerScore BETWEEN 1 AND 5))
);
CREATE INDEX IX_ReviewScores_CriteriaId ON dbo.ReviewScores (CriteriaId);

/* -----------------------------------------------------------------------------
   8. Audit log (written by the application for security-relevant actions)
   ----------------------------------------------------------------------------- */
CREATE TABLE dbo.AuditLogs (
    AuditId    BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_AuditLogs PRIMARY KEY,
    Username   VARCHAR(50)    NULL,
    Action     VARCHAR(50)    NOT NULL,
    EntityType VARCHAR(50)    NULL,
    EntityId   VARCHAR(50)    NULL,
    Details    NVARCHAR(2000) NULL,
    IpAddress  VARCHAR(45)    NULL,
    CreatedAt  DATETIME2(3)   NOT NULL CONSTRAINT DF_AuditLogs_CreatedAt DEFAULT (SYSUTCDATETIME())
);
CREATE INDEX IX_AuditLogs_CreatedAt ON dbo.AuditLogs (CreatedAt DESC);
CREATE INDEX IX_AuditLogs_Entity    ON dbo.AuditLogs (EntityType, EntityId);
GO

DECLARE @tables INT = (SELECT COUNT(*) FROM sys.tables WHERE is_ms_shipped = 0);
PRINT N'Schema created: ' + CAST(@tables AS NVARCHAR(10)) + N' tables.';
GO
