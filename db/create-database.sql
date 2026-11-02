/* =============================================================================
   HRM System – create the database (idempotent)
   Run with:  sqlcmd -S <server> -U <admin> -i db/create-database.sql
   ---------------------------------------------------------------------------
   Collation Latin1_General_100_CI_AI is chosen on purpose: it is case- AND
   accent-insensitive for Vietnamese text, so searching "nguyen van" matches
   "Nguyễn Văn" (Vietnamese_100_CI_AI does not – see docs/02-database-design.md).
   ============================================================================= */
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

-- Readers do not block writers (row versioning) – important for reports running
-- while HR edits data. Requires no other active connection, hence ROLLBACK IMMEDIATE.
IF EXISTS (SELECT 1 FROM sys.databases WHERE name = N'HRM' AND is_read_committed_snapshot_on = 0)
    ALTER DATABASE HRM SET READ_COMMITTED_SNAPSHOT ON WITH ROLLBACK IMMEDIATE;
GO
