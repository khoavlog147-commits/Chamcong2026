package com.example.data

import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import com.example.viewmodel.SalarySummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ShiftConfig(
    val shiftId: String,
    val shiftType: String, // "DAY", "DAY_REST", "NIGHT"
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val checkInWindowStart: String,
    val checkInWindowEnd: String,
    val checkOutWindowStart: String,
    val checkOutWindowEnd: String,
    val breakHours: Double,
    val standardHours: Double = 8.0
)

object SalaryCalculator {

    val SHIFTS = mapOf(
        "ca1" to ShiftConfig(
            shiftId = "ca1",
            shiftType = "DAY",
            startTime = "07:30",
            endTime = "19:30",
            checkInWindowStart = "07:00",
            checkInWindowEnd = "07:30",
            checkOutWindowStart = "19:30",
            checkOutWindowEnd = "20:00",
            breakHours = 0.0,
            standardHours = 8.0
        ),
        "ca2" to ShiftConfig(
            shiftId = "ca2",
            shiftType = "DAY_REST",
            startTime = "07:30",
            endTime = "20:00",
            checkInWindowStart = "07:00",
            checkInWindowEnd = "07:30",
            checkOutWindowStart = "20:00",
            checkOutWindowEnd = "20:30",
            breakHours = 1.5,
            standardHours = 8.0
        ),
        "ca_dem" to ShiftConfig(
            shiftId = "ca_dem",
            shiftType = "NIGHT",
            startTime = "19:30",
            endTime = "07:30",
            checkInWindowStart = "19:00",
            checkInWindowEnd = "19:30",
            checkOutWindowStart = "07:30",
            checkOutWindowEnd = "08:00",
            breakHours = 0.0,
            standardHours = 8.0
        )
    )

    fun getShiftForEntry(entry: TimeEntry): ShiftConfig {
        val shiftId = entry.shiftId
        if (shiftId != null && SHIFTS.containsKey(shiftId)) {
            return SHIFTS[shiftId]!!
        }
        if (entry.shiftType == "NIGHT" || entry.dayType == "NIGHT") {
            return SHIFTS["ca_dem"]!!
        }
        // Fallback: detect based on old data or check-in time
        val inTime = entry.checkInTime ?: return SHIFTS["ca1"]!!
        val cal = Calendar.getInstance().apply { timeInMillis = inTime }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        if (hour >= 15 || hour < 6) {
            return SHIFTS["ca_dem"]!!
        }
        val outTime = entry.checkOutTime
        if (outTime != null) {
            val calOut = Calendar.getInstance().apply { timeInMillis = outTime }
            val outHour = calOut.get(Calendar.HOUR_OF_DAY)
            val outMin = calOut.get(Calendar.MINUTE)
            val outTotalMin = outHour * 60 + outMin
            if (outTotalMin >= 19 * 60 + 45) { // 19:45
                return SHIFTS["ca2"]!!
            }
        }
        return SHIFTS["ca1"]!!
    }

