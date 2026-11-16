package com.example

import com.example.data.SalaryCalculator
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class SalaryCalculatorTest {

    private fun getTimeMs(
        year: Int = 2026,
        month: Int = 9,
        day: Int = 15,
        hour: Int,
        minute: Int
    ): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun bestarOfficialConfig(
        luongCoBan: Double = 5_200_000.0,
        heSoOtNgayThuong: Double = 1.5,
        heSoOtDem: Double = 1.8,
        heSoOtChuNhat: Double = 2.0,
        heSoOtChuNhatDem: Double = 2.7,
        heSoOtNgayLe: Double = 3.0,
        isThuViec: Boolean = false
    ): UserConfig {
        return UserConfig(
            userId = "user1",
            luongCoBan = luongCoBan,
            tienChuyenCanGoc = 300_000.0,
            pcComCa = 0.0,
            pcComOt = 25_000.0,
            pcHieuSuat = 500_000.0,
            pcKhac1 = 500_000.0,
            luongDongBaoHiem = 0.0,
            tiLeDongBaoHiem = 0.0,
            doanPhiCongDoan = 0.0,
            heSoOtNgayThuong = heSoOtNgayThuong,
            heSoOtDem = heSoOtDem,
            heSoOtChuNhat = heSoOtChuNhat,
            heSoOtChuNhatDem = heSoOtChuNhatDem,
            heSoOtNgayLe = heSoOtNgayLe,
            tinhKhauTruNghi = true,
            soGioNghiGiaiLao = 1.5,
            isThuViec = isThuViec,
            allowanceCalcTypes = "pcComCa:PER_WORK_DAY;pcComOt:OT_MEAL_GE_2H;tienChuyenCanGoc:MONTHLY_FLAT;pcHieuSuat:MONTHLY_FLAT;pcKhac1:MONTHLY_FLAT"
        )
    }

    // 1. Ca ngày hành chính chuẩn Bestar: 07:30 - 16:30 (nghỉ trưa 1h, không bị trừ 30p OT)
    @Test
    fun testBestarDayShiftStandard_0730_to_1630() {
        val config = bestarOfficialConfig()
        val entry = TimeEntry(
            userId = "user1",
            date = "2026-09-15",
            shiftId = "ca2",
            checkInTime = getTimeMs(day = 15, hour = 7, minute = 30),
            checkOutTime = getTimeMs(day = 15, hour = 16, minute = 30)
        )

        val result = SalaryCalculator.calculateSingleEntry(entry, config)

        assertEquals(1.0, result.workDay, 0.001)
        assertEquals(0.0, result.otHours, 0.001)
        assertEquals(0, result.lateMinutes)
        assertEquals(0, result.earlyLeaveMinutes)
        assertEquals(1.0, result.customBreakHours ?: -1.0, 0.001)
    }

    // 2. Ca ngày có tăng ca chiều Bestar: 07:30 - 20:00 (1h trưa + 0.5h nghỉ 16:30-17:00 = 1.5h -> 3.0h OT)
    @Test
    fun testBestarDayShiftOT_0730_to_2000() {
        val config = bestarOfficialConfig()
        val entry = TimeEntry(
            userId = "user1",
            date = "2026-09-15",
            shiftId = "ca2",
            checkInTime = getTimeMs(day = 15, hour = 7, minute = 30),
            checkOutTime = getTimeMs(day = 15, hour = 20, minute = 0)
        )

        val result = SalaryCalculator.calculateSingleEntry(entry, config)

        assertEquals(1.0, result.workDay, 0.001)
        assertEquals(3.0, result.otHours, 0.001)
        assertEquals(0, result.lateMinutes)
        assertEquals(0, result.earlyLeaveMinutes)
        assertEquals(1.5, result.customBreakHours ?: -1.0, 0.001)
    }

    // 3. Ca đêm hành chính chuẩn Bestar: 19:30 - 04:30 (nghỉ giữa ca 1h, 8h công, 0 OT)
    @Test
    fun testBestarNightShiftStandard_1930_to_0430() {
        val config = bestarOfficialConfig()
        val entry = TimeEntry(
            userId = "user1",
            date = "2026-09-15",
            shiftId = "ca_dem",
            shiftType = "NIGHT",
            checkInTime = getTimeMs(day = 15, hour = 19, minute = 30),
            checkOutTime = getTimeMs(day = 16, hour = 4, minute = 30)
        )

        val result = SalaryCalculator.calculateSingleEntry(entry, config)

        assertEquals(1.0, result.workDay, 0.001)
        assertEquals(0.0, result.otHours, 0.001)
        assertEquals(0, result.lateMinutes)
        assertEquals(0, result.earlyLeaveMinutes)
        assertEquals(1.0, result.customBreakHours ?: -1.0, 0.001)
    }

    // 4. Ca đêm tăng ca Bestar: 19:30 - 07:30 (1h nghỉ giữa ca, không nghỉ 30p -> 3.0h OT đêm)
    @Test
    fun testBestarNightShiftOT_1930_to_0730() {
        val config = bestarOfficialConfig()
        val entry = TimeEntry(
            userId = "user1",
            date = "2026-09-15",
            shiftId = "ca_dem",
            shiftType = "NIGHT",
            checkInTime = getTimeMs(day = 15, hour = 19, minute = 30),
            checkOutTime = getTimeMs(day = 16, hour = 7, minute = 30)
        )

        val result = SalaryCalculator.calculateSingleEntry(entry, config)

        assertEquals(1.0, result.workDay, 0.001)
        assertEquals(3.0, result.otHours, 0.001)
        assertEquals(1.0, result.customBreakHours ?: -1.0, 0.001)
    }

    // 5. Ca thông (không trừ giờ nghỉ giải lao): 07:30 - 19:30 -> 4.0h OT; 19:30 - 07:30 -> 4.0h OT
    @Test
    fun testContinuousShiftNoBreakDeduction() {
        val config = UserConfig(userId = "user1", tinhKhauTruNghi = false, soGioNghiGiaiLao = 1.5)
        val dayEntry = TimeEntry(
            userId = "user1",
            date = "2026-09-15",
            shiftId = "ca1",
            checkInTime = getTimeMs(day = 15, hour = 7, minute = 30),
            checkOutTime = getTimeMs(day = 15, hour = 19, minute = 30),
            customBreakDeduction = false
        )
        val nightEntry = TimeEntry(
            userId = "user1",
            date = "2026-09-15",
            shiftId = "ca_dem",
            shiftType = "NIGHT",
            checkInTime = getTimeMs(day = 15, hour = 19, minute = 30),
            checkOutTime = getTimeMs(day = 16, hour = 7, minute = 30),
            customBreakDeduction = false
        )

        val resDay = SalaryCalculator.calculateSingleEntry(dayEntry, config)
        val resNight = SalaryCalculator.calculateSingleEntry(nightEntry, config)

        assertEquals(4.0, resDay.otHours, 0.001)
        assertEquals(4.0, resNight.otHours, 0.001)
    }

    // 6. Trễ 15p & về sớm 30p & quên chấm ra
    @Test
    fun testLateEarlyAndWorkingCheckIn() {
        val config = bestarOfficialConfig()
        val lateEntry = SalaryCalculator.calculateSingleEntry(
            TimeEntry(
                userId = "user1",
                date = "2026-09-15",
                shiftId = "ca1",
                checkInTime = getTimeMs(day = 15, hour = 7, minute = 45),
                checkOutTime = getTimeMs(day = 15, hour = 16, minute = 30)
            ),
            config
        )
        assertEquals(15, lateEntry.lateMinutes)
        assertEquals(0.5, lateEntry.workDay, 0.001)

        val earlyEntry = SalaryCalculator.calculateSingleEntry(
            TimeEntry(
                userId = "user1",
                date = "2026-09-15",
                shiftId = "ca1",
                checkInTime = getTimeMs(day = 15, hour = 7, minute = 30),
                checkOutTime = getTimeMs(day = 15, hour = 16, minute = 0)
            ),
            config
        )
        assertEquals(30, earlyEntry.earlyLeaveMinutes)

        val workingEntry = SalaryCalculator.calculateSingleEntry(
            TimeEntry(
                userId = "user1",
                date = "2026-09-15",
                shiftId = "ca1",
                checkInTime = getTimeMs(day = 15, hour = 7, minute = 30),
                checkOutTime = null,
                isWorking = true
            ),
            config
        )
        assertEquals(1.0, workingEntry.workDay, 0.001)
        assertEquals(0.0, workingEntry.otHours, 0.001)
    }

    // 7. Nghỉ có lương / không lương
    @Test
    fun testLeaveWorkDays() {
        val paid = SalaryCalculator.calculateSingleEntry(
            TimeEntry(userId = "user1", date = "2026-09-15", dayType = "PAID_LEAVE")
        )
        val unpaid = SalaryCalculator.calculateSingleEntry(
            TimeEntry(userId = "user1", date = "2026-09-15", dayType = "UNPAID_LEAVE")
        )
        assertEquals(1.0, paid.workDay, 0.001)
        assertEquals(0.0, unpaid.workDay, 0.001)
    }

    // 8. Kiểm tra đủ 5 hệ số tăng ca: Ngày thường (1.5), Đêm thường (1.8), CN ngày (2.0), CN đêm (2.7), Lễ (3.0)
    //    + khả năng user tự sửa hệ số tăng ca
    @Test
    fun testFiveOtCoefficientsDefaultAndUserEditable() {
        val base = 26.0 * 8.0 * 25_000.0 // 5.200.000 => 25.000đ/giờ
        val defaultCfg = bestarOfficialConfig(luongCoBan = base)

        val entries = listOf(
            // T3 2026-09-15: ca2 07:30-20:00 => 3h OT ngày (x1.5)
            TimeEntry(
                userId = "user1", date = "2026-09-15", shiftId = "ca2", dayType = "NORMAL",
                checkInTime = getTimeMs(day = 15, hour = 7, minute = 30),
                checkOutTime = getTimeMs(day = 15, hour = 20, minute = 0)
            ),
            // T4 2026-09-16: ca_dem 19:30-07:30 => 3h OT đêm (x1.8)
            TimeEntry(
                userId = "user1", date = "2026-09-16", shiftId = "ca_dem", shiftType = "NIGHT", dayType = "NIGHT",
                checkInTime = getTimeMs(day = 16, hour = 19, minute = 30),
                checkOutTime = getTimeMs(day = 17, hour = 7, minute = 30)
            ),
            // CN 2026-09-20: ca ngày 07:30-16:30 => 8h CN ngày (x2.0)
            TimeEntry(
                userId = "user1", date = "2026-09-20", shiftId = "ca1", dayType = "SUNDAY",
                checkInTime = getTimeMs(day = 20, hour = 7, minute = 30),
                checkOutTime = getTimeMs(day = 20, hour = 16, minute = 30)
            ),
            // CN 2026-09-27: ca đêm 19:30-04:30 => 8h CN đêm (x2.7)
            TimeEntry(
                userId = "user1", date = "2026-09-27", shiftId = "ca_dem", shiftType = "NIGHT", dayType = "SUNDAY",
                checkInTime = getTimeMs(day = 27, hour = 19, minute = 30),
                checkOutTime = getTimeMs(day = 28, hour = 4, minute = 30)
            ),
            // Lễ: 07:30-18:30 (span 11h, break 1.5h => 9.5h worked => 1.5h OT lễ x3.0)
            TimeEntry(
                userId = "user1", date = "2026-09-02", shiftId = "ca2", dayType = "HOLIDAY",
                checkInTime = getTimeMs(day = 2, hour = 7, minute = 30),
                checkOutTime = getTimeMs(day = 2, hour = 18, minute = 30)
            )
        )

        val summary = SalaryCalculator.calculateMonthlySalary(
            entries = entries,
            config = defaultCfg,
            scheduledDaysSoFar = 3,
            totalScheduledDaysInMonth = 26,
            earliestDate = "2026-09-02",
            selectedMonth = "2026-09",
            todayStr = "2026-09-30",
            isCurrentSelectedMonth = false,
            holidayDatesInMonth = setOf("2026-09-02")
        )

        assertEquals(3.0 * 25_000.0 * 1.5, summary.tienOtNgay, 1.0)
        assertEquals(3.0 * 25_000.0 * 1.8, summary.tienOtDem, 1.0)
        assertEquals(8.0 * 25_000.0 * 2.0, summary.tienChuNhatNgay, 1.0)
        assertEquals(8.0 * 25_000.0 * 2.7, summary.tienChuNhatDem, 1.0)
        assertEquals(1.5 * 25_000.0 * 3.0, summary.tienOtLe, 1.0)
        // OT Meal >= 2h: 15/09 (3h OT) + 16/09 (3h OT) + 20/09 (Sunday 0 OT) + 27/09 (Sunday 0 OT) + 02/09 (1.5h < 2h!) => 2 bữa * 25k = 50k
        assertEquals(50_000.0, summary.pcComOtVal, 1.0)

        // Custom edited coefficients by user
        val editedCfg = bestarOfficialConfig(
            luongCoBan = base,
            heSoOtNgayThuong = 1.6,
            heSoOtDem = 2.0,
            heSoOtChuNhat = 2.2,
            heSoOtChuNhatDem = 2.9,
            heSoOtNgayLe = 3.5
        )
        val editedSummary = SalaryCalculator.calculateMonthlySalary(
            entries = entries,
            config = editedCfg,
            scheduledDaysSoFar = 3,
            totalScheduledDaysInMonth = 26,
            earliestDate = "2026-09-02",
            selectedMonth = "2026-09",
            todayStr = "2026-09-30",
            isCurrentSelectedMonth = false,
            holidayDatesInMonth = setOf("2026-09-02")
        )
        assertEquals(3.0 * 25_000.0 * 1.6, editedSummary.tienOtNgay, 1.0)
        assertEquals(3.0 * 25_000.0 * 2.0, editedSummary.tienOtDem, 1.0)
        assertEquals(8.0 * 25_000.0 * 2.2, editedSummary.tienChuNhatNgay, 1.0)
        assertEquals(8.0 * 25_000.0 * 2.9, editedSummary.tienChuNhatDem, 1.0)
        assertEquals(1.5 * 25_000.0 * 3.5, editedSummary.tienOtLe, 1.0)
    }

    // 9. Quy định chuyên cần Bestar: nghỉ phép riêng 1 ngày (-150k), >=2 ngày (-300k),
    //    quên bấm thẻ (3 lần miễn phí, từ lần 4 -10k/lần), đi trễ/về sớm >=30p (-30k),
    //    vắng không phép (mất chuyên cần + KPI -50% / -100%)
    @Test
    fun testBestarDiligenceAndDisciplineRules() {
        val cfg = bestarOfficialConfig()

        // 1 ngày phép riêng + 4 lần quên bấm thẻ + trễ 30p:
        // Chuyên cần 300k - 150k (1 ngày phép riêng) - 10k (lần quên thứ 4) - 30k (trễ 30p) = 110k
        val entries = listOf(
            TimeEntry(userId = "u", date = "2026-09-14", shiftId = "ca1", checkInTime = getTimeMs(day = 14, hour = 7, minute = 30), checkOutTime = getTimeMs(day = 14, hour = 16, minute = 30), isForgotPunch = true),
            TimeEntry(userId = "u", date = "2026-09-15", shiftId = "ca1", checkInTime = getTimeMs(day = 15, hour = 7, minute = 30), checkOutTime = getTimeMs(day = 15, hour = 16, minute = 30), isForgotPunch = true),
            TimeEntry(userId = "u", date = "2026-09-16", shiftId = "ca1", checkInTime = getTimeMs(day = 16, hour = 7, minute = 30), checkOutTime = getTimeMs(day = 16, hour = 16, minute = 30), isForgotPunch = true),
            TimeEntry(userId = "u", date = "2026-09-17", shiftId = "ca1", checkInTime = getTimeMs(day = 17, hour = 8, minute = 0), checkOutTime = getTimeMs(day = 17, hour = 16, minute = 30), isForgotPunch = true),
            TimeEntry(userId = "u", date = "2026-09-18", dayType = "UNPAID_LEAVE")
        )
        val res1 = SalaryCalculator.calculateMonthlySalary(
            entries = entries,
            config = cfg,
            scheduledDaysSoFar = 5,
            totalScheduledDaysInMonth = 26,
            earliestDate = "2026-09-14",
            selectedMonth = "2026-09",
            todayStr = "2026-09-30",
            isCurrentSelectedMonth = false,
            holidayDatesInMonth = emptySet()
        )
        assertEquals(4, res1.forgotPunchCount)
        assertEquals(30, res1.totalLateEarlyMinutes)
        assertEquals(190_000.0, res1.tienPhatChuyenCan, 1.0)
        assertEquals(110_000.0, res1.phuCapChuyenCan, 1.0)
        assertEquals(500_000.0, res1.pcHieuSuatVal, 1.0)

        // 1 ngày nghỉ không phép -> mất hết chuyên cần, hiệu suất giảm 50% (còn 250k)
        val unauthEntries = listOf(
            TimeEntry(userId = "u", date = "2026-09-14", shiftId = "ca1", checkInTime = getTimeMs(day = 14, hour = 7, minute = 30), checkOutTime = getTimeMs(day = 14, hour = 16, minute = 30)),
            TimeEntry(userId = "u", date = "2026-09-15", dayType = "UNAUTHORIZED_LEAVE")
        )
        val resUnauth = SalaryCalculator.calculateMonthlySalary(
            entries = unauthEntries,
            config = cfg,
            scheduledDaysSoFar = 2,
            totalScheduledDaysInMonth = 26,
            earliestDate = "2026-09-14",
            selectedMonth = "2026-09",
            todayStr = "2026-09-30",
            isCurrentSelectedMonth = false,
            holidayDatesInMonth = emptySet()
        )
        assertEquals(0.0, resUnauth.phuCapChuyenCan, 1.0)
        assertEquals(250_000.0, resUnauth.pcHieuSuatVal, 1.0)
    }

    // 10. Thử việc Bestar: chuyên cần theo ngày thực tế, nghỉ phép mất sạch chuyên cần, khấu trừ 10% TNCN
    @Test
    fun testProbationPit10PercentAndDiligence() {
        val probationCfg = bestarOfficialConfig(
            luongCoBan = 5_310_000.0,
            isThuViec = true
        ).copy(pcHieuSuat = 0.0, pcKhac1 = 500_000.0)

        val entries = (1..30)
            .map { d -> String.format("2026-09-%02d", d) }
            .filter { !SalaryCalculator.isSunday(it) }
            .map { dateStr ->
                val dayNum = dateStr.takeLast(2).toInt()
                TimeEntry(
                    userId = "u",
                    date = dateStr,
                    shiftId = "ca1",
                    checkInTime = getTimeMs(day = dayNum, hour = 7, minute = 30),
                    checkOutTime = getTimeMs(day = dayNum, hour = 16, minute = 30)
                )
            }
        val res = SalaryCalculator.calculateMonthlySalary(
            entries = entries,
            config = probationCfg,
            scheduledDaysSoFar = 26,
            totalScheduledDaysInMonth = 26,
            earliestDate = "2026-09-01",
            selectedMonth = "2026-09",
            todayStr = "2026-09-30",
            isCurrentSelectedMonth = false,
            holidayDatesInMonth = emptySet()
        )

        // Gross = 5.310.000 + 300.000 + 500.000 = 6.110.000; PIT 10% = 611.000; Net = 5.499.000
        assertEquals(300_000.0, res.phuCapChuyenCan, 1.0)
        assertEquals(611_000.0, res.tienThueTncn, 1.0)
        assertEquals(5_499_000.0, res.luongThucNhan, 1.0)
    }
}
