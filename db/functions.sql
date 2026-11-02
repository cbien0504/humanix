/* =============================================================================
   HRM System – reusable functions
   ============================================================================= */
SET NOCOUNT ON;
GO

/* -----------------------------------------------------------------------------
   fn_WorkingDays(@FromDate, @ToDate)
   Inline table-valued function returning every working day in the closed range:
   Monday–Friday and not a row in dbo.Holidays (business rule BR-13).
   * Inline TVF (not a scalar UDF / multi-statement TVF) so the optimizer can
     expand it into the calling query – no per-row function call overhead.
   * Weekday test is independent of @@DATEFIRST / SET LANGUAGE:
     1900-01-01 was a Monday, so DATEDIFF(DAY, '19000101', d) % 7 = 0..4 → Mon..Fri.
   * Tally of up to 10,000 days (~27 years) built from stacked VALUES rows.
   The Java class WorkingDayCalculator implements the same rule.
   ----------------------------------------------------------------------------- */
CREATE OR ALTER FUNCTION dbo.fn_WorkingDays (@FromDate DATE, @ToDate DATE)
RETURNS TABLE
AS
RETURN
    WITH Tally AS (
        SELECT TOP (CASE WHEN @ToDate >= @FromDate THEN DATEDIFF(DAY, @FromDate, @ToDate) + 1 ELSE 0 END)
               ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) - 1 AS N
        FROM (VALUES (0),(0),(0),(0),(0),(0),(0),(0),(0),(0)) AS a(x)
        CROSS JOIN (VALUES (0),(0),(0),(0),(0),(0),(0),(0),(0),(0)) AS b(x)
        CROSS JOIN (VALUES (0),(0),(0),(0),(0),(0),(0),(0),(0),(0)) AS c(x)
        CROSS JOIN (VALUES (0),(0),(0),(0),(0),(0),(0),(0),(0),(0)) AS d(x)
    ), Dates AS (
        SELECT DATEADD(DAY, N, @FromDate) AS WorkDate FROM Tally
    )
    SELECT dt.WorkDate
    FROM Dates AS dt
    WHERE DATEDIFF(DAY, '19000101', dt.WorkDate) % 7 < 5
      AND NOT EXISTS (SELECT 1 FROM dbo.Holidays AS h WHERE h.HolidayDate = dt.WorkDate);
GO
