
package com.example.ui.screens
import com.example.data.model.TimeEntry
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.example.viewmodel.MonthlySalaryPoint

import android.content.ContentValues
import android.content.Context
import com.example.auth.UserSession
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkContainer
import com.example.ui.theme.LightGray
import com.example.ui.theme.MediumGray
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.White
import com.example.viewmodel.SalarySummary
import com.example.viewmodel.TimeSnapViewModel
import com.example.data.SalaryCalculator
import java.io.OutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayslipScreen(
    viewModel: TimeSnapViewModel
) {
    val context = LocalContext.current
    val userSession by viewModel.currentUserSession.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.currentSelectedMonth.collectAsStateWithLifecycle()
    val summary by viewModel.salarySummaryState.collectAsStateWithLifecycle()
    val config by viewModel.userConfig.collectAsStateWithLifecycle()
    val entries by viewModel.monthTimeEntries.collectAsStateWithLifecycle(emptyList())
    val salaryHistoryList by viewModel.salaryHistoryState.collectAsStateWithLifecycle()

    var customOt15DaysCountDay by remember { mutableStateOf(0.0) }
    var customOt15DaysCountNight by remember { mutableStateOf(0.0) }
    LaunchedEffect(selectedMonth) {
        customOt15DaysCountDay = 0.0
        customOt15DaysCountNight = 0.0
    }

    val fmt = DecimalFormat("#,###")
    val df = DecimalFormat("#.#")

    val monthLabel = remember(selectedMonth) {
        try {
            val parser = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val d = parser.parse(selectedMonth) ?: Date()
            val formatter = SimpleDateFormat("MMMM / yyyy", Locale("vi", "VN"))
            formatter.format(d).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            selectedMonth
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phiếu Lương Điện Tử", fontWeight = FontWeight.Bold, color = White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Month Switcher Controller
            val sdfMonth = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
            val currentMonthDate = remember(selectedMonth) { sdfMonth.parse(selectedMonth) ?: Date() }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev Month
                IconButton(onClick = {
                    val cal = Calendar.getInstance()
                    cal.time = currentMonthDate
                    cal.add(Calendar.MONTH, -1)
                    viewModel.selectMonth(sdfMonth.format(cal.time))
                }) {
                    Icon(Icons.Default.ArrowBackIosNew, "Tháng trước", tint = NeonBlue)
                }

                // Month Label
                Text(
                    text = monthLabel,
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Next Month
                IconButton(onClick = {
                    val cal = Calendar.getInstance()
                    cal.time = currentMonthDate
                    cal.add(Calendar.MONTH, 1)
                    viewModel.selectMonth(sdfMonth.format(cal.time))
                }) {
                    Icon(Icons.Default.ArrowForwardIos, "Tháng sau", tint = NeonBlue)
                }
            }

            // Monthly Income Trend Comparison Chart
            MonthlyIncomeTrendChart(
                historyList = salaryHistoryList,
                selectedMonth = selectedMonth,
                onMonthSelected = { viewModel.selectMonth(it) }
            )
            
            if (summary == null || config == null) {
                // Empty state setup
                Spacer(modifier = Modifier.height(60.dp))
                Icon(Icons.Default.ReceiptLong, "Receipt Empty", modifier = Modifier.size(72.dp), tint = MediumGray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Chưa có thông tin phiếu lương", color = LightGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Vui lòng check-in hoặc cấu hình mức lương trước.", color = MediumGray, fontSize = 12.sp, textAlign = TextAlign.Center)
            } else {
                val s = summary!!
                val c = config!!

                // Part 1: Break duration interpretation (Giờ nghỉ giải ca)
                val hourBreakConverted = c.soGioNghiGiaiLao

                // Part 2: Dates calculations
                val todayCal = Calendar.getInstance()
                val currentYear = todayCal.get(Calendar.YEAR)
                val currentMonth = todayCal.get(Calendar.MONTH) + 1
                val todayDayOfMonth = todayCal.get(Calendar.DAY_OF_MONTH)

                val parts = selectedMonth.split("-")
                val targetYear = parts.getOrNull(0)?.toIntOrNull() ?: currentYear
                val targetMonth = parts.getOrNull(1)?.toIntOrNull() ?: currentMonth

                val isCurrentSelectedMonth = (targetYear == currentYear && targetMonth == currentMonth)

                val nightShiftsCount = remember(entries) {
                    entries.count { e ->
                        try {
                            val inCal = Calendar.getInstance()
                            e.checkInTime?.let {
                                inCal.timeInMillis = it
                                val inHour = inCal.get(Calendar.HOUR_OF_DAY)
                                inHour >= 22 || inHour <= 6 || e.dayType == "NIGHT"
                            } ?: false
                        } catch (ex: Exception) { false }
                    }
                }
                
                val lastLoggedDayOfMonth = remember(entries, isCurrentSelectedMonth) {
                    if (!isCurrentSelectedMonth) 0 else {
                        entries.filter { e ->
                            e.checkInTime != null || e.isWorking || com.example.data.SalaryCalculator.isPaidLeaveType(e.dayType) || com.example.data.SalaryCalculator.isUnpaidLeaveType(e.dayType) || e.dayType == "HOLIDAY_LEAVE"
                        }.mapNotNull { e ->
                            try {
                                val parts = if (e.date.contains("/")) e.date.split("/") else e.date.split("-")
                                if (e.date.contains("/")) {
                                    parts.getOrNull(0)?.toIntOrNull()
                                } else {
                                    parts.getOrNull(2)?.toIntOrNull()
                                }
                            } catch (ex: Exception) { null }
                        }.maxOrNull() ?: 0
                    }
                }

                val hasTodayLogged = remember(entries, todayDayOfMonth, isCurrentSelectedMonth) {
                    if (!isCurrentSelectedMonth) false else {
                        entries.any { e ->
                            val day = try {
                                val parts = if (e.date.contains("/")) e.date.split("/") else e.date.split("-")
                                if (e.date.contains("/")) {
                                    parts.getOrNull(0)?.toIntOrNull()
                                } else {
                                    parts.getOrNull(2)?.toIntOrNull()
                                }
                            } catch (ex: Exception) { null }
                            day == todayDayOfMonth && (e.checkInTime != null || e.isWorking || com.example.data.SalaryCalculator.isPaidLeaveType(e.dayType) || com.example.data.SalaryCalculator.isUnpaidLeaveType(e.dayType) || e.dayType == "HOLIDAY_LEAVE")
                        }
                    }
                }

                val tinhDenNgay = if (isCurrentSelectedMonth) {
                    if (hasTodayLogged) todayDayOfMonth else (if (lastLoggedDayOfMonth > 0) lastLoggedDayOfMonth else todayDayOfMonth)
                } else {
                    val tempCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, targetYear)
                        set(Calendar.MONTH, targetMonth - 1)
                    }
                    tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                }

                // Remaining days calculation
                val maxDaysInMonth = remember(targetYear, targetMonth) {
                    val tempCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, targetYear)
                        set(Calendar.MONTH, targetMonth - 1)
                    }
                    tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                }

                val startProjectionDay = remember(hasTodayLogged, lastLoggedDayOfMonth, todayDayOfMonth, isCurrentSelectedMonth, maxDaysInMonth) {
                    if (!isCurrentSelectedMonth) 1 else if (hasTodayLogged) (todayDayOfMonth + 1).coerceAtMost(maxDaysInMonth + 1) else todayDayOfMonth.coerceAtMost(maxDaysInMonth + 1)
                }

                val totalSundaysInMonth = remember(targetYear, targetMonth, maxDaysInMonth) {
                    val cal = Calendar.getInstance()
                    var count = 0
                    for (day in 1..maxDaysInMonth) {
                        cal.set(Calendar.YEAR, targetYear)
                        cal.set(Calendar.MONTH, targetMonth - 1)
                        cal.set(Calendar.DAY_OF_MONTH, day)
                        val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
                        val isHoliday = com.example.data.SalaryCalculator.isHoliday(dateStr)
                        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && !isHoliday) {
                            count++
                        }
                    }
                    count
                }

                val defaultRemainingSundays = remember(targetYear, targetMonth, startProjectionDay, isCurrentSelectedMonth) {
                    if (!isCurrentSelectedMonth) 0 else {
                        val cal = Calendar.getInstance()
                        var count = 0
                        for (day in startProjectionDay..maxDaysInMonth) {
                            cal.set(Calendar.YEAR, targetYear)
                            cal.set(Calendar.MONTH, targetMonth - 1)
                            cal.set(Calendar.DAY_OF_MONTH, day)
                            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
                            val isHoliday = com.example.data.SalaryCalculator.isHoliday(dateStr)
                            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && !isHoliday) {
                                count++
                            }
                        }
                        count
                    }
                }
                var remainingSundaysDay by remember(defaultRemainingSundays) { mutableStateOf(defaultRemainingSundays) }
                var remainingSundaysNight by remember { mutableStateOf(0) }
                val remainingSundays = remainingSundaysDay + remainingSundaysNight

                val remainingWeekdays = remember(targetYear, targetMonth, startProjectionDay, isCurrentSelectedMonth) {
                    if (!isCurrentSelectedMonth) 0 else {
                        val cal = Calendar.getInstance()
                        var count = 0
                        for (day in startProjectionDay..maxDaysInMonth) {
                            cal.set(Calendar.YEAR, targetYear)
                            cal.set(Calendar.MONTH, targetMonth - 1)
                            cal.set(Calendar.DAY_OF_MONTH, day)
                            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
                            val isHoliday = com.example.data.SalaryCalculator.isHoliday(dateStr)
                            if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY && !isHoliday) {
                                count++
                            }
                        }
                        count
                    }
                }

                // Dynamic identification of Sunday scheduling/work history
                val hasWorkedSunday = remember(entries) {
                    entries.any { e ->
                        try {
                            // Check day of week
                            val cal = Calendar.getInstance()
                            val partsDate = if (e.date.contains("/")) e.date.split("/") else e.date.split("-")
                            if (partsDate.size >= 3) {
                                val yr = if (e.date.contains("/")) partsDate[2].toInt() else partsDate[0].toInt()
                                val mo = partsDate[1].toInt() - 1
                                val dy = if (e.date.contains("/")) partsDate[0].toInt() else partsDate[2].toInt()
                                cal.set(yr, mo, dy)
                                cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && e.checkInTime != null
                            } else false
                        } catch (ex: Exception) { false }
                    }
                }

                var includeSundayInProjection by remember { mutableStateOf(false) }
                LaunchedEffect(hasWorkedSunday) {
                    includeSundayInProjection = hasWorkedSunday
                }

                // TAB / SEGMENT CONTROL
                var selectedTab by remember { mutableStateOf(0) }

                val standardTargetDays = (if (isCurrentSelectedMonth && selectedTab == 0) s.expectedWorkDays else s.standardWorkDays).toDouble().coerceAtLeast(1.0)

                val dailySalary = remember(c.luongCoBan, standardTargetDays) {
                    c.luongCoBan / standardTargetDays
                }
                val hourlySalary = dailySalary / 8.0

                val breakHours = if (c.tinhKhauTruNghi) c.soGioNghiGiaiLao else 0.0
                val sundayHoursPerShift = (12.0 - breakHours).coerceAtLeast(0.0)

                val additionalSundaysDayHours = remainingSundaysDay * sundayHoursPerShift
                val additionalSundaysNightHours = remainingSundaysNight * sundayHoursPerShift

                val additionalSundaysDayPay = if (includeSundayInProjection) {
                    additionalSundaysDayHours * hourlySalary * c.heSoOtChuNhat
                } else {
                    0.0
                }
                val additionalSundaysNightPay = if (includeSundayInProjection) {
                    additionalSundaysNightHours * hourlySalary * c.heSoOtChuNhat
                } else {
                    0.0
                }
                val additionalSundaysNightAllowance = if (includeSundayInProjection) {
                    remainingSundaysNight * c.pcCaDem
                } else {
                    0.0
                }
                val additionalSundaysPay = additionalSundaysDayPay + additionalSundaysNightPay
                val projectedSundays = if (includeSundayInProjection) (remainingSundaysDay + remainingSundaysNight) else 0

                val unpaidDaysCount = remember(entries) {
                    entries.count { e -> com.example.data.SalaryCalculator.isUnpaidLeaveType(e.dayType) || e.dayType == "UNAUTHORIZED_LEAVE" }
                }
                val hasLoggedUnpaidOrAbsent = unpaidDaysCount > 0

                val projectedRemainingWorkdays = remainingWeekdays.toDouble()

                val soNgayCongDuKien = if (isCurrentSelectedMonth) {
                    val rawProjected = s.workingDays + projectedRemainingWorkdays
                    if (unpaidDaysCount > 0) {
                        (standardTargetDays - unpaidDaysCount).coerceAtLeast(0.0).coerceAtMost(standardTargetDays)
                    } else {
                        standardTargetDays.coerceAtLeast(rawProjected.coerceAtMost(standardTargetDays))
                    }
                } else {
                    s.workingDays.coerceAtMost(standardTargetDays)
                }
                val soNgayCongDuKienDouble = soNgayCongDuKien

                val fullEntriesForExport = remember(
                    entries, selectedTab, isCurrentSelectedMonth, remainingWeekdays,
                    remainingSundays, includeSundayInProjection, remainingSundaysDay,
                    remainingSundaysNight, customOt15DaysCountDay, customOt15DaysCountNight
                ) {
                    if (selectedTab != 1 || !isCurrentSelectedMonth) {
                        entries
                    } else {
                        val list = entries.toMutableList()
                        val cal = Calendar.getInstance()
                        val currentYear = cal.get(Calendar.YEAR)
                        val currentMonth = cal.get(Calendar.MONTH)
                        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val todayDay = cal.get(Calendar.DAY_OF_MONTH)

                        val existingDates = entries.map { it.date }.toSet()

                        var sunDayLeft = if (includeSundayInProjection) remainingSundaysDay else 0
                        var sunNightLeft = if (includeSundayInProjection) remainingSundaysNight else 0
                        var ot15DayLeft = customOt15DaysCountDay.toInt()
                        var ot15NightLeft = customOt15DaysCountNight.toInt()

                        for (day in (todayDay + 1)..daysInMonth) {
                            val dCal = Calendar.getInstance().apply { set(currentYear, currentMonth, day) }
                            val dateStr = String.format(Locale.US, "%02d/%02d/%04d", day, currentMonth + 1, currentYear)
                            if (existingDates.contains(dateStr)) continue

                            val isSun = dCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                            if (isSun) {
                                if (sunDayLeft > 0) {
                                    list.add(
                                        TimeEntry(
                                            id = 0,
                                            userId = c.maNhanVien,
                                            date = dateStr,
                                            checkInTime = Calendar.getInstance().apply { set(currentYear, currentMonth, day, 7, 30, 0) }.timeInMillis,
                                            checkOutTime = Calendar.getInstance().apply { set(currentYear, currentMonth, day, 19, 30, 0) }.timeInMillis,
                                            shiftType = "DAY",
                                            dayType = "SUNDAY",
                                            note = "Dự kiến (OT CN Ca ngày)"
                                        )
                                    )
                                    sunDayLeft--
                                } else if (sunNightLeft > 0) {
                                    list.add(
                                        TimeEntry(
                                            id = 0,
                                            userId = c.maNhanVien,
                                            date = dateStr,
                                            checkInTime = Calendar.getInstance().apply { set(currentYear, currentMonth, day, 19, 30, 0) }.timeInMillis,
                                            checkOutTime = Calendar.getInstance().apply { set(currentYear, currentMonth, day, 7, 30, 0); add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis,
                                            shiftType = "NIGHT",
                                            dayType = "SUNDAY",
                                            note = "Dự kiến (OT CN Ca đêm)"
                                        )
                                    )
                                    sunNightLeft--
                                }
                            } else {
                                val isHoliday = com.example.data.SalaryCalculator.isHoliday(dateStr)
                                if (!isHoliday) {
                                    val isOtDay = ot15DayLeft > 0
                                    val isOtNight = !isOtDay && ot15NightLeft > 0
                                    if (isOtDay) ot15DayLeft--
                                    else if (isOtNight) ot15NightLeft--
                                    
                                    val shiftT = if (isOtNight) "NIGHT" else "DAY"
                                    val noteStr = if (isOtDay || isOtNight) "Dự kiến (OT 1.5)" else "Dự kiến (Thường)"
                                    
                                    list.add(
                                        TimeEntry(
                                            id = 0,
                                            userId = c.maNhanVien,
                                            date = dateStr,
                                            checkInTime = Calendar.getInstance().apply { set(currentYear, currentMonth, day, 7, 30, 0) }.timeInMillis,
                                            checkOutTime = Calendar.getInstance().apply { set(currentYear, currentMonth, day, 17, 30, 0) }.timeInMillis,
                                            shiftType = shiftT,
                                            dayType = "NORMAL",
                                            note = noteStr
                                        )
                                    )
                                }
                            }
                        }
                        list
                    }
                }

                fun calcPr(fieldName: String, valRaw: Double): Double {
                    return com.example.data.SalaryCalculator.calculateAllowanceValue(
                        fieldName = fieldName,
                        allowanceValue = valRaw,
                        calcType = c.getCalcTypeFor(fieldName),
                        totalWorkDays = soNgayCongDuKienDouble,
                        comCaCount = soNgayCongDuKienDouble.toInt(),
                        comOtCount = 0,
                        nightShiftsCount = s.caDemCount + (if (selectedTab == 1) customOt15DaysCountNight.toInt() else 0) + (if (selectedTab == 1 && includeSundayInProjection) remainingSundaysNight else 0),
                        scheduledDaysSoFar = soNgayCongDuKienDouble.toInt(),
                        totalScheduledDaysInMonth = standardTargetDays.toInt()
                    )
                }

                val pcKyThuatShow = if (selectedTab == 1) calcPr("pcKyThuat", c.pcKyThuat) else s.pcKyThuatVal
                val pcTrachNhiemShow = if (selectedTab == 1) calcPr("pcTrachNhiem", c.pcTrachNhiem) else s.pcTrachNhiemVal
                val pcChucVuShow = if (selectedTab == 1) calcPr("pcChucVu", c.pcChucVu) else s.pcChucVuVal
                val pcHieuSuatShow = if (selectedTab == 1) calcPr("pcHieuSuat", c.pcHieuSuat) else s.pcHieuSuatVal
                val pcSanPhamShow = if (selectedTab == 1) calcPr("pcSanPham", c.pcSanPham) else s.pcSanPhamVal

                val pcComCaShow = if (selectedTab == 1) {
                    if (isCurrentSelectedMonth) {
                        (soNgayCongDuKienDouble + projectedSundays) * c.pcComCa
                    } else {
                        s.pcComCaVal
                    }
                } else {
                    s.pcComCaVal
                }

                val projectedSundaysOtMeals = if (includeSundayInProjection && (sundayHoursPerShift - 8.0) >= 1.0) projectedSundays else 0
                val otMealAllowance = if (selectedTab == 1) ((customOt15DaysCountDay + customOt15DaysCountNight) + projectedSundaysOtMeals) * c.pcComOt else 0.0
                val pcComOtShow = if (selectedTab == 1) s.pcComOtVal + otMealAllowance else s.pcComOtVal

                val pcNhaOShow = if (selectedTab == 1) calcPr("pcNhaO", c.pcNhaO) else s.pcNhaOVal
                val pcDocHaiShow = if (selectedTab == 1) calcPr("pcDocHai", c.pcDocHai) else s.pcDocHaiVal
                val pcDtDoanhThuShow = if (selectedTab == 1) calcPr("pcDtDoanhThu", c.pcDtDoanhThu) else s.pcDtDoanhThuVal
                val pcXangXeShow = if (selectedTab == 1) calcPr("pcXangXe", c.pcXangXe) else s.pcXangXeVal
                val pcKhacShow = if (selectedTab == 1) calcPr("pcCaDem", c.pcCaDem) else s.pcCaDemVal
                val pcKhac1Show = if (selectedTab == 1) calcPr("pcKhac1", c.pcKhac1) else s.pcKhac1Val
                val pcThamNienShow = if (selectedTab == 1) calcPr("pcThamNien", c.pcThamNien) else s.pcThamNienVal

                val pcChuyenCanShow = if (selectedTab == 1) {
                    if (hasLoggedUnpaidOrAbsent) 0.0 else calcPr("tienChuyenCanGoc", c.tienChuyenCanGoc)
                } else {
                    s.phuCapChuyenCan
                }

                val luongDuKienBaseSalary = if (soNgayCongDuKienDouble >= standardTargetDays) c.luongCoBan else Math.round((c.luongCoBan / standardTargetDays) * soNgayCongDuKienDouble).toDouble()

                val currentProratedAllowancesSum = s.pcKyThuatVal + s.pcTrachNhiemVal + s.pcChucVuVal + s.pcHieuSuatVal +
                        s.pcSanPhamVal + s.pcComCaVal + s.pcComOtVal + s.pcNhaOVal + s.pcDocHaiVal + 
                        s.pcDtDoanhThuVal + s.pcXangXeVal + s.pcKhac1Val + s.pcThamNienVal + s.phuCapChuyenCan +
                        s.pcCaDemVal

                val customNightAllowance = if (selectedTab == 1) {
                    customOt15DaysCountNight * c.pcCaDem
                } else 0.0

                val fullProjectedAllowancesSum = pcKyThuatShow + pcTrachNhiemShow + pcChucVuShow + pcHieuSuatShow +
                        pcSanPhamShow + pcComCaShow + pcComOtShow + pcNhaOShow + pcDocHaiShow + 
                        pcDtDoanhThuShow + pcXangXeShow + pcKhac1Show + pcThamNienShow + pcChuyenCanShow +
                        (s.pcCaDemVal + customNightAllowance + additionalSundaysNightAllowance)

                val allowanceAdjustment = fullProjectedAllowancesSum - currentProratedAllowancesSum

                val baseSalaryAdjustment = if (isCurrentSelectedMonth) (luongDuKienBaseSalary - s.baseBasicSalary) else 0.0
                val totalOtDayHours = customOt15DaysCountDay * (4.0 - breakHours).coerceAtLeast(0.0)
                val totalOtNightHours = customOt15DaysCountNight * (4.0 - breakHours).coerceAtLeast(0.0)
                val customOt15PayDayVal = totalOtDayHours * hourlySalary * c.heSoOtNgayThuong
                val customOt15PayNightVal = totalOtNightHours * hourlySalary * c.heSoOtDem
                val customOt15Pay = customOt15PayDayVal + customOt15PayNightVal
                val luongDuKienVal = s.luongThucNhan + baseSalaryAdjustment + additionalSundaysPay + additionalSundaysNightAllowance + allowanceAdjustment + customOt15Pay + customNightAllowance

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(Color(0xFF161618), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                if (dragAmount < -30f) {
                                    selectedTab = 1
                                } else if (dragAmount > 30f) {
                                    selectedTab = 0
                                }
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 0) NeonBlue else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 12.dp)
                            .testTag("actual_payslip_tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "THỰC TẾ ĐẾN NAY",
                            color = if (selectedTab == 0) White else LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) NeonBlue else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 12.dp)
                            .testTag("projected_payslip_tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCurrentSelectedMonth) "DỰ KIẾN CUỐI THÁNG" else "LƯƠNG FULL THÁNG",
                            color = if (selectedTab == 1) White else LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // ELEGANT THERMAL PAPER DARK INVOICE CARD STYLE
                AnimatedContent(
                    targetState = selectedMonth,
                    transitionSpec = {
                        (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300)))
                            .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300)))
                    },
                    label = "PayslipCardTransition"
                ) { targetMonth ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkContainer),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Header ticket logo info
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NeonBlue.copy(alpha = 0.15f))
                                        .border(1.5.dp, NeonBlue, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalAtm,
                                        contentDescription = null,
                                        tint = NeonBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "TIMESNAP PRO",
                                    color = NeonBlue,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (selectedTab == 1) "PHIẾU LƯƠNG DỰ KIẾN CUỐI THÁNG" else "PHIẾU LƯƠNG ĐIỆN TỬ CHI TIẾT",
                                color = if (selectedTab == 1) AccentOrange else White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Kỳ lương: $monthLabel",
                                color = LightGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            if (selectedTab == 1) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "🔮 ĐÃ BÙ TOÀN BỘ CÁC NGÀY CÒN LẠI",
                                    color = NeonBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (s.isCurrentMonth) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "⚠️ TẠM TÍNH ĐẾN NGÀY $tinhDenNgay",
                                    color = AccentOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Separator dashes
                            HorizontalDivider(
                                color = Color(0xFF2C2C2C),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }

                        // Employee Profile rows
                        val employeeName = if (!c.hoVaTen.isNullOrBlank()) c.hoVaTen else (userSession?.displayName ?: "N/A")
                        val employeeCode = if (!c.maNhanVien.isNullOrBlank()) c.maNhanVien else (userSession?.uid?.take(10) ?: "N/A")

                        PayslipProfileRow(label = "Nhân viên:", value = employeeName)
                        PayslipProfileRow(label = "Mã nhân viên (UID):", value = employeeCode, isMono = true)
                        PayslipProfileRow(label = "Mức lương cơ bản:", value = "${fmt.format(c.luongCoBan)}đ")
                        
                        val totalActualWorkDays = s.actualPresenceDays
                        val lcbActualWorkDays = s.workingDays.coerceAtMost(s.standardWorkDays.toDouble())
                        val totalProjectedWorkDays = soNgayCongDuKienDouble + (if (includeSundayInProjection) (remainingSundaysDay + remainingSundaysNight).toDouble() else 0.0)
                        val lcbProjectedWorkDays = soNgayCongDuKienDouble.coerceAtMost(standardTargetDays)

                        // 1. Tiến độ tháng (Số ngày công chuẩn của tháng)
                        val displayStandardDays = if (selectedTab == 1) soNgayCongDuKienDouble else s.actualStandardWorkingDays
                        PayslipProfileRow(
                            label = "Tiến độ tháng (Công chuẩn):",
                            value = "${df.format(displayStandardDays)} / ${s.standardWorkDays} ngày"
                        )

                        // 1b. Ngày nghỉ phép / nghỉ thường (Dynamic)
                        val annualLeavesCount = fullEntriesForExport.count { com.example.data.SalaryCalculator.isAnnualLeaveType(it.dayType) }
                        val holidayLeavesCount = fullEntriesForExport.count { com.example.data.SalaryCalculator.isHolidayLeaveType(it.dayType) }
                        val unpaidLeavesCount = fullEntriesForExport.count { com.example.data.SalaryCalculator.isUnpaidLeaveType(it.dayType) }
                        val leaveParts = mutableListOf<String>()
                        if (annualLeavesCount > 0) leaveParts.add("Phép năm: ${annualLeavesCount} ngày")
                        if (holidayLeavesCount > 0) leaveParts.add("Nghỉ lễ: ${holidayLeavesCount} ngày")
                        if (unpaidLeavesCount > 0) leaveParts.add("Không lương: ${unpaidLeavesCount} ngày")
                        val leaveDaysVal = if (leaveParts.isEmpty()) "0 ngày" else leaveParts.joinToString(", ")
                        PayslipProfileRow(
                            label = "Ngày nghỉ:",
                            value = leaveDaysVal
                        )

                        // 2. Ngày công làm việc (Thực tế hoặc Dự kiến)
                        if (selectedTab == 1) {
                            PayslipProfileRow(
                                label = "Ngày công (Dự kiến):", 
                                value = "${df.format(totalProjectedWorkDays)} / ${standardTargetDays.toInt()} ngày"
                            )
                            if (isCurrentSelectedMonth) {
                                val sundayDetails = buildString {
                                    if (includeSundayInProjection && remainingSundays > 0) {
                                        if (remainingSundaysDay > 0) append(" + $remainingSundaysDay CN ngày")
                                        if (remainingSundaysNight > 0) append(" + $remainingSundaysNight CN đêm")
                                    }
                                }
                                PayslipProfileRow(
                                    label = "Trong đó làm thêm:", 
                                    value = "$remainingWeekdays ngày thường$sundayDetails"
                                )
                            }
                        } else {
                            PayslipProfileRow(
                                label = "Ngày công (Thực tế):", 
                                value = "${df.format(totalActualWorkDays)} / ${s.standardWorkDays} ngày"
                            )
                        }

                        // Interactive Projection Switch inside Receipt Paper
                        if (selectedTab == 1 && isCurrentSelectedMonth) {
                            HorizontalDivider(
                                color = Color(0xFF2C2C2C),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { includeSundayInProjection = !includeSundayInProjection },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Lịch làm việc có Chủ Nhật", color = LightGray, fontSize = 12.sp)
                                    Switch(
                                        checked = includeSundayInProjection,
                                        onCheckedChange = { includeSundayInProjection = it },
                                        modifier = Modifier.scale(0.85f).testTag("sunday_projection_switch")
                                    )
                                }

                                if (includeSundayInProjection) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Phân chia ca làm Chủ Nhật:", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Ca Ngày
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("☀️ Ca ngày (CN):", color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("(x${df.format(c.heSoOtChuNhat)})", color = AccentGreen, fontSize = 11.sp)
                                            }

                                            var sundayDayInputText by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(remainingSundaysDay.toString())) }
                                            LaunchedEffect(remainingSundaysDay) {
                                                if (sundayDayInputText.text != remainingSundaysDay.toString()) {
                                                    sundayDayInputText = sundayDayInputText.copy(
                                                        text = remainingSundaysDay.toString(),
                                                        selection = androidx.compose.ui.text.TextRange(remainingSundaysDay.toString().length)
                                                    )
                                                }
                                            }

                                            OutlinedTextField(
                                                value = sundayDayInputText,
                                                onValueChange = { newValue ->
                                                    val cleanText = newValue.text.filter { it.isDigit() }
                                                    if (cleanText.isEmpty()) {
                                                        sundayDayInputText = newValue.copy(text = "")
                                                        remainingSundaysDay = 0
                                                    } else {
                                                        cleanText.toIntOrNull()?.let { parsed ->
                                                            val maxDayAllowed = (defaultRemainingSundays - remainingSundaysNight).coerceAtLeast(0)
                                                            if (parsed <= maxDayAllowed) {
                                                                sundayDayInputText = newValue.copy(text = cleanText)
                                                                remainingSundaysDay = parsed
                                                            } else {
                                                                val cappedStr = maxDayAllowed.toString()
                                                                sundayDayInputText = androidx.compose.ui.text.input.TextFieldValue(
                                                                    text = cappedStr,
                                                                    selection = androidx.compose.ui.text.TextRange(cappedStr.length)
                                                                )
                                                                remainingSundaysDay = maxDayAllowed
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.width(68.dp).height(46.dp).testTag("sunday_day_count_input"),
                                                textStyle = androidx.compose.ui.text.TextStyle(
                                                    textAlign = TextAlign.Center, 
                                                    color = White, 
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                ),
                                                singleLine = true,
                                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                                ),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = AccentGreen,
                                                    unfocusedBorderColor = Color(0xFF3C3C3C),
                                                    focusedContainerColor = Color(0xFF252525),
                                                    unfocusedContainerColor = Color(0xFF181818),
                                                    focusedTextColor = White,
                                                    unfocusedTextColor = White,
                                                    cursorColor = AccentGreen
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Ca Đêm
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("🌙 Ca đêm (CN):", color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("(x${df.format(c.heSoOtChuNhat)} + đêm)", color = NeonBlue, fontSize = 11.sp)
                                            }

                                            var sundayNightInputText by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(remainingSundaysNight.toString())) }
                                            LaunchedEffect(remainingSundaysNight) {
                                                if (sundayNightInputText.text != remainingSundaysNight.toString()) {
                                                    sundayNightInputText = sundayNightInputText.copy(
                                                        text = remainingSundaysNight.toString(),
                                                        selection = androidx.compose.ui.text.TextRange(remainingSundaysNight.toString().length)
                                                    )
                                                }
                                            }

                                            OutlinedTextField(
                                                value = sundayNightInputText,
                                                onValueChange = { newValue ->
                                                    val cleanText = newValue.text.filter { it.isDigit() }
                                                    if (cleanText.isEmpty()) {
                                                        sundayNightInputText = newValue.copy(text = "")
                                                        remainingSundaysNight = 0
                                                    } else {
                                                        cleanText.toIntOrNull()?.let { parsed ->
                                                            val maxNightAllowed = (defaultRemainingSundays - remainingSundaysDay).coerceAtLeast(0)
                                                            if (parsed <= maxNightAllowed) {
                                                                sundayNightInputText = newValue.copy(text = cleanText)
                                                                remainingSundaysNight = parsed
                                                            } else {
                                                                val cappedStr = maxNightAllowed.toString()
                                                                sundayNightInputText = androidx.compose.ui.text.input.TextFieldValue(
                                                                    text = cappedStr,
                                                                    selection = androidx.compose.ui.text.TextRange(cappedStr.length)
                                                                )
                                                                remainingSundaysNight = maxNightAllowed
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.width(68.dp).height(46.dp).testTag("sunday_night_count_input"),
                                                textStyle = androidx.compose.ui.text.TextStyle(
                                                    textAlign = TextAlign.Center, 
                                                    color = White, 
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                ),
                                                singleLine = true,
                                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                                ),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = NeonBlue,
                                                    unfocusedBorderColor = Color(0xFF3C3C3C),
                                                    focusedContainerColor = Color(0xFF252525),
                                                    unfocusedContainerColor = Color(0xFF181818),
                                                    focusedTextColor = White,
                                                    unfocusedTextColor = White,
                                                    cursorColor = NeonBlue
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedTab == 1) {
                            HorizontalDivider(
                                color = Color(0xFF2C2C2C),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Tăng ca OT 1.5:", color = LightGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    if (customOt15DaysCountDay > 0 || customOt15DaysCountNight > 0) {
                                        Text(
                                            text = "+${fmt.format(customOt15Pay)}đ",
                                            color = AccentGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("☀️ OT 1.5 ca ngày:", color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("(x${df.format(c.heSoOtNgayThuong)})", color = NeonBlue, fontSize = 11.sp)
                                    }

                                    var otDayInputText by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(customOt15DaysCountDay.toInt().toString())) }
                                    LaunchedEffect(customOt15DaysCountDay) {
                                        if (otDayInputText.text != customOt15DaysCountDay.toInt().toString()) {
                                            otDayInputText = otDayInputText.copy(
                                                text = customOt15DaysCountDay.toInt().toString(),
                                                selection = androidx.compose.ui.text.TextRange(customOt15DaysCountDay.toInt().toString().length)
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = otDayInputText,
                                        onValueChange = { newValue ->
                                            val cleanText = newValue.text.filter { it.isDigit() }
                                            if (cleanText.isEmpty()) {
                                                otDayInputText = newValue.copy(text = "")
                                                customOt15DaysCountDay = 0.0
                                            } else {
                                                cleanText.toIntOrNull()?.let { parsed ->
                                                    val maxAllowed = (remainingWeekdays - customOt15DaysCountNight.toInt()).coerceAtLeast(0)
                                                    if (parsed <= maxAllowed) {
                                                        otDayInputText = newValue.copy(text = cleanText)
                                                        customOt15DaysCountDay = parsed.toDouble()
                                                    } else {
                                                        val cappedStr = maxAllowed.toString()
                                                        otDayInputText = androidx.compose.ui.text.input.TextFieldValue(
                                                            text = cappedStr,
                                                            selection = androidx.compose.ui.text.TextRange(cappedStr.length)
                                                        )
                                                        customOt15DaysCountDay = maxAllowed.toDouble()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.width(68.dp).height(46.dp).testTag("ot_15_day_count_input"),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            textAlign = TextAlign.Center, 
                                            color = White, 
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonBlue,
                                            unfocusedBorderColor = Color(0xFF3C3C3C),
                                            focusedContainerColor = Color(0xFF252525),
                                            unfocusedContainerColor = Color(0xFF181818),
                                            focusedTextColor = White,
                                            unfocusedTextColor = White,
                                            cursorColor = NeonBlue
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🌙 OT 1.5 ca đêm:", color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("(x${df.format(c.heSoOtDem)})", color = NeonBlue, fontSize = 11.sp)
                                    }

                                    var otNightInputText by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(customOt15DaysCountNight.toInt().toString())) }
                                    LaunchedEffect(customOt15DaysCountNight) {
                                        if (otNightInputText.text != customOt15DaysCountNight.toInt().toString()) {
                                            otNightInputText = otNightInputText.copy(
                                                text = customOt15DaysCountNight.toInt().toString(),
                                                selection = androidx.compose.ui.text.TextRange(customOt15DaysCountNight.toInt().toString().length)
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = otNightInputText,
                                        onValueChange = { newValue ->
                                            val cleanText = newValue.text.filter { it.isDigit() }
                                            if (cleanText.isEmpty()) {
                                                otNightInputText = newValue.copy(text = "")
                                                customOt15DaysCountNight = 0.0
                                            } else {
                                                cleanText.toIntOrNull()?.let { parsed ->
                                                    val maxAllowed = (remainingWeekdays - customOt15DaysCountDay.toInt()).coerceAtLeast(0)
                                                    if (parsed <= maxAllowed) {
                                                        otNightInputText = newValue.copy(text = cleanText)
                                                        customOt15DaysCountNight = parsed.toDouble()
                                                    } else {
                                                        val cappedStr = maxAllowed.toString()
                                                        otNightInputText = androidx.compose.ui.text.input.TextFieldValue(
                                                            text = cappedStr,
                                                            selection = androidx.compose.ui.text.TextRange(cappedStr.length)
                                                        )
                                                        customOt15DaysCountNight = maxAllowed.toDouble()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.width(68.dp).height(46.dp).testTag("ot_15_night_count_input"),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            textAlign = TextAlign.Center, 
                                            color = White, 
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonBlue,
                                            unfocusedBorderColor = Color(0xFF3C3C3C),
                                            focusedContainerColor = Color(0xFF252525),
                                            unfocusedContainerColor = Color(0xFF181818),
                                            focusedTextColor = White,
                                            unfocusedTextColor = White,
                                            cursorColor = NeonBlue
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = Color(0xFF2C2C2C),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        // ADDITIONS Header
                        Text(
                            text = if (selectedTab == 1) "KHOẢN CỘNG LƯƠNG DỰ KIẾN (+)" else "KHOẢN CỘNG LƯƠNG (+)",
                            color = AccentGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // 1. Lương cơ bản
                        if (selectedTab == 1) {
                            PayslipMoneyRow(label = "LCB thực nhận (${df.format(lcbProjectedWorkDays)} / ${standardTargetDays.toInt()})", value = luongDuKienBaseSalary, isAddition = true)
                        } else {
                            PayslipMoneyRow(label = "LCB thực nhận (${df.format(lcbActualWorkDays)} / ${s.standardWorkDays})", value = s.baseBasicSalary, isAddition = true)
                        }
                        
                        // 2. Chuyên cần
                        if (pcChuyenCanShow > 0.0) {
                            PayslipMoneyRow(label = "Chuyên cần", value = pcChuyenCanShow, isAddition = true)
                        }

                        // 3. Trách nhiệm
                        if (c.pcTrachNhiem > 0.0) {
                            PayslipMoneyRow(label = "Trách nhiệm", value = pcTrachNhiemShow, isAddition = true)
                        }

                        // 4. Kỹ thuật
                        if (c.pcKyThuat > 0.0) {
                            PayslipMoneyRow(label = "Kỹ thuật", value = pcKyThuatShow, isAddition = true)
                        }

                        // 5. Hiệu suất
                        if (c.pcHieuSuat > 0.0) {
                            PayslipMoneyRow(label = "Hiệu suất", value = pcHieuSuatShow, isAddition = true)
                        }

                        // 6. Sản phẩm
                        if (c.pcSanPham > 0.0) {
                            PayslipMoneyRow(label = "Sản phẩm", value = pcSanPhamShow, isAddition = true)
                        }

                        // 7. Chức vụ
                        if (c.pcChucVu > 0.0) {
                            PayslipMoneyRow(label = "Chức vụ", value = pcChucVuShow, isAddition = true)
                        }

                        // 8. Độc hại
                        if (c.pcDocHai > 0.0) {
                            PayslipMoneyRow(label = "Độc hại", value = pcDocHaiShow, isAddition = true)
                        }

                        // 9. Doanh thu
                        if (c.pcDtDoanhThu > 0.0) {
                            PayslipMoneyRow(label = "Doanh thu", value = pcDtDoanhThuShow, isAddition = true)
                        }

                        // 10. Thâm niên
                        if (c.pcThamNien > 0.0) {
                            PayslipMoneyRow(label = "Thâm niên", value = pcThamNienShow, isAddition = true)
                        }

                        // 11. Cơm/ca
                        if (pcComCaShow > 0.0) {
                            PayslipMoneyRow(label = "Cơm/ ca", value = pcComCaShow, isAddition = true)
                        }

                        // 12. Cơm OT
                        if (pcComOtShow > 0.0) {
                            PayslipMoneyRow(label = "Cơm OT", value = pcComOtShow, isAddition = true)
                        }

                        // 13. OT Ngày (Merged)
                        val projectedOtDayHours = if (selectedTab == 1) customOt15DaysCountDay * (4.0 - breakHours).coerceAtLeast(0.0) else 0.0
                        val totalOtDayHours = s.otDayHours + projectedOtDayHours
                        val totalOtDayPay = s.tienOtNgay + (if (selectedTab == 1) customOt15PayDayVal else 0.0)
                        
                        if (totalOtDayHours > 0.0) {
                            PayslipMoneyRow(label = "OT ngày ${df.format(c.heSoOtNgayThuong)} (${df.format(totalOtDayHours)}h)", value = totalOtDayPay, isAddition = true, isAccent = true)
                        }

                        // 14. OT Chủ nhật (Đồng bộ theo cách tính thực tế & dự kiến)
                        val totalSundayDayHours = if (selectedTab == 1 && includeSundayInProjection) s.chuNhatDayHours + additionalSundaysDayHours else s.chuNhatDayHours
                        val totalSundayDayPay = if (selectedTab == 1 && includeSundayInProjection) s.tienChuNhatNgay + additionalSundaysDayPay else s.tienChuNhatNgay

                        val totalSundayNightHours = if (selectedTab == 1 && includeSundayInProjection) s.chuNhatNightHours + additionalSundaysNightHours else s.chuNhatNightHours
                        val totalSundayNightPay = if (selectedTab == 1 && includeSundayInProjection) s.tienChuNhatDem + additionalSundaysNightPay else s.tienChuNhatDem

                        if (totalSundayDayHours > 0.0) {
                            PayslipMoneyRow(label = "OT CN - Ca ngày ${df.format(c.heSoOtChuNhat)} (${df.format(totalSundayDayHours)}h)", value = totalSundayDayPay, isAddition = true, isAccent = true)
                        }
                        if (totalSundayNightHours > 0.0) {
                            PayslipMoneyRow(label = "OT CN - Ca đêm ${df.format(c.heSoOtChuNhat)} (${df.format(totalSundayNightHours)}h)", value = totalSundayNightPay, isAddition = true, isAccent = true)
                        }
                        if (totalSundayDayHours == 0.0 && totalSundayNightHours == 0.0 && (s.tienChuNhat + (if (selectedTab == 1 && includeSundayInProjection) additionalSundaysPay else 0.0)) > 0.0) {
                            val totalSunHours = s.chuNhatHours + (if (selectedTab == 1 && includeSundayInProjection) (remainingSundays * sundayHoursPerShift) else 0.0)
                            val totalSunPay = s.tienChuNhat + (if (selectedTab == 1 && includeSundayInProjection) additionalSundaysPay else 0.0)
                            PayslipMoneyRow(label = "OT chủ nhật ${df.format(c.heSoOtChuNhat)} (${df.format(totalSunHours)}h)", value = totalSunPay, isAddition = true, isAccent = true)
                        }

                        // 15. OT Lễ
                        if (s.tienOtLe > 0.0) {
                            PayslipMoneyRow(label = "OT lễ ${df.format(c.heSoOtNgayLe)} (${df.format(s.otLeHours)}h)", value = s.tienOtLe, isAddition = true, isAccent = true)
                        }

                        // 15.1 OT đêm (Merged)
                        val projectedOtNightHours = if (selectedTab == 1) customOt15DaysCountNight * (4.0 - breakHours).coerceAtLeast(0.0) else 0.0
                        val totalOtNightHours = s.otNightHours + projectedOtNightHours
                        val totalOtNightPay = s.tienOtDem + (if (selectedTab == 1) customOt15PayNightVal else 0.0)

                        if (totalOtNightHours > 0.0) {
                            PayslipMoneyRow(label = "OT đêm ${df.format(c.heSoOtDem)} (${df.format(totalOtNightHours)}h)", value = totalOtNightPay, isAddition = true, isAccent = true)
                        }

                        // 16. Phụ cấp đêm
                        val finalPcCaDemCount = s.caDemCount + (if (selectedTab == 1) customOt15DaysCountNight.toInt() else 0) + (if (selectedTab == 1 && includeSundayInProjection) remainingSundaysNight else 0)
                        val finalPcCaDem = if (selectedTab == 1) (s.pcCaDemVal + customNightAllowance + additionalSundaysNightAllowance) else s.pcCaDemVal
                        if (finalPcCaDem > 0.0) {
                            PayslipMoneyRow(label = "Phụ cấp ca đêm ($finalPcCaDemCount)", value = finalPcCaDem, isAddition = true)
                        }

                        // 17. Xăng xe
                        if (c.pcXangXe > 0.0) {
                            PayslipMoneyRow(label = "Xăng xe", value = pcXangXeShow, isAddition = true)
                        }

                        // 18. Nhà ở
                        if (c.pcNhaO > 0.0) {
                            PayslipMoneyRow(label = "Nhà ở", value = pcNhaOShow, isAddition = true)
                        }

                        // 19. Phụ cấp khác
                        if (c.pcKhac1 > 0.0) {
                            PayslipMoneyRow(label = "Phụ cấp khác", value = pcKhac1Show, isAddition = true)
                        }


                        Spacer(modifier = Modifier.height(16.dp))

                        // DEDUCTIONS Header
                        Text(
                            text = "KHOẢN TRỪ LƯƠNG (-)",
                            color = AccentRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (s.tienBh > 0.0) {
                            PayslipMoneyRow(label = "BHXH/BHYT Khấu trừ (10.5%)", value = s.tienBh, isAddition = false)
                        }
                        if (s.doanPhi > 0.0) {
                            PayslipMoneyRow(label = "Phí Công Đoàn Bắt Buộc", value = s.doanPhi, isAddition = false)
                        }
                        
                        // Missed days deduction - only visible on actual payslip
                        if (selectedTab == 0 && s.tienKhauTruNghi > 0.0) {
                            val missedCount = ((if (s.isCurrentMonth) s.expectedWorkDays else s.standardWorkDays).toDouble() - s.workingDays).coerceAtLeast(0.0)
                            PayslipMoneyRow(
                                label = "Khấu trừ vắng làm (${df.format(missedCount)} ngày)",
                                value = s.tienKhauTruNghi,
                                isAddition = false
                            )
                        }

                        HorizontalDivider(
                            color = Color(0xFF2C2C2C),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        // FINAL NET SALARY
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedTab == 1) "DỰ KIẾN THỰC NHẬN:" else "THỰC NHẬN:",
                                color = White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${fmt.format(if (selectedTab == 1) luongDuKienVal else s.luongThucNhan)}đ",
                                color = AccentGreen,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "* ĐÃ ĐƯỢC PHÊ DUYỆT BỞI HỆ THỐNG TIMESNAP PRO *",
                                color = MediumGray,
                                fontSize = 8.sp,
                                letterSpacing = 1.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "SÁNG LẬP & PHÁT TRIỂN BỞI TRUONGVANKHOA",
                                color = NeonBlue,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                } // End of AnimatedContent

                // JSON structure removed per user request

                // EXPORT HIGH-QUALITY PNG PORTABLE RECEIPT ACTION BUTTON
                Button(
                    onClick = {
                        val isSaved = savePayslipAsPngImage(
                            context = context,
                            entries = fullEntriesForExport,
                            summary = s,
                            config = c,
                            userSession = userSession,
                            monthLabel = monthLabel,
                            selectedMonth = selectedMonth,
                            selectedTab = selectedTab,
                            includeSundayInProjection = includeSundayInProjection,
                            remainingWeekdays = remainingWeekdays,
                            remainingSundays = remainingSundays,
                            remainingSundaysDay = remainingSundaysDay,
                            remainingSundaysNight = remainingSundaysNight,
                            dailySalary = dailySalary,
                            luongDuKienVal = luongDuKienVal,
                            soNgayCongDuKien = soNgayCongDuKien,
                            customOt15DaysCountDay = customOt15DaysCountDay,
                            customOt15DaysCountNight = customOt15DaysCountNight,
                            customOt15PayDay = customOt15PayDayVal,
                            customOt15PayNight = customOt15PayNightVal,
                            customNightAllowance = customNightAllowance,
                            hasLoggedUnpaidOrAbsent = hasLoggedUnpaidOrAbsent,
                            breakHours = breakHours
                        )
                        if (isSaved) {
                            Toast.makeText(context, "Đã lưu phiếu lương thành công vào Gallery ứng dụng của điện thoại!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Lỗi khi lưu ảnh phiếu lương! Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("export_payslip_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Export")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "XUẤT PHIẾU LƯƠNG (ẢNH PNG)",
                        color = White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // EXPORT DETAILED PDF AND SHARE (ZALO, GMAIL, MESSENGER, ETC)
                Button(
                    onClick = {
                        com.example.util.ExportUtils.sharePayslipAndAttendanceAsPdf(
                            context = context,
                            entries = fullEntriesForExport,
                            summary = s,
                            config = c,
                            userSession = userSession,
                            monthLabel = monthLabel,
                            selectedMonth = selectedMonth,
                            selectedTab = selectedTab,
                            includeSundayInProjection = includeSundayInProjection,
                            remainingWeekdays = remainingWeekdays,
                            remainingSundays = remainingSundays,
                            remainingSundaysDay = remainingSundaysDay,
                            remainingSundaysNight = remainingSundaysNight,
                            dailySalary = dailySalary,
                            luongDuKienVal = luongDuKienVal,
                            soNgayCongDuKien = soNgayCongDuKien,
                            customOt15DaysCountDay = customOt15DaysCountDay,
                            customOt15DaysCountNight = customOt15DaysCountNight,
                            customOt15PayDay = customOt15PayDayVal,
                            customOt15PayNight = customOt15PayNightVal,
                            customNightAllowance = customNightAllowance,
                            hasLoggedUnpaidOrAbsent = hasLoggedUnpaidOrAbsent,
                            breakHours = breakHours
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("export_pdf_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share PDF")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "XUẤT PDF",
                        color = White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun PayslipProfileRow(label: String, value: String, isMono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label, 
            color = MediumGray, 
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            color = White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun PayslipMoneyRow(
    label: String,
    value: Double,
    isAddition: Boolean,
    isAccent: Boolean = false
) {
    val fmt = DecimalFormat("#,###")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isAccent) NeonBlue else LightGray,
            fontSize = 13.sp,
            fontWeight = if (isAccent) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isAddition) "+${fmt.format(value)}đ" else "-${fmt.format(value)}đ",
            color = if (isAddition) AccentGreen else AccentRed,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false
        )
    }
}

fun com.example.viewmodel.SalarySummary.toExportSummary(): com.example.util.SalarySummary {
    return com.example.util.SalarySummary(
        workingDays = this.workingDays,
        standardHours = this.standardHours,
        otDayHours = this.otDayHours,
        otNightHours = this.otNightHours,
        tienOtNgay = this.tienOtNgay,
        tienOtDem = this.tienOtDem,
        tongTienCom = this.tongTienCom,
        phuCap = this.phuCap,
        phuCapXangXe = this.phuCapXangXe,
        phuCapDienThoai = this.phuCapDienThoai,
        phuCapNhaO = this.phuCapNhaO,
        phuCapChuyenCan = this.phuCapChuyenCan,
        thuong = this.thuong,
        tienBh = this.tienBh,
        doanPhi = this.doanPhi,
        tienKhauTruNghi = this.tienKhauTruNghi,
        luongThucNhan = this.luongThucNhan,
        baseBasicSalary = this.baseBasicSalary,
        expectedWorkDays = this.expectedWorkDays,
        standardWorkDays = this.standardWorkDays,
        isCurrentMonth = this.isCurrentMonth,
        pcKyThuatVal = this.pcKyThuatVal,
        pcTrachNhiemVal = this.pcTrachNhiemVal,
        pcChucVuVal = this.pcChucVuVal,
        pcHieuSuatVal = this.pcHieuSuatVal,
        pcSanPhamVal = this.pcSanPhamVal,
        pcComCaVal = this.pcComCaVal,
        pcComOtVal = this.pcComOtVal,
        pcNhaOVal = this.pcNhaOVal,
        pcDocHaiVal = this.pcDocHaiVal,
        pcDtDoanhThuVal = this.pcDtDoanhThuVal,
        pcXangXeVal = this.pcXangXeVal,
        pcThamNienVal = this.pcThamNienVal,
        pcKhac1Val = this.pcKhac1Val,
        pcKhacVal = this.pcKhacVal,
        pcCaDemVal = this.pcCaDemVal,
        caDemCount = this.caDemCount,
        tienChuNhat = this.tienChuNhat,
        tienChuNhatNgay = this.tienChuNhatNgay,
        tienChuNhatDem = this.tienChuNhatDem,
        chuNhatHours = this.chuNhatHours,
        chuNhatDayHours = this.chuNhatDayHours,
        chuNhatNightHours = this.chuNhatNightHours,
        otLeHours = this.otLeHours,
        tienOtLe = this.tienOtLe,
        actualPresenceDays = this.actualPresenceDays,
        actualStandardWorkingDays = this.actualStandardWorkingDays
    )
}

// -------------------------------------------------------------
// HIGH QUALITY PRISTINE VECTOR PNG BITMAP GENERATION ENGINE
// -------------------------------------------------------------
fun savePayslipAsPngImage(
    context: Context,
    entries: List<com.example.data.model.TimeEntry>,
    summary: SalarySummary,
    config: com.example.data.model.UserConfig,
    userSession: UserSession?,
    monthLabel: String,
    selectedMonth: String,
    selectedTab: Int = 0,
    includeSundayInProjection: Boolean = false,
    remainingWeekdays: Int = 0,
    remainingSundays: Int = 0,
    remainingSundaysDay: Int = 0,
    remainingSundaysNight: Int = 0,
    dailySalary: Double = 0.0,
    luongDuKienVal: Double = 0.0,
    soNgayCongDuKien: Double = 0.0,
    customOt15DaysCountDay: Double = 0.0,
    customOt15DaysCountNight: Double = 0.0,
    customOt15PayDay: Double = 0.0,
    customOt15PayNight: Double = 0.0,
    customNightAllowance: Double = 0.0,
    hasLoggedUnpaidOrAbsent: Boolean = false,
    breakHours: Double = 0.0
): Boolean {
    return com.example.util.ExportUtils.savePayslipAsPngImage(
        context = context,
        entries = entries,
        summary = summary.toExportSummary(),
        config = config,
        userSession = userSession,
        monthLabel = monthLabel,
        selectedMonth = selectedMonth,
        selectedTab = selectedTab,
        includeSundayInProjection = includeSundayInProjection,
        remainingWeekdays = remainingWeekdays,
        remainingSundays = remainingSundays,
        remainingSundaysDay = remainingSundaysDay,
        remainingSundaysNight = remainingSundaysNight,
        dailySalary = dailySalary,
        luongDuKienVal = luongDuKienVal,
        soNgayCongDuKien = soNgayCongDuKien,
        customOt15DaysCountDay = customOt15DaysCountDay,
        customOt15DaysCountNight = customOt15DaysCountNight,
        customOt15PayDay = customOt15PayDay,
        customOt15PayNight = customOt15PayNight,
        customNightAllowance = customNightAllowance,
        hasLoggedUnpaidOrAbsent = hasLoggedUnpaidOrAbsent,
        breakHours = breakHours
    )
}

fun savePayslipAsPngImageOld(
    context: Context,
    entries: List<com.example.data.model.TimeEntry>,
    summary: SalarySummary,
    config: com.example.data.model.UserConfig,
    userSession: UserSession?,
    monthLabel: String,
    selectedMonth: String,
    selectedTab: Int = 0,
    includeSundayInProjection: Boolean = false,
    remainingWeekdays: Int = 0,
    remainingSundays: Int = 0,
    remainingSundaysDay: Int = 0,
    remainingSundaysNight: Int = 0,
    dailySalary: Double = 0.0,
    luongDuKienVal: Double = 0.0,
    soNgayCongDuKien: Double = 0.0,
    customOt15DaysCount: Double = 0.0,
    customOt15Pay: Double = 0.0,
    selectedOt15Shift: String = "Đêm",
    customNightAllowance: Double = 0.0,
    hasLoggedUnpaidOrAbsent: Boolean = false,
    breakHours: Double = 0.0
): Boolean {
    val df = DecimalFormat("#.#")
    val fmt = DecimalFormat("#,###")

    val todayCal = Calendar.getInstance()
    val currentYear = todayCal.get(Calendar.YEAR)
    val currentMonth = todayCal.get(Calendar.MONTH) + 1
    val isCurrentSelectedMonth = selectedMonth.startsWith(String.format(Locale.US, "%04d-%02d", currentYear, currentMonth))

    val pcKyThuatShowPNG = if (selectedTab == 1) config.pcKyThuat else summary.pcKyThuatVal
    val pcTrachNhiemShowPNG = if (selectedTab == 1) config.pcTrachNhiem else summary.pcTrachNhiemVal
    val pcChucVuShowPNG = if (selectedTab == 1) config.pcChucVu else summary.pcChucVuVal
    val pcHieuSuatShowPNG = if (selectedTab == 1) config.pcHieuSuat else summary.pcHieuSuatVal
    val pcSanPhamShowPNG = if (selectedTab == 1) config.pcSanPham else summary.pcSanPhamVal

    val pcComCaShowPNG = if (selectedTab == 1) {
        if (isCurrentSelectedMonth) {
            summary.pcComCaVal + (remainingWeekdays * config.pcComCa) + (if (includeSundayInProjection) remainingSundays * config.pcComCa else 0.0)
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

    val pcNhaOShowPNG = if (selectedTab == 1) config.pcNhaO else summary.pcNhaOVal
    val pcDocHaiShowPNG = if (selectedTab == 1) config.pcDocHai else summary.pcDocHaiVal
    val pcDtDoanhThuShowPNG = if (selectedTab == 1) config.pcDtDoanhThu else summary.pcDtDoanhThuVal
    val pcXangXeShowPNG = if (selectedTab == 1) config.pcXangXe else summary.pcXangXeVal
    val pcKhacShowPNG = if (selectedTab == 1) config.pcCaDem else summary.pcCaDemVal
    val pcKhac1ShowPNG = if (selectedTab == 1) config.pcKhac1 else summary.pcKhac1Val
    val pcThamNienShowPNG = if (selectedTab == 1) config.pcThamNien else summary.pcThamNienVal

    val pcChuyenCanShowPNG = if (selectedTab == 1) {
        if (hasLoggedUnpaidOrAbsent) 0.0 else config.tienChuyenCanGoc
    } else {
        summary.phuCapChuyenCan
    }

    // 1. Create offline Bitmap with Dynamic Height
    val width = 800
    var estimatedHeight = 500 // Header
    
    // Profile section
    estimatedHeight += 200
    
    // Additions section
    estimatedHeight += 60 // Section title
    estimatedHeight += 45 // Base Salary
    if (selectedTab == 1 && remainingSundays > 0 && includeSundayInProjection) estimatedHeight += 45
    if (selectedTab == 1 && customOt15DaysCount > 0.0) estimatedHeight += 45
    if (config.pcKyThuat > 0.0) estimatedHeight += 45
    if (config.pcTrachNhiem > 0.0) estimatedHeight += 45
    if (config.pcChucVu > 0.0) estimatedHeight += 45
    if (config.pcHieuSuat > 0.0) estimatedHeight += 45
    if (config.pcSanPham > 0.0) estimatedHeight += 45
    if (pcComCaShowPNG > 0.0) estimatedHeight += 45
    if (pcComOtShowPNG > 0.0) estimatedHeight += 45
    if (config.pcNhaO > 0.0) estimatedHeight += 45
    if (config.pcDocHai > 0.0) estimatedHeight += 45
    if (config.pcDtDoanhThu > 0.0) estimatedHeight += 45
    if (config.pcXangXe > 0.0) estimatedHeight += 45
    if (config.pcCaDem > 0.0) estimatedHeight += 45
    if (config.pcKhac1 > 0.0) estimatedHeight += 45
    if (config.pcThamNien > 0.0) estimatedHeight += 45
    if (pcChuyenCanShowPNG > 0.0) estimatedHeight += 45
    if (summary.caDemCount > 0) estimatedHeight += 45
    if (summary.tienOtNgay > 0.0) estimatedHeight += 45
    if (summary.tienChuNhat > 0.0) estimatedHeight += 45
    if (summary.tienOtLe > 0.0) estimatedHeight += 45
    if (summary.tienOtDem > 0.0) estimatedHeight += 45
    
    // Deductions section
    estimatedHeight += 60 // Section title
    if (summary.tienBh > 0.0) estimatedHeight += 45
    if (summary.doanPhi > 0.0) estimatedHeight += 45
    if (selectedTab == 0 && summary.tienKhauTruNghi > 0.0) estimatedHeight += 45
    
    // Total section
    estimatedHeight += 150
    
    // Footer
    estimatedHeight += 200
    
    val height = estimatedHeight.coerceAtLeast(1400)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Setup Paints
    val colorBg = android.graphics.Color.parseColor("#0B0E14")
    val colorCard = android.graphics.Color.parseColor("#1A1D2E")
    val colorPrimary = android.graphics.Color.parseColor("#4C84FF")
    val colorTextMuted = android.graphics.Color.parseColor("#8F9BB3")
    val colorSuccess = android.graphics.Color.parseColor("#00E676")
    val colorError = android.graphics.Color.parseColor("#EB5757")

    val paintBg = Paint().apply { color = colorBg }
    val paintCard = Paint().apply { color = colorCard }
    
    val paintAppName = Paint().apply {
        color = colorPrimary
        textSize = 38f
        isFakeBoldText = true
    }
    
    val paintDocTitle = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 28f
        isFakeBoldText = true
    }
    
    val paintDocInfo = Paint().apply {
        color = colorTextMuted
        textSize = 20f
    }

    val paintSectionTitle = Paint().apply {
        color = colorPrimary
        textSize = 22f
        isFakeBoldText = true
    }

    val paintDivider = Paint().apply {
        color = android.graphics.Color.parseColor("#1C212B")
        strokeWidth = 2f
    }

    val paintLabel = Paint().apply {
        color = colorTextMuted
        textSize = 22f
    }

    val paintValue = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 22f
        isFakeBoldText = true
    }

    val paintGreen = Paint().apply {
        color = colorSuccess
        textSize = 22f
        isFakeBoldText = true
    }

    val paintRed = Paint().apply {
        color = colorError
        textSize = 22f
        isFakeBoldText = true
    }

    // Draw background
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)

    var currentY = 80f
    val paddingX = 60f

    // Header
    canvas.drawText("TIMESNAP PRO", paddingX, currentY, paintAppName)
    currentY += 50f
    canvas.drawText("PHIẾU LƯƠNG ĐIỆN TỬ CHI TIẾT", paddingX, currentY, paintDocTitle)
    currentY += 40f
    val statusText = if (selectedTab == 1) "Trạng thái: Dự kiến" else "Trạng thái: Đã phê duyệt"
    val formattedMonthLabel = if (monthLabel.startsWith("Tháng", ignoreCase = true)) monthLabel else "Tháng $monthLabel"
    canvas.drawText("$formattedMonthLabel | $statusText", paddingX, currentY, paintDocInfo)
    currentY += 80f

    // Helper functions
    fun drawSectionHeader(title: String) {
        canvas.drawText(title, paddingX, currentY, paintSectionTitle)
        currentY += 20f
        canvas.drawLine(paddingX, currentY, width - paddingX, currentY, paintDivider)
        currentY += 50f
    }

    fun drawRow(label: String, value: String, valuePaint: Paint = paintValue) {
        canvas.drawText(label, paddingX, currentY, paintLabel)
        val measure = valuePaint.measureText(value)
        canvas.drawText(value, width - paddingX - measure, currentY, valuePaint)
        currentY += 50f
    }

    // 1. THÔNG TIN NHÂN SỰ
    drawSectionHeader("THÔNG TIN NHÂN SỰ")
    val employeeName = if (!config.hoVaTen.isNullOrBlank()) config.hoVaTen else (userSession?.displayName ?: "N/A")
    val employeeCode = if (!config.maNhanVien.isNullOrBlank()) config.maNhanVien else (userSession?.uid?.take(10) ?: "N/A")
    
    drawRow("Họ và tên:", employeeName)
    drawRow("Mã nhân viên:", employeeCode)
    drawRow("Mức lương cơ bản:", "${fmt.format(config.luongCoBan)}đ")
    
    val progressText = if (selectedTab == 1) "${df.format(soNgayCongDuKien)} / ${summary.standardWorkDays} ngày" else "${df.format(summary.actualStandardWorkingDays)} / ${summary.standardWorkDays} ngày"
    drawRow("Tiến độ tháng (Công chuẩn):", progressText)

    val annualLeavesCount = entries.count { com.example.data.SalaryCalculator.isAnnualLeaveType(it.dayType) }
    val holidayLeavesCount = entries.count { com.example.data.SalaryCalculator.isHolidayLeaveType(it.dayType) }
    val unpaidLeavesCount = entries.count { com.example.data.SalaryCalculator.isUnpaidLeaveType(it.dayType) }
    val leaveParts = mutableListOf<String>()
    if (annualLeavesCount > 0) leaveParts.add("Phép năm: ${annualLeavesCount}n")
    if (holidayLeavesCount > 0) leaveParts.add("Lễ: ${holidayLeavesCount}n")
    if (unpaidLeavesCount > 0) leaveParts.add("Không lương: ${unpaidLeavesCount}n")
    val leaveDaysVal = if (leaveParts.isEmpty()) "0 ngày" else leaveParts.joinToString(", ")
    drawRow("Ngày nghỉ:", leaveDaysVal)
    
    if (selectedTab == 1) {
        drawRow("Ngày công (Dự kiến):", "${df.format(soNgayCongDuKien)} / ${summary.standardWorkDays} ngày")
    } else {
        drawRow("Ngày công (Thực tế):", "${df.format(summary.actualPresenceDays)} / ${summary.standardWorkDays} ngày")
    }
    currentY += 30f

    // 2. THU NHẬP CHI TIẾT
    drawSectionHeader("THU NHẬP CHI TIẾT (+)")
    
    val luongDuKienBaseSalary = Math.round((config.luongCoBan / 26.0) * soNgayCongDuKien).toDouble()
    if (selectedTab == 1) {
        drawRow("LCB thực nhận (${df.format(soNgayCongDuKien)} / ${summary.standardWorkDays})", "+${fmt.format(luongDuKienBaseSalary)}đ", paintGreen)
    } else {
        drawRow("LCB thực nhận (${df.format(summary.workingDays)} / ${summary.standardWorkDays})", "+${fmt.format(summary.baseBasicSalary)}đ", paintGreen)
    }

    if (pcChuyenCanShowPNG > 0.0) drawRow("Phụ cấp chuyên cần", "+${fmt.format(pcChuyenCanShowPNG)}đ", paintGreen)
    if (pcTrachNhiemShowPNG > 0.0) drawRow("Phụ cấp trách nhiệm", "+${fmt.format(pcTrachNhiemShowPNG)}đ", paintGreen)
    if (pcKyThuatShowPNG > 0.0) drawRow("Phụ cấp kỹ thuật", "+${fmt.format(pcKyThuatShowPNG)}đ", paintGreen)
    if (pcHieuSuatShowPNG > 0.0) drawRow("Phụ cấp hiệu suất", "+${fmt.format(pcHieuSuatShowPNG)}đ", paintGreen)
    if (pcSanPhamShowPNG > 0.0) drawRow("Phụ cấp sản phẩm", "+${fmt.format(pcSanPhamShowPNG)}đ", paintGreen)
    if (pcChucVuShowPNG > 0.0) drawRow("Phụ cấp chức vụ", "+${fmt.format(pcChucVuShowPNG)}đ", paintGreen)
    if (pcDocHaiShowPNG > 0.0) drawRow("Phụ cấp độc hại", "+${fmt.format(pcDocHaiShowPNG)}đ", paintGreen)
    if (pcDtDoanhThuShowPNG > 0.0) drawRow("Phụ cấp doanh thu", "+${fmt.format(pcDtDoanhThuShowPNG)}đ", paintGreen)
    if (pcThamNienShowPNG > 0.0) drawRow("Phụ cấp thâm niên", "+${fmt.format(pcThamNienShowPNG)}đ", paintGreen)
    if (pcComCaShowPNG > 0.0) drawRow("Phụ cấp cơm/ ca", "+${fmt.format(pcComCaShowPNG)}đ", paintGreen)
    if (pcComOtShowPNG > 0.0) drawRow("Phụ cấp cơm OT", "+${fmt.format(pcComOtShowPNG)}đ", paintGreen)

    // OT Ngày Merged
    val projOtDayPNG = if (selectedTab == 1 && selectedOt15Shift == "Ngày") customOt15DaysCount * (4.0 - breakHours).coerceAtLeast(0.0) else 0.0
    val totalOtDayPNG = summary.otDayHours + projOtDayPNG
    val totalPayDayPNG = summary.tienOtNgay + (if (selectedTab == 1 && selectedOt15Shift == "Ngày") customOt15Pay else 0.0)
    if (totalOtDayPNG > 0.0) drawRow("OT ngày ${df.format(config.heSoOtNgayThuong)} (${df.format(totalOtDayPNG)}h)", "+${fmt.format(totalPayDayPNG)}đ", paintGreen)

    val sundayHoursPerShiftPNG = (12.0 - breakHours).coerceAtLeast(0.0)
    val hourlySalaryPNG = dailySalary / 8.0
    val totalSunDayHoursPNG = summary.chuNhatDayHours + (if (selectedTab == 1 && includeSundayInProjection) remainingSundaysDay * sundayHoursPerShiftPNG else 0.0)
    val totalSunDayPayPNG = summary.tienChuNhatNgay + (if (selectedTab == 1 && includeSundayInProjection) remainingSundaysDay * sundayHoursPerShiftPNG * hourlySalaryPNG * config.heSoOtChuNhat else 0.0)

    val totalSunNightHoursPNG = summary.chuNhatNightHours + (if (selectedTab == 1 && includeSundayInProjection) remainingSundaysNight * sundayHoursPerShiftPNG else 0.0)
    val totalSunNightPayPNG = summary.tienChuNhatDem + (if (selectedTab == 1 && includeSundayInProjection) remainingSundaysNight * sundayHoursPerShiftPNG * hourlySalaryPNG * config.heSoOtChuNhat else 0.0)

    if (totalSunDayHoursPNG > 0.0) drawRow("OT CN - Ca ngày ${df.format(config.heSoOtChuNhat)} (${df.format(totalSunDayHoursPNG)}h)", "+${fmt.format(totalSunDayPayPNG)}đ", paintGreen)
    if (totalSunNightHoursPNG > 0.0) drawRow("OT CN - Ca đêm ${df.format(config.heSoOtChuNhat)} (${df.format(totalSunNightHoursPNG)}h)", "+${fmt.format(totalSunNightPayPNG)}đ", paintGreen)
    if (totalSunDayHoursPNG == 0.0 && totalSunNightHoursPNG == 0.0 && (summary.tienChuNhat + (if (selectedTab == 1 && includeSundayInProjection) (remainingSundays * sundayHoursPerShiftPNG * hourlySalaryPNG * config.heSoOtChuNhat) else 0.0)) > 0.0) {
        val totalSunHrs = summary.chuNhatHours + (if (selectedTab == 1 && includeSundayInProjection) (remainingSundays * sundayHoursPerShiftPNG) else 0.0)
        val totalSunPay = summary.tienChuNhat + (if (selectedTab == 1 && includeSundayInProjection) (remainingSundays * sundayHoursPerShiftPNG * hourlySalaryPNG * config.heSoOtChuNhat) else 0.0)
        drawRow("OT chủ nhật ${df.format(config.heSoOtChuNhat)} (${df.format(totalSunHrs)}h)", "+${fmt.format(totalSunPay)}đ", paintGreen)
    }

    if (summary.tienOtLe > 0.0) drawRow("OT lễ ${df.format(config.heSoOtNgayLe)} (${df.format(summary.otLeHours)}h)", "+${fmt.format(summary.tienOtLe)}đ", paintGreen)

    // OTĐ Merged
    val projOtNightPNG = if (selectedTab == 1 && selectedOt15Shift == "Đêm") customOt15DaysCount * (4.0 - breakHours).coerceAtLeast(0.0) else 0.0
    val totalOtNightPNG = summary.otNightHours + projOtNightPNG
    val totalPayNightPNG = summary.tienOtDem + (if (selectedTab == 1 && selectedOt15Shift == "Đêm") customOt15Pay else 0.0)
    if (totalOtNightPNG > 0.0) drawRow("OT đêm ${df.format(config.heSoOtDem)} (${df.format(totalOtNightPNG)}h)", "+${fmt.format(totalPayNightPNG)}đ", paintGreen)

    val finalPcCaDemCountPNG = if (selectedTab == 1 && selectedOt15Shift == "Đêm") summary.caDemCount + customOt15DaysCount.toInt() else summary.caDemCount
    val finalPcCaDemPNG = if (selectedTab == 1) (summary.pcCaDemVal + customNightAllowance) else summary.pcCaDemVal
    if (finalPcCaDemPNG > 0.0) drawRow("Phụ cấp ca đêm ($finalPcCaDemCountPNG)", "+${fmt.format(finalPcCaDemPNG)}đ", paintGreen)

    if (pcXangXeShowPNG > 0.0) drawRow("Phụ cấp xăng xe", "+${fmt.format(pcXangXeShowPNG)}đ", paintGreen)
    if (pcNhaOShowPNG > 0.0) drawRow("Phụ cấp nhà ở", "+${fmt.format(pcNhaOShowPNG)}đ", paintGreen)
    if (pcKhac1ShowPNG > 0.0) drawRow("Phụ cấp khác", "+${fmt.format(pcKhac1ShowPNG)}đ", paintGreen)
    
    currentY += 30f

    // 3. KHẤU TRỪ
    drawSectionHeader("KHẤU TRỪ & NGHĨA VỤ (-)")
    if (summary.tienBh > 0.0) drawRow("BHXH/BHYT (10.5%)", "-${fmt.format(summary.tienBh)}đ", paintRed)
    if (summary.doanPhi > 0.0) drawRow("Phí công đoàn", "-${fmt.format(summary.doanPhi)}đ", paintRed)
    if (selectedTab == 0 && summary.tienKhauTruNghi > 0.0) {
        val missed = ((if (summary.isCurrentMonth) summary.expectedWorkDays else summary.standardWorkDays).toDouble() - summary.workingDays).coerceAtLeast(0.0)
        drawRow("Khấu trừ vắng (${df.format(missed)} ngày)", "-${fmt.format(summary.tienKhauTruNghi)}đ", paintRed)
    }
    currentY += 50f

    // Total Card
    val cardHeight = 100f
    val cardRect = RectF(paddingX, currentY, width - paddingX, currentY + cardHeight)
    canvas.drawRoundRect(cardRect, 8f, 8f, paintCard)
    
    val paintTotalLabel = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 26f
        isFakeBoldText = true
    }
    val paintTotalValue = Paint().apply {
        color = colorSuccess
        textSize = 30f
        isFakeBoldText = true
    }
    
    val totalLabel = if (selectedTab == 1) "DỰ KIẾN THỰC NHẬN" else "TỔNG LƯƠNG THỰC NHẬN"
    val totalValue = if (selectedTab == 1) luongDuKienVal else summary.luongThucNhan
    val totalValueStr = "${fmt.format(totalValue)} VNĐ"
    
    canvas.drawText(totalLabel, paddingX + 30f, currentY + (cardHeight / 2) + 10f, paintTotalLabel)
    val measureTotal = paintTotalValue.measureText(totalValueStr)
    canvas.drawText(totalValueStr, width - paddingX - 30f - measureTotal, currentY + (cardHeight / 2) + 12f, paintTotalValue)
    
    currentY += cardHeight + 150f

    // Footer
    val paintFooter = Paint().apply {
        color = colorPrimary
        textSize = 18f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    val paintDev = Paint().apply {
        color = colorTextMuted
        textSize = 14f
        textAlign = Paint.Align.CENTER
    }
    
    canvas.drawText("XUẤT TỪ HỆ THỐNG QUẢN LÝ TIMESNAP PRO", width / 2f, currentY, paintFooter)
    currentY += 30f
    canvas.drawText("DEVELOPED BY TRUONGVANKHOA", width / 2f, currentY, paintDev)

    // Save Bitmap to MediaStore
    try {
        val filename = "TimeSnap_Pro_Payslip_${selectedMonth}_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null

        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/TimeSnapPro")
            }
            val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                fos = contentResolver.openOutputStream(imageUri)
                true
            } else {
                false
            }
        } else {
            val imagesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES).toString()
            val file = java.io.File(imagesDir, filename)
            fos = java.io.FileOutputStream(file)
            true
        }

        if (fos != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()
            return true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return false
}

@Composable
fun MonthlyIncomeTrendChart(
    historyList: List<MonthlySalaryPoint>,
    selectedMonth: String,
    onMonthSelected: (String) -> Unit
) {
    if (historyList.isEmpty()) return

    val fmt = remember { DecimalFormat("#,###") }
    
    // Average salary calculation
    val averageSalary = remember(historyList) {
        if (historyList.isNotEmpty()) historyList.map { it.luongThucNhan }.average() else 0.0
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkContainer),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2D3748)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title & Trend Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "SO SÁNH THU NHẬP THỰC TẾ",
                        color = NeonBlue,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Xu hướng thu nhập 6 tháng gần nhất",
                        color = LightGray,
                        fontSize = 11.sp
                    )
                }
                
                // Average Indicator tag
                Box(
                    modifier = Modifier
                        .background(AccentGreen.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .border(1.dp, AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Lương TB",
                            color = AccentGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${fmt.format(averageSalary)}đ",
                            color = White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chart area calculations
            val maxIncome = remember(historyList) {
                (historyList.maxOfOrNull { it.luongThucNhan } ?: 10000000.0).coerceAtLeast(1000000.0)
            }

            // Balanced Chart Grid Container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                historyList.forEach { pt ->
                    val isSelected = pt.monthStr == selectedMonth
                    
                    // Format month label cleanly e.g., "2026-08" -> "T8"
                    val label = remember(pt.monthStr) {
                        try {
                            val parts = pt.monthStr.split("-")
                            val mNum = parts.getOrNull(1)?.toIntOrNull()
                            if (mNum != null) "T$mNum" else pt.monthStr
                        } catch (e: Exception) {
                            pt.monthStr
                        }
                    }

                    // Format income value above bar cleanly
                    val incomeLabel = remember(pt.luongThucNhan) {
                        if (pt.luongThucNhan >= 1_000_000) {
                            val millions = pt.luongThucNhan / 1_000_000.0
                            if (millions >= 10.0) {
                                String.format(Locale.US, "%.1fM", millions)
                            } else {
                                String.format(Locale.US, "%.1fM", millions)
                            }
                        } else if (pt.luongThucNhan > 0) {
                            "${(pt.luongThucNhan / 1000).toInt()}k"
                        } else {
                            "0đ"
                        }
                    }

                    // Height factor calculation
                    val targetFraction = if (pt.luongThucNhan <= 0) 0.05f else (pt.luongThucNhan / maxIncome).coerceIn(0.08, 1.0).toFloat()
                    val animatedFraction by animateFloatAsState(
                        targetValue = targetFraction,
                        animationSpec = tween(durationMillis = 350)
                    )

                    val barAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.45f,
                        animationSpec = tween(durationMillis = 300)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onMonthSelected(pt.monthStr) }
                            .padding(horizontal = 2.dp)
                    ) {
                        // 1. Top Value Badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .background(NeonBlue.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .border(0.8.dp, NeonBlue, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = incomeLabel,
                                        color = NeonBlue,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            } else {
                                Text(
                                    text = incomeLabel,
                                    color = LightGray.copy(alpha = 0.7f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 2. Bar Gauge Area with Background Track
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Background track for consistent visual structure
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxHeight()
                                    .background(
                                        color = Color.White.copy(alpha = 0.04f),
                                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                    )
                            )

                            // Foreground Active Bar
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxHeight(animatedFraction)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = if (isSelected) {
                                                listOf(NeonBlue, Color(0xFF0284C7))
                                            } else {
                                                listOf(Color(0xFF64748B).copy(alpha = barAlpha), Color(0xFF334155).copy(alpha = 0.3f * barAlpha))
                                            }
                                        ),
                                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                            )
                                        } else Modifier
                                    )
                            )
                        }

                        // Subtle baseline divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF334155))
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 3. Month Label Badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .background(NeonBlue, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = Color(0xFF0F172A),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            } else {
                                Text(
                                    text = label,
                                    color = LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

