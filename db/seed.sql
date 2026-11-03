SET NOCOUNT ON;
SET XACT_ABORT ON;
EXEC sys.sp_set_session_context @key = N'app_user', @value = 'seed';
GO

INSERT INTO dbo.Roles (RoleCode, RoleName, Description) VALUES
 ('ADMIN',    N'Quản trị hệ thống', N'Toàn quyền: tài khoản, cấu hình, nhật ký, kiểm tra dữ liệu'),
 ('HR',       N'Nhân sự',           N'Quản lý hồ sơ, chấm công, nghỉ phép, lương, tuyển dụng, đánh giá'),
 ('MANAGER',  N'Quản lý',           N'Duyệt nghỉ phép, đánh giá nhân viên trong phạm vi quản lý'),
 ('EMPLOYEE', N'Nhân viên',         N'Tự phục vụ: chấm công, xin nghỉ, xem phiếu lương, tự đánh giá');

INSERT INTO dbo.Departments (DepartmentCode, DepartmentName, Description) VALUES
 ('BOD',   N'Ban Giám đốc',                N'Điều hành chung'),
 ('HR',    N'Phòng Nhân sự',               N'Tuyển dụng, C&B, đào tạo, văn hóa doanh nghiệp'),
 ('FIN',   N'Phòng Tài chính - Kế toán',   N'Kế toán, thuế, ngân quỹ, chi lương'),
 ('IT',    N'Phòng Công nghệ thông tin',   N'Phát triển phần mềm, hạ tầng, an toàn thông tin'),
 ('SALES', N'Phòng Kinh doanh',            N'Bán hàng B2B/B2C, chăm sóc khách hàng'),
 ('MKT',   N'Phòng Marketing',             N'Thương hiệu, digital marketing, truyền thông');

INSERT INTO dbo.Positions (PositionCode, PositionName, JobLevel, MinSalary, MaxSalary) VALUES
 ('CEO',        N'Tổng Giám đốc',            10, 80000000, 200000000),
 ('MGR',        N'Trưởng phòng',              8, 35000000,  80000000),
 ('TL',         N'Trưởng nhóm',               6, 25000000,  45000000),
 ('SYSADMIN',   N'Quản trị hệ thống',         5, 18000000,  35000000),
 ('SR_DEV',     N'Lập trình viên Senior',     5, 25000000,  45000000),
 ('DEV',        N'Lập trình viên',            4, 12000000,  28000000),
 ('QA',         N'Kiểm thử phần mềm',         4, 10000000,  25000000),
 ('HR_SPEC',    N'Chuyên viên Nhân sự',       4, 10000000,  22000000),
 ('ACCT',       N'Kế toán viên',              4, 10000000,  22000000),
 ('MKT_SPEC',   N'Chuyên viên Marketing',     4, 10000000,  22000000),
 ('SALES_EXEC', N'Nhân viên Kinh doanh',      3,  8000000,  20000000),
 ('ADMIN_ASST', N'Thư ký - Hành chính',       2,  7000000,  14000000),
 ('INTERN',     N'Thực tập sinh',             1,  3000000,   6000000);
GO

DECLARE @e TABLE (
    Code VARCHAR(20), FullName NVARCHAR(150), Gender CHAR(1), Dob DATE, Email VARCHAR(150), Phone VARCHAR(20),
    Address NVARCHAR(300), NationalId VARCHAR(12), TaxCode VARCHAR(14), BankAccount VARCHAR(30), BankName NVARCHAR(100),
    Dept VARCHAR(20), Pos VARCHAR(20), ManagerCode VARCHAR(20), HireDate DATE, TermDate DATE, Status VARCHAR(20),
    BaseSalary DECIMAL(18,2), Allowance DECIMAL(18,2), Dependents TINYINT);

