package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.auth.UserSession
import com.example.data.AttendanceRecord
import com.example.data.SalaryCalculator
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

data class SalarySummary(
    val workingDays: Double,
    val standardHours: Double,
    val otDayHours: Double,
    val otNightHours: Double,
    val tienOtNgay: Double,
    val tienOtDem: Double,
    val tongTienCom: Double,
    val phuCap: Double,
    val phuCapXangXe: Double = 0.0,
    val phuCapDienThoai: Double = 0.0,
    val phuCapNhaO: Double = 0.0,
    val phuCapChuyenCan: Double = 0.0,
    val thuong: Double,
    val tienBh: Double,
    val doanPhi: Double,
    val tienKhauTruNghi: Double,
    val luongThucNhan: Double,
    val baseBasicSalary: Double = 0.0,
    val expectedWorkDays: Int = 26,
    val standardWorkDays: Int = 26,
    val isCurrentMonth: Boolean = false,
    
    val pcKyThuatVal: Double = 0.0,
    val pcTrachNhiemVal: Double = 0.0,
    val pcChucVuVal: Double = 0.0,
    val pcHieuSuatVal: Double = 0.0,
    val pcSanPhamVal: Double = 0.0,
    val pcComCaVal: Double = 0.0,
    val pcComOtVal: Double = 0.0,
    val pcNhaOVal: Double = 0.0,
    val pcDocHaiVal: Double = 0.0,
    val pcDtDoanhThuVal: Double = 0.0,
    val pcXangXeVal: Double = 0.0,
    val pcThamNienVal: Double = 0.0,
    val pcKhac1Val: Double = 0.0,
    val pcKhacVal: Double = 0.0,
    val pcCaDemVal: Double = 0.0,
    val caDemCount: Int = 0,
    val tienChuNhat: Double = 0.0,
    val tienChuNhatNgay: Double = 0.0,
    val tienChuNhatDem: Double = 0.0,
    val chuNhatHours: Double = 0.0,
    val chuNhatDayHours: Double = 0.0,
    val chuNhatNightHours: Double = 0.0,
    val otLeHours: Double = 0.0,
    val tienOtLe: Double = 0.0
)

object ExportUtils {

    fun isHolidayDate(dateStr: String): Boolean {
        return com.example.data.SalaryCalculator.isHoliday(dateStr)
    }

    fun isSundayDate(dateStr: String): Boolean {
        return com.example.data.SalaryCalculator.isSunday(dateStr)
    }

    fun isRecordInMonth(dateStr: String, monthYmd: String): Boolean {
        if (monthYmd.isEmpty()) return true
        val parts = monthYmd.split("-")
        if (parts.size < 2) return dateStr.contains(monthYmd)
        val year = parts[0]
        val monthStr = parts[1]
        val monthIntStr = monthStr.toIntOrNull()?.toString() ?: monthStr
        
        val slashPattern1 = "/$monthStr/$year"
        val slashPattern2 = "/$monthIntStr/$year"
        val dashPattern1 = "-$monthStr-$year"
        val dashPattern2 = "-$monthIntStr-$year"
        val altDashPattern1 = "$year-$monthStr"
        val altDashPattern2 = "$year-$monthIntStr"
        
        return dateStr.startsWith(altDashPattern1) || 
               dateStr.startsWith(altDashPattern2) ||
               dateStr.contains(slashPattern1) || 
               dateStr.contains(slashPattern2) || 
               dateStr.contains(dashPattern1) || 
               dateStr.contains(dashPattern2) || 
               dateStr.endsWith(slashPattern1) || 
               dateStr.endsWith(slashPattern2)
    }

    fun calculateSalarySummary(
        entries: List<TimeEntry>, 
        config: UserConfig, 
        selectedMonth: String,
        firstEntryDate: String? = null
    ): SalarySummary {
        var targetYear = 2026
        var targetMonth = 5
        try {
            val parts = selectedMonth.split("-")
            targetYear = parts[0].toInt()
            targetMonth = parts[1].toInt()
        } catch (e: Exception) {}

        val todayCal = Calendar.getInstance()
        val currentYear = todayCal.get(Calendar.YEAR)
        val currentMonth = todayCal.get(Calendar.MONTH) + 1
        val todayDayOfMonth = todayCal.get(Calendar.DAY_OF_MONTH)

        val isCurrentSelectedMonth = (targetYear == currentYear && targetMonth == currentMonth)
        val todayStr = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, todayDayOfMonth)

