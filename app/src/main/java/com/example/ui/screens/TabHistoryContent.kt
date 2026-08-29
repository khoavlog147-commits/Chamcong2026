
package com.example.ui.screens
import androidx.compose.foundation.interaction.collectIsFocusedAsState

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.AttendanceRecord
import com.example.data.DatabaseHelper
import com.example.data.model.UserConfig
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TabHistoryContent(
    userId: String,
    userConfig: UserConfig,
    attendanceLogs: List<AttendanceRecord>,
    onRecordsChanged: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val dbHelper = DatabaseHelper.getInstance(context)

    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    
    val attendanceMap = remember(attendanceLogs, currentMonth) {
        val targetMonth = currentMonth.get(Calendar.MONTH) + 1
        val targetYear = currentMonth.get(Calendar.YEAR)
        attendanceLogs.filter { log ->
            val parts = log.dateString.split("/")
            if (parts.size >= 3) {
                val m = parts[1].toIntOrNull()
                val y = parts[2].toIntOrNull()
                m == targetMonth && y == targetYear
            } else false
        }.associateBy { it.dateString }
    }

    var selectedDates by remember { mutableStateOf(setOf<String>()) }
    var isMultiSelectMode by remember { mutableStateOf(false) }

    var showSingleDayDialog by remember { mutableStateOf<String?>(null) }
    var showBatchDialog by remember { mutableStateOf(false) }

    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonth = currentMonth.clone() as Calendar
    firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)
    
    // adjust for Monday = 1
    var firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
    if (firstDayOfWeek < 0) firstDayOfWeek += 7

    val headerFormatter = SimpleDateFormat("MM/yyyy", Locale.getDefault())
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val monthHolidays = remember(currentMonth) {
        val targetMonth = currentMonth.get(Calendar.MONTH) + 1
        val targetYear = currentMonth.get(Calendar.YEAR)
        val list = mutableListOf<String>()
        val maxDays = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (d in 1..maxDays) {
            val dStr = String.format(Locale.US, "%02d/%02d/%04d", d, targetMonth, targetYear)
            if (com.example.data.SalaryCalculator.isHoliday(dStr)) {
                list.add(dStr)
            }
        }
        list
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Multi-select actions header
        if (isMultiSelectMode) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Đã chọn ${selectedDates.size} ngày",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { isMultiSelectMode = false; selectedDates = emptySet() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("Hủy")
                    }
                    Button(
                        onClick = { showBatchDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)),
                        enabled = selectedDates.isNotEmpty()
                    ) {
                        Text("Sửa Hàng Loạt")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = {
                        val next = currentMonth.clone() as Calendar
                        next.add(Calendar.MONTH, -1)
                        currentMonth = next
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Tháng trước", tint = Color.White)
                    }
                    Text(
                        text = "Tháng ${headerFormatter.format(currentMonth.time)}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        val next = currentMonth.clone() as Calendar
                        next.add(Calendar.MONTH, 1)
                        currentMonth = next
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Tháng sau", tint = Color.White)
                    }
                }

                Button(
                    onClick = { isMultiSelectMode = true; selectedDates = emptySet() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A86FF)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Chọn Nhiều", fontSize = 12.sp)
                }
            }
        }

        // Holiday Summary Banner
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = if (monthHolidays.isNotEmpty()) Color(0xFF3B2A03) else Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (monthHolidays.isNotEmpty()) Color(0xFFFFD700) else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (monthHolidays.isNotEmpty()) {
                    val holidayDetails = monthHolidays.map { d ->
                        val name = com.example.data.SalaryCalculator.getHolidayName(d) ?: "Lễ"
                        "$d: $name"
                    }
                    Text(
                        text = "Tháng ${headerFormatter.format(currentMonth.time)} có ${monthHolidays.size} ngày lễ:\n${holidayDetails.joinToString(" • ")}",
                        color = Color(0xFFFFD700),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp
                    )
                } else {
                    Text(
                        text = "Tháng ${headerFormatter.format(currentMonth.time)} không có ngày lễ chính thức",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Calendar Grid Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { day ->
                Text(
                    text = day,
                    color = if (day == "CN") Color(0xFFFF5252) else Color.White,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(firstDayOfWeek) {
                Box(modifier = Modifier.aspectRatio(1f))
            }
            items((1..daysInMonth).toList()) { day ->
                val cal = currentMonth.clone() as Calendar
                cal.set(Calendar.DAY_OF_MONTH, day)
                val dateStr = dateFormatter.format(cal.time)
                val record = attendanceMap[dateStr]
                
                val isSelected = selectedDates.contains(dateStr)
                val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                val isHolidayDate = com.example.data.SalaryCalculator.isHoliday(dateStr)

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isSelected -> Color(0xFF3A86FF).copy(alpha = 0.5f)
                                isHolidayDate -> Color(0xFFFFD700).copy(alpha = 0.35f)
                                record != null -> {
                                    val isLeave = com.example.data.SalaryCalculator.isLeaveType(record.status)
                                    val isPaidLeave = com.example.data.SalaryCalculator.isPaidLeaveType(record.status)
                                    val isUnpaidLeave = com.example.data.SalaryCalculator.isUnpaidLeaveType(record.status)
                                    val stUpper = record.status.uppercase(Locale.ROOT)
                                    val isUnauthorizedLeave = isLeave && (stUpper.contains("UNAUTHORIZED") || stUpper == "KP" || stUpper.contains("KHONGPHEP"))
                                    val isHolidayLeave = isLeave && (stUpper.contains("HOLIDAY") || stUpper == "PAIDHOLIDAYLEAVE" || stUpper == "HOLIDAY_LEAVE")
                                    val isNight = !isLeave && com.example.data.SalaryCalculator.isNightShift(record.clockInTime, record.clockOutTime)
                                    
                                    when {
                                        isHolidayLeave -> Color(0xFF9B51E0).copy(alpha = 0.2f)
                                        isPaidLeave -> Color(0xFFF2C94C).copy(alpha = 0.2f)
                                        isUnauthorizedLeave -> Color(0xFFEB5757).copy(alpha = 0.2f)
                                        isUnpaidLeave -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                        isNight -> Color(0xFFFF9800).copy(alpha = 0.3f)
                                        else -> Color(0xFF2ECC71).copy(alpha = 0.2f)
                                    }
                                }
                                else -> Color(0xFF1E1E1E)
                            }
                        )
                        .clickable {
                            if (isMultiSelectMode) {
                                selectedDates = if (isSelected) {
                                    selectedDates - dateStr
                                } else {
                                    selectedDates + dateStr
                                }
                            } else {
                                showSingleDayDialog = dateStr
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = day.toString(),
                                color = if (isHolidayDate) Color(0xFFFFD700) else if (isSunday) Color(0xFFFF5252) else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            if (isHolidayDate) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("🚩", fontSize = 8.sp)
                            }
                        }
                        if (record != null) {
                            val isLeave = com.example.data.SalaryCalculator.isLeaveType(record.status)
                            val isPaidLeave = com.example.data.SalaryCalculator.isPaidLeaveType(record.status)
                            val isUnpaidLeave = com.example.data.SalaryCalculator.isUnpaidLeaveType(record.status)
                            val stUpper = record.status.uppercase(Locale.ROOT)
                            val isUnauthorizedLeave = isLeave && (stUpper.contains("UNAUTHORIZED") || stUpper == "KP" || stUpper.contains("KHONGPHEP"))
                            val isHolidayLeave = isLeave && (stUpper.contains("HOLIDAY") || stUpper == "PAIDHOLIDAYLEAVE" || stUpper == "HOLIDAY_LEAVE")

                            if (isHolidayLeave) {
                                Text(text = "Nghỉ lễ\ncó lương", fontSize = 9.sp, color = Color(0xFFBB6BD9), textAlign = TextAlign.Center, lineHeight = 10.sp)
                            } else if (isPaidLeave) {
                                Text(text = "Nghỉ phép\ncó lương", fontSize = 9.sp, color = Color(0xFFF2C94C), textAlign = TextAlign.Center, lineHeight = 10.sp)
                            } else if (isUnauthorizedLeave) {
                                Text(text = "Nghỉ không\nphép", fontSize = 9.sp, color = Color(0xFFEB5757), textAlign = TextAlign.Center, lineHeight = 10.sp)
                            } else if (isUnpaidLeave) {
                                Text(text = "Nghỉ không\nlương", fontSize = 9.sp, color = Color(0xFFFF9800), textAlign = TextAlign.Center, lineHeight = 10.sp)
                            } else {
                                val inStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.clockInTime))
                                val outStr = if (record.clockOutTime != null && record.clockOutTime > 0L) {
                                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.clockOutTime))
                                } else "..."
                                Text(text = "$inStr\n$outStr", fontSize = 9.sp, color = Color.LightGray, textAlign = TextAlign.Center)
                            }
                        } else if (isHolidayDate) {
                            Text(text = "Nghỉ lễ", fontSize = 8.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    val currentDialogDate = showSingleDayDialog
    if (currentDialogDate != null) {
        val dateStr = currentDialogDate
        SingleDayEntryDialog(
            dateStr = dateStr,
            initialRecord = attendanceMap[dateStr],
            onDismiss = { showSingleDayDialog = null },
            onSave = { clockInTime, clockOutTime, finalStatus ->
                scope.launch {
                    if (finalStatus == "DELETE") {
                        dbHelper.deleteAttendanceRecord(userId, dateStr)
                    } else {
                        val r = AttendanceRecord(
                            id = attendanceMap[dateStr]?.id ?: 0,
                            uid = userId,
                            dateString = dateStr,
                            clockInTime = clockInTime,
                            clockOutTime = clockOutTime,
                            status = finalStatus
                        )
                        dbHelper.insertManualRecord(r)
                    }
                    onRecordsChanged()
                }
                showSingleDayDialog = null
            }
        )
    }

    if (showBatchDialog) {
        BatchEntryDialog(
            selectedDates = selectedDates.toList(),
            onDismiss = { showBatchDialog = false },
            onSave = { action, inHour, inMin, outHour, outMin, skipSunday, skipHoliday ->
                val datesToSave = selectedDates.toList()
                showBatchDialog = false
                isMultiSelectMode = false
                selectedDates = emptySet()

                scope.launch {
                    val recordsList = mutableListOf<AttendanceRecord>()
                    datesToSave.forEach { d ->
                        val cal = Calendar.getInstance()
                        val parsedDate = dateFormatter.parse(d)
                        if (parsedDate != null) {
                            cal.time = parsedDate
                            
                            val isSun = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                            val isHol = com.example.data.SalaryCalculator.isHoliday(d)
                            
                            if (skipSunday && isSun) return@forEach
                            if (skipHoliday && isHol) return@forEach

                            when (action) {
                                "DELETE" -> {
                                    dbHelper.deleteAttendanceRecord(userId, d)
                                }
                                "PAID_LEAVE" -> {
                                    val r = AttendanceRecord(
                                        id = attendanceMap[d]?.id ?: 0,
                                        uid = userId,
                                        dateString = d,
                                        clockInTime = 0L,
                                        clockOutTime = null,
                                        status = "PAID_LEAVE",
                                        notes = "Nghỉ phép có lương"
                                    )
                                    recordsList.add(r)
                                }
                                "UNPAID_LEAVE" -> {
                                    val r = AttendanceRecord(
                                        id = attendanceMap[d]?.id ?: 0,
                                        uid = userId,
                                        dateString = d,
                                        clockInTime = 0L,
                                        clockOutTime = null,
                                        status = "UNPAID_LEAVE",
                                        notes = "Nghỉ không lương"
                                    )
                                    recordsList.add(r)
                                }
                                "UNAUTHORIZED_LEAVE" -> {
                                    val r = AttendanceRecord(
                                        id = attendanceMap[d]?.id ?: 0,
                                        uid = userId,
                                        dateString = d,
                                        clockInTime = 0L,
                                        clockOutTime = null,
                                        status = "UNAUTHORIZED_LEAVE",
                                        notes = "Nghỉ không phép"
                                    )
                                    recordsList.add(r)
                                }
                                "HOLIDAY_LEAVE" -> {
                                    val r = AttendanceRecord(
                                        id = attendanceMap[d]?.id ?: 0,
                                        uid = userId,
                                        dateString = d,
                                        clockInTime = 0L,
                                        clockOutTime = null,
                                        status = "HOLIDAY_LEAVE",
                                        notes = "Nghỉ lễ có lương"
                                    )
                                    recordsList.add(r)
                                }
                                else -> {
                                    val cIn = cal.clone() as Calendar
                                    cIn.set(Calendar.HOUR_OF_DAY, inHour)
                                    cIn.set(Calendar.MINUTE, inMin)
                                    cIn.set(Calendar.SECOND, 0)
                                    cIn.set(Calendar.MILLISECOND, 0)

                                    val cOut = cal.clone() as Calendar
                                    cOut.set(Calendar.HOUR_OF_DAY, outHour)
                                    cOut.set(Calendar.MINUTE, outMin)
                                    cOut.set(Calendar.SECOND, 0)
                                    cOut.set(Calendar.MILLISECOND, 0)
                                    
                                    if (cOut.timeInMillis <= cIn.timeInMillis) {
                                        cOut.add(Calendar.DAY_OF_MONTH, 1)
                                    }

                                    val r = AttendanceRecord(
                                        id = attendanceMap[d]?.id ?: 0,
                                        uid = userId,
                                        dateString = d,
                                        clockInTime = cIn.timeInMillis,
                                        clockOutTime = cOut.timeInMillis,
                                        status = "Completed"
                                    )
                                    recordsList.add(r)
                                }
                            }
                        }
                    }
                    if (recordsList.isNotEmpty()) {
                        dbHelper.insertManualRecords(recordsList)
                    }
                    onRecordsChanged()
                }
            }
        )
    }
}

@Composable
fun SingleDayEntryDialog(
    dateStr: String,
    initialRecord: AttendanceRecord?,
    onDismiss: () -> Unit,
    onSave: (Long, Long?, String) -> Unit
) {
    var inHour by remember { mutableStateOf(TextFieldValue(initialRecord?.let { Calendar.getInstance().apply { timeInMillis = it.clockInTime }.get(Calendar.HOUR_OF_DAY).toString() } ?: "08")) }
    var inMin by remember { mutableStateOf(TextFieldValue(initialRecord?.let { Calendar.getInstance().apply { timeInMillis = it.clockInTime }.get(Calendar.MINUTE).toString() } ?: "00")) }
    
    var outHour by remember { mutableStateOf(TextFieldValue(initialRecord?.clockOutTime?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY).toString() } ?: "")) }
    var outMin by remember { mutableStateOf(TextFieldValue(initialRecord?.clockOutTime?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE).toString() } ?: "")) }

    val focusRequesters = remember { List(4) { FocusRequester() } }
    
    var selectedStatus by remember { mutableStateOf(initialRecord?.status ?: "Completed") }

    fun createTextField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, label: String, focusRequester: FocusRequester, nextFocusRequester: FocusRequester? = null, modifier: Modifier = Modifier): @Composable () -> Unit = {
        val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()
        
        LaunchedEffect(isFocused) {
            if (isFocused) {
                kotlinx.coroutines.delay(50)
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
            interactionSource = interactionSource,
            modifier = modifier.focusRequester(focusRequester),
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = if (nextFocusRequester != null) ImeAction.Next else ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Chấm công ngày $dateStr", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Trạng thái công", color = Color.LightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStatus == "Completed" || selectedStatus == "Active" || selectedStatus == "NORMAL",
                            onClick = { selectedStatus = "Completed" }
                        )
                        Text("Đi làm", color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        RadioButton(
                            selected = selectedStatus == "PaidLeave" || selectedStatus == "PAID_LEAVE",
                            onClick = { selectedStatus = "PAID_LEAVE" }
                        )
                        Text("Nghỉ có lương", color = Color.White, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStatus == "UnpaidLeave" || selectedStatus == "UNPAID_LEAVE",
                            onClick = { selectedStatus = "UNPAID_LEAVE" }
                        )
                        Text("Nghỉ không lương", color = Color(0xFFFF9800), fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        RadioButton(
                            selected = selectedStatus == "UNAUTHORIZED_LEAVE" || selectedStatus == "KP",
                            onClick = { selectedStatus = "UNAUTHORIZED_LEAVE" }
                        )
                        Text("Nghỉ không phép", color = Color(0xFFEB5757), fontSize = 13.sp)
                    }
                }

                val isNearHoliday = com.example.data.SalaryCalculator.isNearHolidayWindow(dateStr, daysRange = 3)
                if (isNearHoliday) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStatus == "PaidHolidayLeave" || selectedStatus == "HOLIDAY_LEAVE",
                            onClick = { selectedStatus = "HOLIDAY_LEAVE" }
                        )
                        Text("Nghỉ lễ có lương (Cty cho nghỉ)", color = Color(0xFFBB6BD9), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Conditionally display hour input only for actual work shifts
                val isWorkingStatus = selectedStatus != "PaidLeave" && 
                                     selectedStatus != "PAID_LEAVE" && 
                                     selectedStatus != "UnpaidLeave" && 
                                     selectedStatus != "UNPAID_LEAVE" && 
                                     selectedStatus != "UNAUTHORIZED_LEAVE" && 
                                     selectedStatus != "PaidHolidayLeave" && 
                                     selectedStatus != "HOLIDAY_LEAVE"

                if (isWorkingStatus) {
                    Text("Giờ Vào Ca", color = Color.LightGray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        createTextField(inHour, { 
                            if (it.text.length <= 2) {
                                val h = it.text.toIntOrNull() ?: 0
                                if (h > 24) {
                                    inHour = it.copy(text = "07", selection = TextRange(2))
                                    inMin = inMin.copy(text = "30", selection = TextRange(2))
                                } else if (h == 24 && (inMin.text.toIntOrNull() ?: 0) > 0) {
                                    inHour = it.copy(text = "07", selection = TextRange(2))
                                    inMin = inMin.copy(text = "30", selection = TextRange(2))
                                } else {
                                    inHour = it 
                                }
                            }
                        }, "Giờ (0-23)", focusRequesters[0], focusRequesters[1], Modifier.weight(1f))()
                        createTextField(inMin, { 
                            if (it.text.length <= 2) {
                                val m = it.text.toIntOrNull() ?: 0
                                if (m > 59) {
                                    inMin = it.copy(text = "59", selection = TextRange(2))
                                } else if (m > 0 && (inHour.text.toIntOrNull() ?: 0) == 24) {
                                    inHour = inHour.copy(text = "07", selection = TextRange(2))
                                    inMin = it.copy(text = "30", selection = TextRange(2))
                                } else {
                                    inMin = it
                                }
                            }
                        }, "Phút (0-59)", focusRequesters[1], focusRequesters[2], Modifier.weight(1f))()
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Giờ Ra Ca (Để trống nếu quên chấm ra)", color = Color.LightGray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        createTextField(outHour, { 
                            if (it.text.length <= 2) {
                                val h = it.text.toIntOrNull() ?: 0
                                if (h > 24) {
                                    outHour = it.copy(text = "07", selection = TextRange(2))
                                    outMin = outMin.copy(text = "30", selection = TextRange(2))
                                } else if (h == 24 && (outMin.text.toIntOrNull() ?: 0) > 0) {
                                    outHour = it.copy(text = "07", selection = TextRange(2))
                                    outMin = outMin.copy(text = "30", selection = TextRange(2))
                                } else {
                                    outHour = it
                                }
                            }
                        }, "Giờ", focusRequesters[2], focusRequesters[3], Modifier.weight(1f))()
                        createTextField(outMin, { 
                            if (it.text.length <= 2) {
                                val m = it.text.toIntOrNull() ?: 0
                                if (m > 59) {
                                    outMin = it.copy(text = "59", selection = TextRange(2))
                                } else if (m > 0 && (outHour.text.toIntOrNull() ?: 0) == 24) {
                                    outHour = outHour.copy(text = "07", selection = TextRange(2))
                                    outMin = it.copy(text = "30", selection = TextRange(2))
                                } else {
                                    outMin = it
                                }
                            }
                        }, "Phút", focusRequesters[3], null, Modifier.weight(1f))()
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                    ) {
                        Text(
                            text = "Hệ thống sẽ ghi nhận công ngày nghỉ hưởng 100% lương ngày cơ bản tiêu chuẩn.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (initialRecord != null) {
                        TextButton(
                            onClick = { onSave(0L, null, "DELETE") }
                        ) {
                            Text("Xóa Ngày Này", color = Color(0xFFFF5252))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row {
                        TextButton(onClick = onDismiss) { Text("Hủy", color = Color.LightGray) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                try {
                                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    val date = sdf.parse(dateStr) ?: Date()
                                    
                                    val cin = Calendar.getInstance()
                                    cin.time = date
                                    cin.set(Calendar.HOUR_OF_DAY, inHour.text.toIntOrNull() ?: 8)
                                    cin.set(Calendar.MINUTE, inMin.text.toIntOrNull() ?: 0)

                                    val coutHour = outHour.text.toIntOrNull()
                                    val coutMin = outMin.text.toIntOrNull()
                                    
                                    var coutMillis: Long? = null
                                    if (coutHour != null && coutMin != null) {
                                        val cout = Calendar.getInstance()
                                        cout.time = date
                                        cout.set(Calendar.HOUR_OF_DAY, coutHour)
                                        cout.set(Calendar.MINUTE, coutMin)
                                        if (cout.timeInMillis <= cin.timeInMillis) {
                                            cout.add(Calendar.DAY_OF_MONTH, 1)
                                        }
                                        coutMillis = cout.timeInMillis
                                    }

                                    val isLeave = selectedStatus == "PaidLeave" || 
                                                  selectedStatus == "PAID_LEAVE" || 
                                                  selectedStatus == "UnpaidLeave" || 
                                                  selectedStatus == "UNPAID_LEAVE" || 
                                                  selectedStatus == "UNAUTHORIZED_LEAVE" || 
                                                  selectedStatus == "KP" ||
                                                  selectedStatus == "PaidHolidayLeave" || 
                                                  selectedStatus == "HOLIDAY_LEAVE" ||
                                                  com.example.data.SalaryCalculator.isLeaveType(selectedStatus)

                                    val finalStatus = if (isLeave) {
                                        when (selectedStatus) {
                                            "PaidLeave", "PAID_LEAVE" -> "PAID_LEAVE"
                                            "UnpaidLeave", "UNPAID_LEAVE" -> "UNPAID_LEAVE"
                                            "UNAUTHORIZED_LEAVE", "KP" -> "UNAUTHORIZED_LEAVE"
                                            "PaidHolidayLeave", "HOLIDAY_LEAVE" -> "HOLIDAY_LEAVE"
                                            else -> selectedStatus
                                        }
                                    } else {
                                        if (coutHour != null && coutMin != null) "Completed" else "Active"
                                    }

                                    val finalCin = if (isLeave) 0L else cin.timeInMillis
                                    val finalCout = if (isLeave) null else coutMillis

                                    onSave(finalCin, finalCout, finalStatus)
                                } catch (e: Exception) {
                                    android.util.Log.e("SingleDayEntryDialog", "Failed to parse/save manual record", e)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71))
                        ) { Text("Lưu") }
                    }
                }
            }
        }
    }
}

