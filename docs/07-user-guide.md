# 07 – Hướng dẫn sử dụng

> Dành cho người dùng cuối: nhân viên, quản lý, phòng Nhân sự và quản trị hệ thống.
> Mỗi người chỉ thấy các mục menu đúng với vai trò của mình. Môi trường demo: mật khẩu mọi tài khoản là `Demo@123`.

| Tài khoản demo | Vai trò | Dùng để xem |
|---|---|---|
| `employee` | Nhân viên (Trần Quốc Việt – Lập trình viên) | chấm công, nghỉ phép, phiếu lương, tự đánh giá |
| `manager` | Quản lý (Trịnh Đức Minh – Trưởng phòng CNTT) | + duyệt phép, dữ liệu chấm công và đánh giá của đội |
| `hr` | Nhân sự (Lê Thị Lan – Trưởng phòng Nhân sự) | + hồ sơ, bảng lương, tuyển dụng, kỳ đánh giá, báo cáo, import |
| `admin` | Quản trị hệ thống | + tài khoản, nhật ký hệ thống, kiểm tra dữ liệu, tham số lương |

## 1. Đăng nhập và tài khoản

- Đăng nhập bằng tên đăng nhập và mật khẩu ([ảnh](images/01-login.png)). Nhập sai **5 lần liên tiếp** thì tài khoản bị **khóa 15 phút**
  (thông báo cho biết giờ mở lại); quản trị viên có thể mở khóa sớm.
- Tài khoản mới hoặc vừa được đặt lại mật khẩu dùng **mật khẩu tạm** và phải đổi ở lần đăng nhập đầu tiên.
- Đổi mật khẩu: menu tên người dùng (góc phải) → **Đổi mật khẩu**. Mật khẩu mới tối thiểu 8 ký tự, có chữ hoa, chữ thường và chữ số,
  không trùng tên đăng nhập.
- Phiên đăng nhập hết hạn sau 30 phút không thao tác. Nếu gặp lỗi, trang lỗi hiện **Mã tra cứu** – gửi mã đó cho bộ phận IT.

## 2. Nhân viên

### Tổng quan
Trang đầu tiên sau khi đăng nhập: trạng thái chấm công hôm nay, số ngày phép còn lại, lương thực lĩnh kỳ gần nhất và việc cần làm
(phiếu tự đánh giá, đơn đang chờ duyệt).

### Chấm công ([ảnh](images/09-attendance-employee.png))
- **Chấm công → Check-in** khi đến, **Check-out** khi về. Giờ được lấy theo **đồng hồ máy chủ**, không theo máy của bạn.
- Giờ vào làm 08:30, ân hạn 5 phút: check-in sau **08:35** là *Đi muộn* (số phút muộn tính từ 08:30). Làm **dưới 4 giờ** là *Nửa ngày*.
- Mỗi ngày chỉ check-in một lần; không check-in được vào ngày đang nghỉ phép đã duyệt.
- Bảng bên dưới và ô "Tổng hợp tháng" hiển thị công từng ngày; đổi tháng bằng ô chọn tháng. Quên chấm công thì báo phòng Nhân sự điều chỉnh.

### Nghỉ phép
1. **Nghỉ phép → Tạo đơn nghỉ**, chọn loại nghỉ, ngày bắt đầu/kết thúc, nhập lý do → **Gửi đơn**.
2. Hệ thống tự đếm **ngày làm việc** (bỏ thứ Bảy, Chủ nhật, ngày lễ) và kiểm tra:
   số ngày phép **khả dụng** (đã trừ các đơn đang chờ duyệt), đơn không chồng lên đơn khác, không kéo sang năm sau, không lùi quá 30 ngày.
3. Đơn chờ duyệt có thể **Hủy**; đơn đã duyệt chỉ hủy được khi chưa tới ngày nghỉ (ngày phép được hoàn lại).

### Phiếu lương ([ảnh](images/10-payslip.png))
- **Phiếu lương** liệt kê các kỳ đã được duyệt; mở từng phiếu để xem ngày công, thu nhập, bảo hiểm, thuế TNCN và thực lĩnh; nút **In phiếu lương**.
- Phiếu của kỳ chưa duyệt không hiển thị; không ai ngoài bạn và phòng Nhân sự xem được phiếu lương của bạn.
- **Tính thử**: nhập lương, phụ cấp, số người phụ thuộc để ước tính lương thực lĩnh theo tham số hiện hành.