INSERT INTO @e VALUES
 ('EMP0001',N'Nguyễn Văn Hùng','M','1975-03-12','hung.nv@example.com','0903111001',N'12 Lý Thường Kiệt, Hoàn Kiếm, Hà Nội','001075003312','8001234501','0011000123401',N'Vietcombank','BOD','CEO',NULL,'2012-01-03',NULL,'ACTIVE',120000000,10000000,2),
 ('EMP0002',N'Trần Thị Mai','F','1990-07-21','mai.tt@example.com','0912111002',N'45 Kim Mã, Ba Đình, Hà Nội','001190004521','8001234502','19033344455501',N'Techcombank','BOD','ADMIN_ASST','EMP0001','2018-05-02',NULL,'ACTIVE',12000000,1000000,0),
 ('EMP0003',N'Lê Thị Lan','F','1985-11-02','lan.lt@example.com','0983111003',N'88 Trần Duy Hưng, Cầu Giấy, Hà Nội','036185002210','8001234503','21510000123403',N'BIDV','HR','MGR','EMP0001','2014-06-16',NULL,'ACTIVE',45000000,5000000,2),
 ('EMP0004',N'Phạm Minh Tuấn','M','1992-04-18','tuan.pm@example.com','0868111004',N'21 Nguyễn Trãi, Thanh Xuân, Hà Nội','034092005671','8001234504','0011000123404',N'Vietcombank','HR','HR_SPEC','EMP0003','2019-03-11',NULL,'ACTIVE',18000000,1500000,1),
 ('EMP0005',N'Hoàng Thị Thu Trang','F','1996-09-09','trang.htt@example.com','0977111005',N'5 Xuân Thủy, Cầu Giấy, Hà Nội','001196007788','8001234505','19033344455505',N'Techcombank','HR','HR_SPEC','EMP0003','2022-08-01',NULL,'ACTIVE',13000000,1000000,0),
 ('EMP0006',N'Vũ Ngọc Ánh','F','2002-01-15','anh.vn@example.com','0388111006',N'102 Phạm Văn Đồng, Bắc Từ Liêm, Hà Nội','030302001122',NULL,'0350100123406',N'MB Bank','HR','INTERN','EMP0003','2026-07-01',NULL,'PROBATION',4500000,0,0),
 ('EMP0007',N'Đặng Quốc Bảo','M','1983-12-05','bao.dq@example.com','0904111007',N'17 Láng Hạ, Đống Đa, Hà Nội','038083006543','8001234507','21510000123407',N'BIDV','FIN','MGR','EMP0001','2013-09-01',NULL,'ACTIVE',50000000,5000000,3),
 ('EMP0008',N'Bùi Thị Hồng Nhung','F','1991-02-27','nhung.bth@example.com','0915111008',N'9 Tây Sơn, Đống Đa, Hà Nội','027191003456','8001234508','0011000123408',N'Vietcombank','FIN','ACCT','EMP0007','2017-04-03',NULL,'ACTIVE',19000000,1500000,1),
 ('EMP0009',N'Đỗ Văn Khánh','M','1994-06-30','khanh.dv@example.com','0936111009',N'33 Hoàng Quốc Việt, Cầu Giấy, Hà Nội','040094008765','8001234509','10221234509',N'VPBank','FIN','ACCT','EMP0007','2020-10-12',NULL,'ACTIVE',15000000,1000000,0),
 ('EMP0010',N'Ngô Thị Phương','F','1998-10-10','phuong.nt@example.com','0962111010',N'71 Giải Phóng, Hai Bà Trưng, Hà Nội','001198002468','8001234510','0350100123410',N'MB Bank','FIN','ACCT','EMP0007','2024-02-19',NULL,'ACTIVE',11500000,1000000,0),
 ('EMP0011',N'Trịnh Đức Minh','M','1986-08-08','minh.td@example.com','0989111011',N'2 Duy Tân, Cầu Giấy, Hà Nội','001086009753','8001234511','19033344455511',N'Techcombank','IT','MGR','EMP0001','2015-02-02',NULL,'ACTIVE',60000000,5000000,2),
 ('EMP0012',N'Phan Thanh Sơn','M','1990-05-14','son.pt@example.com','0905111012',N'14 Trần Thái Tông, Cầu Giấy, Hà Nội','031090001357','8001234512','0011000123412',N'Vietcombank','IT','SYSADMIN','EMP0011','2016-07-18',NULL,'ACTIVE',30000000,2000000,1),
 ('EMP0013',N'Lý Hoàng Nam','M','1991-03-03','nam.lh@example.com','0916111013',N'60 Nguyễn Chí Thanh, Đống Đa, Hà Nội','079091004680','8001234513','21510000123413',N'BIDV','IT','TL','EMP0011','2017-11-06',NULL,'ACTIVE',40000000,3000000,1),
 ('EMP0014',N'Mai Anh Tuấn','M','1993-12-22','tuan.ma@example.com','0937111014',N'8 Tôn Thất Thuyết, Cầu Giấy, Hà Nội','048093002579','8001234514','10221234514',N'VPBank','IT','SR_DEV','EMP0013','2018-08-20',NULL,'ACTIVE',35000000,2000000,1),
 ('EMP0015',N'Nguyễn Thị Thảo','F','1995-07-07','thao.nt@example.com','0963111015',N'27 Lê Văn Lương, Thanh Xuân, Hà Nội','036195006801','8001234515','0011000123415',N'Vietcombank','IT','SR_DEV','EMP0013','2019-06-03',NULL,'ACTIVE',32000000,2000000,0),
 ('EMP0016',N'Trần Quốc Việt','M','1997-01-25','viet.tq@example.com','0981111016',N'110 Cầu Giấy, Cầu Giấy, Hà Nội','001097005432','8001234516','19033344455516',N'Techcombank','IT','DEV','EMP0013','2021-03-15',NULL,'ACTIVE',22000000,1500000,0),
 ('EMP0017',N'Lê Hoàng Long','M','1998-04-12','long.lh@example.com','0397111017',N'3 Mỹ Đình, Nam Từ Liêm, Hà Nội','040098003219','8001234517','0350100123417',N'MB Bank','IT','DEV','EMP0013','2022-07-11',NULL,'ACTIVE',18000000,1000000,0),
 ('EMP0018',N'Phạm Thị Ngọc Hà','F','1999-09-19','ha.ptn@example.com','0358111018',N'19 Hồ Tùng Mậu, Bắc Từ Liêm, Hà Nội','034199007310','8001234518','10221234518',N'VPBank','IT','DEV','EMP0013','2023-09-05',NULL,'ACTIVE',16000000,1000000,0),
 ('EMP0019',N'Đinh Công Phúc','M','2000-11-30','phuc.dc@example.com','0329111019',N'42 Quan Hoa, Cầu Giấy, Hà Nội','001200004126',NULL,'0011000123419',N'Vietcombank','IT','DEV','EMP0013','2026-08-17',NULL,'PROBATION',14000000,1000000,0),
 ('EMP0020',N'Võ Thị Kim Chi','F','1996-02-14','chi.vtk@example.com','0909111020',N'250 Nguyễn Văn Cừ, Long Biên, Hà Nội','079196008642','8001234520','21510000123420',N'BIDV','IT','QA','EMP0011','2020-05-18',NULL,'ACTIVE',20000000,1500000,1),
 ('EMP0021',N'Huỳnh Gia Bảo','M','1999-06-06','bao.hg@example.com','0918111021',N'7 Trung Kính, Cầu Giấy, Hà Nội','079099001975','8001234521','19033344455521',N'Techcombank','IT','QA','EMP0011','2024-04-01',NULL,'ACTIVE',13000000,1000000,0),
 ('EMP0022',N'Dương Minh Khoa','M','2003-03-20','khoa.dm@example.com','0339111022',N'66 Nguyễn Phong Sắc, Cầu Giấy, Hà Nội','001203006284',NULL,'0350100123422',N'MB Bank','IT','INTERN','EMP0013','2026-09-14',NULL,'PROBATION',4000000,0,0),
 ('EMP0023',N'Cao Văn Thắng','M','1984-10-01','thang.cv@example.com','0903111023',N'15 Thái Hà, Đống Đa, Hà Nội','038084002937','8001234523','0011000123423',N'Vietcombank','SALES','MGR','EMP0001','2014-03-17',NULL,'ACTIVE',50000000,5000000,2),
 ('EMP0024',N'Lâm Thị Bích Ngọc','F','1992-08-15','ngoc.ltb@example.com','0938111024',N'90 Chùa Láng, Đống Đa, Hà Nội','079192005148','8001234524','10221234524',N'VPBank','SALES','SALES_EXEC','EMP0023','2018-01-08',NULL,'ACTIVE',17000000,2000000,1),
 ('EMP0025',N'Tạ Quang Huy','M','1994-01-09','huy.tq@example.com','0964111025',N'31 Lạc Long Quân, Tây Hồ, Hà Nội','001094003861','8001234525','21510000123425',N'BIDV','SALES','SALES_EXEC','EMP0023','2019-09-16',NULL,'ACTIVE',15000000,2000000,0),
 ('EMP0026',N'Kiều Thị Diễm','F','1997-05-27','diem.kt@example.com','0975111026',N'4 Âu Cơ, Tây Hồ, Hà Nội','031197007492','8001234526','0350100123426',N'MB Bank','SALES','SALES_EXEC','EMP0023','2021-11-01',NULL,'ACTIVE',12000000,2000000,0),
 ('EMP0027',N'Hà Văn Đức','M','1995-12-12','duc.hv@example.com','0986111027',N'58 Minh Khai, Hai Bà Trưng, Hà Nội','027095004053','8001234527','19033344455527',N'Techcombank','SALES','SALES_EXEC','EMP0023','2020-06-22','2026-06-30','TERMINATED',13500000,2000000,1),
 ('EMP0028',N'Châu Minh Tâm','M','1998-07-04','tam.cm@example.com','0707111028',N'12 Nguyễn Văn Linh, Quận 7, TP.HCM','079098006615','8001234528','0011000123428',N'Vietcombank','SALES','SALES_EXEC','EMP0023','2023-03-06',NULL,'ACTIVE',11000000,2000000,0),
 ('EMP0029',N'Lương Thị Hạnh','F','1993-03-08','hanh.lt@example.com','0778111029',N'77 Lê Duẩn, Hoàn Kiếm, Hà Nội','040193002286','8001234529','10221234529',N'VPBank','SALES','SALES_EXEC','EMP0023','2017-10-02',NULL,'ON_LEAVE',16000000,2000000,2),
 ('EMP0030',N'Đoàn Thị Thanh Hương','F','1988-09-23','huong.dtt@example.com','0813111030',N'23 Hàng Bài, Hoàn Kiếm, Hà Nội','001188005907','8001234530','21510000123430',N'BIDV','MKT','MGR','EMP0001','2016-04-04',NULL,'ACTIVE',42000000,4000000,1),
 ('EMP0031',N'Nguyễn Hữu Phước','M','1995-11-11','phuoc.nh@example.com','0834111031',N'39 Đội Cấn, Ba Đình, Hà Nội','048095003374','8001234531','0350100123431',N'MB Bank','MKT','MKT_SPEC','EMP0030','2020-01-13',NULL,'ACTIVE',17000000,1500000,0),
 ('EMP0032',N'Trương Mỹ Linh','F','1997-04-04','linh.tm@example.com','0855111032',N'101 Võ Văn Tần, Quận 3, TP.HCM','079197008519','8001234532','0011000123432',N'Vietcombank','MKT','MKT_SPEC','EMP0030','2022-03-21',NULL,'ACTIVE',14000000,1500000,0),
 ('EMP0033',N'Quách Tuấn Kiệt','M','2000-08-18','kiet.qt@example.com','0886111033',N'6 Nguyễn Hoàng, Nam Từ Liêm, Hà Nội','001200009830',NULL,'19033344455533',N'Techcombank','MKT','MKT_SPEC','EMP0030','2025-05-12',NULL,'ACTIVE',12000000,1000000,0),
 ('EMP0034',N'Bạch Ngọc Diệp','F','1999-12-01','diep.bn@example.com','0397111034',N'50 Hoàng Hoa Thám, Ba Đình, Hà Nội','030199004417','8001234534','10221234534',N'VPBank','HR','HR_SPEC','EMP0003','2025-02-10',NULL,'ACTIVE',12500000,1000000,0),
 ('EMP0035',N'La Văn Tài','M','1990-02-02','tai.lv@example.com','0942111035',N'18 Bà Triệu, Hai Bà Trưng, Hà Nội','036090007068','8001234535','21510000123435',N'BIDV','SALES','SALES_EXEC','EMP0023','2016-08-01',NULL,'ACTIVE',19500000,2000000,2),
 ('EMP0036',N'Tô Thị Yến','F','2001-05-05','yen.tt@example.com','0326111036',N'25 Vạn Phúc, Hà Đông, Hà Nội','034301005521',NULL,'0350100123436',N'MB Bank','FIN','ACCT','EMP0007','2025-10-06',NULL,'ACTIVE',10500000,800000,0);

