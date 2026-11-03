SET NOCOUNT ON;
GO

IF DB_ID(N'HRM') IS NULL
BEGIN
    PRINT N'Creating database HRM...';
    CREATE DATABASE HRM COLLATE Latin1_General_100_CI_AI;
END
ELSE
    PRINT N'Database HRM already exists – skipped.';
GO

IF EXISTS (SELECT 1 FROM sys.databases WHERE name = N'HRM' AND is_read_committed_snapshot_on = 0)
    ALTER DATABASE HRM SET READ_COMMITTED_SNAPSHOT ON WITH ROLLBACK IMMEDIATE;
GO