### Đánh giá của tôi ([ảnh](images/11-performance-review.png))
Khi kỳ đánh giá mở, phiếu xuất hiện ở trạng thái *Chờ tự đánh giá*: chấm **1–5** cho từng tiêu chí, viết nhận xét, gửi.
Sau đó quản lý chấm; điểm cuối = 30 % điểm tự đánh giá + 70 % điểm quản lý, xếp loại A (≥ 4,5) · B (≥ 3,5) · C (≥ 2,5) · D (≥ 1,5) · E.

## 3. Quản lý

| Việc | Ở đâu | Ghi chú |
|---|---|---|
| Duyệt / từ chối đơn nghỉ ([ảnh](images/08-leave-approvals.png)) | **Duyệt nghỉ phép** | chỉ thấy đơn của nhân viên mình quản lý; từ chối phải ghi lý do; không tự duyệt đơn của chính mình |
| Xem chấm công của đội | **Dữ liệu chấm công** | lọc theo ngày, nhân viên, trạng thái |
| Xem hồ sơ nhân viên | **Nhân viên** | chỉ nhân viên trong đội; không hiển thị lương, CCCD, tài khoản ngân hàng |
| Chấm điểm đánh giá | **Đánh giá hiệu suất** | phiếu ở trạng thái *Chờ quản lý chấm* của nhân viên do bạn đánh giá |
| Ứng viên của phòng mình | **Tuyển dụng** | xem pipeline, cho điểm sao, lịch phỏng vấn, ghi chú |
| Báo cáo phòng mình | **Báo cáo** | thống kê nhân sự, chấm công của các phòng bạn phụ trách |

## 4. Phòng Nhân sự

### Hồ sơ nhân viên ([ảnh](images/03-employees.png))
- **Nhân viên**: tìm theo mã, tên (gõ không dấu cũng được), email, SĐT; lọc phòng ban/chức danh/trạng thái; sắp xếp theo cột; **Xuất Excel** theo bộ lọc.
- **Thêm nhân viên** / **Sửa**: mã nhân viên gợi ý tự động; lương cơ bản phải nằm trong **khung lương của chức danh**; nhân viên phải đủ 15 tuổi
  tại ngày vào làm; không chọn được quản lý tạo thành vòng lặp. Đổi lương được ghi vào **lịch sử lương** (ai đổi, lúc nào).
- **Xóa** chỉ áp dụng cho hồ sơ chưa có dữ liệu chấm công, nghỉ phép, lương, đánh giá; các trường hợp khác chuyển trạng thái *Đã nghỉ việc*.
- **Phòng ban**, **Chức danh**: quản lý danh mục và khung lương; không xóa được khi còn nhân viên.

### Chấm công và nghỉ phép
- **Dữ liệu chấm công → Nhập / điều chỉnh**: sửa công một ngày (trạng thái và số phút muộn tính lại tự động). Máy chấm công có thể gửi dữ liệu qua API (`docs/api.md`).
- **Duyệt nghỉ phép** hiển thị đơn của toàn công ty. Đầu năm khởi tạo số ngày phép cho tất cả nhân viên (12 ngày + 1 ngày cho mỗi 5 năm thâm niên; người vào làm trong năm được tính theo tỷ lệ số tháng làm việc).

### Bảng lương ([ảnh](images/04-payroll-period.png))
1. **Bảng lương → Tính lương** cho tháng cần tính (không tính được tháng tương lai). Có thể tính lại nhiều lần khi dữ liệu công thay đổi.
2. Kiểm tra bảng lương: tổng gross/net, tổng theo phòng ban, từng phiếu; tải **Excel** (định dạng sẵn, có dòng tổng) hoặc **CSV**.
3. **Duyệt** → kỳ lương bị khóa, nhân viên bắt đầu xem được phiếu lương.
4. Sau khi chuyển khoản: **Đã chi trả**. Kỳ đã duyệt/đã chi không tính lại được.