INSERT INTO dbo.Employees (EmployeeCode, FullName, Gender, DateOfBirth, Email, Phone, Address, NationalId, TaxCode,
    BankAccount, BankName, DepartmentId, PositionId, HireDate, TerminationDate, Status, BaseSalary, Allowance, Dependents)
SELECT e.Code, e.FullName, e.Gender, e.Dob, e.Email, e.Phone, e.Address, e.NationalId, e.TaxCode,
       e.BankAccount, e.BankName, d.DepartmentId, p.PositionId, e.HireDate, e.TermDate, e.Status,
       e.BaseSalary, e.Allowance, e.Dependents
FROM @e AS e
JOIN dbo.Departments AS d ON d.DepartmentCode = e.Dept
JOIN dbo.Positions   AS p ON p.PositionCode   = e.Pos
ORDER BY e.Code;

UPDATE emp SET ManagerId = mgr.EmployeeId
FROM dbo.Employees AS emp
JOIN @e AS src ON src.Code = emp.EmployeeCode
JOIN dbo.Employees AS mgr ON mgr.EmployeeCode = src.ManagerCode;

UPDATE d SET ManagerId = e.EmployeeId
FROM dbo.Departments AS d
JOIN (VALUES ('BOD','EMP0001'),('HR','EMP0003'),('FIN','EMP0007'),('IT','EMP0011'),('SALES','EMP0023'),('MKT','EMP0030'))
     AS m(Dept, Code) ON m.Dept = d.DepartmentCode
JOIN dbo.Employees AS e ON e.EmployeeCode = m.Code;
GO

DECLARE @hash CHAR(60) = '$2a$10$bHo/3H0v.DBCJf.5.8FcxukevHCCH3nDUjK1MAQAVm5GkySg1RB8.';

INSERT INTO dbo.Users (Username, PasswordHash, EmployeeId, IsActive)
SELECT CASE e.EmployeeCode WHEN 'EMP0012' THEN 'admin' WHEN 'EMP0003' THEN 'hr'
                           WHEN 'EMP0011' THEN 'manager' WHEN 'EMP0016' THEN 'employee'
                           ELSE LOWER(e.EmployeeCode) END,
       @hash, e.EmployeeId,
       CASE WHEN e.Status = 'TERMINATED' THEN 0 ELSE 1 END
FROM dbo.Employees AS e
WHERE e.EmployeeCode NOT IN ('EMP0006', 'EMP0022', 'EMP0029');

INSERT INTO dbo.UserRoles (UserId, RoleId)
SELECT u.UserId, r.RoleId
FROM dbo.Users AS u
JOIN dbo.Employees AS e ON e.EmployeeId = u.EmployeeId
JOIN dbo.Roles AS r ON
       r.RoleCode = 'EMPLOYEE'
    OR (r.RoleCode = 'ADMIN'   AND u.Username = 'admin')
    OR (r.RoleCode = 'HR'      AND e.EmployeeCode IN ('EMP0003', 'EMP0004', 'EMP0005', 'EMP0034'))
    OR (r.RoleCode = 'MANAGER' AND e.EmployeeCode IN ('EMP0001', 'EMP0003', 'EMP0007', 'EMP0011', 'EMP0013', 'EMP0023', 'EMP0030'));