@Composable
fun BatchEntryDialog(
    selectedDates: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Int, Int, Boolean, Boolean) -> Unit
) {
    var selectedAction by remember { mutableStateOf("WORK") }
    var inHour by remember { mutableStateOf(TextFieldValue("08")) }
    var inMin by remember { mutableStateOf(TextFieldValue("00")) }
    var outHour by remember { mutableStateOf(TextFieldValue("17")) }
    var outMin by remember { mutableStateOf(TextFieldValue("00")) }
    
    val focusRequesters = remember { List(4) { FocusRequester() } }
    
    var skipSunday by remember { mutableStateOf(false) }
    var skipHoliday by remember { mutableStateOf(false) }
    var autoRecognizeOT by remember { mutableStateOf(true) }

    androidx.compose.runtime.LaunchedEffect(skipSunday, skipHoliday) {
        if (skipSunday || skipHoliday) {
            autoRecognizeOT = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Chỉnh sửa hàng loạt (${selectedDates.size} ngày đã chọn)",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Hành động thực hiện:", color = Color.LightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedAction == "WORK", onClick = { selectedAction = "WORK" })
                        Text("Chấm công làm việc (Vào / Ra)", color = Color.White, fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedAction == "PAID_LEAVE", onClick = { selectedAction = "PAID_LEAVE" })
                        Text("Ghi nhận Nghỉ phép có lương", color = Color(0xFFF2C94C), fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedAction == "UNPAID_LEAVE", onClick = { selectedAction = "UNPAID_LEAVE" })
                        Text("Ghi nhận Nghỉ không lương", color = Color(0xFFFF9800), fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedAction == "UNAUTHORIZED_LEAVE", onClick = { selectedAction = "UNAUTHORIZED_LEAVE" })
                        Text("Ghi nhận Nghỉ không phép (Vắng)", color = Color(0xFFEB5757), fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedAction == "HOLIDAY_LEAVE", onClick = { selectedAction = "HOLIDAY_LEAVE" })
                        Text("Ghi nhận Nghỉ lễ có lương", color = Color(0xFFBB6BD9), fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedAction == "DELETE", onClick = { selectedAction = "DELETE" })
                        Text("Xóa dữ liệu chấm công các ngày chọn", color = Color(0xFFFF5252), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedAction == "WORK") {
                    fun createTextField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, label: String, focusRequester: FocusRequester, nextFocusRequester: FocusRequester? = null, modifier: Modifier = Modifier): @Composable () -> Unit = {
                        val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        val isFocused by interactionSource.collectIsFocusedAsState()
                        
                        LaunchedEffect(isFocused) {
                            if (isFocused) {
                                kotlinx.coroutines.delay(50)
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
                            interactionSource = interactionSource,
                            modifier = modifier.focusRequester(focusRequester),
                            label = { Text(label) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = if (nextFocusRequester != null) ImeAction.Next else ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.LightGray)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        createTextField(inHour, { inHour = it }, "Giờ Vào", focusRequesters[0], focusRequesters[1], Modifier.weight(1f))()
                        createTextField(inMin, { inMin = it }, "Phút Vào", focusRequesters[1], focusRequesters[2], Modifier.weight(1f))()
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        createTextField(outHour, { outHour = it }, "Giờ Ra", focusRequesters[2], focusRequesters[3], Modifier.weight(1f))()
                        createTextField(outMin, { outMin = it }, "Phút Ra", focusRequesters[3], null, Modifier.weight(1f))()
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { skipSunday = !skipSunday }) {
                        Checkbox(checked = skipSunday, onCheckedChange = { skipSunday = it })
                        Text("Bỏ qua ngày Chủ Nhật", color = Color.White, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { skipHoliday = !skipHoliday }) {
                        Checkbox(checked = skipHoliday, onCheckedChange = { skipHoliday = it })
                        Text("Bỏ qua Ngày Lễ", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Hủy", color = Color.LightGray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val iH = inHour.text.toIntOrNull() ?: 8
                            val iM = inMin.text.toIntOrNull() ?: 0
                            val oH = outHour.text.toIntOrNull() ?: 17
                            val oM = outMin.text.toIntOrNull() ?: 0
                            onSave(selectedAction, iH, iM, oH, oM, skipSunday, skipHoliday)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedAction == "DELETE") Color(0xFFFF5252) else Color(0xFF2ECC71)
                        )
                    ) { 
                        Text(if (selectedAction == "DELETE") "Xóa Hàng Loạt" else "Áp Dụng") 
                    }
                }
            }
        }
    }
}
