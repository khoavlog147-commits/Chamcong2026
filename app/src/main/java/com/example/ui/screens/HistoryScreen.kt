package com.example.ui.screens

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import kotlinx.coroutines.delay
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkContainer
import com.example.ui.theme.LightGray
import com.example.ui.theme.MediumGray
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.White
import com.example.ui.theme.NightPurple
import com.example.viewmodel.TimeSnapViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: TimeSnapViewModel
) {
    val selectedMonth by viewModel.currentSelectedMonth.collectAsStateWithLifecycle()
    val entries by viewModel.monthTimeEntries.collectAsStateWithLifecycle()
    val configState by viewModel.userConfig.collectAsStateWithLifecycle()
    val activeEntry by viewModel.activeWorkingEntry.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Screen State Toggle: Single select vs Multi-select
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedDates = remember { mutableStateListOf<String>() }
    var rangeStartStr by remember { mutableStateOf<String?>(null) } // Helper for range selection (e.g. Day 1 -> Day 20)

    // Dialog control triggers
    var showSingleDayDialog by remember { mutableStateOf<CalendarDayInfo?>(null) }
    var showBulkDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showBulkDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Month parsing calculations
    val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val currentMonthDate = remember(selectedMonth) { sdfMonth.parse(selectedMonth) ?: Date() }

    // Calendar generation
    val daysInMonth = remember(currentMonthDate) {
        getCalendarDaysForMonth(currentMonthDate)
    }

    // List of public holidays in the currently selected month
    val holidaysInMonth = remember(daysInMonth, entries) {
        val list = mutableListOf<Pair<String, String>>()
        daysInMonth.filter { !it.isEmpty }.forEach { day ->
            val holidayName = day.holidayName ?: run {
                val e = entries.find { it.date == day.dateString }
                if (e?.dayType == "HOLIDAY" || e?.dayType == "HOLIDAY_LEAVE") "Nghỉ lễ" else null
            }
            if (holidayName != null) {
                val dStr = if (day.dateString.contains("/")) {
                    val parts = day.dateString.split("/")
                    "${parts.getOrNull(0)?.padStart(2, '0')}/${parts.getOrNull(1)?.padStart(2, '0')}"
                } else day.dateString
                list.add(Pair(dStr, holidayName))
            }
        }
        list.distinctBy { it.first }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch Sử Chấm Công", fontWeight = FontWeight.Bold, color = White) },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Toggle Mode Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkContainer)
                                .clickable {
                                    isMultiSelectMode = !isMultiSelectMode
                                    selectedDates.clear()
                                    rangeStartStr = null
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("toggle_multi_select_mode")
                        ) {
                            Icon(
                                imageVector = if (isMultiSelectMode) Icons.Default.Close else Icons.Default.LibraryAddCheck,
                                contentDescription = "Mode Icon",
                                tint = NeonBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMultiSelectMode) "Hủy chọn" else "Chọn nhiều",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Clear Month Button
                        IconButton(
                            onClick = { showDeleteAllDialog = true },
                            modifier = Modifier.testTag("clear_all_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Xoá cả tháng",
                                tint = AccentRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            
            // Month Switcher Controller
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev Month
                IconButton(onClick = {
                    val cal = Calendar.getInstance()
                    cal.time = currentMonthDate
                    cal.add(Calendar.MONTH, -1)
                    viewModel.selectMonth(sdfMonth.format(cal.time))
                    selectedDates.clear()
                    rangeStartStr = null
                }) {
                    Icon(Icons.Default.ArrowBackIosNew, "Tháng trước", tint = NeonBlue)
                }

                // Month Label
                val vietnameseMonthLabel = remember(selectedMonth) {
                    val parser = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    val d = parser.parse(selectedMonth) ?: Date()
                    val formatter = SimpleDateFormat("MMMM yyyy", Locale("vi", "VN"))
                    formatter.format(d).replaceFirstChar { it.uppercase() }
                }

                Text(
                    text = vietnameseMonthLabel,
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
                    selectedDates.clear()
                    rangeStartStr = null
                }) {
                    Icon(Icons.Default.ArrowForwardIos, "Tháng sau", tint = NeonBlue)
                }
            }

            // Holiday Highlights Banner for Currently Selected Month
            AnimatedVisibility(visible = holidaysInMonth.isNotEmpty()) {
                Surface(
                    color = Color(0xFF2E2405),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFFFD700).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⭐", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🎉 Tháng này có ${holidaysInMonth.size} ngày Lễ Quốc Gia:",
                                color = Color(0xFFFFD700),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = holidaysInMonth.joinToString(" • ") { "${it.first}: ${it.second}" } + "\n(Hưởng 100% lương khi nghỉ hoặc x300% lương khi đi làm)",
                                color = Color(0xFFFFE57F),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // Grid header: Mon -> Sun (Thứ 2 đến Chủ Nhật VN Calendar starting T2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val headers = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                headers.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (label == "CN") AccentRed else MediumGray
                    )
                }
            }

            // Days Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(daysInMonth) { day ->
                    if (day.isEmpty) {
                        Box(modifier = Modifier.aspectRatio(1f))
                    } else {
                        val isSelected = selectedDates.contains(day.dateString)
                        val matchingEntry = entries.find { it.date == day.dateString }

                        DayGridCell(
                            day = day,
                            isSelected = isSelected,
                            entry = matchingEntry,
                            isMultiSelectMode = isMultiSelectMode,
                            config = configState,
                            onClick = {
                                if (isMultiSelectMode) {
                                    if (isSelected) {
                                        selectedDates.remove(day.dateString)
                                        rangeStartStr = null
                                    } else {
                                        if (rangeStartStr == null) {
                                            rangeStartStr = day.dateString
                                            selectedDates.add(day.dateString)
                                        } else {
                                            // Perform full range acquisition
                                            val start = rangeStartStr!!
                                            val end = day.dateString
                                            val d1 = if (isDateBeforeOrEqual(start, end)) start else end
                                            val d2 = if (isDateBeforeOrEqual(start, end)) end else start

                                            val fillDates = getDatesInRange(d1, d2)
                                            selectedDates.clear()
                                            selectedDates.addAll(fillDates)
                                            rangeStartStr = null
                                        }
                                    }
                                } else {
                                    showSingleDayDialog = day
                                }
                            }
                        )
                    }
                }
            }

            // Quick Color Legend Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFD700), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ngày Lễ", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(AccentGreen, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Đi làm", color = LightGray, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(NightPurple, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ca đêm", color = LightGray, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(NeonBlue, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Phép", color = LightGray, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(AccentRed, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chủ nhật", color = LightGray, fontSize = 11.sp)
                }
            }

            // CONTROL OVERLAY PANEL: BULK ACTIONS & SWITCHES
            AnimatedVisibility(visible = isMultiSelectMode && selectedDates.isNotEmpty()) {
                Surface(
                    color = DarkContainer,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Đã chọn ${selectedDates.size} ngày",
                            color = NeonBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showBulkDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("bulk_submit_button")
                            ) {
                                Text("Chấm công", color = White, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { showBulkDeleteConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("bulk_delete_button")
                            ) {
                                Text("Xóa công", color = White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Guide text
            if (!isMultiSelectMode) {
                Text(
                    text = "* Ấn vào ngày bất kỳ trên lưới lịch để sửa giờ chấm công hoặc bù chấm công trễ.",
                    color = MediumGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            } else {
                Text(
                    text = if (rangeStartStr == null) 
                        "* Ấn chọn ngày bắt đầu, sau đó chọn ngày kết thúc để tự động bôi đen toàn bộ dải ngày."
                        else 
                        "* Đã chọn ngày bắt đầu. Hãy ấn chọn ngày kết thúc tiếp theo để hoàn tất dải ngày.",
                    color = NeonBlue,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(72.dp))
        }

        // ==================== SINGLE ENTRY DIALOG POPUP (CHẾ ĐỘ 1) ====================
        showSingleDayDialog?.let { day ->
            val todayDate = Date()
            val parser = if (day.dateString.contains("/")) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = remember { parser.format(todayDate) }
            val isFutureDate = remember(day.dateString) {
                try {
                    val d = parser.parse(day.dateString)
                    val t = parser.parse(todayStr)
                    d != null && t != null && d.after(t)
                } catch(e: Exception) {
                    false
                }
            }

            var checkInHour by remember { mutableStateOf(TextFieldValue("08")) }
            var checkInMin by remember { mutableStateOf(TextFieldValue("00")) }
            var checkOutHour by remember { mutableStateOf(TextFieldValue("17")) }
            var checkOutMin by remember { mutableStateOf(TextFieldValue("00")) }
            
            val focusRequesters = remember { List(4) { FocusRequester() } }
            
            var leaveCheckOutEmpty by remember { mutableStateOf(false) }
            
            var selectedDayType by remember { mutableStateOf("NORMAL") }
            var noteString by remember { mutableStateOf("") }
            var isBreakDeducted by remember { mutableStateOf(configState?.tinhKhauTruNghi ?: true) }

            // Pre-fill if entry already exists
            val existing = entries.find { it.date == day.dateString }
            LaunchedEffect(day) {
                if (existing != null) {
                    noteString = existing.note ?: ""
                    selectedDayType = existing.dayType
                    isBreakDeducted = existing.customBreakDeduction ?: configState?.tinhKhauTruNghi ?: true
                    
                    if (existing.checkInTime != null) {
                        val cal = Calendar.getInstance().apply { timeInMillis = existing.checkInTime }
                        checkInHour = TextFieldValue(String.format("%02d", cal.get(Calendar.HOUR_OF_DAY)))
                        checkInMin = TextFieldValue(String.format("%02d", cal.get(Calendar.MINUTE)))
                        
                        if (existing.checkOutTime != null) {
                            val outCal = Calendar.getInstance().apply { timeInMillis = existing.checkOutTime }
                            checkOutHour = TextFieldValue(String.format("%02d", outCal.get(Calendar.HOUR_OF_DAY)))
                            checkOutMin = TextFieldValue(String.format("%02d", outCal.get(Calendar.MINUTE)))
                            leaveCheckOutEmpty = false
                        } else {
                            checkOutHour = TextFieldValue("17")
                            checkOutMin = TextFieldValue("00")
                            leaveCheckOutEmpty = true
                        }
                    }
                } else {
                    noteString = ""
                    if (isFutureDate) {
                        // Future date defaults strictly to paid leave booking
                        selectedDayType = "PAID_LEAVE"
                    } else {
                        selectedDayType = if (day.isSunday) "SUNDAY" else "NORMAL"
                    }
                    isBreakDeducted = configState?.tinhKhauTruNghi ?: true
                    checkInHour = TextFieldValue("08")
                    checkInMin = TextFieldValue("00")
                    checkOutHour = TextFieldValue("17")
                    checkOutMin = TextFieldValue("00")
                    leaveCheckOutEmpty = false
                }
            }

            AlertDialog(
                onDismissRequest = { showSingleDayDialog = null },
                containerColor = DarkContainer,
                title = {
                    Text(
                        text = if (isFutureDate) "Đặt lịch nghỉ Ngày ${day.dayNumber}" else "Chấm công Ngày ${day.dayNumber}",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val holidayName = day.holidayName ?: com.example.data.SalaryCalculator.getHolidayName(day.dateString)
                        if (holidayName != null) {
                            Surface(
                                color = Color(0xFF2E2405),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⭐", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "NGÀY LỄ QUỐC GIA: $holidayName",
                                            color = Color(0xFFFFD700),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Nghỉ lễ hưởng 100% lương ngày công cơ bản hoặc hưởng x300% lương khi đi làm.",
                                            color = Color(0xFFFFE57F),
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        val active = activeEntry
                        if (active != null && active.date != day.dateString) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, AccentRed, RoundedCornerShape(10.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = AccentRed, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "BẠN ĐANG TRONG CA LÀM VIỆC!",
                                            color = AccentRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Bạn đã bấm vào ca ngày ${active.date} và chưa bấm 'Ra ca'. Bạn không thể chấm công thêm ngày ${day.dateString} trừ khi bấm ra ca ca làm hiện tại trước.",
                                            color = White,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        fun createTimeField(
                            value: TextFieldValue,
                            onValueChange: (TextFieldValue) -> Unit,
                            focusRequester: FocusRequester,
                            nextFocusRequester: FocusRequester? = null,
                            modifier: Modifier = Modifier,
                            enabled: Boolean = true
                        ): @Composable () -> Unit = {
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()
                            
                            LaunchedEffect(isFocused) {
                                if (isFocused) {
                                    delay(50)
                                    onValueChange(value.copy(selection = TextRange(0, value.text.length)))
                                }
                            }
                            
                            OutlinedTextField(
                                value = value,
                                onValueChange = { 
                                    if (it.text.length <= 2) {
                                        val textChanged = it.text != value.text
                                        onValueChange(it)
                                        if (textChanged && it.text.length == 2 && nextFocusRequester != null) {
                                            nextFocusRequester.requestFocus()
                                        }
                                    }
                                },
                                modifier = modifier.focusRequester(focusRequester),
                                interactionSource = interactionSource,
                                enabled = enabled,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 15.sp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = if (nextFocusRequester != null) ImeAction.Next else ImeAction.Done)
                            )
                        }

                        // Choice of Day Type
                        Text("Phân loại ngày này:", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // If future day, do NOT render the standard working options - only allow PAID_LEAVE or UNPAID_LEAVE
                            val isHolidayOrNear = com.example.data.SalaryCalculator.isNearHolidayWindow(day.dateString, daysRange = 3)
                                            
                            val types = if (isFutureDate) {
                                val list = mutableListOf(
                                    Triple("PAID_LEAVE", "Có lương", AccentGreen),
                                    Triple("UNPAID_LEAVE", "Không lương", AccentOrange),
                                    Triple("UNAUTHORIZED_LEAVE", "Không phép", Color(0xFFEB5757))
                                )
                                if (isHolidayOrNear) {
                                    list.add(0, Triple("HOLIDAY_LEAVE", "Lễ có lương", Color(0xFFBB86FC)))
                                }
                                list
                            } else {
                                val list = mutableListOf(
                                    Triple("NORMAL", "Đi làm", NeonBlue),
                                    Triple("PAID_LEAVE", "Có lương", AccentGreen),
                                    Triple("UNPAID_LEAVE", "Không lương", AccentOrange),
                                    Triple("UNAUTHORIZED_LEAVE", "Không phép", Color(0xFFEB5757))
                                )
                                if (isHolidayOrNear) {
                                    list.add(1, Triple("HOLIDAY_LEAVE", "Lễ có lương", Color(0xFFBB86FC)))
                                }
                                list
                            }

                            types.forEach { (typeKey, typeLabel, typeColor) ->
                                val isChosen = selectedDayType == typeKey
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isChosen) typeColor.copy(alpha = 0.2f) else DarkBackground)
                                        .border(
                                            1.5.dp, 
                                            if (isChosen) typeColor else Color.Transparent, 
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { 
                                            selectedDayType = typeKey 
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = typeLabel,
                                        color = if (isChosen) typeColor else LightGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Hours Selection Area (Disabled completely for holiday/paid/unpaid leaves)
                        if (!isFutureDate && selectedDayType != "PAID_LEAVE" && selectedDayType != "UNPAID_LEAVE" && selectedDayType != "HOLIDAY_LEAVE") {
                            // CheckIn Selection
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Giờ Vào Ca:", color = LightGray, modifier = Modifier.width(90.dp), fontWeight = FontWeight.Bold)

                                createTimeField(
                                    value = checkInHour,
                                    onValueChange = { 
                                        val h = it.text.toIntOrNull() ?: 0
                                        if (h > 24) {
                                            checkInHour = it.copy(text = "07", selection = TextRange(2))
                                            checkInMin = checkInMin.copy(text = "30", selection = TextRange(2))
                                        } else if (h == 24 && (checkInMin.text.toIntOrNull() ?: 0) > 0) {
                                            checkInHour = it.copy(text = "07", selection = TextRange(2))
                                            checkInMin = checkInMin.copy(text = "30", selection = TextRange(2))
                                        } else {
                                            checkInHour = it
                                        }
                                    },
                                    focusRequester = focusRequesters[0],
                                    nextFocusRequester = focusRequesters[1],
                                    modifier = Modifier.width(62.dp)
                                )()

                                Text(":", color = White, fontWeight = FontWeight.Black)

                                createTimeField(
                                    value = checkInMin,
                                    onValueChange = { 
                                        val m = it.text.toIntOrNull() ?: 0
                                        if (m > 59) {
                                            checkInMin = it.copy(text = "59", selection = TextRange(2))
                                        } else if (m > 0 && (checkInHour.text.toIntOrNull() ?: 0) == 24) {
                                            checkInHour = checkInHour.copy(text = "07", selection = TextRange(2))
                                            checkInMin = it.copy(text = "30", selection = TextRange(2))
                                        } else {
                                            checkInMin = it
                                        }
                                    },
                                    focusRequester = focusRequesters[1],
                                    nextFocusRequester = focusRequesters[2],
                                    modifier = Modifier.width(62.dp)
                                )()
                            }

                            // CheckOut Selection
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Giờ Ra Ca:", color = if (leaveCheckOutEmpty) MediumGray else LightGray, modifier = Modifier.width(90.dp), fontWeight = FontWeight.Bold)

                                createTimeField(
                                    value = checkOutHour,
                                    onValueChange = { 
                                        val h = it.text.toIntOrNull() ?: 0
                                        if (h > 24) {
                                            checkOutHour = it.copy(text = "07", selection = TextRange(2))
                                            checkOutMin = checkOutMin.copy(text = "30", selection = TextRange(2))
                                        } else if (h == 24 && (checkOutMin.text.toIntOrNull() ?: 0) > 0) {
                                            checkOutHour = it.copy(text = "07", selection = TextRange(2))
                                            checkOutMin = checkOutMin.copy(text = "30", selection = TextRange(2))
                                        } else {
                                            checkOutHour = it
                                        }
                                    },
                                    focusRequester = focusRequesters[2],
                                    nextFocusRequester = focusRequesters[3],
                                    enabled = !leaveCheckOutEmpty,
                                    modifier = Modifier.width(62.dp)
                                )()

                                Text(":", color = if (leaveCheckOutEmpty) MediumGray else White, fontWeight = FontWeight.Black)

                                createTimeField(
                                    value = checkOutMin,
                                    onValueChange = { 
                                        val m = it.text.toIntOrNull() ?: 0
                                        if (m > 59) {
                                            checkOutMin = it.copy(text = "59", selection = TextRange(2))
                                        } else if (m > 0 && (checkOutHour.text.toIntOrNull() ?: 0) == 24) {
                                            checkOutHour = checkOutHour.copy(text = "07", selection = TextRange(2))
                                            checkOutMin = it.copy(text = "30", selection = TextRange(2))
                                        } else {
                                            checkOutMin = it
                                        }
                                    },
                                    focusRequester = focusRequesters[3],
                                    nextFocusRequester = null,
                                    enabled = !leaveCheckOutEmpty,
                                    modifier = Modifier.width(62.dp)
                                )()
                            }

                            // Special switch: "Quên chấm công khi đang trong ca"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { leaveCheckOutEmpty = !leaveCheckOutEmpty }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = leaveCheckOutEmpty,
                                    onCheckedChange = { leaveCheckOutEmpty = it },
                                    colors = CheckboxDefaults.colors(checkedColor = NeonBlue)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Quên bấm chấm công ra (Để trống Giờ ra)",
                                    color = LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.W500
                                )
                            }

                            // Special switch: "Thông ca (Không khấu trừ nghỉ)"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { isBreakDeducted = !isBreakDeducted }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = !isBreakDeducted, // Checked means CONTINUOUS (not deducted)
                                    onCheckedChange = { isBreakDeducted = !it },
                                    colors = CheckboxDefaults.colors(checkedColor = AccentGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Làm thông ca (Không khấu trừ ${configState?.soGioNghiGiaiLao ?: 1.5}g nghỉ ca này)",
                                    color = AccentGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (isFutureDate) {
                            // Notice for Future Leave setups
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AccentOrange.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "⚠️ Bạn đang xin nghỉ trong tương lai. Bạn không được chấm công làm cho ngày tương lai. Đến ngày này hệ thống sẽ tự động cập nhật là bạn nghỉ và tính mức lương lưu vết tương ứng.",
                                    color = AccentOrange,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // Leave day explanation notice for past days
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NeonBlue.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = if (selectedDayType == "PAID_LEAVE") 
                                        "✓ Đăng ký vắng nghỉ có lương. Hệ thống bảo lưu lương cơ bản ngày công này."
                                        else 
                                        "✓ Đăng ký vắng nghỉ không lương. Ngày công này sẽ bị khấu trừ vào lương thực nhận.",
                                    color = LightGray,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // NOTE INPUT FIELD
                        OutlinedTextField(
                            value = noteString,
                            onValueChange = { noteString = it },
                            label = { Text("Ghi chú lý do nghỉ hoặc vắng...") },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonBlue,
                                focusedLabelColor = NeonBlue,
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            )
                        )
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Delete entry
                        if (existing != null) {
                            IconButton(
                                onClick = {
                                    viewModel.deleteEntry(existing)
                                    showSingleDayDialog = null
                                }
                            ) {
                                Icon(Icons.Default.Delete, "Xoá", tint = AccentRed)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { showSingleDayDialog = null }) {
                                Text("Huỷ", color = LightGray)
                            }
                            Button(
                                onClick = {
                                    val active = activeEntry
                                    if (active != null && active.date != day.dateString) {
                                        Toast.makeText(
                                            context,
                                            "⚠️ Bạn đang trong ca làm việc ngày ${active.date}. Vui lòng bấm 'Ra ca' trước khi chấm công ngày mới!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@Button
                                    }

                                    if (isFutureDate) {
                                        // Force save as Leave types for Future dates
                                        viewModel.addSingleEntry(
                                            dateStr = day.dateString,
                                            checkInHour = 0,
                                            checkInMin = 0,
                                            checkOutHour = null,
                                            checkOutMin = null,
                                            dayTypeOverride = selectedDayType,
                                            noteStr = noteString.ifEmpty { "Nghỉ phép trước" }
                                        )
                                    } else {
                                        val inHour = checkInHour.text.toIntOrNull() ?: 8
                                        val inMin = checkInMin.text.toIntOrNull() ?: 0
                                        val outHour = if (leaveCheckOutEmpty) null else (checkOutHour.text.toIntOrNull() ?: 17)
                                        val outMin = if (leaveCheckOutEmpty) null else (checkOutMin.text.toIntOrNull() ?: 0)

                                        viewModel.addSingleEntry(
                                            dateStr = day.dateString,
                                            checkInHour = inHour,
                                            checkInMin = inMin,
                                            checkOutHour = outHour,
                                            checkOutMin = outMin,
                                            dayTypeOverride = selectedDayType,
                                            noteStr = noteString.ifEmpty { null },
                                            customBreakDeduction = isBreakDeducted
                                        )
                                    }
                                    showSingleDayDialog = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                            ) {
                                Text("Lưu", color = White)
                            }
                        }
                    }
                }
            )
        }

        // ==================== CLEAR MONTH ENTRIES DIALOG ====================
        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                containerColor = DarkContainer,
                title = {
                    Text(
                        text = "Cảnh báo xóa ngày công",
                        color = AccentRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "Bạn đang xóa tất cả ngày công của tháng này. Thao tác này sẽ xoá toàn bộ lịch sử trong tháng để bạn có thể thêm lại từ đầu. Có ai lỡ ấn nhầm cũng sẽ hiểu rõ hành động này.",
                        color = LightGray,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteAllDialog = false
                            viewModel.clearAllEntriesInSelectedMonth()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Xác nhận xóa", color = White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text("Hủy", color = LightGray)
                    }
                }
            )
        }

        // ==================== CONFIRM BULK DELETE DIALOG ====================
        if (showBulkDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showBulkDeleteConfirmDialog = false },
                containerColor = DarkContainer,
                title = {
                    Text(
                        text = "Xác nhận xóa ngày công",
                        color = AccentRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "Bạn đang thực hiện xóa hàng loạt tuyển tập ${selectedDates.size} ngày công đã chọn. Thao tác này sẽ xoá sạch lịch sử công để tránh trường hợp nhập sai. Bạn có chắc chắn muốn xóa ngày công này?",
                        color = LightGray,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteBulkEntries(selectedDates.toList())
                            showBulkDeleteConfirmDialog = false
                            selectedDates.clear()
                            isMultiSelectMode = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Xác nhận xóa", color = White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBulkDeleteConfirmDialog = false }) {
                        Text("Hủy", color = LightGray)
                    }
                }
            )
        }

        // ==================== BULK SHIFT SELECT PRESET DIALOG (CHẾ ĐỘ 2) ====================
        if (showBulkDialog) {
            var startHour by remember { mutableStateOf(TextFieldValue("08")) }
            var startMin by remember { mutableStateOf(TextFieldValue("00")) }
            var endHour by remember { mutableStateOf(TextFieldValue("17")) }
            var endMin by remember { mutableStateOf(TextFieldValue("00")) }
            
            val bulkFocusRequesters = remember { List(4) { FocusRequester() } }

            var skipSunday by remember { mutableStateOf(false) }
            var skipHoliday by remember { mutableStateOf(false) }
            var autoRecognizeOtCoefficients by remember { mutableStateOf(true) }

            androidx.compose.runtime.LaunchedEffect(skipSunday, skipHoliday) {
                if (skipSunday || skipHoliday) {
                    autoRecognizeOtCoefficients = false
                }
            }

            AlertDialog(
                onDismissRequest = { showBulkDialog = false },
                containerColor = DarkContainer,
                title = { Text("Chấm công Hàng loạt", color = White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        fun createBulkTimeField(
                            value: TextFieldValue,
                            onValueChange: (TextFieldValue) -> Unit,
                            focusRequester: FocusRequester,
                            nextFocusRequester: FocusRequester? = null,
                            modifier: Modifier = Modifier
                        ): @Composable () -> Unit = {
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()
                            
                            LaunchedEffect(isFocused) {
                                if (isFocused) {
                                    delay(50)
                                    onValueChange(value.copy(selection = TextRange(0, value.text.length)))
                                }
                            }
                            
                            OutlinedTextField(
                                value = value,
                                onValueChange = { 
                                    if (it.text.length <= 2) {
                                        val textChanged = it.text != value.text
                                        onValueChange(it)
                                        if (textChanged && it.text.length == 2 && nextFocusRequester != null) {
                                            nextFocusRequester.requestFocus()
                                        }
                                    }
                                },
                                modifier = modifier.focusRequester(focusRequester),
                                interactionSource = interactionSource,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = if (nextFocusRequester != null) ImeAction.Next else ImeAction.Done)
                            )
                        }

                        Text(
                            text = "Cấu hình giờ chung cho ${selectedDates.size} ngày đã chọn:",
                            color = NeonBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Hours Vào
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Giờ Vào:", color = LightGray, modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold)

                            createBulkTimeField(
                                value = startHour,
                                onValueChange = { 
                                    val h = it.text.toIntOrNull() ?: 0
                                    if (h > 24) {
                                        startHour = it.copy(text = "07", selection = TextRange(2))
                                        startMin = startMin.copy(text = "30", selection = TextRange(2))
                                    } else if (h == 24 && (startMin.text.toIntOrNull() ?: 0) > 0) {
                                        startHour = it.copy(text = "07", selection = TextRange(2))
                                        startMin = startMin.copy(text = "30", selection = TextRange(2))
                                    } else {
                                        startHour = it
                                    }
                                },
                                focusRequester = bulkFocusRequesters[0],
                                nextFocusRequester = bulkFocusRequesters[1],
                                modifier = Modifier.width(62.dp)
                            )()

                            Text(":", color = White)

                            createBulkTimeField(
                                value = startMin,
                                onValueChange = { 
                                    val m = it.text.toIntOrNull() ?: 0
                                    if (m > 59) {
                                        startMin = it.copy(text = "59", selection = TextRange(2))
                                    } else if (m > 0 && (startHour.text.toIntOrNull() ?: 0) == 24) {
                                        startHour = startHour.copy(text = "07", selection = TextRange(2))
                                        startMin = it.copy(text = "30", selection = TextRange(2))
                                    } else {
                                        startMin = it
                                    }
                                },
                                focusRequester = bulkFocusRequesters[1],
                                nextFocusRequester = bulkFocusRequesters[2],
                                modifier = Modifier.width(62.dp)
                            )()
                        }

                        // Hours Ra
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Giờ Ra:", color = LightGray, modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold)

                            createBulkTimeField(
                                value = endHour,
                                onValueChange = { 
                                    val h = it.text.toIntOrNull() ?: 0
                                    if (h > 24) {
                                        endHour = it.copy(text = "07", selection = TextRange(2))
                                        endMin = endMin.copy(text = "30", selection = TextRange(2))
                                    } else if (h == 24 && (endMin.text.toIntOrNull() ?: 0) > 0) {
                                        endHour = it.copy(text = "07", selection = TextRange(2))
                                        endMin = endMin.copy(text = "30", selection = TextRange(2))
                                    } else {
                                        endHour = it
                                    }
                                },
                                focusRequester = bulkFocusRequesters[2],
                                nextFocusRequester = bulkFocusRequesters[3],
                                modifier = Modifier.width(62.dp)
                            )()

                            Text(":", color = White)

                            createBulkTimeField(
                                value = endMin,
                                onValueChange = { 
                                    val m = it.text.toIntOrNull() ?: 0
                                    if (m > 59) {
                                        endMin = it.copy(text = "59", selection = TextRange(2))
                                    } else if (m > 0 && (endHour.text.toIntOrNull() ?: 0) == 24) {
                                        endHour = endHour.copy(text = "07", selection = TextRange(2))
                                        endMin = it.copy(text = "30", selection = TextRange(2))
                                    } else {
                                        endMin = it
                                    }
                                },
                                focusRequester = bulkFocusRequesters[3],
                                nextFocusRequester = null,
                                modifier = Modifier.width(62.dp)
                            )()
                        }

                        Divider(color = Color(0xFF2C2C2C))

                        // Checkbox A: Bỏ qua (OT 2.0)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { skipSunday = !skipSunday }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = skipSunday,
                                onCheckedChange = { skipSunday = it },
                                colors = CheckboxDefaults.colors(checkedColor = NeonBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Bỏ qua Chủ Nhật (Không thêm CN)", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Tự loại ngày công chủ nhật ra khi tạo hàng loạt", color = MediumGray, fontSize = 10.sp)
                            }
                        }

                        // Checkbox B: Bỏ qua (OT 3.0)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { skipHoliday = !skipHoliday }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = skipHoliday,
                                onCheckedChange = { skipHoliday = it },
                                colors = CheckboxDefaults.colors(checkedColor = NeonBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Bỏ qua Ngày Lễ (Không thêm Ngày Lễ)", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Hệ thống tự loại ngày nghỉ lễ ra", color = MediumGray, fontSize = 10.sp)
                            }
                        }

                        // Checkbox C: Tự động nhận diện OT 2.0, OT 3.0
                        val isAutoEnabled = !skipSunday && !skipHoliday
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(enabled = isAutoEnabled) { autoRecognizeOtCoefficients = !autoRecognizeOtCoefficients }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = autoRecognizeOtCoefficients && isAutoEnabled,
                                onCheckedChange = { if (isAutoEnabled) autoRecognizeOtCoefficients = it },
                                enabled = isAutoEnabled,
                                colors = CheckboxDefaults.colors(checkedColor = NeonBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Tự động nhận diện Lễ (3.0) & Chủ Nhật (2.0)", 
                                    color = if (isAutoEnabled) White else Color.Gray, 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Nhân hệ số lương phù hợp cho ngày đã tạo", 
                                    color = if (isAutoEnabled) AccentGreen else Color.DarkGray, 
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showBulkDialog = false }) {
                            Text("Huỷ", color = LightGray)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val active = activeEntry
                                if (active != null) {
                                    Toast.makeText(
                                        context,
                                        "⚠️ Bạn đang trong ca làm việc ngày ${active.date}. Vui lòng bấm 'Ra ca' trước khi chấm công hàng loạt!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@Button
                                }

                                val todayDate = Date()
                                val todayParser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val todayStr = todayParser.format(todayDate)
                                
                                val hasFuture = selectedDates.any { dateStr ->
                                    try {
                                        val p = if (dateStr.contains("/")) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        val d = p.parse(dateStr)
                                        val t = p.parse(todayStr)
                                        d != null && t != null && d.after(t)
                                    } catch(e: Exception) {
                                        false
                                    }
                                }
                                if (hasFuture) {
                                    Toast.makeText(context, "Không được chấm công cho ngày tương lai", Toast.LENGTH_LONG).show()
                                }

                                val validDates = selectedDates.filter { dateStr ->
                                    try {
                                        val p = if (dateStr.contains("/")) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        val d = p.parse(dateStr)
                                        val t = p.parse(todayStr)
                                        d != null && t != null && !d.after(t)
                                    } catch(e: Exception) {
                                        true
                                    }
                                }
                                if (validDates.isNotEmpty()) {
                                    val inH = startHour.text.toIntOrNull() ?: 8
                                    val inM = startMin.text.toIntOrNull() ?: 0
                                    val outH = endHour.text.toIntOrNull() ?: 17
                                    val outM = endMin.text.toIntOrNull() ?: 0
                                    val startTotalMinutes = inH * 60 + inM
                                    val isNightShiftOverride = startTotalMinutes in (18 * 60)..(19 * 60 + 30)

                                    viewModel.addBulkEntries(
                                        selectedDates = validDates,
                                        checkInHour = inH,
                                        checkInMin = inM,
                                        checkOutHour = outH,
                                        checkOutMin = outM,
                                        skipSunday = skipSunday,
                                        skipHoliday = skipHoliday,
                                        autoRecognizeOt = autoRecognizeOtCoefficients,
                                        isNightShiftOverride = isNightShiftOverride
                                    )
                                }

                                showBulkDialog = false
                                isMultiSelectMode = false
                                selectedDates.clear()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                        ) {
                            Text("Thực hiện", color = White)
                        }
                    }
                }
            )
        }
    }
}