GO

INSERT INTO dbo.Holidays (HolidayDate, HolidayName) VALUES
 ('2025-01-01', N'Tết Dương lịch'),
 ('2025-01-27', N'Tết Nguyên đán'), ('2025-01-28', N'Tết Nguyên đán'), ('2025-01-29', N'Tết Nguyên đán'),
 ('2025-01-30', N'Tết Nguyên đán'), ('2025-01-31', N'Tết Nguyên đán'),
 ('2025-04-07', N'Giỗ Tổ Hùng Vương'),
 ('2025-04-30', N'Ngày Giải phóng miền Nam'), ('2025-05-01', N'Quốc tế Lao động'), ('2025-05-02', N'Nghỉ bù (hoán đổi)'),
 ('2025-09-01', N'Quốc khánh'), ('2025-09-02', N'Quốc khánh'),
 ('2026-01-01', N'Tết Dương lịch'),
 ('2026-02-16', N'Tết Nguyên đán'), ('2026-02-17', N'Tết Nguyên đán'), ('2026-02-18', N'Tết Nguyên đán'),
 ('2026-02-19', N'Tết Nguyên đán'), ('2026-02-20', N'Tết Nguyên đán'),
 ('2026-04-27', N'Giỗ Tổ Hùng Vương (nghỉ bù)'),
 ('2026-04-30', N'Ngày Giải phóng miền Nam'), ('2026-05-01', N'Quốc tế Lao động'),
 ('2026-09-01', N'Quốc khánh'), ('2026-09-02', N'Quốc khánh');

INSERT INTO dbo.LeaveTypes (LeaveTypeCode, LeaveTypeName, IsPaid, RequiresBalance, MaxDaysPerRequest) VALUES
 ('ANNUAL',      N'Nghỉ phép năm',              1, 1, NULL),
 ('SICK',        N'Nghỉ ốm (hưởng BHXH)',       0, 0, 30),
 ('UNPAID',      N'Nghỉ không lương',           0, 0, 30),
 ('MARRIAGE',    N'Nghỉ kết hôn',               1, 0, 3),
 ('BEREAVEMENT', N'Nghỉ tang',                  1, 0, 3),
 ('MATERNITY',   N'Nghỉ thai sản (hưởng BHXH)', 0, 0, NULL);

INSERT INTO dbo.PayrollSettings (SettingKey, SettingValue, Description) VALUES
 ('SOCIAL_INSURANCE_RATE',       0.0800,   N'BHXH người lao động đóng (8%)'),
 ('HEALTH_INSURANCE_RATE',       0.0150,   N'BHYT người lao động đóng (1,5%)'),
 ('UNEMPLOYMENT_INSURANCE_RATE', 0.0100,   N'BHTN người lao động đóng (1%)'),
 ('BASE_SALARY_REFERENCE',       2340000,  N'Lương cơ sở – trần BHXH/BHYT = hệ số trần × lương cơ sở'),
 ('REGIONAL_MINIMUM_WAGE',       5310000,  N'Lương tối thiểu vùng I – trần BHTN = hệ số trần × mức này'),
 ('INSURANCE_CAP_MULTIPLIER',    20,       N'Hệ số trần mức lương đóng bảo hiểm'),
 ('PERSONAL_DEDUCTION',          15500000, N'Giảm trừ gia cảnh bản thân / tháng'),
 ('DEPENDENT_DEDUCTION',         6200000,  N'Giảm trừ mỗi người phụ thuộc / tháng'),
 ('UNPAID_DAYS_NO_INSURANCE',    14,       N'Nghỉ không hưởng lương từ số ngày này trở lên trong tháng thì không đóng bảo hiểm');

INSERT INTO dbo.TaxBrackets (BracketNo, LowerBound, UpperBound, Rate) VALUES
 (1,        0,  5000000, 0.05),
 (2,  5000000, 10000000, 0.10),
 (3, 10000000, 18000000, 0.15),
 (4, 18000000, 32000000, 0.20),
 (5, 32000000, 52000000, 0.25),
 (6, 52000000, 80000000, 0.30),
 (7, 80000000,     NULL, 0.35);
GO

EXEC dbo.sp_InitLeaveBalances @Year = 2026;
GO

DECLARE @lr TABLE (Emp VARCHAR(20), LeaveType VARCHAR(20), StartDate DATE, EndDate DATE, Reason NVARCHAR(500),
                   Status VARCHAR(20), Approver VARCHAR(20), Comment NVARCHAR(500));
