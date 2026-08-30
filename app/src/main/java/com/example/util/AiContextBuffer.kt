package com.example.util

import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import com.example.viewmodel.MonthlySalaryPoint
import com.example.viewmodel.SalarySummary
import java.text.DecimalFormat

/**
 * AiContextBuffer: Bộ đệm ngữ cảnh màn hình thời gian thực (Real-time Live Screen Context Buffer)
 * Cho phép các mô hình Trợ Lý AI (Gemini, OpenRouter) đọc trực tiếp các thông tin quan trọng 
 * đang hiển thị trên màn hình hiện tại (như Bảng Lương Thực Tế, Bảng LƯƠNG DỰ KIẾN, số ngày công, 
 * ca làm việc, hệ số OT, cài đặt hợp đồng, 12 phụ cấp) trước khi trả lời người dùng.
 */
object AiContextBuffer {

    fun buildScreenContextBuffer(
        currentTab: String,
        userConfig: UserConfig?,
        summaryState: SalarySummary?,
        timeEntries: List<TimeEntry>,
        salaryHistory: List<MonthlySalaryPoint>
    ): String {
        val fmt = DecimalFormat("#,###")

        val tabNameVi = when (currentTab) {
            "home" -> "Trang Chủ Chấm Công (Giao diện theo dõi ngày công & giờ làm hôm nay)"
            "history" -> "Lịch Sử Chấm Công (Giao diện chi tiết nhật ký vào/ra ca)"
            "payslip" -> "Phiếu Lương & Thu Nhập (Giao diện Bảng lương thực tế & Bảng lương dự kiến)"
            "settings" -> "Cài Đặt Ứng Dụng (Giao diện thiết lập thông số lương, BHXH, phụ cấp & AI)"
            "admin" -> "Quản Trị Hệ Thống (Giao diện quản lý toàn bộ cấu hình công ty)"
            "notifications" -> "Trung Tâm Thông Báo (Giao diện nhắc nhở ca làm & chốt lương)"
            else -> "Màn hình ứng dụng ($currentTab)"
        }

        // 1. SYSTEM & SCREEN PERMISSION BUFFER
        val permissionsBuffer = """
            ===================================================================================
            [CONTEXT BUFFER - CẤP FULL QUYỀN ĐỌC, THÊM, SỬA, XÓA DỮ LIỆU & ĐIỀU KHIỂN HỆ THỐNG]
            ===================================================================================
            - TRẠNG THÁI QUYỀN ĐỌC (READ): AI ĐÃ ĐƯỢC CẤP QUYỀN ĐỌC 100% TOÀN BỘ DỮ LIỆU ĐANG HIỂN THỊ TRÊN MÀN HÌNH (Lương thực tế, Lương dự kiến tròn tháng, 12 phụ cấp, giờ OT, BHXH, lịch sử).
            - TRẠNG THÁI QUYỀN THỰC THI (FULL ACTION WRITE / ADD / EDIT / DELETE / CONTROL): AI ĐÃ ĐƯỢC CẤP TOÀN QUYỀN TRỢ LÝ CÁ NHÂN ĐỂ THAY MẶT NGƯỜI DÙNG THỰC HIỆN BẤT KỲ THAO TÁC NÀO TRONG ỨNG DỤNG.
            - MÀN HÌNH NGƯỜI DÙNG ĐANG MỞ: $tabNameVi
            - QUY TẮC PHÁT ÂM & CHÍNH TẢ VIỆT NAM:
              * Trả lời bằng tiếng Việt chuẩn ngữ pháp, rõ ràng, gãy gọn, KHÔNG chèn khoảng trắng thừa trước dấu câu (dấu phẩy, dấu chấm, dấu hai chấm).
              * Viết số tiền rõ ràng (ví dụ: 12.000.000đ hoặc 12 triệu đồng), tránh viết ngắt quãng.
            - QUY TẮC THỰC THI HÀNH ĐỘNG:
              Khi người dùng yêu cầu THỰC HIỆN BẤT KỲ THAO TÁC NÀO (Chấm công, thêm công, nghỉ phép, xóa công, chỉnh lương/phụ cấp, xem tháng khác, đồng bộ, đọc thông báo, chuyển màn hình), bạn HÃY GẮN MÃ HÀNH ĐỘNG TƯƠNG ỨNG Ở CUỐI CÂU TRẢ LỜI:
              + Chấm công vào ca: [[ACTION:CHECK_IN]] hoặc [[ACTION:CHECK_IN:07:30]]
              + Chấm công ra ca: [[ACTION:CHECK_OUT]] hoặc [[ACTION:CHECK_OUT:19:30]]
              + Thêm/Làm bù ngày công: [[ACTION:ADD_WORK_DAY:YYYY-MM-DD|07:30|19:30|NORMAL|Ghi chú]] (ví dụ: [[ACTION:ADD_WORK_DAY:2026-08-25|07:30|19:30|NORMAL|Làm bù]])
              + Thêm ngày nghỉ phép/lễ: [[ACTION:ADD_LEAVE_DAY:YYYY-MM-DD|PAID_LEAVE|Nghỉ phép năm]] hoặc [[ACTION:ADD_LEAVE_DAY:YYYY-MM-DD|UNPAID_LEAVE|Nghỉ việc riêng]] hoặc [[ACTION:ADD_LEAVE_DAY:YYYY-MM-DD|HOLIDAY|Nghỉ lễ]]
              + Thêm nhiều ngày công hàng loạt: [[ACTION:ADD_BULK_WORK_DAYS:2026-08-01,2026-08-02,2026-08-03|07:30|19:30]]
              + Xóa 1 ngày công: [[ACTION:DELETE_DATE:YYYY-MM-DD]]
              + Xóa nhiều ngày công: [[ACTION:DELETE_DATES:YYYY-MM-DD,YYYY-MM-DD]]
              + Xóa toàn bộ công tháng: [[ACTION:CLEAR_MONTH]]
              + Cập nhật Lương cơ bản: [[ACTION:UPDATE_BASE_SALARY:12000000]]
              + Cập nhật Phụ cấp: [[ACTION:UPDATE_ALLOWANCE:pcTrachNhiem|1000000]] (các phụ cấp: pcKyThuat, pcTrachNhiem, pcChucVu, pcHieuSuat, pcSanPham, pcComCa, pcComOt, pcNhaO, pcDocHai, pcDtDoanhThu, pcXangXe, pcThamNien, pcCaDem, tienChuyenCanGoc, luongDongBaoHiem, doanPhiCongDoan, tiLeDongBaoHiem)
              + Cập nhật Quỹ phép năm: [[ACTION:UPDATE_LEAVE_QUOTA:12]]
              + Cập nhật Thông tin cá nhân: [[ACTION:UPDATE_USER_INFO:hoVaTen|Nguyễn Văn A]] hoặc [[ACTION:UPDATE_USER_INFO:boPhan|Kỹ thuật]] hoặc [[ACTION:UPDATE_USER_INFO:maNhanVien|NV001]] hoặc [[ACTION:UPDATE_USER_INFO:soDienThoai|0912345678]]
              + Cập nhật Cài đặt hệ thống: [[ACTION:UPDATE_CONFIG:ngayChotLuong|25]] hoặc [[ACTION:UPDATE_CONFIG:soGioNghiGiaiLao|1.5]] hoặc [[ACTION:UPDATE_CONFIG:lichTrinh|07:30 - 19:30]]
              + Xem/chuyển tháng làm việc: [[ACTION:SELECT_MONTH:YYYY-MM]] (ví dụ: [[ACTION:SELECT_MONTH:2026-07]])
              + Đánh dấu đã đọc thông báo: [[ACTION:MARK_NOTIFICATIONS_READ]]
              + Đồng bộ dữ liệu lên máy chủ: [[ACTION:SYNC_DATA]]
              + Cập nhật Ghi chú công hôm nay: [[ACTION:UPDATE_NOTE:Nội dung ghi chú]]
              + Chuyển tab màn hình: [[ACTION:NAVIGATE_TAB:home|payslip|history|settings|admin|notifications]]
        """.trimIndent()

        // 2. DISPLAYED SALARY & WORK LOG BUFFER
        val salaryBuffer = summaryState?.let { s ->
            val hourlyRate = if (s.standardWorkDays > 0 && s.baseBasicSalary > 0) {
                s.baseBasicSalary / (s.standardWorkDays * 8.0)
            } else 0.0

            val config = userConfig
            val lcbFull = config?.luongCoBan ?: s.baseBasicSalary
            val totalPhuCapCoDinh = config?.let { c ->
                c.pcKyThuat + c.pcTrachNhiem + c.pcChucVu + c.pcHieuSuat +
                        c.pcSanPham + c.pcNhaO + c.pcDocHai + c.pcDtDoanhThu +
                        c.pcXangXe + c.pcThamNien + c.pcKhac1 + c.tienChuyenCanGoc
            } ?: s.phuCap

            val estimatedFullMonthNet = if (config != null) {
                lcbFull + totalPhuCapCoDinh + (s.standardWorkDays * config.pcComCa) - s.tienBh - s.doanPhi + s.tienOtNgay + s.tienOtDem + s.tienChuNhat + s.tienOtLe + s.thuong - s.tienKhauTruNghi
            } else {
                s.luongThucNhan
            }

            """
            -----------------------------------------------------------------------------------
            [BUFFER DỮ LIỆU LƯƠNG & NGÀY CÔNG TRỰC TIẾP HIỂN THỊ TRÊN MÀN HÌNH (LIVE DISPLAYED DATA)]
            -----------------------------------------------------------------------------------
            1. BẢNG LƯƠNG THỰC TẾ (Làm đến hôm nay):
               * LƯƠNG THỰC NHẬN ĐẾN HÔM NAY (NET): ${fmt.format(s.luongThucNhan)}đ
               * Đơn giá 1 giờ công chuẩn (LCB / Công chuẩn / 8g): ${fmt.format(hourlyRate)}đ/giờ
               * Số ngày công thực tế làm được: ${s.workingDays} / ${s.standardWorkDays} ngày công
               * Tổng số giờ làm chuẩn: ${s.standardHours} giờ | Số ca làm đêm: ${s.caDemCount} ca

            2. BẢNG LƯƠNG DỰ KIẾN (Dự báo nếu đi làm đủ tròn tháng):
               * LƯƠNG DỰ KIẾN TRÒN THÁNG (NET): ${fmt.format(estimatedFullMonthNet)}đ
               * Ngày công dự kiến tròn tháng: ${s.standardWorkDays} ngày công
               * Lương cơ bản dự kiến đủ công: ${fmt.format(lcbFull)}đ
               * Tổng phụ cấp dự kiến đủ tháng: ${fmt.format(totalPhuCapCoDinh)}đ
               * Dự tính tiền cơm ca tròn tháng: ${fmt.format(s.standardWorkDays * (config?.pcComCa ?: 0.0))}đ

            3. CHI TIẾT CÁC KHOẢN CÔNG & TĂNG CA VÀO CA THÁNG NÀY:
               * OT Ngày thường: ${s.otDayHours} giờ  => Thành tiền OT ngày: ${fmt.format(s.tienOtNgay)}đ
               * OT Ca đêm: ${s.otNightHours} giờ       => Thành tiền OT đêm: ${fmt.format(s.tienOtDem)}đ
               * Tăng ca Chủ nhật: ${s.chuNhatHours} giờ (Ngày: ${s.chuNhatDayHours}g, Đêm: ${s.chuNhatNightHours}g) => Tiền CN: ${fmt.format(s.tienChuNhat)}đ (CN Ngày: ${fmt.format(s.tienChuNhatNgay)}đ, CN Đêm: ${fmt.format(s.tienChuNhatDem)}đ)
               * Tăng ca Ngày lễ/Tết: ${s.otLeHours} giờ => Thành tiền OT lễ: ${fmt.format(s.tienOtLe)}đ
               * Tiền cơm: Cơm ca + Cơm OT = ${fmt.format(s.tongTienCom)}đ
               * Tổng phụ cấp đã nhận: ${fmt.format(s.phuCap)}đ | Tiền thưởng: ${fmt.format(s.thuong)}đ
               * Các khoản trích trừ: BHXH (${fmt.format(s.tienBh)}đ), Đoàn phí (${fmt.format(s.doanPhi)}đ), Phạt/Nghỉ không lương (${fmt.format(s.tienKhauTruNghi)}đ)
            """.trimIndent()
        } ?: """
            -----------------------------------------------------------------------------------
            [BUFFER DỮ LIỆU LƯƠNG]: Chưa có dữ liệu tổng hợp tháng này.
            -----------------------------------------------------------------------------------
        """.trimIndent()

        // 3. CONTRACT & ALLOWANCE BUFFER
        val configBuffer = userConfig?.let { c ->
            val totalPhuCap = c.pcKyThuat + c.pcTrachNhiem + c.pcChucVu + c.pcHieuSuat +
                    c.pcSanPham + c.pcComCa + c.pcComOt + c.pcNhaO + c.pcDocHai +
                    c.pcDtDoanhThu + c.pcXangXe + c.pcThamNien + c.pcCaDem + c.pcKhac1

            """
            -----------------------------------------------------------------------------------
            [BUFFER HỢP ĐỒNG & 12 PHỤ CẤP NHÂN VIÊN (EMPLOYEE CONTRACT BUFFER)]
            -----------------------------------------------------------------------------------
            * Nhân viên: ${c.hoVaTen} (Mã NV: ${c.maNhanVien}) | Chức vụ: ${c.roleName.ifBlank { "Nhân viên" }} | Bộ phận: ${c.boPhan.ifBlank { "Chưa phân bổ" }}
            * Công ty: ${c.companyName} | Lịch trình/Ca: ${c.lichTrinh} | Ngày vào làm: ${c.ngayVaoLam.ifBlank { "Chưa cập nhật" }}
            * Lương cơ bản HĐ: ${fmt.format(c.luongCoBan)}đ | Lương đóng BHXH: ${fmt.format(c.luongDongBaoHiem)}đ (${c.tiLeDongBaoHiem}%)
            * Đoàn phí công đoàn: ${fmt.format(c.doanPhiCongDoan)}đ | Ngày chốt lương: Ngày ${c.ngayChotLuong} hàng tháng
            * Hệ số Tăng ca: Ngày thường x${c.heSoOtNgayThuong} | Chủ nhật x${c.heSoOtChuNhat} | Ngày lễ x${c.heSoOtNgayLe} | Ca đêm x${c.heSoOtDem} (Ca đêm: ${c.caDemStart} - ${c.caDemEnd})
            * Giờ nghỉ giải lao: ${c.soGioNghiGiaiLao}g (${if (c.tinhKhauTruNghi) "Có trừ vào tổng giờ công" else "Không trừ vào tổng giờ công"})
            * Quỹ phép năm: ${c.soNgayPhepNam} ngày (Còn lại: ${c.phepNamConLai} ngày) | Chuyên cần gốc: ${fmt.format(c.tienChuyenCanGoc)}đ
            * Chi tiết 12 khoản phụ cấp & hỗ trợ:
              + Cơm ca: ${fmt.format(c.pcComCa)}đ/công | Cơm OT: ${fmt.format(c.pcComOt)}đ/suất
              + Xăng xe: ${fmt.format(c.pcXangXe)}đ | Nhà ở: ${fmt.format(c.pcNhaO)}đ | Điện thoại: ${fmt.format(c.pcDtDoanhThu)}đ
              + Trách nhiệm: ${fmt.format(c.pcTrachNhiem)}đ | Kỹ thuật: ${fmt.format(c.pcKyThuat)}đ | Chức vụ: ${fmt.format(c.pcChucVu)}đ
              + Hiệu suất: ${fmt.format(c.pcHieuSuat)}đ | Sản phẩm: ${fmt.format(c.pcSanPham)}đ | Độc hại: ${fmt.format(c.pcDocHai)}đ
              + Thâm niên: ${fmt.format(c.pcThamNien)}đ | Ca đêm: ${fmt.format(c.pcCaDem)}đ | Khác: ${fmt.format(c.pcKhac1)}đ
              => TỔNG PHỤ CẤP CỐ ĐỊNH: ${fmt.format(totalPhuCap)}đ
            """.trimIndent()
        } ?: "[BUFFER HỢP ĐỒNG]: Chưa thiết lập cấu hình lương nhân viên."

        // 4. HISTORICAL SALARY COMPARISON BUFFER
        val historyBuffer = if (salaryHistory.isNotEmpty()) {
            val historyListStr = salaryHistory.joinToString("\n") { p ->
                "  * Tháng ${p.monthStr}: Lương NET = ${fmt.format(p.luongThucNhan)}đ, Số ngày công = ${p.workingDays} ngày"
            }
            """
            -----------------------------------------------------------------------------------
            [BUFFER LỊCH SỬ LƯƠNG CÁC THÁNG TRƯỚC (6 THÁNG GẦN NHẤT DÙNG ĐỂ SO SÁNH)]
            -----------------------------------------------------------------------------------
            $historyListStr
            """.trimIndent()
        } else {
            "[BUFFER LỊCH SỬ LƯƠNG]: Chưa có dữ liệu lịch sử các tháng trước."
        }

        return """
            $permissionsBuffer
            
            $salaryBuffer
            
            $configBuffer
            
            $historyBuffer
            ===================================================================================
            [CONTEXT BUFFER END]
            ===================================================================================
        """.trimIndent()
    }
}
