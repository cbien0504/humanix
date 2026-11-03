SET NOCOUNT ON;
GO

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