INSERT INTO @lr VALUES
 ('EMP0004','ANNUAL',     '2026-08-10','2026-08-12',N'Về quê thăm gia đình',            'APPROVED','EMP0003',N'Đồng ý'),
 ('EMP0008','ANNUAL',     '2026-08-24','2026-08-28',N'Du lịch cùng gia đình',           'APPROVED','EMP0007',N'Đã bàn giao công việc, đồng ý'),
 ('EMP0009','BEREAVEMENT','2026-08-17','2026-08-18',N'Gia đình có tang',                'APPROVED','EMP0007',N'Chia buồn cùng gia đình'),
 ('EMP0014','SICK',       '2026-09-07','2026-09-08',N'Sốt xuất huyết, có giấy bệnh viện','APPROVED','EMP0013',N'Chúc mau khỏe'),
 ('EMP0016','ANNUAL',     '2026-09-14','2026-09-15',N'Việc gia đình',                   'APPROVED','EMP0013',N'OK'),
 ('EMP0024','ANNUAL',     '2026-09-21','2026-09-25',N'Nghỉ phép năm',                   'APPROVED','EMP0023',N'Đồng ý'),
 ('EMP0031','MARRIAGE',   '2026-10-05','2026-10-07',N'Đám cưới',                        'APPROVED','EMP0030',N'Chúc mừng hạnh phúc!'),
 ('EMP0017','UNPAID',     '2026-10-12','2026-10-16',N'Giải quyết việc cá nhân',         'APPROVED','EMP0013',N'Đồng ý nghỉ không lương'),
 ('EMP0029','MATERNITY',  '2026-08-03','2026-12-31',N'Nghỉ chế độ thai sản',            'APPROVED','EMP0023',N'Chúc mẹ tròn con vuông'),
 ('EMP0020','ANNUAL',     '2026-10-26','2026-10-27',N'Khám sức khỏe định kỳ',           'APPROVED','EMP0011',N'Đồng ý'),
 ('EMP0025','ANNUAL',     '2026-09-28','2026-10-02',N'Đi du lịch',                      'REJECTED','EMP0023',N'Cuối quý cần tập trung chốt doanh số, đề nghị dời lịch'),
 ('EMP0018','ANNUAL',     '2026-10-19','2026-10-20',N'Việc cá nhân',                    'REJECTED','EMP0013',N'Đang trong giai đoạn release, đề nghị chọn ngày khác'),
 ('EMP0015','ANNUAL',     '2026-08-31','2026-09-04',N'Nghỉ lễ dài ngày',                'CANCELLED',NULL,NULL),
 ('EMP0021','ANNUAL',     '2026-11-12','2026-11-13',N'Về quê',                          'APPROVED','EMP0011',N'Đồng ý'),
 ('EMP0013','ANNUAL',     '2026-12-28','2026-12-31',N'Nghỉ cuối năm',                   'APPROVED','EMP0011',N'Đồng ý'),
 ('EMP0032','ANNUAL',     '2026-11-05','2026-11-06',N'Việc gia đình',                   'PENDING', NULL,NULL),
 ('EMP0005','ANNUAL',     '2026-11-09','2026-11-10',N'Tham dự đám cưới bạn',            'PENDING', NULL,NULL),
 ('EMP0016','ANNUAL',     '2026-11-16','2026-11-18',N'Đi du lịch Đà Nẵng',              'PENDING', NULL,NULL),
 ('EMP0017','ANNUAL',     '2026-11-23','2026-11-24',N'Việc cá nhân',                    'PENDING', NULL,NULL),
 ('EMP0026','ANNUAL',     '2026-12-21','2026-12-24',N'Nghỉ phép cuối năm',              'PENDING', NULL,NULL);

INSERT INTO dbo.LeaveRequests (EmployeeId, LeaveTypeId, StartDate, EndDate, TotalDays, Reason, Status,
                               ApproverId, DecidedAt, ApproverComment, CreatedAt)
SELECT e.EmployeeId, lt.LeaveTypeId, r.StartDate, r.EndDate, wd.Days, r.Reason, r.Status,
       a.EmployeeId,
       CASE WHEN r.Status IN ('APPROVED', 'REJECTED') THEN
            CASE WHEN n.Decided < t.Now THEN n.Decided
                 ELSE DATEADD(SECOND, DATEDIFF(SECOND, c.CreatedAt, t.Now) / 2, c.CreatedAt) END END,
       r.Comment,
       c.CreatedAt
FROM @lr AS r
JOIN dbo.Employees  AS e  ON e.EmployeeCode   = r.Emp
JOIN dbo.LeaveTypes AS lt ON lt.LeaveTypeCode = r.LeaveType
LEFT JOIN dbo.Employees AS a ON a.EmployeeCode = r.Approver
CROSS APPLY (SELECT CAST(COUNT(*) AS DECIMAL(5,1)) AS Days FROM dbo.fn_WorkingDays(r.StartDate, r.EndDate)) AS wd
CROSS APPLY (SELECT CAST(SYSUTCDATETIME() AS DATETIME2(0)) AS Now, ABS(CHECKSUM(r.Emp, r.StartDate)) AS H) AS t
CROSS APPLY (SELECT DATEADD(DAY, -5, CAST(r.StartDate AS DATETIME2(0)))                AS Created,
                    DATEADD(HOUR, 2, DATEADD(DAY, -3, CAST(r.StartDate AS DATETIME2(0)))) AS Decided) AS n
CROSS APPLY (SELECT CASE WHEN n.Created < t.Now THEN n.Created
                         ELSE DATEADD(HOUR, -(26 + t.H % 48), t.Now) END AS CreatedAt) AS c
WHERE wd.Days > 0;
GO

DECLARE @From DATE = '2026-08-01';
DECLARE @To   DATE = DATEADD(DAY, -1, CAST(SYSDATETIME() AS DATE));

WITH EmpDays AS (
    SELECT e.EmployeeId, d.WorkDate,
           ABS(CHECKSUM(e.EmployeeId, d.WorkDate) % 100)          AS R,
           ABS(CHECKSUM(d.WorkDate, e.EmployeeId * 7919) % 1000)  AS R2
    FROM dbo.Employees AS e
    JOIN dbo.fn_WorkingDays(@From, @To) AS d
      ON d.WorkDate >= e.HireDate AND (e.TerminationDate IS NULL OR d.WorkDate <= e.TerminationDate)
    WHERE NOT EXISTS (SELECT 1 FROM dbo.LeaveRequests AS lr
                      WHERE lr.EmployeeId = e.EmployeeId AND lr.Status IN ('APPROVED', 'PENDING')
                        AND d.WorkDate BETWEEN lr.StartDate AND lr.EndDate)
), Shaped AS (
    SELECT EmployeeId, WorkDate, R2,
           CASE WHEN R < 10 THEN 'LATE' WHEN R < 13 THEN 'HALF_DAY' ELSE 'PRESENT' END AS Status,
           CASE WHEN R < 10 THEN 516 + R2 % 45
                WHEN R < 13 THEN 480 + R2 % 30
                ELSE 465 + R2 % 51 END AS InMinute,
           R
    FROM EmpDays
    WHERE R < 98
)
INSERT INTO dbo.Attendance (EmployeeId, WorkDate, CheckIn, CheckOut, Status, LateMinutes, Source)
SELECT EmployeeId, WorkDate,
       DATEADD(MINUTE, InMinute, CAST(WorkDate AS DATETIME2(0))),
       DATEADD(MINUTE, CASE WHEN Status = 'HALF_DAY' THEN 690 + R2 % 30
                            ELSE 1050 + R2 % 76 END,
               CAST(WorkDate AS DATETIME2(0))),
       Status,
       CASE WHEN Status = 'LATE' THEN InMinute - 510 ELSE 0 END,
       CASE WHEN R % 7 = 0 THEN 'API' ELSE 'WEB' END
FROM Shaped;

IF DATEDIFF(DAY, '19000101', CAST(SYSDATETIME() AS DATE)) % 7 < 5
   AND NOT EXISTS (SELECT 1 FROM dbo.Holidays WHERE HolidayDate = CAST(SYSDATETIME() AS DATE))
   AND CAST(SYSDATETIME() AS TIME) >= '09:00'