Công thức: lương và phụ cấp theo ngày công hưởng lương; BHXH 8 %, BHYT 1,5 % (trần 20 × lương cơ sở), BHTN 1 % (trần 20 × lương tối thiểu vùng),
không đóng khi nghỉ không lương từ 14 ngày; giảm trừ bản thân 15,5 triệu, người phụ thuộc 6,2 triệu; thuế TNCN lũy tiến 7 bậc.

### Tuyển dụng ([ảnh](images/05-recruitment-pipeline.png))
1. **Tuyển dụng → Tạo tin tuyển dụng** (Nháp) → chuyển *Đang tuyển* để nhận hồ sơ.
2. **Thêm ứng viên** vào tin đang tuyển; bảng pipeline: Mới nộp → Sàng lọc → Phỏng vấn → Đề nghị nhận việc; có thể Từ chối hoặc ghi nhận Rút hồ sơ ở mọi bước.
3. Ứng viên ở bước *Đề nghị nhận việc* → **Tiếp nhận**: hệ thống tạo hồ sơ nhân viên thử việc; tin tự đóng khi đủ số lượng cần tuyển.

### Đánh giá hiệu suất
**Đánh giá hiệu suất → Tạo kỳ đánh giá** (Nháp) → **Mở kỳ**: hệ thống tạo phiếu cho mọi nhân viên đang làm việc có người đánh giá
(tổng trọng số tiêu chí phải bằng 100 %) → theo dõi tiến độ → **Đóng kỳ**; xem phân bố xếp loại.

### Báo cáo ([ảnh](images/06-reports.png)) và import ([ảnh](images/07-import.png))
- **Báo cáo**: thống kê theo phòng ban, báo cáo chấm công theo khoảng ngày, danh sách nhân viên – xem trên màn hình hoặc tải Excel/CSV.
- **Import dữ liệu**: tải **file mẫu CSV**, điền dữ liệu (hoặc dùng .xlsx cùng tiêu đề cột), chạy **Chỉ kiểm tra** trước, sửa theo bảng lỗi
  (đúng dòng, đúng cột; tải **Báo cáo lỗi (CSV)**), rồi chọn **Import dòng hợp lệ** hoặc **Tất cả hoặc không**. Chi tiết cột: `docs/api.md` mục 8.

## 5. Quản trị hệ thống

| Mục | Chức năng |
|---|---|
| **Tài khoản** | **Tạo tài khoản** cho nhân viên (mật khẩu tạm hiển thị **một lần**), đổi vai trò, vô hiệu hóa, **Đặt lại mật khẩu**, **Mở khóa** tài khoản bị khóa |
| **Nhật ký hệ thống** | tra cứu ai đã làm gì: đăng nhập, sửa lương, duyệt phép, tính/duyệt lương, import… lọc theo người, hành động, thời gian |
| **Kiểm tra dữ liệu** | chạy 16 kiểm tra toàn vẹn (trùng mã, lương âm, phép chồng lấn, bảng lương sai công thức…) |
| **Tham số lương** | tỷ lệ bảo hiểm, lương cơ sở, lương tối thiểu vùng, mức giảm trừ… – áp dụng cho lần tính lương tiếp theo |

Quản trị viên không thể tự vô hiệu hóa hoặc tự gỡ quyền quản trị của chính mình.

## 6. Câu hỏi thường gặp

| Câu hỏi | Trả lời |
|---|---|
| Check-in báo "đã check-in hôm nay"? | Mỗi ngày chỉ một lần check-in; giờ check-in đã ghi hiển thị ngay trên thẻ chấm công. |
| Không gửi được đơn nghỉ vì "không đủ số ngày phép"? | Số ngày khả dụng đã trừ các đơn đang chờ duyệt; hủy bớt đơn chờ hoặc chọn loại nghỉ khác. |
| Không thấy phiếu lương tháng này? | Phiếu chỉ hiển thị sau khi phòng Nhân sự **duyệt** bảng lương. |
| Tài khoản bị khóa? | Chờ 15 phút hoặc nhờ quản trị viên mở khóa. |
| Import Excel báo số điện thoại sai dù nhìn đúng? | Excel đã bỏ số 0 đầu vì cột đang là kiểu số – định dạng cột SĐT/CCCD là Text rồi nhập lại. |
| Gặp trang lỗi? | Gửi **Mã tra cứu** trên trang lỗi cho bộ phận IT để tra log. |