        val maxDaysInMo = Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, targetMonth - 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)

        val holidayDatesInMonth = mutableSetOf<String>()
        for (day in 1..maxDaysInMo) {
            val dateStrYmd = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
            val dateStrDmy = String.format(Locale.US, "%02d/%02d/%04d", day, targetMonth, targetYear)
            if (isHolidayDate(dateStrYmd) || isHolidayDate(dateStrDmy)) {
                holidayDatesInMonth.add(dateStrYmd)
                holidayDatesInMonth.add(dateStrDmy)
            }
        }

        val effectiveJoinDate: String? = if (config.ngayVaoLam.isNotBlank()) {
            config.ngayVaoLam.trim()
        } else if (firstEntryDate != null && firstEntryDate.startsWith(selectedMonth)) {
            firstEntryDate
        } else {
            null
        }

        val effectiveJoinDateYmd: String? = if (!effectiveJoinDate.isNullOrBlank()) {
            val s = effectiveJoinDate.trim()
            if (s.contains("/")) {
                val p = s.split("/")
                if (p.size == 3) {
                    val d = p[0].padStart(2, '0')
                    val m = p[1].padStart(2, '0')
                    val y = p[2]
                    "$y-$m-$d"
                } else s
            } else if (s.contains("-") && !s.startsWith("20")) {
                val p = s.split("-")
                if (p.size == 3) {
                    val d = p[0].padStart(2, '0')
                    val m = p[1].padStart(2, '0')
                    val y = p[2]
                    "$y-$m-$d"
                } else s
            } else s
        } else null

        var expectedWorkDaysSoFar = 0
        var totalWorkDaysInMonth = 0

        for (day in 1..maxDaysInMo) {
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
            val cal = Calendar.getInstance()
            cal.set(targetYear, targetMonth - 1, day)
            val isSunday = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            val isHoliday = isHolidayDate(dateStr)
            if (!isSunday && !isHoliday) {
                totalWorkDaysInMonth++
            }
        }

        if (isCurrentSelectedMonth) {
            for (day in 1 until todayDayOfMonth) {
                val dateStr = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, day)
                if (effectiveJoinDateYmd != null && dateStr < effectiveJoinDateYmd) {
                    continue
                }
                val cal = Calendar.getInstance()
                cal.set(currentYear, currentMonth - 1, day)
                val isSunday = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                val isHoliday = isHolidayDate(dateStr)
                if (!isSunday && !isHoliday) {
                    expectedWorkDaysSoFar++
                }
            }
        } else {
            if (effectiveJoinDateYmd != null && effectiveJoinDateYmd.startsWith(selectedMonth)) {
                for (day in 1..maxDaysInMo) {
                    val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
                    if (dateStr < effectiveJoinDateYmd) {
                        continue
                    }
                    val cal = Calendar.getInstance()
                    cal.set(targetYear, targetMonth - 1, day)
                    val isSunday = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                    val isHoliday = isHolidayDate(dateStr)
                    if (!isSunday && !isHoliday) {
                        expectedWorkDaysSoFar++
                    }
                }
            } else {
                expectedWorkDaysSoFar = totalWorkDaysInMonth
            }
        }

        val monthEntries = entries.filter { e ->
            val parts = selectedMonth.split("-")
            val isSameMonth = if (parts.size == 2) {
                e.date.endsWith("/${parts[1]}/${parts[0]}") || e.date.contains("/${parts[1]}/${parts[0]}") || e.date.startsWith(selectedMonth)
            } else {
                e.date.contains(selectedMonth)
            }
            isSameMonth
        }

        val vmSummary = com.example.data.SalaryCalculator.calculateMonthlySalary(
            entries = monthEntries,
            config = config,
            scheduledDaysSoFar = expectedWorkDaysSoFar,
            totalScheduledDaysInMonth = totalWorkDaysInMonth,
            earliestDate = effectiveJoinDateYmd,
            selectedMonth = selectedMonth,
            todayStr = todayStr,
            isCurrentSelectedMonth = isCurrentSelectedMonth,
            holidayDatesInMonth = holidayDatesInMonth
        )

        return SalarySummary(
            workingDays = vmSummary.workingDays,
            standardHours = vmSummary.standardHours,
            otDayHours = vmSummary.otDayHours,
            otNightHours = vmSummary.otNightHours,
            tienOtNgay = vmSummary.tienOtNgay,
            tienOtDem = vmSummary.tienOtDem,
            tongTienCom = vmSummary.tongTienCom,
            phuCap = vmSummary.phuCap,
            phuCapXangXe = vmSummary.phuCapXangXe,
            phuCapDienThoai = vmSummary.phuCapDienThoai,
            phuCapNhaO = vmSummary.phuCapNhaO,
            phuCapChuyenCan = vmSummary.phuCapChuyenCan,
            thuong = vmSummary.thuong,
            tienBh = vmSummary.tienBh,
            doanPhi = vmSummary.doanPhi,
            tienKhauTruNghi = vmSummary.tienKhauTruNghi,
            luongThucNhan = vmSummary.luongThucNhan,
            baseBasicSalary = vmSummary.baseBasicSalary,
            expectedWorkDays = vmSummary.expectedWorkDays,
            standardWorkDays = vmSummary.standardWorkDays,
            isCurrentMonth = vmSummary.isCurrentMonth,
            pcKyThuatVal = vmSummary.pcKyThuatVal,
            pcTrachNhiemVal = vmSummary.pcTrachNhiemVal,
            pcChucVuVal = vmSummary.pcChucVuVal,
            pcHieuSuatVal = vmSummary.pcHieuSuatVal,
            pcSanPhamVal = vmSummary.pcSanPhamVal,
            pcComCaVal = vmSummary.pcComCaVal,
            pcComOtVal = vmSummary.pcComOtVal,
            pcNhaOVal = vmSummary.pcNhaOVal,
            pcDocHaiVal = vmSummary.pcDocHaiVal,
            pcDtDoanhThuVal = vmSummary.pcDtDoanhThuVal,
            pcXangXeVal = vmSummary.pcXangXeVal,
            pcThamNienVal = vmSummary.pcThamNienVal,
            pcKhac1Val = vmSummary.pcKhac1Val,
            pcKhacVal = vmSummary.pcKhacVal,
            pcCaDemVal = vmSummary.pcCaDemVal,
            caDemCount = vmSummary.caDemCount,
            tienChuNhat = vmSummary.tienChuNhat,
            tienChuNhatNgay = vmSummary.tienChuNhatNgay,
            tienChuNhatDem = vmSummary.tienChuNhatDem,
            chuNhatHours = vmSummary.chuNhatHours,
            chuNhatDayHours = vmSummary.chuNhatDayHours,
            chuNhatNightHours = vmSummary.chuNhatNightHours,
            otLeHours = vmSummary.otLeHours,
            tienOtLe = vmSummary.tienOtLe
        )
    }

    fun savePayslipAsPngImage(
        context: Context,
        summary: SalarySummary,
        config: UserConfig,
        userSession: UserSession?,
        monthLabel: String,
        selectedMonth: String,
        selectedTab: Int = 0,
        includeSundayInProjection: Boolean = false,
        remainingWeekdays: Int = 0,
        remainingSundays: Int = 0,
        dailySalary: Double = 0.0,
        luongDuKienVal: Double = 0.0,
        soNgayCongDuKien: Double = 0.0,
        customOt15DaysCount: Double = 0.0,
        customOt15Pay: Double = 0.0,
        selectedOt15Shift: String = "Đêm",
        customNightAllowance: Double = 0.0,
        hasLoggedUnpaidOrAbsent: Boolean = false
    ): Boolean {
        val df = DecimalFormat("#.#")
        val fmt = DecimalFormat("#,###")

        val todayCal = Calendar.getInstance()
        val currentYear = todayCal.get(Calendar.YEAR)
        val currentMonth = todayCal.get(Calendar.MONTH) + 1
        val isCurrentSelectedMonth = selectedMonth.startsWith(String.format(Locale.US, "%04d-%02d", currentYear, currentMonth))

        // UI Pre-calculations
        val standardTargetDays = (if (isCurrentSelectedMonth) summary.expectedWorkDays else summary.standardWorkDays).toDouble().coerceAtLeast(1.0)
        val effectiveSoNgayCong = if (soNgayCongDuKien > 0.0) {
            soNgayCongDuKien
        } else {
            if (isCurrentSelectedMonth) {
                val rawProjected = summary.workingDays + remainingWeekdays.toDouble()
                standardTargetDays.coerceAtLeast(rawProjected.coerceAtMost(standardTargetDays))
            } else {
                summary.workingDays.coerceAtMost(standardTargetDays)
            }
        }
        val soNgayCongDuKienDouble = effectiveSoNgayCong
        fun calcPrPNG(fieldName: String, valRaw: Double): Double {
            return com.example.data.SalaryCalculator.calculateAllowanceValue(
                fieldName = fieldName,
                allowanceValue = valRaw,
                calcType = config.getCalcTypeFor(fieldName),
                totalWorkDays = soNgayCongDuKienDouble,
                comCaCount = soNgayCongDuKienDouble.toInt(),
                comOtCount = 0,
                nightShiftsCount = summary.caDemCount + (if (selectedTab == 1 && selectedOt15Shift == "Đêm") customOt15DaysCount.toInt() else 0),
                scheduledDaysSoFar = soNgayCongDuKienDouble.toInt(),
                totalScheduledDaysInMonth = standardTargetDays.toInt()
            )
        }

        val pcKyThuatShowPNG = if (selectedTab == 1) calcPrPNG("pcKyThuat", config.pcKyThuat) else summary.pcKyThuatVal
        val pcTrachNhiemShowPNG = if (selectedTab == 1) calcPrPNG("pcTrachNhiem", config.pcTrachNhiem) else summary.pcTrachNhiemVal
        val pcChucVuShowPNG = if (selectedTab == 1) calcPrPNG("pcChucVu", config.pcChucVu) else summary.pcChucVuVal
        val pcHieuSuatShowPNG = if (selectedTab == 1) calcPrPNG("pcHieuSuat", config.pcHieuSuat) else summary.pcHieuSuatVal
        val pcSanPhamShowPNG = if (selectedTab == 1) calcPrPNG("pcSanPham", config.pcSanPham) else summary.pcSanPhamVal

        val pcComCaShowPNG = if (selectedTab == 1) {
            if (isCurrentSelectedMonth) {
                soNgayCongDuKienDouble * config.pcComCa
            } else {
                summary.pcComCaVal
            }
        } else {
            summary.pcComCaVal
        }

        val pcComOtShowPNG = if (selectedTab == 1) {
            summary.pcComOtVal + (customOt15DaysCount * config.pcComOt)
        } else {
            summary.pcComOtVal
        }

        val pcNhaOShowPNG = if (selectedTab == 1) calcPrPNG("pcNhaO", config.pcNhaO) else summary.pcNhaOVal
        val pcDocHaiShowPNG = if (selectedTab == 1) calcPrPNG("pcDocHai", config.pcDocHai) else summary.pcDocHaiVal
        val pcDtDoanhThuShowPNG = if (selectedTab == 1) calcPrPNG("pcDtDoanhThu", config.pcDtDoanhThu) else summary.pcDtDoanhThuVal
        val pcXangXeShowPNG = if (selectedTab == 1) calcPrPNG("pcXangXe", config.pcXangXe) else summary.pcXangXeVal
        val pcKhacShowPNG = if (selectedTab == 1) calcPrPNG("pcCaDem", config.pcCaDem) else summary.pcCaDemVal
        val pcKhac1ShowPNG = if (selectedTab == 1) calcPrPNG("pcKhac1", config.pcKhac1) else summary.pcKhac1Val
        val pcThamNienShowPNG = if (selectedTab == 1) calcPrPNG("pcThamNien", config.pcThamNien) else summary.pcThamNienVal

        val pcChuyenCanShowPNG = if (selectedTab == 1) {
            if (hasLoggedUnpaidOrAbsent) 0.0 else calcPrPNG("tienChuyenCanGoc", config.tienChuyenCanGoc)
        } else {
            summary.phuCapChuyenCan
        }

        // 1. Create offline Bitmap with Dynamic Height
        val width = 1000
        var estimatedHeight = 1850
        
        val bitmap = Bitmap.createBitmap(width, estimatedHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Paints
        val paintBg = Paint().apply { color = android.graphics.Color.parseColor("#0F111A") }
        val paintHeader = Paint().apply { color = android.graphics.Color.parseColor("#1A1D2E") }
        val paintDivider = Paint().apply { color = android.graphics.Color.parseColor("#2C3149"); strokeWidth = 1.5f }
        val paintTextTitle = Paint().apply { color = android.graphics.Color.parseColor("#4C84FF"); textSize = 42f; isFakeBoldText = true; textAlign = Paint.Align.LEFT }
        val paintTextSubTitle = Paint().apply { color = android.graphics.Color.WHITE; textSize = 28f; isFakeBoldText = true }
        val paintTextMonth = Paint().apply { color = android.graphics.Color.parseColor("#8F9BB3"); textSize = 22f }
        val paintLabel = Paint().apply { color = android.graphics.Color.parseColor("#8F9BB3"); textSize = 24f }
        val paintValue = Paint().apply { color = android.graphics.Color.WHITE; textSize = 24f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }
        val paintGreen = Paint().apply { color = android.graphics.Color.parseColor("#00E676"); textSize = 24f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }
        val paintRed = Paint().apply { color = android.graphics.Color.parseColor("#FF5252"); textSize = 24f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }
        val paintBrand = Paint().apply { color = android.graphics.Color.parseColor("#4C84FF"); textSize = 18f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }

        // Background
        canvas.drawRect(0f, 0f, width.toFloat(), estimatedHeight.toFloat(), paintBg)
        
        // Header Card
        canvas.drawRect(0f, 0f, width.toFloat(), 220f, paintHeader)
        
        var currentY = 80f
        canvas.drawText("TIMESNAP PRO", 60f, currentY, paintTextTitle)
        
        currentY += 50f
        val docType = if (selectedTab == 1) "BẢNG LƯƠNG DỰ KIẾN (TỰ ĐỘNG)" else "PHIẾU LƯƠNG ĐIỆN TỬ CHI TIẾT"
        canvas.drawText(docType, 60f, currentY, paintTextSubTitle)
        
        currentY += 40f
        val formattedMonthLabel = if (monthLabel.startsWith("Tháng", ignoreCase = true)) monthLabel else "Tháng $monthLabel"
        val statusText = if (selectedTab == 1) "Trạng thái: Dự kiến" else "Trạng thái: Đã phê duyệt"
        canvas.drawText("$formattedMonthLabel | $statusText", 60f, currentY, paintTextMonth)

        // Main content starts
        currentY = 280f
        
        fun drawSectionHeader(title: String) {
            currentY += 20f
            canvas.drawText(title, 60f, currentY, Paint().apply { color = android.graphics.Color.parseColor("#4C84FF"); textSize = 20f; isFakeBoldText = true })
            currentY += 25f
            canvas.drawLine(60f, currentY, (width - 60).toFloat(), currentY, paintDivider)
            currentY += 50f
        }

        fun drawRow(label: String, value: String, color: Paint = paintValue) {
            canvas.drawText(label, 60f, currentY, paintLabel)
            canvas.drawText(value, (width - 60).toFloat(), currentY, color)
            currentY += 55f
        }

        // Section 1: Thông tin nhân sự
        drawSectionHeader("THÔNG TIN NHÂN SỰ")
        val employeeName = config.hoVaTen.ifBlank { userSession?.displayName ?: "N/A" }
        val employeeCode = config.maNhanVien.ifBlank { userSession?.uid?.take(8) ?: "N/A" }
        drawRow("Họ và tên:", employeeName)
        drawRow("Mã nhân viên:", employeeCode)
        if (config.boPhan.isNotBlank()) {
            drawRow("Bộ phận:", config.boPhan)
        }
        if (config.emailDangKy.isNotBlank()) {
            drawRow("Email:", config.emailDangKy)
        }
        drawRow("Mức lương cơ bản:", "${fmt.format(config.luongCoBan)}đ")
        
        val totalProjectedWorkDaysPNG = soNgayCongDuKienDouble + (if (includeSundayInProjection) remainingSundays.toDouble() else 0.0)
        val lcbProjectedWorkDaysPNG = soNgayCongDuKienDouble.coerceAtMost(standardTargetDays)
        val lcbActualWorkDaysPNG = summary.workingDays.coerceAtMost(summary.standardWorkDays.toDouble())

        val attendanceInfo = if (selectedTab == 1) "${df.format(totalProjectedWorkDaysPNG)} / ${summary.standardWorkDays} ngày" 
                             else "${df.format(summary.workingDays)} / ${summary.standardWorkDays} ngày"
        drawRow("Công làm việc:", attendanceInfo)

        // Section 2: Thu nhập chi tiết
        currentY += 20f
        drawSectionHeader("THU NHẬP CHI TIẾT (+)")
        
        val luongDuKienBaseSalary = if (soNgayCongDuKienDouble >= standardTargetDays) config.luongCoBan else Math.round((config.luongCoBan / standardTargetDays) * soNgayCongDuKienDouble).toDouble()
        val baseSalaryLabel = if (selectedTab == 1) "LCB thực nhận (${df.format(lcbProjectedWorkDaysPNG)} / ${summary.standardWorkDays})" 
                              else "LCB thực nhận (${df.format(lcbActualWorkDaysPNG)} / ${summary.standardWorkDays})"
        val baseSalaryValue = if (selectedTab == 1) luongDuKienBaseSalary else summary.baseBasicSalary
        drawRow(baseSalaryLabel, "+${fmt.format(baseSalaryValue)}đ", paintGreen)
        
        if (pcChuyenCanShowPNG > 0.0) drawRow("Phụ cấp chuyên cần", "+${fmt.format(pcChuyenCanShowPNG)}đ", paintGreen)
        if (pcTrachNhiemShowPNG > 0.0) drawRow("Phụ cấp trách nhiệm", "+${fmt.format(pcTrachNhiemShowPNG)}đ", paintGreen)
        if (pcKyThuatShowPNG > 0.0) drawRow("Phụ cấp kỹ thuật", "+${fmt.format(pcKyThuatShowPNG)}đ", paintGreen)
        if (pcHieuSuatShowPNG > 0.0) drawRow("Phụ cấp hiệu suất", "+${fmt.format(pcHieuSuatShowPNG)}đ", paintGreen)
        if (pcSanPhamShowPNG > 0.0) drawRow("Phụ cấp sản phẩm", "+${fmt.format(pcSanPhamShowPNG)}đ", paintGreen)
        if (pcChucVuShowPNG > 0.0) drawRow("Phụ cấp chức vụ", "+${fmt.format(pcChucVuShowPNG)}đ", paintGreen)
        if (pcDocHaiShowPNG > 0.0) drawRow("Phụ cấp độc hại", "+${fmt.format(pcDocHaiShowPNG)}đ", paintGreen)
        if (pcDtDoanhThuShowPNG > 0.0) drawRow("Phụ cấp doanh thu", "+${fmt.format(pcDtDoanhThuShowPNG)}đ", paintGreen)
        if (pcThamNienShowPNG > 0.0) drawRow("Phụ cấp thâm niên", "+${fmt.format(pcThamNienShowPNG)}đ", paintGreen)
        if (pcComCaShowPNG > 0.0) drawRow("Phụ cấp cơm ca", "+${fmt.format(pcComCaShowPNG)}đ", paintGreen)
        if (pcComOtShowPNG > 0.0) drawRow("Phụ cấp cơm OT", "+${fmt.format(pcComOtShowPNG)}đ", paintGreen)
        
        if (summary.tienOtNgay > 0.0) drawRow("OT ngày ${df.format(config.heSoOtNgayThuong)} (${df.format(summary.otDayHours)}h)", "+${fmt.format(summary.tienOtNgay)}đ", paintGreen)
        if (summary.tienChuNhatNgay > 0.0) drawRow("OT CN - Ca ngày ${df.format(config.heSoOtChuNhat)} (${df.format(summary.chuNhatDayHours)}h)", "+${fmt.format(summary.tienChuNhatNgay)}đ", paintGreen)
        if (summary.tienChuNhatDem > 0.0) drawRow("OT CN - Ca đêm ${df.format(config.heSoOtChuNhat)} (${df.format(summary.chuNhatNightHours)}h)", "+${fmt.format(summary.tienChuNhatDem)}đ", paintGreen)
        if (summary.tienChuNhatNgay == 0.0 && summary.tienChuNhatDem == 0.0 && summary.tienChuNhat > 0.0) {
            drawRow("OT chủ nhật ${df.format(config.heSoOtChuNhat)} (${df.format(summary.chuNhatHours)}h)", "+${fmt.format(summary.tienChuNhat)}đ", paintGreen)
        }
        if (summary.tienOtLe > 0.0) drawRow("OT lễ ${df.format(config.heSoOtNgayLe)} (${df.format(summary.otLeHours)}h)", "+${fmt.format(summary.tienOtLe)}đ", paintGreen)
        if (summary.tienOtDem > 0.0) drawRow("OT đêm ${df.format(config.heSoOtDem)} (${df.format(summary.otNightHours)}h)", "+${fmt.format(summary.tienOtDem)}đ", paintGreen)
        
        if (summary.pcCaDemVal > 0.0) drawRow("Phụ cấp ca đêm (${summary.caDemCount} ca)", "+${fmt.format(summary.pcCaDemVal)}đ", paintGreen)
        if (pcXangXeShowPNG > 0.0) drawRow("Phụ cấp xăng xe", "+${fmt.format(pcXangXeShowPNG)}đ", paintGreen)
        if (pcNhaOShowPNG > 0.0) drawRow("Phụ cấp nhà ở", "+${fmt.format(pcNhaOShowPNG)}đ", paintGreen)
        if (pcKhac1ShowPNG > 0.0) drawRow("Phụ cấp khác", "+${fmt.format(pcKhac1ShowPNG)}đ", paintGreen)

        // Section 3: Khấu trừ & Nghĩa vụ
        currentY += 20f
        drawSectionHeader("KHẤU TRỪ & NGHĨA VỤ (-)")
        if (summary.tienBh > 0.0) drawRow("Bảo hiểm xã hội (10.5%)", "-${fmt.format(summary.tienBh)}đ", paintRed)
        if (summary.doanPhi > 0.0) drawRow("Kinh phí công đoàn", "-${fmt.format(summary.doanPhi)}đ", paintRed)
        
        // Total Footer
        currentY += 40f
        canvas.drawRect(60f, currentY, (width - 60).toFloat(), currentY + 120f, Paint().apply { color = android.graphics.Color.parseColor("#1A1D2E") })
        
        currentY += 75f
        val totalLabel = if (selectedTab == 1) "DỰ KIẾN THỰC NHẬN" else "TỔNG LƯƠNG THỰC NHẬN"
        val totalValue = if (selectedTab == 1) luongDuKienVal else summary.luongThucNhan
        canvas.drawText(totalLabel, 90f, currentY, paintTextSubTitle)
        
        val netText = "${fmt.format(totalValue)} VNĐ"
        canvas.drawText(netText, (width - 90).toFloat(), currentY, Paint().apply { color = android.graphics.Color.parseColor("#00E676"); textSize = 38f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT })

        // Footer Brand
        currentY = estimatedHeight - 100f
        canvas.drawText("XUẤT TỪ HỆ THỐNG QUẢN LÝ TIMESNAP PRO", (width / 2).toFloat(), currentY, paintBrand)
        currentY += 30f
        canvas.drawText("DEVELOPED BY TRUONGVANKHOA", (width / 2).toFloat(), currentY, Paint().apply { color = android.graphics.Color.parseColor("#8F9BB3"); textSize = 14f; textAlign = Paint.Align.CENTER })

        try {
            val filename = "TimeSnap_Payslip_${employeeCode}_${selectedMonth.replace("-","_")}_${System.currentTimeMillis()}.png"
            var fos: OutputStream? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/TimeSnapPro")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val imageUri = context.contentResolver.insert(collection, contentValues)
                if (imageUri != null) {
                    fos = context.contentResolver.openOutputStream(imageUri)
                    if (fos != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        fos.flush(); fos.close()
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(imageUri, contentValues, null, null)
                        return true
                    }
                }
            } else {
                val imagesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).toString()
                val file = java.io.File(imagesDir, "TimeSnapPro")
                if (!file.exists()) file.mkdirs()
                val outFile = java.io.File(file, filename)
                fos = java.io.FileOutputStream(outFile)
            }
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush(); fos.close()
                return true
            }
        } catch (e: Exception) { e.printStackTrace() }
        return false
    }

    fun sharePayslipAndAttendanceAsPdf(
        context: Context,
        entries: List<TimeEntry>,
        summary: com.example.viewmodel.SalarySummary,
        config: UserConfig,
        userSession: UserSession?,
        monthLabel: String,
        selectedMonth: String,
        selectedTab: Int = 0,
        includeSundayInProjection: Boolean = false,
        remainingWeekdays: Int = 0,
        remainingSundays: Int = 0,
        dailySalary: Double = 0.0,
        luongDuKienVal: Double = 0.0,
        soNgayCongDuKien: Double = 0.0,
        customOt15DaysCount: Double = 0.0,
        customOt15Pay: Double = 0.0,
        selectedOt15Shift: String = "Đêm",
        customNightAllowance: Double = 0.0,
        hasLoggedUnpaidOrAbsent: Boolean = false
    ) {
        val df = DecimalFormat("#.#")
        val fmt = DecimalFormat("#,###")

        val todayCal = Calendar.getInstance()
        val currentYear = todayCal.get(Calendar.YEAR)
        val currentMonth = todayCal.get(Calendar.MONTH) + 1
        val isCurrentSelectedMonth = selectedMonth.startsWith(String.format(Locale.US, "%04d-%02d", currentYear, currentMonth))

        val standardTargetDays = (if (isCurrentSelectedMonth) summary.expectedWorkDays else summary.standardWorkDays).toDouble().coerceAtLeast(1.0)
        val effectiveSoNgayCong = if (soNgayCongDuKien > 0.0) {
            soNgayCongDuKien
        } else {
            if (isCurrentSelectedMonth) {
                val rawProjected = summary.workingDays + remainingWeekdays.toDouble()
                standardTargetDays.coerceAtLeast(rawProjected.coerceAtMost(standardTargetDays))
            } else {
                summary.workingDays.coerceAtMost(standardTargetDays)
            }
        }
        val soNgayCongDuKienDouble = effectiveSoNgayCong
        val totalProjectedWorkDaysPDF = soNgayCongDuKienDouble + (if (includeSundayInProjection) remainingSundays.toDouble() else 0.0)
        val lcbProjectedWorkDaysPDF = soNgayCongDuKienDouble.coerceAtMost(standardTargetDays)
        val lcbActualWorkDaysPDF = summary.workingDays.coerceAtMost(summary.standardWorkDays.toDouble())
        fun calcPrPDF(fieldName: String, valRaw: Double): Double {
            return com.example.data.SalaryCalculator.calculateAllowanceValue(
                fieldName = fieldName,
                allowanceValue = valRaw,
                calcType = config.getCalcTypeFor(fieldName),
                totalWorkDays = soNgayCongDuKienDouble,
                comCaCount = soNgayCongDuKienDouble.toInt(),
                comOtCount = 0,
                nightShiftsCount = summary.caDemCount + (if (selectedTab == 1 && selectedOt15Shift == "Đêm") customOt15DaysCount.toInt() else 0),
                scheduledDaysSoFar = soNgayCongDuKienDouble.toInt(),
                totalScheduledDaysInMonth = standardTargetDays.toInt()
            )
        }

        val pcKyThuatShow = if (selectedTab == 1) calcPrPDF("pcKyThuat", config.pcKyThuat) else summary.pcKyThuatVal
        val pcTrachNhiemShow = if (selectedTab == 1) calcPrPDF("pcTrachNhiem", config.pcTrachNhiem) else summary.pcTrachNhiemVal
        val pcChucVuShow = if (selectedTab == 1) calcPrPDF("pcChucVu", config.pcChucVu) else summary.pcChucVuVal
        val pcHieuSuatShow = if (selectedTab == 1) calcPrPDF("pcHieuSuat", config.pcHieuSuat) else summary.pcHieuSuatVal
        val pcSanPhamShow = if (selectedTab == 1) calcPrPDF("pcSanPham", config.pcSanPham) else summary.pcSanPhamVal

        val pcComCaShow = if (selectedTab == 1) {
            if (isCurrentSelectedMonth) {
                soNgayCongDuKienDouble * config.pcComCa
            } else {
                summary.pcComCaVal
            }
        } else {
            summary.pcComCaVal
        }

        val pcComOtShow = if (selectedTab == 1) {
            summary.pcComOtVal + (customOt15DaysCount * config.pcComOt)
        } else {
            summary.pcComOtVal
        }

        val pcNhaOShow = if (selectedTab == 1) calcPrPDF("pcNhaO", config.pcNhaO) else summary.pcNhaOVal
        val pcDocHaiShow = if (selectedTab == 1) calcPrPDF("pcDocHai", config.pcDocHai) else summary.pcDocHaiVal
        val pcDtDoanhThuShow = if (selectedTab == 1) calcPrPDF("pcDtDoanhThu", config.pcDtDoanhThu) else summary.pcDtDoanhThuVal
        val pcXangXeShow = if (selectedTab == 1) calcPrPDF("pcXangXe", config.pcXangXe) else summary.pcXangXeVal
        val pcKhacShow = if (selectedTab == 1) calcPrPDF("pcCaDem", config.pcCaDem) else summary.pcCaDemVal
        val pcKhac1Show = if (selectedTab == 1) calcPrPDF("pcKhac1", config.pcKhac1) else summary.pcKhac1Val
        val pcThamNienShow = if (selectedTab == 1) calcPrPDF("pcThamNien", config.pcThamNien) else summary.pcThamNienVal

        val pcChuyenCanShow = if (selectedTab == 1) {
            if (hasLoggedUnpaidOrAbsent) 0.0 else calcPrPDF("tienChuyenCanGoc", config.tienChuyenCanGoc)
        } else {
            summary.phuCapChuyenCan
        }

        val pdfDocument = PdfDocument()

        val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        val canvas1 = page1.canvas

        val primaryColor = android.graphics.Color.parseColor("#1A73E8")
        val navyColor = android.graphics.Color.parseColor("#1E293B")
        val textColor = android.graphics.Color.parseColor("#334155")
        val grayLabelColor = android.graphics.Color.parseColor("#64748B")
        val bgLightColor = android.graphics.Color.parseColor("#F8FAFC")
        val borderLightColor = android.graphics.Color.parseColor("#E2E8F0")
        val greenColor = android.graphics.Color.parseColor("#16A34A")
        val redColor = android.graphics.Color.parseColor("#DC2626")

        val paintBg = Paint().apply { color = android.graphics.Color.WHITE }
        val paintHeaderBg = Paint().apply { color = bgLightColor }
        val paintBorder = Paint().apply { color = borderLightColor; strokeWidth = 1f; style = Paint.Style.STROKE }
        val paintTitle = Paint().apply { color = primaryColor; textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val paintSubtitle = Paint().apply { color = navyColor; textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val paintDate = Paint().apply { color = grayLabelColor; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
        val paintSectionHeader = Paint().apply { color = primaryColor; textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val paintLabel = Paint().apply { color = grayLabelColor; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
        val paintValBold = Paint().apply { color = navyColor; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val paintValNormal = Paint().apply { color = textColor; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
        val paintValGreen = Paint().apply { color = greenColor; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT }
        val paintValRed = Paint().apply { color = redColor; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT }
        val paintFooter = Paint().apply { color = grayLabelColor; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); textAlign = Paint.Align.CENTER }

        canvas1.drawRect(0f, 0f, 595f, 842f, paintBg)
        canvas1.drawRect(0f, 0f, 595f, 130f, paintHeaderBg)
        canvas1.drawLine(0f, 130f, 595f, 130f, paintBorder)

        var currentY = 32f
        val paintCompany = Paint().apply { color = navyColor; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas1.drawText("CÔNG TY TNHH CÔNG NGHỆ TIMESNAP PRO", 40f, currentY, paintCompany)

        currentY = 62f
        canvas1.drawText("TIMESNAP PRO", 40f, currentY, paintTitle)
        
        currentY += 28f
        val docType = if (selectedTab == 1) "BẢNG LƯƠNG DỰ KIẾN (TỰ ĐỘNG)" else "PHIẾU LƯƠNG ĐIỆN TỬ CHI TIẾT"
        canvas1.drawText(docType, 40f, currentY, paintSubtitle)

        currentY += 22f
        val formattedMonthLabel = if (monthLabel.startsWith("Tháng", ignoreCase = true)) monthLabel else "Tháng $monthLabel"
        val statusText = if (selectedTab == 1) "Dự kiến" else "Đã phê duyệt"
        canvas1.drawText("Kỳ tính lương: $formattedMonthLabel | Trạng thái: $statusText | Ngày xuất: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", 40f, currentY, paintDate)

        currentY = 160f
        canvas1.drawText("I. THÔNG TIN NHÂN SỰ & CÔNG LAO ĐỘNG", 40f, currentY, paintSectionHeader)
        currentY += 8f
        canvas1.drawLine(40f, currentY, 555f, currentY, paintBorder)

        currentY += 18f
        val empName = config.hoVaTen.ifBlank { userSession?.displayName ?: "N/A" }
        val empCode = config.maNhanVien.ifBlank { userSession?.uid?.take(8) ?: "N/A" }

        // Row 1
        canvas1.drawText("Họ và tên:", 45f, currentY, paintLabel)
        canvas1.drawText(empName, 150f, currentY, paintValBold)
        canvas1.drawText("Công chuẩn:", 320f, currentY, paintLabel)
        val standardDaysVal = if (selectedTab == 1) "${summary.standardWorkDays} ngày" else "${if (summary.isCurrentMonth) summary.expectedWorkDays else summary.standardWorkDays} ngày"
        canvas1.drawText(standardDaysVal, 440f, currentY, paintValBold)

        // Row 2
        currentY += 18f
        canvas1.drawText("Mã nhân viên:", 45f, currentY, paintLabel)
        canvas1.drawText(empCode, 150f, currentY, paintValNormal)
        canvas1.drawText("Công làm việc:", 320f, currentY, paintLabel)
        val actualDaysVal = if (selectedTab == 1) "${df.format(totalProjectedWorkDaysPDF)} / ${summary.standardWorkDays} ngày" else "${df.format(summary.workingDays)} / ${summary.standardWorkDays} ngày"
        canvas1.drawText(actualDaysVal, 440f, currentY, paintValBold)

        // Row 3
        currentY += 18f
        canvas1.drawText("Bộ phận:", 45f, currentY, paintLabel)
        canvas1.drawText(config.boPhan.ifBlank { "N/A" }, 150f, currentY, paintValNormal)
        canvas1.drawText("Ngày nghỉ phép:", 320f, currentY, paintLabel)
        val leaveDaysVal = "${entries.count { it.dayType.equals("LEAVE", ignoreCase = true) }} ngày"
        canvas1.drawText(leaveDaysVal, 440f, currentY, paintValNormal)

        // Row 4
        currentY += 18f
        canvas1.drawText("Mức lương cơ bản:", 45f, currentY, paintLabel)
        canvas1.drawText("${fmt.format(config.luongCoBan)} VNĐ", 150f, currentY, paintValBold)
        canvas1.drawText("Giờ làm thêm:", 320f, currentY, paintLabel)
        val totalOtHrsVal = "${df.format(summary.otDayHours + summary.chuNhatHours + summary.otLeHours + summary.otNightHours)} giờ"
        canvas1.drawText(totalOtHrsVal, 440f, currentY, paintValNormal)

        currentY += 26f
        canvas1.drawText("II. CHI TIẾT THU NHẬP VÀ KHẤU TRỪ (+ / -)", 40f, currentY, paintSectionHeader)
        currentY += 8f
        canvas1.drawLine(40f, currentY, 555f, currentY, paintBorder)

        currentY += 22f
        val tblCol1 = 50f
        val tblCol2 = 380f
        val tblCol3 = 540f

        val paintTblHeader = Paint().apply { color = navyColor; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val paintTblHeaderRight = Paint().apply { color = navyColor; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT }
        
        canvas1.drawText("Khoản mục / Chi tiết", tblCol1, currentY, paintTblHeader)
        canvas1.drawText("Cộng (+)", tblCol2, currentY, paintTblHeaderRight)
        canvas1.drawText("Trừ (-)", tblCol3, currentY, paintTblHeaderRight)

        currentY += 10f
        canvas1.drawLine(40f, currentY, 555f, currentY, paintBorder)
        currentY += 20f

        fun drawPdfRow(label: String, plusVal: Double, minusVal: Double) {
            canvas1.drawText(label, tblCol1, currentY, paintValNormal)
            if (plusVal > 0.0) {
                canvas1.drawText("+${fmt.format(plusVal)}", tblCol2, currentY, paintValGreen)
            } else {
                canvas1.drawText("-", tblCol2, currentY, Paint().apply { color = grayLabelColor; textSize = 10f; textAlign = Paint.Align.RIGHT })
            }
            if (minusVal > 0.0) {
                canvas1.drawText("-${fmt.format(minusVal)}", tblCol3, currentY, paintValRed)
            } else {
                canvas1.drawText("-", tblCol3, currentY, Paint().apply { color = grayLabelColor; textSize = 10f; textAlign = Paint.Align.RIGHT })
            }
            currentY += 16f
        }

        val baseSalaryValue = if (selectedTab == 1) {
            if (soNgayCongDuKienDouble >= standardTargetDays) config.luongCoBan else Math.round((config.luongCoBan / standardTargetDays) * soNgayCongDuKienDouble).toDouble()
        } else summary.baseBasicSalary
        val baseSalaryLabelText = if (selectedTab == 1) "LCB thực nhận (${df.format(lcbProjectedWorkDaysPDF)} / ${summary.standardWorkDays})" 
                              else "LCB thực nhận (${df.format(lcbActualWorkDaysPDF)} / ${summary.standardWorkDays})"
        drawPdfRow(baseSalaryLabelText, baseSalaryValue, 0.0)

        // OT Lương
        val otNormalPay = summary.tienOtNgay
        val otSundayDayPay = summary.tienChuNhatNgay
        val otSundayNightPay = summary.tienChuNhatDem
        val otSundayPay = summary.tienChuNhat
        val otHolidayPay = summary.tienOtLe
        val otNightPay = summary.tienOtDem
        val totalOtPay = otNormalPay + otSundayPay + otHolidayPay + otNightPay
        if (totalOtPay > 0.0) {
            if (otNormalPay > 0.0) drawPdfRow("Lương tăng ca ngày thường (${df.format(summary.otDayHours)}h)", otNormalPay, 0.0)
            if (otSundayDayPay > 0.0) drawPdfRow("Lương tăng ca CN - Ca ngày (${df.format(summary.chuNhatDayHours)}h)", otSundayDayPay, 0.0)
            if (otSundayNightPay > 0.0) drawPdfRow("Lương tăng ca CN - Ca đêm (${df.format(summary.chuNhatNightHours)}h)", otSundayNightPay, 0.0)
            if (otSundayDayPay == 0.0 && otSundayNightPay == 0.0 && otSundayPay > 0.0) {
                drawPdfRow("Lương tăng ca chủ nhật (${df.format(summary.chuNhatHours)}h)", otSundayPay, 0.0)
            }
            if (otHolidayPay > 0.0) drawPdfRow("Lương tăng ca ngày lễ (${df.format(summary.otLeHours)}h)", otHolidayPay, 0.0)
            if (otNightPay > 0.0) drawPdfRow("Lương tăng ca đêm (${df.format(summary.otNightHours)}h)", otNightPay, 0.0)
        } else {
            drawPdfRow("Lương tăng ca (OT)", 0.0, 0.0)
        }

        // Required flat allowances
        drawPdfRow("Phụ cấp ăn trưa (Cơm ca)", pcComCaShow, 0.0)
        drawPdfRow("Phụ cấp khác", pcKhac1Show, 0.0)
        drawPdfRow("Phụ cấp xăng xe", pcXangXeShow, 0.0)

        // Required bonus/thưởng
        val bonusShow = pcHieuSuatShow + pcSanPhamShow
        drawPdfRow("Thưởng hiệu suất & Sản phẩm", bonusShow, 0.0)

        // Other active allowances
        if (pcChuyenCanShow > 0.0) drawPdfRow("Phụ cấp chuyên cần", pcChuyenCanShow, 0.0)
        if (pcTrachNhiemShow > 0.0) drawPdfRow("Phụ cấp trách nhiệm", pcTrachNhiemShow, 0.0)
        if (pcKyThuatShow > 0.0) drawPdfRow("Phụ cấp kỹ thuật", pcKyThuatShow, 0.0)
        if (pcChucVuShow > 0.0) drawPdfRow("Phụ cấp chức vụ", pcChucVuShow, 0.0)
        if (pcDocHaiShow > 0.0) drawPdfRow("Phụ cấp độc hại", pcDocHaiShow, 0.0)
        if (pcThamNienShow > 0.0) drawPdfRow("Phụ cấp thâm niên", pcThamNienShow, 0.0)
        if (pcNhaOShow > 0.0) drawPdfRow("Phụ cấp nhà ở", pcNhaOShow, 0.0)
        if (pcComOtShow > 0.0) drawPdfRow("Phụ cấp cơm OT", pcComOtShow, 0.0)
        if (summary.pcCaDemVal > 0.0) drawPdfRow("Phụ cấp ca đêm (${summary.caDemCount} ca)", summary.pcCaDemVal, 0.0)
        if (pcDtDoanhThuShow > 0.0) drawPdfRow("Phụ cấp doanh thu", pcDtDoanhThuShow, 0.0)

        // Required trích trừ
        drawPdfRow("BHXH bắt buộc người lao động đóng (10.5%)", 0.0, summary.tienBh)
        drawPdfRow("Thuế thu nhập cá nhân (TNCN)", 0.0, 0.0)
        drawPdfRow("Tạm ứng lương", 0.0, 0.0)

        // Optional trích trừ
        if (summary.doanPhi > 0.0) drawPdfRow("Kinh phí công đoàn", 0.0, summary.doanPhi)
        if (summary.tienKhauTruNghi > 0.0 && selectedTab == 0) drawPdfRow("Khấu trừ nghỉ không phép/vắng", 0.0, summary.tienKhauTruNghi)

        currentY += 15f
        canvas1.drawRect(40f, currentY, 555f, currentY + 50f, paintHeaderBg)
        canvas1.drawRect(40f, currentY, 555f, currentY + 50f, paintBorder)

        val totalLabelText = if (selectedTab == 1) "DỰ KIẾN THỰC NHẬN:" else "TỔNG THỰC NHẬN:"
        val totalValue = if (selectedTab == 1) luongDuKienVal else summary.luongThucNhan

        canvas1.drawText(totalLabelText, 55f, currentY + 31f, Paint().apply { color = navyColor; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        canvas1.drawText("${fmt.format(totalValue)} VNĐ", 540f, currentY + 31f, Paint().apply { color = greenColor; textSize = 15f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT })

        currentY = Math.max(675f, currentY + 70f)
        val signHeaderPaint = Paint().apply { color = navyColor; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
        val signSubPaint = Paint().apply { color = grayLabelColor; textSize = 7.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); textAlign = Paint.Align.CENTER }

        // Col 4: Chữ ký người nhận (X = 495f)
        canvas1.drawText("Chữ ký người nhận", 495f, currentY, signHeaderPaint)
        canvas1.drawText("(Ký nhận thực tế)", 495f, currentY + 14f, signSubPaint)

        canvas1.drawText("XUẤT TỪ ỨNG DỤNG TIMESNAP PRO - QUẢN LÝ BẢNG LƯƠNG THÔNG MINH", 297f, 800f, paintFooter)
        canvas1.drawText("Trang 1 / 2", 297f, 815f, paintFooter)

        pdfDocument.finishPage(page1)

        val pageInfo2 = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        val page2 = pdfDocument.startPage(pageInfo2)
        val canvas2 = page2.canvas

        canvas2.drawRect(0f, 0f, 595f, 842f, paintBg)
        canvas2.drawRect(0f, 0f, 595f, 110f, paintHeaderBg)
        canvas2.drawLine(0f, 110f, 595f, 110f, paintBorder)

        canvas2.drawText("BẢNG CHI TIẾT CHẤM CÔNG CÁ NHÂN", 40f, 50f, paintSubtitle)
        canvas2.drawText("Tháng $monthLabel | Nhân viên: $empName ($empCode)", 40f, 75f, paintDate)

        currentY = 140f
        canvas2.drawText("III. CHI TIẾT LỊCH SỬ CHẤM CÔNG TRONG THÁNG", 40f, currentY, paintSectionHeader)
        currentY += 8f
        canvas2.drawLine(40f, currentY, 555f, currentY, paintBorder)

        currentY += 25f
        
        val attCol1 = 45f
        val attCol2 = 140f
        val attCol3 = 210f
        val attCol4 = 280f
        val attCol5 = 390f

        val paintAttHeader = Paint().apply { color = navyColor; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        
        canvas2.drawText("Ngày", attCol1, currentY, paintAttHeader)
        canvas2.drawText("Giờ vào", attCol2, currentY, paintAttHeader)
        canvas2.drawText("Giờ ra", attCol3, currentY, paintAttHeader)
        canvas2.drawText("Ca làm / Trạng thái", attCol4, currentY, paintAttHeader)
        canvas2.drawText("Ghi chú", attCol5, currentY, paintAttHeader)

        currentY += 8f
        canvas2.drawLine(40f, currentY, 555f, currentY, paintBorder)
        currentY += 18f

        val sdfDateParser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sortedEntries = entries.sortedWith { e1, e2 ->
            try {
                val d1 = sdfDateParser.parse(e1.date) ?: Date(0)
                val d2 = sdfDateParser.parse(e2.date) ?: Date(0)
                d1.compareTo(d2)
            } catch (e: Exception) {
                e1.date.compareTo(e2.date)
            }
        }

        val sdfTimeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

        if (sortedEntries.isEmpty()) {
            canvas2.drawText("Không có dữ liệu chấm công nào trong tháng này.", 45f, currentY + 15f, paintValNormal)
        } else {
            sortedEntries.forEachIndexed { idx, entry ->
                if (currentY > 750f) {
                    return@forEachIndexed
                }

                canvas2.drawLine(40f, currentY + 4f, 555f, currentY + 4f, paintBorder)

                canvas2.drawText(entry.date, attCol1, currentY, paintValBold)

                val inText = if (entry.checkInTime != null && entry.checkInTime > 0) sdfTimeFormatter.format(Date(entry.checkInTime)) else "--:--"
                canvas2.drawText(inText, attCol2, currentY, paintValNormal)

                val outText = if (entry.checkOutTime != null && entry.checkOutTime > 0) sdfTimeFormatter.format(Date(entry.checkOutTime)) else "--:--"
                canvas2.drawText(outText, attCol3, currentY, paintValNormal)

                val isNightShift = entry.shiftType == "NIGHT" || entry.dayType == "NIGHT" || entry.shiftId == "ca_dem" || run {
                    val inTime = entry.checkInTime
                    if (inTime != null && inTime > 0) {
                        val cal = Calendar.getInstance().apply { timeInMillis = inTime }
                        val hour = cal.get(Calendar.HOUR_OF_DAY)
                        hour >= 15 || hour < 6
                    } else false
                }

                val isSun = entry.dayType == "SUNDAY" || isSundayDate(entry.date)
                val isHol = entry.dayType == "HOLIDAY" || isHolidayDate(entry.date)

                val (friendlyStatus, statusColor) = run {
                    val dtUpper = entry.dayType.uppercase()
                    if (dtUpper in listOf("OFF", "NGHỈ", "LEAVE", "NGHỈ PHÉP", "PAID_LEAVE", "ABSENT", "VẮNG MẶT", "UNPAID_LEAVE", "UNAUTHORIZED_LEAVE", "HOLIDAY_LEAVE", "HOLIDAY", "KP", "NP", "PHÉP NĂM", "PHÉP THƯỜNG", "KHÔNG PHÉP")) {
                        when {
                            dtUpper in listOf("HOLIDAY_LEAVE", "HOLIDAY", "LỄ") -> "Lễ" to primaryColor
                            dtUpper in listOf("PAID_LEAVE", "LEAVE", "NGHỈ PHÉP", "NP", "PHÉP NĂM") -> "Phép năm" to greenColor
                            dtUpper in listOf("UNAUTHORIZED_LEAVE", "ABSENT", "VẮNG MẶT", "KP", "KHÔNG PHÉP") -> "Không phép" to redColor
                            dtUpper in listOf("UNPAID_LEAVE", "PHÉP THƯỜNG") -> "Phép thường" to redColor
                            else -> "Phép thường" to textColor
                        }
                    } else {
                        val shiftNameStr = if (isNightShift) "Ca đêm" else "Ca ngày"
                        val dayTypeStr = when {
                            isHol -> " (Lễ)"
                            isSun -> " (Chủ nhật)"
                            else -> ""
                        }
                        val label = shiftNameStr + dayTypeStr
                        val color = when {
                            isHol || isSun -> primaryColor
                            isNightShift -> navyColor
                            else -> textColor
                        }
                        label to color
                    }
                }

                canvas2.drawText(friendlyStatus, attCol4, currentY, Paint().apply {
                    color = statusColor
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                })

                val noteText = entry.note?.take(22) ?: ""
                canvas2.drawText(noteText, attCol5, currentY, paintValNormal)

                currentY += 18f
            }
        }

        canvas2.drawText("XUẤT TỪ ỨNG DỤNG TIMESNAP PRO - QUẢN LÝ BẢNG LƯƠNG THÔNG MINH", 297f, 800f, paintFooter)
        canvas2.drawText("Trang 2 / 2", 297f, 815f, paintFooter)

        pdfDocument.finishPage(page2)

        try {
            val exportDir = File(context.cacheDir, "TimeSnapPro_Exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            val pdfFile = File(exportDir, "TimeSnap_BaoCaoLuong_Cong_${empCode}_${selectedMonth.replace("-", "_")}.pdf")
            val fos = FileOutputStream(pdfFile)
            pdfDocument.writeTo(fos)
            fos.flush()
            fos.close()
            pdfDocument.close()

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "TimeSnap Pro: Phiếu Lương & Bảng Công $formattedMonthLabel")
                putExtra(Intent.EXTRA_TEXT, "Kính gửi, tôi xin gửi chi tiết phiếu lương và bảng công cá nhân tháng $formattedMonthLabel được xuất từ ứng dụng TimeSnap Pro.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Gửi báo cáo qua:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Lỗi khi tạo PDF: ${e.message}", Toast.LENGTH_LONG).show()
            pdfDocument.close()
        }
    }
}

fun AttendanceRecord.toTimeEntry(): TimeEntry {
    val isLeave = com.example.data.SalaryCalculator.isLeaveType(this.status)
    var rawOut = if (isLeave) null else this.clockOutTime
    if (!isLeave && this.clockInTime > 0 && rawOut != null && rawOut <= this.clockInTime) {
        rawOut += 24 * 3600 * 1000L
    }
    return TimeEntry(
        id = this.id.toInt(),
        userId = this.uid,
        date = com.example.data.SalaryCalculator.normalizeDateToDmy(this.dateString),
        checkInTime = if (isLeave) null else this.clockInTime,
        checkOutTime = rawOut,
        dayType = if (this.status.isNotBlank()) this.status else "NORMAL",
        isWorking = !isLeave && this.clockOutTime == null && this.clockInTime > 0,
        note = this.notes
    )
}