BEGIN
    DECLARE @Today DATE = CAST(SYSDATETIME() AS DATE);
    INSERT INTO dbo.Attendance (EmployeeId, WorkDate, CheckIn, Status, LateMinutes, Source)
    SELECT e.EmployeeId, @Today, DATEADD(MINUTE, x.InMinute, CAST(@Today AS DATETIME2(0))),
           CASE WHEN x.InMinute > 515 THEN 'LATE' ELSE 'PRESENT' END,
           CASE WHEN x.InMinute > 515 THEN x.InMinute - 510 ELSE 0 END, 'WEB'
    FROM dbo.Employees AS e
    CROSS APPLY (SELECT 465 + ABS(CHECKSUM(e.EmployeeId, @Today) % 60) AS InMinute,
                        ABS(CHECKSUM(@Today, e.EmployeeId) % 100) AS R) AS x
    WHERE e.Status IN ('ACTIVE', 'PROBATION') AND e.HireDate <= @Today
      AND e.EmployeeCode NOT IN ('EMP0003', 'EMP0011', 'EMP0012', 'EMP0016')
      AND x.R < 85
      AND NOT EXISTS (SELECT 1 FROM dbo.LeaveRequests AS lr
                      WHERE lr.EmployeeId = e.EmployeeId AND lr.Status IN ('APPROVED', 'PENDING')
                        AND @Today BETWEEN lr.StartDate AND lr.EndDate)
      AND NOT EXISTS (SELECT 1 FROM dbo.Attendance AS a WHERE a.EmployeeId = e.EmployeeId AND a.WorkDate = @Today);
END;
GO

INSERT INTO dbo.JobPostings (JobCode, Title, DepartmentId, PositionId, Openings, EmploymentType, Location,
                             SalaryMin, SalaryMax, Description, Requirements, Status, OpenDate, CloseDate, CreatedBy)
SELECT j.Code, j.Title, d.DepartmentId, p.PositionId, j.Openings, j.EmpType, j.Location, j.SalMin, j.SalMax,
       j.Descr, j.Req, j.Status, j.OpenDate, j.CloseDate, 'hr'
FROM (VALUES
 ('JOB-2026-001', N'Senior Java Developer', 'IT', 'SR_DEV', 2, 'FULL_TIME', N'Hà Nội', 25000000, 45000000,
  N'Phát triển và bảo trì hệ thống HRM, ERP nội bộ trên nền Java.',
  N'4+ năm Java, Servlet/JSP hoặc Spring, SQL Server, Git; ưu tiên có kinh nghiệm nghiệp vụ nhân sự.',
  'OPEN', '2026-09-01', '2026-11-30'),
 ('JOB-2026-002', N'Lập trình viên Java', 'IT', 'DEV', 1, 'FULL_TIME', N'Hà Nội', 12000000, 20000000,
  N'Tham gia phát triển các module nghiệp vụ.', N'1+ năm Java, SQL; biết JavaScript/jQuery.',
  'CLOSED', '2026-06-15', '2026-08-10'),
 ('JOB-2026-003', N'Nhân viên Kinh doanh B2B', 'SALES', 'SALES_EXEC', 3, 'FULL_TIME', N'Hà Nội', 10000000, 18000000,
  N'Tìm kiếm và chăm sóc khách hàng doanh nghiệp.', N'Kỹ năng giao tiếp tốt, ưu tiên có kinh nghiệm bán phần mềm.',
  'OPEN', '2026-10-01', '2026-12-15'),
 ('JOB-2026-004', N'Kiểm thử phần mềm (QA)', 'IT', 'QA', 1, 'FULL_TIME', N'Hà Nội', 12000000, 20000000,
  N'Kiểm thử chức năng, viết test case, kiểm thử API.', N'1+ năm QA, biết SQL cơ bản, Postman.',
  'ON_HOLD', '2026-09-15', NULL),
 ('JOB-2026-005', N'Chuyên viên Digital Marketing', 'MKT', 'MKT_SPEC', 1, 'FULL_TIME', N'TP.HCM', 12000000, 20000000,
  N'Lập kế hoạch và vận hành chiến dịch digital.', N'2+ năm digital marketing, Google Ads, Facebook Ads.',
  'DRAFT', NULL, NULL)
) AS j(Code, Title, Dept, Pos, Openings, EmpType, Location, SalMin, SalMax, Descr, Req, Status, OpenDate, CloseDate)
JOIN dbo.Departments AS d ON d.DepartmentCode = j.Dept
JOIN dbo.Positions   AS p ON p.PositionCode   = j.Pos;

INSERT INTO dbo.Candidates (JobPostingId, FullName, Email, Phone, Source, ExpectedSalary, Rating, InterviewAt, Notes, AppliedDate)
SELECT jp.JobPostingId, c.FullName, c.Email, c.Phone, c.Source, c.Salary, c.Rating, c.InterviewAt, c.Notes, c.Applied
FROM (VALUES
 ('JOB-2026-001', N'Nguyễn Minh Quân', 'quan.nm@example.net',  '0912000101', 'LINKEDIN',     38000000, 4,    CAST('2026-11-05T10:00:00' AS DATETIME2(0)), N'5 năm Java, từng làm hệ thống chấm công', CAST('2026-09-08' AS DATE)),
 ('JOB-2026-001', N'Trần Bảo Ngọc',    'ngoc.tb@example.net',  '0912000102', 'REFERRAL',     42000000, 5,    NULL, N'Được giới thiệu bởi EMP0014, phỏng vấn rất tốt', '2026-09-03'),
 ('JOB-2026-001', N'Lê Văn Phát',      'phat.lv@example.net',  '0912000103', 'TOPCV',        30000000, NULL, NULL, NULL, '2026-10-12'),
 ('JOB-2026-001', N'Phạm Hồng Sơn',    'son.ph@example.net',   '0912000104', 'WEBSITE',      35000000, NULL, NULL, NULL, '2026-10-25'),
 ('JOB-2026-001', N'Vũ Thị Hằng',      'hang.vt@example.net',  '0912000105', 'TOPCV',        40000000, 2,    NULL, N'Kiến thức SQL chưa đạt yêu cầu', '2026-09-15'),
 ('JOB-2026-001', N'Đỗ Quang Vinh',    'vinh.dq@example.net',  '0912000106', 'VIETNAMWORKS', 33000000, NULL, NULL, NULL, '2026-10-28'),
 ('JOB-2026-001', N'Hồ Ngọc Anh',      'anh.hn@example.net',   '0912000107', 'LINKEDIN',     45000000, NULL, NULL, N'Ứng viên đã nhận offer nơi khác', '2026-09-10'),
 ('JOB-2026-002', N'Đinh Công Phúc',   'phuc.dc@example.net',  '0329111019', 'TOPCV',        14000000, 4,    NULL, N'Đã tiếp nhận – EMP0019', '2026-06-20'),
 ('JOB-2026-002', N'Bùi Văn Lâm',      'lam.bv@example.net',   '0912000109', 'WEBSITE',      15000000, NULL, NULL, N'Chưa đủ kinh nghiệm', '2026-06-22'),
 ('JOB-2026-002', N'Mạc Thị Thủy',     'thuy.mt@example.net',  '0912000110', 'LINKEDIN',     18000000, 3,    NULL, N'Mức lương mong muốn vượt khung', '2026-06-25'),
 ('JOB-2026-003', N'Phùng Văn Tiến',   'tien.pv@example.net',  '0912000111', 'VIETNAMWORKS', 12000000, NULL, NULL, NULL, '2026-10-05'),
 ('JOB-2026-003', N'Lục Thị Mơ',       'mo.lt@example.net',    '0912000112', 'WEBSITE',      11000000, NULL, NULL, NULL, '2026-10-20'),
 ('JOB-2026-003', N'Âu Dương Phong',   'phong.ad@example.net', '0912000113', 'REFERRAL',     15000000, 3,    CAST('2026-11-06T14:00:00' AS DATETIME2(0)), N'Có tệp khách hàng doanh nghiệp', '2026-10-08'),
 ('JOB-2026-003', N'Kim Ngọc Bích',    'bich.kn@example.net',  '0912000114', 'TOPCV',        10000000, NULL, NULL, NULL, '2026-10-30'),
 ('JOB-2026-004', N'Ông Văn Bình',     'binh.ov@example.net',  '0912000115', 'LINKEDIN',     16000000, NULL, NULL, NULL, '2026-09-20'),
 ('JOB-2026-004', N'Thái Mỹ Duyên',    'duyen.tm@example.net', '0912000116', 'WEBSITE',      13000000, NULL, NULL, NULL, '2026-09-25')
) AS c(Job, FullName, Email, Phone, Source, Salary, Rating, InterviewAt, Notes, Applied)
JOIN dbo.JobPostings AS jp ON jp.JobCode = c.Job;

