SET NOCOUNT ON;
GO

IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'hrm_app')
    CREATE LOGIN hrm_app WITH PASSWORD = N'$(APP_DB_PASSWORD)', DEFAULT_DATABASE = HRM, CHECK_POLICY = ON;
ELSE
    ALTER LOGIN hrm_app WITH PASSWORD = N'$(APP_DB_PASSWORD)';
GO

IF DATABASE_PRINCIPAL_ID(N'hrm_app') IS NULL
    CREATE USER hrm_app FOR LOGIN hrm_app WITH DEFAULT_SCHEMA = dbo;
IF DATABASE_PRINCIPAL_ID(N'hrm_app_role') IS NULL
    CREATE ROLE hrm_app_role;
GO

GRANT SELECT, INSERT, UPDATE, DELETE ON SCHEMA::dbo TO hrm_app_role;
GRANT EXECUTE ON SCHEMA::dbo TO hrm_app_role;
IF IS_ROLEMEMBER(N'hrm_app_role', N'hrm_app') = 0
    ALTER ROLE hrm_app_role ADD MEMBER hrm_app;
GO

PRINT N'Login hrm_app is ready (DML + EXECUTE on schema dbo only).';
GO