// Custom Grid cell widget representation
@Composable
fun DayGridCell(
    day: CalendarDayInfo,
    isSelected: Boolean,
    entry: TimeEntry?,
    isMultiSelectMode: Boolean,
    config: UserConfig?,
    onClick: () -> Unit
) {
    val isNightShift = remember(entry) {
        if (entry != null && entry.checkInTime != null) {
            val inCal = java.util.Calendar.getInstance().apply { timeInMillis = entry.checkInTime }
            val inHour = inCal.get(java.util.Calendar.HOUR_OF_DAY)
            val inMin = inCal.get(java.util.Calendar.MINUTE)
            val inTotalMin = inHour * 60 + inMin
            (inTotalMin in (18 * 60)..(19 * 60 + 30)) || 
            inHour >= 22 || inHour <= 6 || 
            entry.dayType == "NIGHT"
        } else {
            entry?.dayType == "NIGHT"
        }
    }

    val isActualWorkingDay = entry != null && entry.checkInTime != null && entry.checkOutTime != null
    val isHoliday = day.isHoliday || entry?.dayType == "HOLIDAY" || entry?.dayType == "HOLIDAY_LEAVE" || com.example.data.SalaryCalculator.isHoliday(day.dateString)
    val isPaidLeave = com.example.data.SalaryCalculator.isPaidLeaveType(entry?.dayType) && !isActualWorkingDay && !isHoliday
    val isUnpaidLeave = com.example.data.SalaryCalculator.isUnpaidLeaveType(entry?.dayType) && !isActualWorkingDay
    val isUnauthorizedLeave = entry?.dayType == "UNAUTHORIZED_LEAVE" && !isActualWorkingDay

    val borderColor = when {
        isSelected -> NeonBlue
        isHoliday -> Color(0xFFFFD700)
        isPaidLeave -> NeonBlue
        isUnpaidLeave -> AccentOrange
        isUnauthorizedLeave -> Color(0xFFEB5757)
        entry?.isWorking == true -> AccentOrange
        isNightShift -> NightPurple
        entry != null -> AccentGreen
        else -> Color.Transparent
    }

    val backgroundColor = when {
        isSelected -> NeonBlue.copy(alpha = 0.25f)
        isHoliday -> Color(0xFFFFD700).copy(alpha = 0.22f)
        isPaidLeave -> NeonBlue.copy(alpha = 0.15f)
        isUnpaidLeave -> AccentOrange.copy(alpha = 0.12f)
        isUnauthorizedLeave -> Color(0xFFEB5757).copy(alpha = 0.15f)
        entry?.isWorking == true -> AccentOrange.copy(alpha = 0.15f)
        isNightShift -> NightPurple.copy(alpha = 0.2f)
        entry != null -> AccentGreen.copy(alpha = 0.12f)
        day.isSunday -> AccentRed.copy(alpha = 0.08f)
        else -> DarkContainer
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(if (isHoliday) 2.dp else 1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(4.dp),
         contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = day.dayNumber.toString(),
                    color = when {
                        isHoliday -> Color(0xFFFFD700)
                        day.isSunday -> AccentRed
                        else -> White
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Hours label or status
            if (entry != null) {
                when {
                    entry.dayType == "PAID_LEAVE" && !isHoliday -> {
                        Text(
                            text = "PHÉP",
                            color = NeonBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    entry.dayType == "UNPAID_LEAVE" -> {
                        Text(
                            text = "VẮNG",
                            color = AccentOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    entry.dayType == "UNAUTHORIZED_LEAVE" -> {
                        Text(
                            text = "K.PHÉP",
                            color = Color(0xFFEB5757),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    entry.dayType == "HOLIDAY" || entry.dayType == "HOLIDAY_LEAVE" || isHoliday -> {
                        if (entry.checkInTime != null && entry.checkOutTime != null) {
                            val processed = com.example.data.SalaryCalculator.calculateSingleEntry(entry, config)
                            val shift = com.example.data.SalaryCalculator.getShiftForEntry(entry)
                            val stdHrs = processed.workDay * shift.standardHours
                            val totalHrs = stdHrs + processed.otHours
                            val df = DecimalFormat("#.#")
                            Text(
                                text = "${df.format(totalHrs)}h",
                                color = Color(0xFFFFD700),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        } else {
                            Text(
                                text = "LỄ",
                                color = Color(0xFFFFD700),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    else -> {
                        if (entry.isWorking) {
                            Text(
                                text = "Vào ca",
                                color = if (isNightShift) NightPurple else AccentOrange,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (entry.checkInTime != null && entry.checkOutTime != null) {
                            val processed = com.example.data.SalaryCalculator.calculateSingleEntry(entry, config)
                            val shift = com.example.data.SalaryCalculator.getShiftForEntry(entry)
                            val stdHrs = processed.workDay * shift.standardHours
                            val totalHrs = stdHrs + processed.otHours
                            
                            val df = DecimalFormat("#.#")
                            Text(
                                text = "${df.format(totalHrs)}h",
                                color = if (isNightShift) NightPurple else AccentGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (!entry.note.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(if (isHoliday) Color(0xFFFFD700) else White, CircleShape)
                    )
                }
            } else if (isHoliday) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "LỄ",
                    color = Color(0xFFFFD700),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            } else {
                Spacer(modifier = Modifier.height(2.dp))
                Box(modifier = Modifier.size(4.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(2.dp)))
            }
        }
    }
}

// Wrapper for calendar cell date information
data class CalendarDayInfo(
    val dayNumber: Int,
    val dateString: String,
    val isSunday: Boolean,
    val isHoliday: Boolean = false,
    val holidayName: String? = null,
    val isEmpty: Boolean = false
)

// Algorithm to build grid days layout start on Monday
private fun getCalendarDaysForMonth(monthDate: Date): List<CalendarDayInfo> {
    val cal = Calendar.getInstance()
    cal.time = monthDate
    cal.set(Calendar.DAY_OF_MONTH, 1)

    val startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val list = ArrayList<CalendarDayInfo>()

    val indexOffset = if (startDayOfWeek == Calendar.SUNDAY) 6 else startDayOfWeek - 2

    for (i in 0 until indexOffset) {
        list.add(CalendarDayInfo(0, "", false, false, null, true))
    }

    for (day in 1..maxDays) {
        cal.set(Calendar.DAY_OF_MONTH, day)
        val dateString = sdf.format(cal.time)
        val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        val holidayName = com.example.data.SalaryCalculator.getHolidayName(dateString)
        val isHoliday = holidayName != null
        list.add(CalendarDayInfo(day, dateString, isSunday, isHoliday, holidayName, false))
    }

    return list
}

private fun isDateBeforeOrEqual(dateStr1: String, dateStr2: String): Boolean {
    val parser1 = if (dateStr1.contains("/")) SimpleDateFormat("dd/MM/yyyy", Locale.US) else SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val parser2 = if (dateStr2.contains("/")) SimpleDateFormat("dd/MM/yyyy", Locale.US) else SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return try {
        val d1 = parser1.parse(dateStr1)
        val d2 = parser2.parse(dateStr2)
        if (d1 != null && d2 != null) {
            !d1.after(d2)
        } else {
            dateStr1 <= dateStr2
        }
    } catch (e: Exception) {
        dateStr1 <= dateStr2
    }
}

private fun getDatesInRange(startStr: String, endStr: String): List<String> {
    val list = ArrayList<String>()
    val sdf = if (startStr.contains("/")) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    try {
        val start = sdf.parse(startStr)
        val end = sdf.parse(endStr)
        if (start != null && end != null) {
            val cal = Calendar.getInstance()
            cal.time = start
            while (!cal.time.after(end)) {
                list.add(sdf.format(cal.time))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}