UPDATE dbo.Candidates SET Stage = 'SCREENING' WHERE Email IN ('quan.nm@example.net','ngoc.tb@example.net','phat.lv@example.net',
    'hang.vt@example.net','anh.hn@example.net','phuc.dc@example.net','lam.bv@example.net','thuy.mt@example.net',
    'tien.pv@example.net','phong.ad@example.net','binh.ov@example.net');
UPDATE dbo.Candidates SET Stage = 'INTERVIEW' WHERE Email IN ('quan.nm@example.net','ngoc.tb@example.net','hang.vt@example.net',
    'phuc.dc@example.net','thuy.mt@example.net','phong.ad@example.net');
UPDATE dbo.Candidates SET Stage = 'OFFER' WHERE Email IN ('ngoc.tb@example.net','phuc.dc@example.net');
UPDATE c SET Stage = 'HIRED', HiredEmployeeId = e.EmployeeId
FROM dbo.Candidates AS c JOIN dbo.Employees AS e ON e.EmployeeCode = 'EMP0019'
WHERE c.Email = 'phuc.dc@example.net';
UPDATE dbo.Candidates SET Stage = 'REJECTED'  WHERE Email IN ('hang.vt@example.net','lam.bv@example.net','thuy.mt@example.net');
UPDATE dbo.Candidates SET Stage = 'WITHDRAWN' WHERE Email IN ('anh.hn@example.net');

UPDATE h SET ChangedAt = DATEADD(HOUR, 9, CAST(DATEADD(DAY, 4 * (x.StepNo - 1), c.AppliedDate) AS DATETIME2(0))),
             ChangedBy = 'hr'
FROM dbo.CandidateStageHistory AS h
JOIN dbo.Candidates AS c ON c.CandidateId = h.CandidateId
JOIN (SELECT HistoryId, ROW_NUMBER() OVER (PARTITION BY CandidateId ORDER BY HistoryId) AS StepNo
      FROM dbo.CandidateStageHistory) AS x ON x.HistoryId = h.HistoryId;
UPDATE dbo.Candidates SET CreatedAt = CAST(AppliedDate AS DATETIME2(0));
GO

INSERT INTO dbo.ReviewCriteria (CriteriaName, Description, Weight, DisplayOrder) VALUES
 (N'Chất lượng công việc',    N'Độ chính xác, đầy đủ, ít lỗi, đáp ứng tiêu chuẩn',     30, 1),
 (N'Hiệu suất & tiến độ',     N'Khối lượng công việc, đúng hạn, đạt KPI',                25, 2),
 (N'Kỹ năng chuyên môn',      N'Kiến thức, kỹ năng nghiệp vụ, khả năng học hỏi',         20, 3),
 (N'Tinh thần hợp tác',       N'Làm việc nhóm, hỗ trợ đồng nghiệp, giao tiếp',           15, 4),
 (N'Chủ động & sáng tạo',     N'Đề xuất cải tiến, tự giải quyết vấn đề',                 10, 5);

INSERT INTO dbo.ReviewCycles (CycleName, PeriodStart, PeriodEnd, DueDate, Status, CreatedBy) VALUES
 (N'Đánh giá 6 tháng đầu năm 2026',  '2026-01-01', '2026-06-30', '2026-07-15', 'CLOSED', 'hr'),
 (N'Đánh giá 6 tháng cuối năm 2026', '2026-07-01', '2026-12-31', '2027-01-15', 'OPEN',   'hr');

DECLARE @target TABLE (ReviewId INT PRIMARY KEY, TargetStatus VARCHAR(20));

INSERT INTO dbo.PerformanceReviews (CycleId, EmployeeId, ReviewerId, Status)
SELECT c.CycleId, e.EmployeeId, e.ManagerId, 'PENDING_SELF'
FROM dbo.ReviewCycles AS c
JOIN dbo.Employees AS e ON e.HireDate <= c.PeriodEnd AND e.ManagerId IS NOT NULL
WHERE e.Status IN ('ACTIVE', 'PROBATION');

INSERT INTO @target (ReviewId, TargetStatus)
SELECT r.ReviewId,
       CASE WHEN c.Status = 'CLOSED' THEN 'COMPLETED'
            WHEN e.EmployeeCode IN ('EMP0016', 'EMP0012') THEN 'PENDING_SELF'
            WHEN e.EmployeeCode IN ('EMP0013', 'EMP0014', 'EMP0004') THEN 'PENDING_MANAGER'
            ELSE CASE ABS(CHECKSUM(e.EmployeeId, c.CycleId) % 10)
                      WHEN 0 THEN 'COMPLETED' WHEN 1 THEN 'COMPLETED' WHEN 2 THEN 'COMPLETED'
                      WHEN 3 THEN 'PENDING_MANAGER' WHEN 4 THEN 'PENDING_MANAGER' WHEN 5 THEN 'PENDING_MANAGER'
                      ELSE 'PENDING_SELF' END
       END