    private fun getMillisForTime(baseTimeMs: Long, timeStr: String, dayOffset: Int = 0): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = baseTimeMs }
        val parts = timeStr.split(":")
        val hour = parts[0].toInt()
        val min = parts[1].toInt()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, min)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (dayOffset != 0) {
            cal.add(Calendar.DAY_OF_MONTH, dayOffset)
        }
        return cal.timeInMillis
    }

    fun normalizeDateToDmy(dateStr: String): String {
        val s = dateStr.trim()
        if (s.contains("-")) {
            val parts = s.split("-")
            if (parts.size == 3) {
                return if (parts[0].length == 4) {
                    // yyyy-MM-dd -> dd/MM/yyyy
                    val dd = parts[2].padStart(2, '0')
                    val mm = parts[1].padStart(2, '0')
                    val yyyy = parts[0]
                    "$dd/$mm/$yyyy"
                } else if (parts[2].length == 4) {
                    // dd-MM-yyyy -> dd/MM/yyyy
                    val dd = parts[0].padStart(2, '0')
                    val mm = parts[1].padStart(2, '0')
                    val yyyy = parts[2]
                    "$dd/$mm/$yyyy"
                } else {
                    s.replace("-", "/")
                }
            }
        } else if (s.contains("/")) {
            val parts = s.split("/")
            if (parts.size == 3) {
                val dd = parts[0].padStart(2, '0')
                val mm = parts[1].padStart(2, '0')
                val yyyy = parts[2]
                return "$dd/$mm/$yyyy"
            }
        }
        return s.replace("-", "/")
    }

    fun normalizeToYmd(dateStr: String): String {
        val s = dateStr.trim()
        if (s.contains("/")) {
            val parts = s.split("/")
            if (parts.size == 3) {
                val dd = parts[0].padStart(2, '0')
                val mm = parts[1].padStart(2, '0')
                val yyyy = parts[2]
                return "$yyyy-$mm-$dd"
            }
        } else if (s.contains("-")) {
            val parts = s.split("-")
            if (parts.size == 3) {
                if (parts[0].length == 4) {
                    val yyyy = parts[0]
                    val mm = parts[1].padStart(2, '0')
                    val dd = parts[2].padStart(2, '0')
                    return "$yyyy-$mm-$dd"
                } else if (parts[2].length == 4) {
                    val dd = parts[0].padStart(2, '0')
                    val mm = parts[1].padStart(2, '0')
                    val yyyy = parts[2]
                    return "$yyyy-$mm-$dd"
                }
            }
        }
        return s
    }

    fun isPaidLeaveType(dayType: String?): Boolean {
        if (dayType.isNullOrBlank()) return false
        val upper = dayType.uppercase(Locale.ROOT)
        return (upper == "PAID_LEAVE" || upper == "PAIDLEAVE" || upper == "NP" || upper == "PHEP" || upper == "PHÉP" || upper == "HOLIDAY_LEAVE" || upper == "HOLIDAYLEAVE" || upper == "ANNUAL_LEAVE" || upper.contains("PAID") || upper.contains("PHÉP") || upper.contains("PHEP")) &&
               !upper.contains("UNPAID") && !upper.contains("UNAUTHORIZED") && !upper.contains("KP") && !upper.contains("KHONG") && !upper.contains("KHÔNG")
    }

    fun isUnpaidLeaveType(dayType: String?): Boolean {
        if (dayType.isNullOrBlank()) return false
        val upper = dayType.uppercase(Locale.ROOT)
        return upper == "UNPAID_LEAVE" || upper == "UNPAIDLEAVE" || upper == "UNAUTHORIZED_LEAVE" || upper == "UNAUTHORIZEDLEAVE" || upper == "KP" || upper == "KHONGPHEP" || upper.contains("UNPAID") || upper.contains("UNAUTHORIZED") || upper.contains("KHÔNG LƯƠNG") || upper.contains("KHONG LUONG") || upper.contains("KHÔNG PHÉP") || upper.contains("KHONG PHEP")
    }

    fun isLeaveType(dayType: String?): Boolean {
        if (dayType.isNullOrBlank()) return false
        val upper = dayType.uppercase(Locale.ROOT)
        return isPaidLeaveType(dayType) || isUnpaidLeaveType(dayType) ||
               upper == "PAID" || upper == "UNPAID" || upper == "UNAUTHORIZED" ||
               upper == "NP" || upper == "PHEP" || upper == "KP" || upper == "KHONGPHEP" || upper == "ABSENT" ||
               upper.contains("LEAVE") || upper.contains("PHÉP") || upper.contains("PHEP") || upper.contains("NGHỈ") || upper.contains("NGHI")
    }

    /**
     * Step 1 - 6: Process and calculate single record details.
     * Returns a new TimeEntry with populated/re-calculated fields.
     */
    fun calculateSingleEntry(entry: TimeEntry, config: UserConfig? = null): TimeEntry {
        if (isLeaveType(entry.dayType)) {
            val upper = entry.dayType.uppercase(Locale.ROOT)
            val workD = if (upper.contains("PAID") || upper == "NP" || upper.contains("PHEP") || upper.contains("PHÉP") || upper.contains("HOLIDAY") || upper.contains("LỄ") || upper.contains("LE")) 1.0 else 0.0
            return entry.copy(
                workDay = workD,
                otHours = 0.0,
                lateMinutes = 0,
                earlyLeaveMinutes = 0,
                rawCheckIn = null,
                rawCheckOut = null,
                normalizedCheckIn = null,
                normalizedCheckOut = null
            )
        }

        val hasTimes = entry.checkInTime != null && entry.checkOutTime != null
        val workingEntry = entry

        val rawInRaw = workingEntry.checkInTime ?: return workingEntry.copy(workDay = 0.0, otHours = 0.0, lateMinutes = 0, earlyLeaveMinutes = 0)
        // Round to nearest minute to avoid sub-minute floating point variance across different days
        val rawIn = Math.round(rawInRaw / 60000.0) * 60000L
        val rawOutRaw = workingEntry.checkOutTime
        val rawOut = rawOutRaw?.let { Math.round(it / 60000.0) * 60000L }

        // 1. Load Shift configuration
        val shift = getShiftForEntry(workingEntry)

        // 2. Normalization
        val stdInMs = getMillisForTime(rawIn, shift.startTime, 0)

        // Check-In Normalization: Early check-in buffer is 30 minutes. 
        // Any check-in at or before standard start time (stdInMs) is normalized to stdInMs.
        val normInMs = if (rawIn <= stdInMs) {
            stdInMs
        } else {
            rawIn
        }

        // Check-Out Normalization: Any check-out within the shift's check-out window (stdOutMs to checkOutWindowEnd)
        // is normalized to standard shift end (stdOutMs), so no extra OT or variation is counted.
        // Any check-out exceeding the window is kept raw to count extra OT. Any check-out before standard end is kept raw (early leave).
        val normOutMs = if (rawOut != null) {
            val dayOffset = if (shift.shiftType == "NIGHT") 1 else 0
            val stdOutMs = getMillisForTime(rawIn, shift.endTime, dayOffset)

            if (rawOut >= stdOutMs) {
                val diffMs = rawOut - stdOutMs
                if (diffMs < 30 * 60000L) {
                    stdOutMs
                } else {
                    rawOut
                }
            } else {
                rawOut
            }
        } else {
            null
        }

        // 3. Late Check-In Minutes
        val lateMin = if (normInMs > stdInMs) {
            ((normInMs - stdInMs) / 60000.0).toInt()
        } else {
            0
        }

        // 4. Early Leave Minutes
        val earlyLeaveMin = if (normOutMs != null) {
            val dayOffset = if (shift.shiftType == "NIGHT") 1 else 0
            val stdOutMs = getMillisForTime(rawIn, shift.endTime, dayOffset)
            if (normOutMs < stdOutMs) {
                ((stdOutMs - normOutMs) / 60000.0).toInt()
            } else {
                0
            }
        } else {
            0
        }

        val resolvedBreakDeduction = workingEntry.customBreakDeduction ?: config?.tinhKhauTruNghi ?: (shift.breakHours > 0.0)
        val breakHrsToUse = if (resolvedBreakDeduction) {
            workingEntry.customBreakHours ?: config?.soGioNghiGiaiLao ?: shift.breakHours
        } else {
            0.0
        }

        // 5. Calculate WorkDay according to company rules
        val maxLateOrEarly = Math.max(lateMin, earlyLeaveMin)
        val workD = if (rawOut == null) {
            // Checked in but still working
            if (lateMin < 15) 1.0 else if (lateMin < 120) 0.5 else 0.0
        } else {
            // Completed check-out: Calculate actual hours worked
            val outMs = normOutMs ?: rawOut
            val workedHrs = (outMs - normInMs) / 3600000.0
            val actualWorkedHrs = (workedHrs - breakHrsToUse).coerceAtLeast(0.0)
            when {
                actualWorkedHrs >= 8.0 -> 1.0
                actualWorkedHrs >= 4.0 -> 0.5
                maxLateOrEarly < 15 -> 1.0
                maxLateOrEarly < 120 -> 0.5
                else -> 0.0
            }
        }

        // 6. Calculate OT Hours according to shift
        val otHrs = if (normOutMs != null) {
            val workedHrs = (normOutMs - normInMs) / 3600000.0
            val actualWorkedHrs = (workedHrs - breakHrsToUse).coerceAtLeast(0.0)
            (actualWorkedHrs - shift.standardHours).coerceAtLeast(0.0)
        } else {
            0.0
        }

        return workingEntry.copy(
            shiftId = shift.shiftId,
            shiftType = shift.shiftType,
            rawCheckIn = rawIn,
            rawCheckOut = rawOut,
            normalizedCheckIn = normInMs,
            normalizedCheckOut = normOutMs,
            workDay = workD,
            otHours = otHrs,
            lateMinutes = lateMin,
            earlyLeaveMinutes = earlyLeaveMin,
            customBreakDeduction = resolvedBreakDeduction,
            customBreakHours = breakHrsToUse
        )
    }

    fun calculateAllowanceValue(
        fieldName: String,
        allowanceValue: Double,
        calcType: String,
        totalWorkDays: Double,
        comCaCount: Int,
        comOtCount: Int,
        nightShiftsCount: Int,
        scheduledDaysSoFar: Int,
        totalScheduledDaysInMonth: Int
    ): Double {
        return when (calcType) {
            "MONTHLY_PRO_RATED" -> {
                // Pro-rata based on actual work days out of 26 standard days, capped at 100% (1.0)
                val ratio = (totalWorkDays / 26.0).coerceAtMost(1.0)
                allowanceValue * ratio
            }
            "MONTHLY_FLAT" -> {
                allowanceValue
            }
            "PER_WORK_DAY" -> {
                if (fieldName == "pcComCa") {
                    comCaCount * allowanceValue
                } else {
                    totalWorkDays * allowanceValue
                }
            }
            "OT_MEAL_GE_2H", "OT_MEAL_GE_1H" -> {
                comOtCount * allowanceValue
            }
            "PER_NIGHT_SHIFT" -> {
                nightShiftsCount * allowanceValue
            }
            else -> {
                val ratio = (totalWorkDays / 26.0).coerceAtMost(1.0)
                allowanceValue * ratio
            }
        }
    }

    /**
     * Steps 7 & 8: Calculate Diligence and Monthly Salary Summary
     */
    fun calculateMonthlySalary(
        entries: List<TimeEntry>,
        config: UserConfig,
        scheduledDaysSoFar: Int,
        totalScheduledDaysInMonth: Int,
        earliestDate: String?,
        selectedMonth: String,
        todayStr: String,
        isCurrentSelectedMonth: Boolean,
        holidayDatesInMonth: Set<String>
    ): SalarySummary {
        val luongBasic = config.luongCoBan
        val dailySalary = luongBasic / 26.0
        val hourlySalary = dailySalary / 8.0

        // Process all entries through steps 1-6
        val processedEntries = entries.map { calculateSingleEntry(it, config) }

        // Identify which holiday dates have been worked (have check-in logged)
        val workedHolidayDates = processedEntries.filter { e ->
            holidayDatesInMonth.contains(e.date) && e.rawCheckIn != null
        }.map { it.date }.toSet()

        // Unworked holidays automatically merit full 1-day standard salary as a Holiday Leave
        val unworkedHolidaysCount = (holidayDatesInMonth - workedHolidayDates).size

        // Aggregators
        var totalWorkDays = unworkedHolidaysCount.toDouble()
        var actualPresenceDaysCount = 0
        var totalStandardHours = unworkedHolidaysCount * 8.0
        var totalOtDayHours = 0.0
        var totalOtNightHours = 0.0
        var totalOtLeHours = 0.0
        var totalSundayHours = 0.0
        var totalSundayDayHours = 0.0
        var totalSundayNightHours = 0.0

        var otDayPay = 0.0
        var otLePay = 0.0
        var otNightPay = 0.0
        var sundayDayPay = 0.0
        var sundayNightPay = 0.0
        var sundayPay = 0.0
        var nightShiftsCount = 0

        val breakHours = if (config.tinhKhauTruNghi) config.soGioNghiGiaiLao else 0.0

        val normTodayStr = normalizeToYmd(todayStr)

        for (e in processedEntries) {
            val normEntryDate = normalizeToYmd(e.date)
            // Do not calculate future days/leaves as they have not happened yet if they are unworked
            if (isCurrentSelectedMonth && normEntryDate > normTodayStr && e.rawCheckIn == null) {
                continue
            }

            val isHolidayDateVal = holidayDatesInMonth.contains(e.date) || holidayDatesInMonth.contains(normEntryDate) || holidayDatesInMonth.contains(normalizeDateToDmy(e.date))
            if (isHolidayDateVal && e.rawCheckIn == null) {
                // Already counted automatically as unworked holiday, skip processing to avoid duplication
                continue
            }

            if (isPaidLeaveType(e.dayType)) {
                totalWorkDays += 1.0
                totalStandardHours += 8.0
                continue
            }
            if (isUnpaidLeaveType(e.dayType)) {
                continue
            }

            if (e.rawCheckIn == null) continue

            // Night shift count
            val isNight = e.shiftType == "NIGHT"
            if (isNight) {
                nightShiftsCount++
            }

            val isSundayVal = e.dayType == "SUNDAY" || isSunday(e.date)

            // If checked-in but currently working (no check-out yet)
            if (e.rawCheckOut == null && e.isWorking) {
                if (isSundayVal) {
                    actualPresenceDaysCount++
                    val activeHours = 8.0
                    totalSundayHours += activeHours
                    if (isNight) {
                        totalSundayNightHours += activeHours
                        sundayNightPay += activeHours * hourlySalary * config.heSoOtChuNhat
                    } else {
                        totalSundayDayHours += activeHours
                        sundayDayPay += activeHours * hourlySalary * config.heSoOtChuNhat
                    }
                    sundayPay = sundayDayPay + sundayNightPay
                } else {
                    totalWorkDays += e.workDay
                    actualPresenceDaysCount++
                    totalStandardHours += 8.0
                }
                continue
            }

            if (e.rawCheckOut == null) continue

            val eBreakHours = e.customBreakHours ?: breakHours

            // Standard Day Work Contribution
            if (isSundayVal) {
                actualPresenceDaysCount++
                val workedHrs = (e.normalizedCheckOut!! - e.normalizedCheckIn!!) / 3600000.0
                val actualHours = (workedHrs - eBreakHours).coerceAtLeast(0.0)
                totalSundayHours += actualHours
                if (isNight) {
                    totalSundayNightHours += actualHours
                    sundayNightPay += actualHours * hourlySalary * config.heSoOtChuNhat
                } else {
                    totalSundayDayHours += actualHours
                    sundayDayPay += actualHours * hourlySalary * config.heSoOtChuNhat
                }
                sundayPay = sundayDayPay + sundayNightPay
            } else {
                totalWorkDays += e.workDay
                actualPresenceDaysCount++
                
                val workedHrs = (e.normalizedCheckOut!! - e.normalizedCheckIn!!) / 3600000.0
                val actualHours = (workedHrs - eBreakHours).coerceAtLeast(0.0)
                val finalStandardHours = actualHours.coerceAtMost(8.0)
                totalStandardHours += finalStandardHours

                val finalOtHours = e.otHours
                if (finalOtHours > 0.0) {
                    if (e.dayType == "HOLIDAY") {
                        totalOtLeHours += finalOtHours
                        otLePay += finalOtHours * (hourlySalary * config.heSoOtNgayLe)
                    } else if (e.shiftType == "NIGHT") {
                        totalOtNightHours += finalOtHours
                        otNightPay += finalOtHours * (hourlySalary * config.heSoOtDem)
                    } else {
                        totalOtDayHours += finalOtHours
                        otDayPay += finalOtHours * (hourlySalary * config.heSoOtNgayThuong)
                    }
                }
            }
        }

        // Calculate counts for meal and OT allowances
        var comCaCount = 0
        var comOtCount = 0

        for (e in processedEntries) {
            val normEntryDate = normalizeToYmd(e.date)
            if (isCurrentSelectedMonth && normEntryDate > normTodayStr && e.rawCheckIn == null) {
                continue
            }
            if (e.rawCheckIn == null) continue

            // comCaCount: Full shift (workDay >= 1.0)
            if (e.workDay >= 1.0) {
                comCaCount++
            }
            
            // comOtCount: Only count if calculated OT is >= 1.0h (satisfying OT meal >= 1 hour condition)
            if (e.otHours >= 1.0) {
                comOtCount++
            }
        }

        // Dynamic Allowance Calculation Engine based on Calculation Types
        val pcKyThuatPr = calculateAllowanceValue("pcKyThuat", config.pcKyThuat, config.getCalcTypeFor("pcKyThuat"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcTrachNhiemPr = calculateAllowanceValue("pcTrachNhiem", config.pcTrachNhiem, config.getCalcTypeFor("pcTrachNhiem"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcChucVuPr = calculateAllowanceValue("pcChucVu", config.pcChucVu, config.getCalcTypeFor("pcChucVu"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcHieuSuatPr = calculateAllowanceValue("pcHieuSuat", config.pcHieuSuat, config.getCalcTypeFor("pcHieuSuat"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcSanPhamPr = calculateAllowanceValue("pcSanPham", config.pcSanPham, config.getCalcTypeFor("pcSanPham"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcComCaPr = calculateAllowanceValue("pcComCa", config.pcComCa, config.getCalcTypeFor("pcComCa"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcComOtPr = calculateAllowanceValue("pcComOt", config.pcComOt, config.getCalcTypeFor("pcComOt"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcNhaOPr = calculateAllowanceValue("pcNhaO", config.pcNhaO, config.getCalcTypeFor("pcNhaO"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcDocHaiPr = calculateAllowanceValue("pcDocHai", config.pcDocHai, config.getCalcTypeFor("pcDocHai"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcDtDoanhThuPr = calculateAllowanceValue("pcDtDoanhThu", config.pcDtDoanhThu, config.getCalcTypeFor("pcDtDoanhThu"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcXangXePr = calculateAllowanceValue("pcXangXe", config.pcXangXe, config.getCalcTypeFor("pcXangXe"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcThamNienPr = calculateAllowanceValue("pcThamNien", config.pcThamNien, config.getCalcTypeFor("pcThamNien"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcKhac1Pr = calculateAllowanceValue("pcKhac1", config.pcKhac1, config.getCalcTypeFor("pcKhac1"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        
        // pcCaDem is Phụ cấp ca đêm (mỗi ca)
        val pcCaDemPr = calculateAllowanceValue("pcCaDem", config.pcCaDem, config.getCalcTypeFor("pcCaDem"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)

        val phuCapTong = pcKyThuatPr + pcTrachNhiemPr + pcChucVuPr + pcHieuSuatPr + 
                pcSanPhamPr + pcComCaPr + pcComOtPr + pcNhaOPr + 
                pcDocHaiPr + pcDtDoanhThuPr + pcXangXePr + pcThamNienPr + 
                pcKhac1Pr + pcCaDemPr

        // 7. Calculate Chuyên cần (Diligence)
        val normEarliestDate = earliestDate?.let { normalizeToYmd(it) }
        val hasUnpaidOrAbsent = processedEntries.any { 
            val normD = normalizeToYmd(it.date)
            isUnpaidLeaveType(it.dayType) && (normEarliestDate == null || normD >= normEarliestDate)
        } || (scheduledDaysSoFar > 0 && totalWorkDays < scheduledDaysSoFar)

        val chuyenCanValue = if (hasUnpaidOrAbsent) {
            0.0
        } else {
            calculateAllowanceValue(
                "tienChuyenCanGoc",
                config.tienChuyenCanGoc,
                config.getCalcTypeFor("tienChuyenCanGoc"),
                totalWorkDays,
                comCaCount,
                comOtCount,
                nightShiftsCount,
                scheduledDaysSoFar,
                totalScheduledDaysInMonth
            )
        }

        // Deductions
        val tieuBaoHiem = Math.round(config.luongDongBaoHiem * (config.tiLeDongBaoHiem / 100.0)).toDouble()
        val doanPhi = config.doanPhiCongDoan
        val tienKhauTruNghi = 0.0

        // 8. Calculate Monthly Salary
        val baseBasicSalary = Math.round(totalWorkDays * dailySalary).toDouble()

        val roundedOtDay = Math.round(otDayPay).toDouble()
        val roundedOtLePay = Math.round(otLePay).toDouble()
        val roundedOtNight = Math.round(otNightPay).toDouble()
        val roundedSundayPay = Math.round(sundayPay).toDouble()

        val grossAdditions = baseBasicSalary + roundedOtDay + roundedOtLePay + roundedOtNight + roundedSundayPay + phuCapTong + chuyenCanValue
        val totalDeductions = tieuBaoHiem + doanPhi + tienKhauTruNghi
        val luongThucNhan = Math.round(grossAdditions - totalDeductions).coerceAtLeast(0L).toDouble()

        return SalarySummary(
            workingDays = totalWorkDays,
            standardHours = totalStandardHours,
            otDayHours = totalOtDayHours,
            otNightHours = totalOtNightHours,
            tienOtNgay = roundedOtDay,
            tienOtDem = roundedOtNight,
            tongTienCom = pcComCaPr + pcComOtPr,
            phuCap = phuCapTong,
            phuCapXangXe = pcXangXePr,
            phuCapDienThoai = pcDtDoanhThuPr,
            phuCapNhaO = pcNhaOPr,
            phuCapChuyenCan = chuyenCanValue,
            thuong = 0.0,
            tienBh = tieuBaoHiem,
            doanPhi = doanPhi,
            tienKhauTruNghi = tienKhauTruNghi,
            luongThucNhan = luongThucNhan,
            baseBasicSalary = baseBasicSalary,
            expectedWorkDays = totalScheduledDaysInMonth,
            standardWorkDays = 26,
            scheduledDaysSoFar = scheduledDaysSoFar,
            isCurrentMonth = isCurrentSelectedMonth,
            
            pcKyThuatVal = pcKyThuatPr,
            pcTrachNhiemVal = pcTrachNhiemPr,
            pcChucVuVal = pcChucVuPr,
            pcHieuSuatVal = pcHieuSuatPr,
            pcSanPhamVal = pcSanPhamPr,
            pcComCaVal = pcComCaPr,
            pcComOtVal = pcComOtPr,
            pcNhaOVal = pcNhaOPr,
            pcDocHaiVal = pcDocHaiPr,
            pcDtDoanhThuVal = pcDtDoanhThuPr,
            pcXangXeVal = pcXangXePr,
            pcThamNienVal = pcThamNienPr,
            pcKhac1Val = pcKhac1Pr,
            pcKhacVal = pcCaDemPr,
            pcCaDemVal = pcCaDemPr,
            caDemCount = nightShiftsCount,
            
            tienChuNhat = roundedSundayPay,
            tienChuNhatNgay = Math.round(sundayDayPay).toDouble(),
            tienChuNhatDem = Math.round(sundayNightPay).toDouble(),
            chuNhatHours = totalSundayHours,
            chuNhatDayHours = totalSundayDayHours,
            chuNhatNightHours = totalSundayNightHours,
            otLeHours = totalOtLeHours,
            tienOtLe = roundedOtLePay
        )
    }

    // Static compatibility methods for holiday/sunday detection
    private val LUNAR_HOLIDAY_EXACT_MAP = mapOf(
        // 2023
        "20/01/2023" to "29 Tết Nguyên Đán",
        "21/01/2023" to "30 Tết (Giao Thừa)",
        "22/01/2023" to "Mùng 1 Tết Nguyên Đán",
        "23/01/2023" to "Mùng 2 Tết Nguyên Đán",
        "24/01/2023" to "Mùng 3 Tết Nguyên Đán",
        "25/01/2023" to "Mùng 4 Tết Nguyên Đán",
        "26/01/2023" to "Mùng 5 Tết Nguyên Đán",
        "29/04/2023" to "Giỗ Tổ Hùng Vương (10/3 ÂL)",
        // 2024
        "08/02/2024" to "29 Tết Nguyên Đán",
        "09/02/2024" to "30 Tết (Giao Thừa)",
        "10/02/2024" to "Mùng 1 Tết Nguyên Đán",
        "11/02/2024" to "Mùng 2 Tết Nguyên Đán",
        "12/02/2024" to "Mùng 3 Tết Nguyên Đán",
        "13/02/2024" to "Mùng 4 Tết Nguyên Đán",
        "14/02/2024" to "Mùng 5 Tết Nguyên Đán",
        "18/04/2024" to "Giỗ Tổ Hùng Vương (10/3 ÂL)",
        // 2025
        "27/01/2025" to "28 Tết Nguyên Đán",
        "28/01/2025" to "29 Tết (Giao Thừa)",
        "29/01/2025" to "Mùng 1 Tết Nguyên Đán",
        "30/01/2025" to "Mùng 2 Tết Nguyên Đán",
        "31/01/2025" to "Mùng 3 Tết Nguyên Đán",
        "01/02/2025" to "Mùng 4 Tết Nguyên Đán",
        "02/02/2025" to "Mùng 5 Tết Nguyên Đán",
        "07/04/2025" to "Giỗ Tổ Hùng Vương (10/3 ÂL)",
        // 2026
        "15/02/2026" to "28 Tết Nguyên Đán",
        "16/02/2026" to "29 Tết (Giao Thừa)",
        "17/02/2026" to "Mùng 1 Tết Nguyên Đán",
        "18/02/2026" to "Mùng 2 Tết Nguyên Đán",
        "19/02/2026" to "Mùng 3 Tết Nguyên Đán",
        "20/02/2026" to "Mùng 4 Tết Nguyên Đán",
        "21/02/2026" to "Mùng 5 Tết Nguyên Đán",
        "26/04/2026" to "Giỗ Tổ Hùng Vương (10/3 ÂL)",
        // 2027
        "05/02/2027" to "29 Tết Nguyên Đán",
        "06/02/2027" to "30 Tết (Giao Thừa)",
        "07/02/2027" to "Mùng 1 Tết Nguyên Đán",
        "08/02/2027" to "Mùng 2 Tết Nguyên Đán",
        "09/02/2027" to "Mùng 3 Tết Nguyên Đán",
        "10/02/2027" to "Mùng 4 Tết Nguyên Đán",
        "11/02/2027" to "Mùng 5 Tết Nguyên Đán",
        "16/04/2027" to "Giỗ Tổ Hùng Vương (10/3 ÂL)",
        // 2028
        "25/01/2028" to "29 Tết Nguyên Đán",
        "26/01/2028" to "30 Tết (Giao Thừa)",
        "27/01/2028" to "Mùng 1 Tết Nguyên Đán",
        "28/01/2028" to "Mùng 2 Tết Nguyên Đán",
        "29/01/2028" to "Mùng 3 Tết Nguyên Đán",
        "30/01/2028" to "Mùng 4 Tết Nguyên Đán",
        "31/01/2028" to "Mùng 5 Tết Nguyên Đán",
        "04/04/2028" to "Giỗ Tổ Hùng Vương (10/3 ÂL)",
        // 2029
        "12/02/2029" to "29 Tết (Giao Thừa)",
        "13/02/2029" to "Mùng 1 Tết Nguyên Đán",
        "14/02/2029" to "Mùng 2 Tết Nguyên Đán",
        "15/02/2029" to "Mùng 3 Tết Nguyên Đán",
        "16/02/2029" to "Mùng 4 Tết Nguyên Đán",
        "17/02/2029" to "Mùng 5 Tết Nguyên Đán",
        "23/04/2029" to "Giỗ Tổ Hùng Vương (10/3 ÂL)",
        // 2030
        "02/02/2030" to "29 Tết (Giao Thừa)",
        "03/02/2030" to "Mùng 1 Tết Nguyên Đán",
        "04/02/2030" to "Mùng 2 Tết Nguyên Đán",
        "05/02/2030" to "Mùng 3 Tết Nguyên Đán",
        "06/02/2030" to "Mùng 4 Tết Nguyên Đán",
        "07/02/2030" to "Mùng 5 Tết Nguyên Đán",
        "12/04/2030" to "Giỗ Tổ Hùng Vương (10/3 ÂL)"
    )

    fun getHolidayName(dateString: String): String? {
        return try {
            val parser = if (dateString.contains("/")) {
                SimpleDateFormat("dd/MM/yyyy", Locale.US)
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.US)
            }
            val date = parser.parse(dateString) ?: return null
            val cal = Calendar.getInstance().apply { time = date }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val month = cal.get(Calendar.MONTH) + 1
            val year = cal.get(Calendar.YEAR)
            val md = String.format(Locale.US, "%02d/%02d", day, month)
            val dmy = String.format(Locale.US, "%02d/%02d/%04d", day, month, year)

            // 1. Check fixed Solar Holidays (Chỉ các ngày lễ chính thức)
            when (md) {
                "01/01" -> return "Tết Dương Lịch"
                "30/04" -> return "Giải Phóng Miền Nam"
                "01/05" -> return "Quốc Tế Lao Động"
                "02/09" -> return "Quốc Khánh (2/9)"
            }

            // 2. Check Vietnamese Lunar Holidays (Tết Âm Lịch & Giỗ Tổ Hùng Vương)
            LUNAR_HOLIDAY_EXACT_MAP[dmy]
        } catch (e: Exception) {
            null
        }
    }

    fun isHoliday(dateString: String): Boolean {
        return getHolidayName(dateString) != null
    }

    fun isSunday(dateString: String): Boolean {
        return try {
            val parser = if (dateString.contains("-")) {
                SimpleDateFormat("yyyy-MM-dd", Locale.US)
            } else {
                SimpleDateFormat("dd/MM/yyyy", Locale.US)
            }
            val date = parser.parse(dateString) ?: return false
            val cal = Calendar.getInstance()
            cal.time = date
            cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        } catch (e: Exception) {
            false
        }
    }

    fun isNightShift(clockInTime: Long, clockOutTime: Long?): Boolean {
        val outTime = clockOutTime ?: System.currentTimeMillis()
        val calIn = Calendar.getInstance().apply { timeInMillis = clockInTime }
        val startHour = calIn.get(Calendar.HOUR_OF_DAY)
        if (startHour >= 18 || startHour <= 5) return true

        val calOut = Calendar.getInstance().apply { timeInMillis = outTime }
        val endHour = calOut.get(Calendar.HOUR_OF_DAY)
        if (endHour >= 22 || endHour <= 7) return true

        return false
    }

    fun getDayTypeLabel(dateString: String): String {
        return when {
            isHoliday(dateString) -> "NGÀY LỄ"
            isSunday(dateString) -> "CHỦ NHẬT"
            else -> "NGÀY THƯỜNG"
        }
    }

    fun getRoundedTime(timeMillis: Long, isClockIn: Boolean): Long {
        return timeMillis
    }

    fun calculatePIT(taxableIncome: Double): Double {
        if (taxableIncome <= 0) return 0.0
        return when {
            taxableIncome <= 5000000 -> taxableIncome * 0.05
            taxableIncome <= 10000000 -> taxableIncome * 0.10 - 250000
            taxableIncome <= 18000000 -> taxableIncome * 0.15 - 750000
            taxableIncome <= 32000000 -> taxableIncome * 0.20 - 1650000
            taxableIncome <= 52000000 -> taxableIncome * 0.25 - 3250000
            taxableIncome <= 80000000 -> taxableIncome * 0.30 - 5850000
            else -> taxableIncome * 0.35 - 9850000
        }
    }
}