FROM dbo.PerformanceReviews AS r
JOIN dbo.ReviewCycles AS c ON c.CycleId = r.CycleId
JOIN dbo.Employees AS e ON e.EmployeeId = r.EmployeeId;

INSERT INTO dbo.ReviewScores (ReviewId, CriteriaId, SelfScore, ManagerScore)
SELECT t.ReviewId, rc.CriteriaId,
       3 + ABS(CHECKSUM(t.ReviewId, rc.CriteriaId) % 3),
       CASE WHEN t.TargetStatus = 'COMPLETED' THEN 2 + ABS(CHECKSUM(rc.CriteriaId, t.ReviewId * 13) % 4) END
FROM @target AS t
CROSS JOIN dbo.ReviewCriteria AS rc
WHERE t.TargetStatus IN ('PENDING_MANAGER', 'COMPLETED');

UPDATE r SET
    Status       = t.TargetStatus,
    SelfScore    = s.SelfAvg,
    ManagerScore = s.MgrAvg,
    FinalScore   = f.Final,
    Rating       = CASE WHEN f.Final IS NULL THEN NULL WHEN f.Final >= 4.5 THEN 'A' WHEN f.Final >= 3.5 THEN 'B'
                        WHEN f.Final >= 2.5 THEN 'C' WHEN f.Final >= 1.5 THEN 'D' ELSE 'E' END,
    SelfComment  = CASE ABS(CHECKSUM(r.ReviewId) % 3)
                        WHEN 0 THEN N'Hoàn thành đầy đủ các mục tiêu, chủ động hỗ trợ đồng nghiệp.'
                        WHEN 1 THEN N'Đã cải thiện kỹ năng chuyên môn, cần quản lý thời gian tốt hơn.'
                        ELSE N'Đảm bảo tiến độ công việc, mong muốn được tham gia dự án lớn hơn.' END,
    ManagerComment = CASE WHEN t.TargetStatus = 'COMPLETED' THEN
                        CASE ABS(CHECKSUM(r.ReviewId * 7) % 3)
                             WHEN 0 THEN N'Làm việc có trách nhiệm, chất lượng ổn định.'
                             WHEN 1 THEN N'Cần chủ động hơn trong việc đề xuất giải pháp.'
                             ELSE N'Đóng góp tốt cho đội nhóm, tiếp tục phát huy.' END END,
    SelfSubmittedAt    = CASE WHEN c.Status = 'CLOSED' THEN DATEADD(DAY, -12, CAST(c.DueDate AS DATETIME2(0)))
                              ELSE DATEADD(DAY, -(3 + ABS(CHECKSUM(r.ReviewId) % 7)), SYSUTCDATETIME()) END,
    ManagerSubmittedAt = CASE WHEN t.TargetStatus <> 'COMPLETED' THEN NULL
                              WHEN c.Status = 'CLOSED' THEN DATEADD(DAY, -5, CAST(c.DueDate AS DATETIME2(0)))
                              ELSE DATEADD(DAY, -(1 + ABS(CHECKSUM(r.ReviewId) % 2)), SYSUTCDATETIME()) END
FROM dbo.PerformanceReviews AS r
JOIN @target AS t ON t.ReviewId = r.ReviewId
JOIN dbo.ReviewCycles AS c ON c.CycleId = r.CycleId
CROSS APPLY (
    SELECT CAST(SUM(rs.SelfScore * rc.Weight) / SUM(rc.Weight) AS DECIMAL(3,2)) AS SelfAvg,
           CAST(SUM(ISNULL(rs.ManagerScore, 0) * rc.Weight)
                / NULLIF(SUM(CASE WHEN rs.ManagerScore IS NOT NULL THEN rc.Weight ELSE 0 END), 0) AS DECIMAL(3,2)) AS MgrAvg
    FROM dbo.ReviewScores AS rs
    JOIN dbo.ReviewCriteria AS rc ON rc.CriteriaId = rs.CriteriaId
    WHERE rs.ReviewId = r.ReviewId
) AS s
CROSS APPLY (SELECT CAST(ROUND(c.SelfWeight * s.SelfAvg + (1 - c.SelfWeight) * s.MgrAvg, 2) AS DECIMAL(3,2)) AS Final) AS f
WHERE t.TargetStatus IN ('PENDING_MANAGER', 'COMPLETED');
GO

EXEC dbo.sp_CalculateMonthlyPayroll @PeriodYear = 2026, @PeriodMonth = 8,  @CalculatedBy = 'hr';
EXEC dbo.sp_CalculateMonthlyPayroll @PeriodYear = 2026, @PeriodMonth = 9,  @CalculatedBy = 'hr';
EXEC dbo.sp_CalculateMonthlyPayroll @PeriodYear = 2026, @PeriodMonth = 10, @CalculatedBy = 'hr';

UPDATE dbo.PayrollPeriods
   SET Status = 'PAID', ApprovedBy = 'admin',
       ApprovedAt = DATEADD(HOUR, 2, CAST(DATEADD(DAY, 3, EOMONTH(DATEFROMPARTS(PeriodYear, PeriodMonth, 1))) AS DATETIME2(0))),
       PaidAt     = DATEADD(HOUR, 3, CAST(DATEADD(DAY, 5, EOMONTH(DATEFROMPARTS(PeriodYear, PeriodMonth, 1))) AS DATETIME2(0)))
WHERE PeriodYear = 2026 AND PeriodMonth IN (8, 9);
GO

PRINT N'Seed completed.';
SELECT 'Departments' AS TableName, COUNT(*) AS Rows FROM dbo.Departments
UNION ALL SELECT 'Positions', COUNT(*) FROM dbo.Positions
UNION ALL SELECT 'Employees', COUNT(*) FROM dbo.Employees
UNION ALL SELECT 'Users', COUNT(*) FROM dbo.Users
UNION ALL SELECT 'Attendance', COUNT(*) FROM dbo.Attendance
UNION ALL SELECT 'LeaveRequests', COUNT(*) FROM dbo.LeaveRequests
UNION ALL SELECT 'Candidates', COUNT(*) FROM dbo.Candidates
UNION ALL SELECT 'PerformanceReviews', COUNT(*) FROM dbo.PerformanceReviews
UNION ALL SELECT 'Payroll', COUNT(*) FROM dbo.Payroll;
GO
