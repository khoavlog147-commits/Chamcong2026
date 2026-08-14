package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.auth.UserSession
import com.example.data.model.TimeEntry
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DangerRed
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkContainer
import com.example.ui.theme.LightGray
import com.example.ui.theme.MediumGray
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.White
import com.example.ui.theme.NightPurple
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.DividerColor
import com.example.viewmodel.TimeSnapViewModel
import kotlinx.coroutines.delay
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

private fun getCoeff(config: com.example.data.model.UserConfig?): Double {
    return when {
        Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY -> config?.heSoOtChuNhat ?: 2.0
        run {
            val sdfMonthDay = SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
            val md = sdfMonthDay.format(java.util.Date())
            md == "01-01" || md == "04-30" || md == "05-01" || md == "09-02"
        } -> config?.heSoOtNgayLe ?: 3.0
        else -> 1.0
    }
}

private fun getCoeffLabel(config: com.example.data.model.UserConfig?): String {
    return when {
        Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY -> " (Chủ nhật x2)"
        run {
            val sdfMonthDay = SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
            val md = sdfMonthDay.format(java.util.Date())
            md == "01-01" || md == "04-30" || md == "05-01" || md == "09-02"
        } -> " (Ngày lễ x3)"
        else -> ""
    }
}

@Composable
fun QuickStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkContainer),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(iconTint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(15.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SalaryMetricColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
    horizontalAlignment: Alignment.Horizontal = if (alignEnd) Alignment.End else Alignment.Start
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TimeSnapViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToNotifications: () -> Unit = {}
) {
    val context = LocalContext.current
    val notificationPrefs = remember(context) { context.getSharedPreferences("notification_prefs", android.content.Context.MODE_PRIVATE) }
    var notificationsEnabled by remember { mutableStateOf(notificationPrefs.getBoolean("notifications_enabled", true)) }
    var autoClockInOutEnabled by remember { mutableStateOf(notificationPrefs.getBoolean("auto_clock_in_out_enabled", false)) }
    var reminderMinutes by remember { mutableStateOf(notificationPrefs.getString("reminder_minutes_before", "15") ?: "15") }
    var showReminderDialog by remember { mutableStateOf(false) }

    var showNotificationConfigDialog by remember { mutableStateOf(false) }
    var showAutoCheckoutSetupDialog by remember { mutableStateOf(false) }
    var autoCheckoutEnabled by remember { mutableStateOf(notificationPrefs.getBoolean("auto_checkout_enabled", false)) }
    var customCheckoutTime by remember { mutableStateOf(notificationPrefs.getString("custom_checkout_time", "") ?: "") }

    // Refresh settings when returning to screen or dialog closes
    LaunchedEffect(showNotificationConfigDialog) {
        notificationsEnabled = notificationPrefs.getBoolean("notifications_enabled", true)
        autoClockInOutEnabled = notificationPrefs.getBoolean("auto_clock_in_out_enabled", false)
        autoCheckoutEnabled = notificationPrefs.getBoolean("auto_checkout_enabled", false)
        customCheckoutTime = notificationPrefs.getString("custom_checkout_time", "") ?: ""
    }
    val scope = rememberCoroutineScope()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val userSession by viewModel.currentUserSession.collectAsStateWithLifecycle()
    val activeEntry by viewModel.activeWorkingEntry.collectAsStateWithLifecycle()
    val runningTimeText by viewModel.runningDurationText.collectAsStateWithLifecycle()
    val summaryState by viewModel.salarySummaryState.collectAsStateWithLifecycle()
    val recentEntries by viewModel.monthTimeEntries.collectAsStateWithLifecycle()
    val configState by viewModel.userConfig.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.currentSelectedMonth.collectAsStateWithLifecycle()
    val unreadNotifCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()

    var liveTimeString by remember { mutableStateOf("") }
    var liveHMString by remember { mutableStateOf("") }
    var liveDateString by remember { mutableStateOf("") }

    // Live clock ticks
    LaunchedEffect(Unit) {
        val clockFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val clockHMFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, 'ngày' dd 'tháng' MM 'năm' yyyy", Locale("vi", "VN"))
        while (true) {
            val now = Date()
            liveTimeString = clockFormat.format(now)
            liveHMString = clockHMFormat.format(now)
            liveDateString = dateFormat.format(now).replaceFirstChar { it.uppercase() }
            delay(1000)
        }
    }

    var quickNoteText by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    var showRetroactiveDialog by remember { mutableStateOf(false) }
    LaunchedEffect(activeEntry) {
        quickNoteText = activeEntry?.note ?: ""
    }

    val isWorking = activeEntry != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "TIMESNAP PRO",
                            fontWeight = FontWeight.Black,
                            color = PrimaryBlue,
                            fontSize = 17.sp,
                            letterSpacing = 1.2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Hệ thống quản trị lương toàn diện",
                            color = TextSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        // Icon Trung tâm thông báo có đếm cờ chưa đọc
                        IconButton(
                            onClick = onNavigateToNotifications,
                            modifier = Modifier.testTag("btn_top_notif_center")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifCount > 0) {
                                        Badge(
                                            containerColor = AccentOrange,
                                            contentColor = White
                                        ) {
                                            Text(
                                                text = if (unreadNotifCount > 99) "99+" else "$unreadNotifCount",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (unreadNotifCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = "Trung tâm thông báo",
                                    tint = if (unreadNotifCount > 0) NeonBlue else TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                showNotificationConfigDialog = true
                            },
                            modifier = Modifier.testTag("btn_top_reminder_config")
                        ) {
                            val alarmIcon = when {
                                notificationsEnabled -> Icons.Default.AlarmOn
                                autoClockInOutEnabled -> Icons.Default.Alarm
                                else -> Icons.Default.AlarmOff
                            }
                            val alarmTint = when {
                                notificationsEnabled -> AccentOrange
                                autoClockInOutEnabled -> NeonBlue
                                else -> TextSecondary
                            }
                            val alarmDesc = when {
                                notificationsEnabled -> "Nhắc nhở chấm công: Đang bật"
                                autoClockInOutEnabled -> "Tự động chấm công: Đang bật"
                                else -> "Nhắc nhở: Đang tắt"
                            }
                            
                            Icon(
                                imageVector = alarmIcon,
                                contentDescription = alarmDesc,
                                tint = alarmTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        val userFullName = configState?.hoVaTen ?: userSession?.displayName ?: "TRƯƠNG VĂN KHOA"
                        val initials = remember(userFullName) {
                            userFullName.trim().split("\\s+".toRegex())
                                .takeLast(2)
                                .mapNotNull { it.firstOrNull()?.uppercase() }
                                .joinToString("")
                                .ifEmpty { "TK" }
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.2f))
                                .border(1.dp, PrimaryBlue.copy(alpha = 0.5f), CircleShape)
                                .clickable {
                                    viewModel.authController.signOut {
                                        onNavigateToLogin()
                                    }
                                }
                                .testTag("logout_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                color = PrimaryBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            
            // Welcome Header Block
            Spacer(modifier = Modifier.height(12.dp))
            val displayName = (configState?.hoVaTen ?: userSession?.displayName ?: "TRƯƠNG VĂN KHOA").uppercase()
            
            Text(
                text = "Xin chào,",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = displayName,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = liveDateString.ifEmpty { "Chủ nhật, ngày 26 tháng 07 năm 2026" },
                color = TextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // MONTH NAVIGATION BAR (< Tháng MM/yyyy >)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(DarkContainer, RoundedCornerShape(14.dp))
                    .border(1.dp, PrimaryBlue.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sdfMonth = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
                val currentMonthDate = remember(selectedMonth) {
                    try { sdfMonth.parse(selectedMonth) ?: Date() } catch (e: Exception) { Date() }
                }

                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        cal.time = currentMonthDate
                        cal.add(Calendar.MONTH, -1)
                        viewModel.selectMonth(sdfMonth.format(cal.time))
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Tháng trước",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }

                val displayMonthStr = remember(selectedMonth) {
                    try {
                        val d = sdfMonth.parse(selectedMonth) ?: Date()
                        val fmt = SimpleDateFormat("'Tháng' MM/yyyy", Locale("vi", "VN"))
                        fmt.format(d)
                    } catch (e: Exception) {
                        "Tháng $selectedMonth"
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = displayMonthStr,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        cal.time = currentMonthDate
                        cal.add(Calendar.MONTH, 1)
                        viewModel.selectMonth(sdfMonth.format(cal.time))
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "Tháng sau",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CARD LƯƠNG (SALARY CARD WITH SUBTLE GREEN GLOW BORDER)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkContainer),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, SuccessGreen.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                    .padding(bottom = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    val realSalary = summaryState?.luongThucNhan ?: 0.0
                    val formattedSalary = DecimalFormat("#,###").format(realSalary) + " đ"
                    val workingDays = summaryState?.workingDays ?: 0.0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = "THỰC LĨNH DỰ KIẾN (NET)",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formattedSalary,
                                color = SuccessGreen,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        // Right side: Progress Bar
                        Column(
                            modifier = Modifier.weight(0.8f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Tiến độ tháng",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${DecimalFormat("#.#").format(workingDays)} / 26 ngày",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (workingDays / 26.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SuccessGreen,
                                trackColor = SuccessGreen.copy(alpha = 0.15f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = DividerColor)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 Columns: Ngày công, OT tích lũy, Lương giờ (Dynamic rate based on day of week)
                    val totalOt = (summaryState?.otDayHours ?: 0.0) + 
                                  (summaryState?.otNightHours ?: 0.0) +
                                  (summaryState?.chuNhatHours ?: 0.0) +
                                  (summaryState?.otLeHours ?: 0.0)
                    val fmtOtStr = DecimalFormat("#.#").format(totalOt) + " giờ"

                    val totalHrs = (summaryState?.standardHours ?: 0.0) + totalOt
                    val fmtHrsStr = DecimalFormat("#.##").format(totalHrs) + " giờ"

                    val lcb = configState?.luongCoBan ?: 6000000.0
                    val baseHrRate = lcb / 26.0 / 8.0
                    val todayCoeff = getCoeff(configState)
                    val currentHrRate = baseHrRate * todayCoeff
                    val fmtHrRateStr = DecimalFormat("#,###").format(currentHrRate) + " đ"

                    val hrLabel = if (todayCoeff > 1.0) {
                        "Lương giờ (OT ${DecimalFormat("#.#").format(todayCoeff)})"
                    } else {
                        "Lương giờ"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SalaryMetricColumn(
                            label = "Ngày công",
                            value = "${DecimalFormat("#.#").format(workingDays)}/26",
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        )
                        SalaryMetricColumn(
                            label = "Tổng giờ",
                            value = fmtHrsStr,
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        )
                        SalaryMetricColumn(
                            label = "OT tích lũy",
                            value = fmtOtStr,
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        )
                        SalaryMetricColumn(
                            label = hrLabel,
                            value = fmtHrRateStr,
                            modifier = Modifier.weight(1.1f),
                            horizontalAlignment = Alignment.End
                        )
                    }
                }
            }

            // CARD CHẤM CÔNG (CHECK-IN TERMINAL CARD)
            val buttonColor by animateColorAsState(
                targetValue = if (isWorking) DangerRed else SuccessGreen,
                animationSpec = tween(300), label = ""
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkContainer),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val clockTimeDisplay = if (liveHMString.isNotEmpty()) liveHMString else "03:44"

                    // Status Badge & Clock in a balanced horizontal row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Status Badge & Subtitle
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        if (isWorking) DangerRed.copy(alpha = 0.12f) else PrimaryBlue.copy(alpha = 0.12f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isWorking) DangerRed else PrimaryBlue)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isWorking) "ĐANG TRONG CA" else "ĐANG NGOÀI CA",
                                    color = if (isWorking) DangerRed else PrimaryBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isWorking) (if (runningTimeText.isNotEmpty()) runningTimeText else "Đang tính giờ ca làm...") else "Bắt đầu làm việc",
                                color = if (isWorking) AccentOrange else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                             )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Right: Big Clock showing HH:mm without seconds
                        Text(
                            text = clockTimeDisplay,
                            color = TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.End,
                            letterSpacing = 1.5.sp,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Hero Round Fingerprint Button centered in the middle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            buttonColor,
                                            buttonColor.copy(alpha = 0.4f),
                                            buttonColor.copy(alpha = 0.1f)
                                        )
                                    )
                                )
                                .clickable {
                                    viewModel.toggleCheckIn(quickNoteText, autoCheckoutEnabled, customCheckoutTime)
                                    if (!isWorking && autoCheckoutEnabled) {
                                        val msg = if (customCheckoutTime.isNotBlank()) "🤖 Đã bật Tự động ra ca lúc $customCheckoutTime" else "🤖 Đã bật Tự động ra ca (Tự học từ lịch sử cũ)"
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                                .testTag("in_out_action_button")
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Chấm công vân tay",
                                    tint = White,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isWorking) "RA CA" else "VÀO CA",
                                    color = White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // Estimated earnings if working
                    val active = activeEntry
                    if (isWorking && active != null && active.checkInTime != null) {
                        val lcb = configState?.luongCoBan ?: 6000000.0
                        val hrRate = lcb / 26.0 / 8.0
                        val checkInVal = active.checkInTime
                        val elapsedMs = remember(liveTimeString) { System.currentTimeMillis() - checkInVal }
                        val elapsedHours = (elapsedMs.coerceAtLeast(0L)) / 3600000.0
                        val breakHours = if (configState?.tinhKhauTruNghi == true) (configState?.soGioNghiGiaiLao ?: 1.5) else 0.0
                        val actualElapsedHours = (elapsedHours - breakHours).coerceAtLeast(0.0)

                        val earnings = if (actualElapsedHours <= 8.0) {
                            actualElapsedHours * hrRate
                        } else {
                            val stdPay = 8.0 * hrRate
                            val otPay = (actualElapsedHours - 8.0) * hrRate * 1.5
                            stdPay + otPay
                        }

                        val decFmt = DecimalFormat("#,###")
                        val formattedEarned = decFmt.format(earnings) + " đ"

                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "ƯỚC TÍNH SỐ TIỀN ĐÃ LÀM ĐƯỢC CA NÀY",
                                    color = SuccessGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formattedEarned,
                                    color = SuccessGreen,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Cập nhật lần cuối ${clockTimeDisplay}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // GHI CHÚ NHANH CA LÀM
                    OutlinedTextField(
                        value = quickNoteText,
                        onValueChange = { 
                            quickNoteText = it
                            if (isWorking) {
                                viewModel.updateActiveEntryNote(it)
                            }
                        },
                        label = { Text("Ghi chú nhanh ca làm...", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = CardBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // State for Dialog
            var showMonthlyChartDialog by remember { mutableStateOf(false) }

            // BIỂU ĐỒ (MONTHLY ACTIVITY CHART CARD)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkContainer),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .clickable { showMonthlyChartDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = PrimaryBlue.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Biểu đồ hoạt động tháng",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Xem chi tiết giờ làm việc",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // --- MONTHLY WORK HOURS POPUP DIALOG ---
            if (showMonthlyChartDialog) {
                val monthlyData = remember(recentEntries, selectedMonth) {
                    calculateMonthlyChartData(selectedMonth, recentEntries)
                }
                
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showMonthlyChartDialog = false },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(20.dp),
                        color = DarkContainer,
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.BarChart, contentDescription = null, tint = PrimaryBlue)
                                Text(
                                    "Biểu Đồ Giờ Làm Trong Tháng",
                                    color = White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            MonthlyActivityChart(data = monthlyData, selectedMonth = selectedMonth)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showMonthlyChartDialog = false }) {
                                    Text("Đóng", color = NeonBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // LỊCH SỬ CHẤM CÔNG (ATTENDANCE HISTORY CARD)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkContainer),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isExpanded) "CHẤM CÔNG TRONG THÁNG" else "CHẤM CÔNG GẦN ĐÂY",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isExpanded) "Thu gọn" else "Xem tất cả",
                                color = PrimaryBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ChevronRight,
                                contentDescription = "Toggle Expand",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val todayStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
                    val sortedLogs = remember(recentEntries, todayStr, isExpanded, selectedMonth) {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val todayTime = try { sdf.parse(todayStr)?.time ?: Long.MAX_VALUE } catch(e: Exception) { Long.MAX_VALUE }
                        val filtered = recentEntries.filter { 
                            try {
                                val t = (if (it.date.contains("/")) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())).parse(it.date)?.time ?: 0L
                                t <= todayTime
                            } catch(e: Exception) {
                                true
                            }
                        }
                        if (isExpanded) {
                            filtered
                                .filter { com.example.util.ExportUtils.isRecordInMonth(it.date, selectedMonth) }
                                .sortedWith { a, b ->
                                    try {
                                        val ta = (if (a.date.contains("/")) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())).parse(a.date)?.time ?: 0L
                                        val tb = (if (b.date.contains("/")) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())).parse(b.date)?.time ?: 0L
                                        tb.compareTo(ta)
                                    } catch(e: Exception) {
                                        b.date.compareTo(a.date)
                                    }
                                }
                        } else {
                            filtered
                                .sortedWith { a, b ->
                                    try {
                                        val ta = (if (a.date.contains("/")) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())).parse(a.date)?.time ?: 0L
                                        val tb = (if (b.date.contains("/")) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())).parse(b.date)?.time ?: 0L
                                        tb.compareTo(ta)
                                    } catch(e: Exception) {
                                        b.date.compareTo(a.date)
                                    }
                                }
                                .take(3)
                        }
                    }

                    if (sortedLogs.isEmpty()) {
                        Text(
                            text = "Chưa có dữ liệu chấm công gần đây.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        sortedLogs.forEachIndexed { index, entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val formattedDate = remember(entry.date) {
                                        try {
                                            val parser = if (entry.date.contains("/")) {
                                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                            } else {
                                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            }
                                            val dateVal = parser.parse(entry.date)
                                            if (dateVal != null) {
                                                val formatter = SimpleDateFormat("EEEE, dd/MM", Locale("vi", "VN"))
                                                formatter.format(dateVal).replaceFirstChar { it.uppercase() }
                                            } else {
                                                entry.date
                                            }
                                        } catch (e: Exception) {
                                            entry.date
                                        }
                                    }

                                    val processedEntry = remember(entry, configState) {
                                        com.example.data.SalaryCalculator.calculateSingleEntry(entry, configState)
                                    }
                                    val shift = remember(entry) {
                                        com.example.data.SalaryCalculator.getShiftForEntry(entry)
                                    }
                                    val isNightShift = remember(shift, entry) {
                                        shift.shiftType == "NIGHT" || shift.shiftId == "ca_dem"
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = formattedDate,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Surface(
                                            color = if (isNightShift) NightPurple.copy(alpha = 0.25f) else AccentOrange.copy(alpha = 0.18f),
                                            border = BorderStroke(1.dp, if (isNightShift) NightPurple.copy(alpha = 0.6f) else AccentOrange.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(
                                                    text = if (isNightShift) "Ca đêm" else "Ca ngày",
                                                    color = if (isNightShift) Color(0xFFE9D5FF) else Color(0xFFFFEDD5),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    val shiftTimeStr = remember(entry) {
                                        if (entry.checkInTime != null && entry.checkOutTime != null) {
                                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                            val inStr = timeFormat.format(Date(entry.checkInTime))
                                            val outStr = timeFormat.format(Date(entry.checkOutTime))
                                            "$inStr → $outStr"
                                        } else "Ca thường"
                                    }

                                    val stdHrs = processedEntry.workDay * shift.standardHours
                                    val otHrs = processedEntry.otHours
                                    val shiftName = if (isNightShift) "Ca đêm" else "Ca ngày"

                                    Text(
                                        text = "$shiftName ($shiftTimeStr) • ${processedEntry.workDay} công • ${DecimalFormat("#.#").format(stdHrs)}g" +
                                                (if (otHrs > 0) " • OT ${DecimalFormat("#.#").format(otHrs)}g" else ""),
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    val earnings = remember(entry, configState) {
                                        calculateDayEarnings(entry, configState)
                                    }
                                    val formattedEarnings = DecimalFormat("#,###").format(earnings)

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$formattedEarnings đ",
                                            color = SuccessGreen,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            if (index < sortedLogs.lastIndex) {
                                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showRetroactiveDialog) {
        val context = LocalContext.current
        RetroactiveCheckInDialog(
            onDismiss = { showRetroactiveDialog = false },
            onSubmit = { dateStr, inH, inM, outH, outM, noteStr ->
                viewModel.addSingleEntry(
                    dateStr = dateStr,
                    checkInHour = inH,
                    checkInMin = inM,
                    checkOutHour = outH,
                    checkOutMin = outM,
                    dayTypeOverride = null,
                    noteStr = noteStr
                )
                Toast.makeText(context, "Đã bù chấm công ngày $dateStr thành công!", Toast.LENGTH_SHORT).show()
                showRetroactiveDialog = false
            }
        )
    }

    if (showAutoCheckoutSetupDialog) {
        val context = LocalContext.current
        var localTimeTf by remember { mutableStateOf(TextFieldValue(customCheckoutTime)) }

        AlertDialog(
            onDismissRequest = { showAutoCheckoutSetupDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "CÀI ĐẶT TỰ ĐỘNG RA CA",
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bật tự động ra ca:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                        Switch(
                            checked = autoCheckoutEnabled,
                            onCheckedChange = {
                                autoCheckoutEnabled = it
                                notificationPrefs.edit().putBoolean("auto_checkout_enabled", it).apply()
                            }
                        )
                    }

                    OutlinedTextField(
                        value = localTimeTf,
                        onValueChange = { newVal ->
                            val digits = newVal.text.filter { it.isDigit() }.take(4)
                            val formatted = when {
                                digits.length >= 3 -> {
                                    var hours = digits.substring(0, 2)
                                    val h = hours.toIntOrNull() ?: 0
                                    if (h > 24) hours = "24"
                                    var minutes = digits.substring(2)
                                    if (hours == "24" && minutes.isNotEmpty()) {
                                        minutes = "00".take(minutes.length)
                                    } else {
                                        val m = minutes.toIntOrNull() ?: 0
                                        if (m > 59) minutes = "59"
                                    }
                                    "$hours:$minutes"
                                }
                                digits.length == 2 -> {
                                    val h = digits.toIntOrNull() ?: 0
                                    if (h > 24) "24" else digits
                                }
                                else -> digits
                            }
                            localTimeTf = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                        },
                        label = { Text("Giờ ra ca", fontSize = 12.sp) },
                        placeholder = { Text("Để trống để tự học", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        )
                    )


                }
            },
            confirmButton = {
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        val trimmed = localTimeTf.text.trim()
                        customCheckoutTime = trimmed
                        notificationPrefs.edit()
                            .putBoolean("auto_checkout_enabled", autoCheckoutEnabled)
                            .putString("custom_checkout_time", trimmed)
                            .apply()
                        showAutoCheckoutSetupDialog = false
                        val statusMsg = if (autoCheckoutEnabled) {
                            if (trimmed.isNotBlank()) "Đã lưu hẹn giờ ra ca lúc $trimmed" else "Đã lưu tự động ra ca (tự học lịch sử)"
                        } else "Đã tắt tự động ra ca"
                        Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Lưu cài đặt", color = White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutoCheckoutSetupDialog = false }) {
                    Text("Hủy", color = LightGray)
                }
            }
        )
    }

    if (showNotificationConfigDialog) {
        val notificationPrefs = remember(context) { context.getSharedPreferences("notification_prefs", android.content.Context.MODE_PRIVATE) }
        var notificationsEnabled by remember { mutableStateOf(notificationPrefs.getBoolean("notifications_enabled", true)) }
        var reminderMinutes by remember { mutableStateOf(notificationPrefs.getString("reminder_minutes_before", "15") ?: "15") }
        
        var autoClockInOutEnabled by remember { mutableStateOf(notificationPrefs.getBoolean("auto_clock_in_out_enabled", false)) }
        var customCheckInTime by remember { mutableStateOf(notificationPrefs.getString("custom_check_in_time", "") ?: "") }
        var customCheckoutTime by remember { mutableStateOf(notificationPrefs.getString("custom_checkout_time", "") ?: "") }

        var estimatedInTime by remember { mutableStateOf("Đang tính...") }
        var estimatedOutTime by remember { mutableStateOf("Đang tính...") }

        LaunchedEffect(Unit) {
            userSession?.let { session ->
                val inMs = com.example.notification.NotificationHelper.estimateHistoricalCheckInTime(context, session.uid)
                estimatedInTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(inMs))
                
                // Giả lập ca active bằng giờ vào ca dự kiến để tính giờ ra ca chính xác
                val mockEntry = com.example.data.model.TimeEntry(userId = session.uid, date = "", checkInTime = inMs)
                val outMs = com.example.notification.NotificationHelper.estimateHistoricalCheckoutTime(context, session.uid, mockEntry)
                estimatedOutTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(outMs))
            }
        }

        AlertDialog(
            onDismissRequest = { showNotificationConfigDialog = false },
            containerColor = DarkContainer,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AlarmOn,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NHẮC NHỞ CHẤM CÔNG",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }
            },
            text = {
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Thông báo nhắc nhở
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Thông báo nhắc nhở", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { isEnabled ->
                                notificationsEnabled = isEnabled
                                val editor = notificationPrefs.edit()
                                    .putBoolean("notifications_enabled", isEnabled)
                                    .putBoolean("smart_learning_enabled", isEnabled)
                                
                                if (isEnabled) {
                                    autoClockInOutEnabled = false
                                    editor.putBoolean("auto_clock_in_out_enabled", false)
                                    editor.putBoolean("auto_check_in_enabled", false)
                                    editor.putBoolean("auto_checkout_enabled", false)
                                }
                                editor.apply()
                                
                                userSession?.let { session ->
                                    if (isEnabled) {
                                        com.example.notification.NotificationHelper.scheduleNextCheckInReminder(context, session.uid)
                                        com.example.notification.NotificationHelper.cancelAutoCheckIn(context, session.uid)
                                    } else {
                                        com.example.notification.NotificationHelper.cancelCheckOutReminder(context, session.uid)
                                        androidx.work.WorkManager.getInstance(context).cancelUniqueWork("checkin_reminder_${session.uid}")
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = White,
                                checkedTrackColor = PrimaryBlue
                            )
                        )
                    }

                    if (notificationsEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showReminderDialog = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Số phút nhắc trước ca", color = LightGray, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "$reminderMinutes phút", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = LightGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.Gray.copy(alpha = 0.2f)))

                    // 2. Tự động Vào/Ra Ca
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "🤖 Tự động vào/ra ca", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = autoClockInOutEnabled,
                            onCheckedChange = { isEnabled ->
                                autoClockInOutEnabled = isEnabled
                                val editor = notificationPrefs.edit()
                                    .putBoolean("auto_clock_in_out_enabled", isEnabled)
                                    .putBoolean("auto_check_in_enabled", isEnabled)
                                    .putBoolean("auto_checkout_enabled", isEnabled)
                                
                                if (isEnabled) {
                                    notificationsEnabled = false
                                    editor.putBoolean("notifications_enabled", false)
                                    userSession?.let { session ->
                                        com.example.notification.NotificationHelper.cancelCheckOutReminder(context, session.uid)
                                        androidx.work.WorkManager.getInstance(context).cancelUniqueWork("checkin_reminder_${session.uid}")
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val targetMs = com.example.notification.NotificationHelper.estimateHistoricalCheckInTime(context, session.uid)
                                            com.example.notification.NotificationHelper.scheduleAutoCheckIn(context, session.uid, targetMs)
                                            
                                            // Nếu đang làm việc, đặt lịch ra ca tự động luôn
                                            val currentActive = viewModel.activeWorkingEntry.value
                                            if (currentActive != null && currentActive.isWorking) {
                                                val outMs = com.example.notification.NotificationHelper.estimateHistoricalCheckoutTime(context, session.uid, currentActive)
                                                com.example.notification.NotificationHelper.scheduleAutoCheckOut(context, session.uid, outMs)
                                            }
                                        }
                                    }
                                } else {
                                    userSession?.let { session ->
                                        com.example.notification.NotificationHelper.cancelAutoCheckIn(context, session.uid)
                                        com.example.notification.NotificationHelper.cancelAutoCheckOut(context, session.uid)
                                    }
                                }
                                editor.apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = White,
                                checkedTrackColor = PrimaryBlue
                            )
                        )
                    }

                    if (autoClockInOutEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            var inTimeTf by remember { mutableStateOf(TextFieldValue(customCheckInTime, selection = TextRange(customCheckInTime.length))) }
                            OutlinedTextField(
                                value = inTimeTf,
                                onValueChange = { newVal ->
                                    val raw = newVal.text
                                    if (raw.isEmpty()) {
                                        inTimeTf = newVal
                                        customCheckInTime = ""
                                        notificationPrefs.edit().putString("custom_check_in_time", "").apply()
                                        return@OutlinedTextField
                                    }
                                    val digits = raw.filter { it.isDigit() }.take(4)
                                    val formatted = when {
                                        digits.length >= 3 -> {
                                            var hours = digits.substring(0, 2)
                                            val h = hours.toIntOrNull() ?: 0
                                            if (h > 24) hours = "24"
                                            var minutes = digits.substring(2)
                                            if (hours == "24" && minutes.isNotEmpty()) {
                                                minutes = "00".take(minutes.length)
                                            } else {
                                                val m = minutes.toIntOrNull() ?: 0
                                                if (m > 59) minutes = "59"
                                            }
                                            "$hours:$minutes"
                                        }
                                        digits.length == 2 -> {
                                            val h = digits.toIntOrNull() ?: 0
                                            if (h > 24) "24" else digits
                                        }
                                        else -> digits
                                    }
                                    inTimeTf = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                                    customCheckInTime = formatted
                                    notificationPrefs.edit().putString("custom_check_in_time", formatted).apply()
                                    if (formatted.length == 5 || formatted.isEmpty()) {
                                        userSession?.let { session ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                val targetMs = com.example.notification.NotificationHelper.estimateHistoricalCheckInTime(context, session.uid)
                                                com.example.notification.NotificationHelper.scheduleAutoCheckIn(context, session.uid, targetMs)
                                            }
                                        }
                                    }
                                },
                                label = { Text("Giờ vào ca", fontSize = 11.sp, color = LightGray) },
                                placeholder = { Text(text = estimatedInTime, fontSize = 14.sp, color = LightGray.copy(alpha = 0.4f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White)
                            )

                            // Input Giờ Ra
                            var outTimeTf by remember { mutableStateOf(TextFieldValue(customCheckoutTime, selection = TextRange(customCheckoutTime.length))) }
                            OutlinedTextField(
                                value = outTimeTf,
                                onValueChange = { newVal ->
                                    val raw = newVal.text
                                    if (raw.isEmpty()) {
                                        outTimeTf = newVal
                                        customCheckoutTime = ""
                                        notificationPrefs.edit().putString("custom_checkout_time", "").apply()
                                        return@OutlinedTextField
                                    }
                                    val digits = raw.filter { it.isDigit() }.take(4)
                                    val formatted = when {
                                        digits.length >= 3 -> {
                                            var hours = digits.substring(0, 2)
                                            val h = hours.toIntOrNull() ?: 0
                                            if (h > 24) hours = "24"
                                            var minutes = digits.substring(2)
                                            if (hours == "24" && minutes.isNotEmpty()) {
                                                minutes = "00".take(minutes.length)
                                            } else {
                                                val m = minutes.toIntOrNull() ?: 0
                                                if (m > 59) minutes = "59"
                                            }
                                            "$hours:$minutes"
                                        }
                                        digits.length == 2 -> {
                                            val h = digits.toIntOrNull() ?: 0
                                            if (h > 24) "24" else digits
                                        }
                                        else -> digits
                                    }
                                    outTimeTf = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                                    customCheckoutTime = formatted
                                    notificationPrefs.edit().putString("custom_checkout_time", formatted).apply()
                                    if (formatted.length == 5 || formatted.isEmpty()) {
                                        userSession?.let { session ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                val currentActive = viewModel.activeWorkingEntry.value
                                                if (currentActive != null && currentActive.isWorking) {
                                                    val targetMs = com.example.notification.NotificationHelper.estimateHistoricalCheckoutTime(context, session.uid, currentActive)
                                                    com.example.notification.NotificationHelper.scheduleAutoCheckOut(context, session.uid, targetMs)
                                                }
                                            }
                                        }
                                    }
                                },
                                label = { Text("Giờ ra ca", fontSize = 11.sp, color = LightGray) },
                                placeholder = { Text(text = estimatedOutTime, fontSize = 14.sp, color = LightGray.copy(alpha = 0.4f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White)
                            )

                            // Input Chu kỳ đổi ca (tuần)
                            var shiftRotationWeeks by remember { mutableStateOf(notificationPrefs.getInt("shift_rotation_weeks", 2)) }
                            var rotationWeeksTf by remember { mutableStateOf(TextFieldValue(shiftRotationWeeks.toString())) }
                            OutlinedTextField(
                                value = rotationWeeksTf,
                                onValueChange = { newVal ->
                                    val digits = newVal.text.filter { it.isDigit() }
                                    if (digits.isEmpty()) {
                                        rotationWeeksTf = newVal
                                        return@OutlinedTextField
                                    }
                                    var num = digits.toIntOrNull() ?: 2
                                    if (num < 0) num = 0
                                    if (num > 5) num = 5
                                    val formatted = num.toString()
                                    rotationWeeksTf = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                                    shiftRotationWeeks = num
                                    val editor = notificationPrefs.edit()
                                    editor.putInt("shift_rotation_weeks", num)
                                    if (notificationPrefs.getLong("shift_anchor_time", 0L) <= 0L) {
                                        editor.putLong("shift_anchor_time", System.currentTimeMillis())
                                    }
                                    editor.apply()

                                    userSession?.let { session ->
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val inMs = com.example.notification.NotificationHelper.estimateHistoricalCheckInTime(context, session.uid)
                                            estimatedInTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(inMs))
                                            val mockEntry = com.example.data.model.TimeEntry(userId = session.uid, date = "", checkInTime = inMs)
                                            val outMs = com.example.notification.NotificationHelper.estimateHistoricalCheckoutTime(context, session.uid, mockEntry)
                                            estimatedOutTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(outMs))
                                            
                                            if (autoClockInOutEnabled) {
                                                com.example.notification.NotificationHelper.scheduleAutoCheckIn(context, session.uid, inMs)
                                            }
                                        }
                                    }
                                },
                                label = { Text("Chu kỳ đổi ca (tuần)", fontSize = 11.sp, color = LightGray) },
                                placeholder = { Text("2", fontSize = 14.sp, color = LightGray.copy(alpha = 0.4f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White)
                            )

                            // Cấu hình Ngày mốc và Ca mốc bắt đầu
                            var shiftAnchorTime by remember { mutableStateOf(notificationPrefs.getLong("shift_anchor_time", System.currentTimeMillis())) }
                            var shiftAnchorType by remember { mutableStateOf(notificationPrefs.getString("shift_anchor_type", "DAY") ?: "DAY") }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ngày mốc bắt đầu:", fontSize = 12.sp, color = LightGray, fontWeight = FontWeight.Medium)
                                val anchorDateStr = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi", "VN")).format(Date(shiftAnchorTime))
                                OutlinedButton(
                                    onClick = {
                                        val curCal = Calendar.getInstance().apply { timeInMillis = shiftAnchorTime }
                                        android.app.DatePickerDialog(
                                            context,
                                            { _, yr, mo, dy ->
                                                val c = Calendar.getInstance()
                                                c.set(yr, mo, dy)
                                                val newTime = c.timeInMillis
                                                shiftAnchorTime = newTime
                                                notificationPrefs.edit().putLong("shift_anchor_time", newTime).apply()
                                                
                                                userSession?.let { session ->
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        val inMs = com.example.notification.NotificationHelper.estimateHistoricalCheckInTime(context, session.uid)
                                                        estimatedInTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(inMs))
                                                        val mockEntry = com.example.data.model.TimeEntry(userId = session.uid, date = "", checkInTime = inMs)
                                                        val outMs = com.example.notification.NotificationHelper.estimateHistoricalCheckoutTime(context, session.uid, mockEntry)
                                                        estimatedOutTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(outMs))
                                                        
                                                        if (autoClockInOutEnabled) {
                                                            com.example.notification.NotificationHelper.scheduleAutoCheckIn(context, session.uid, inMs)
                                                        }
                                                    }
                                                }
                                            },
                                            curCal.get(Calendar.YEAR),
                                            curCal.get(Calendar.MONTH),
                                            curCal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    modifier = Modifier.height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("📅 $anchorDateStr", fontSize = 11.sp, color = White)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ca tại ngày mốc:", fontSize = 12.sp, color = LightGray, fontWeight = FontWeight.Medium)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val isNight = shiftAnchorType == "NIGHT"
                                    Button(
                                        onClick = {
                                            shiftAnchorType = "DAY"
                                            notificationPrefs.edit().putString("shift_anchor_type", "DAY").apply()
                                            
                                            userSession?.let { session ->
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    val inMs = com.example.notification.NotificationHelper.estimateHistoricalCheckInTime(context, session.uid)
                                                    estimatedInTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(inMs))
                                                    val mockEntry = com.example.data.model.TimeEntry(userId = session.uid, date = "", checkInTime = inMs)
                                                    val outMs = com.example.notification.NotificationHelper.estimateHistoricalCheckoutTime(context, session.uid, mockEntry)
                                                    estimatedOutTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(outMs))
                                                    
                                                    if (autoClockInOutEnabled) {
                                                        com.example.notification.NotificationHelper.scheduleAutoCheckIn(context, session.uid, inMs)
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!isNight) PrimaryBlue else MediumGray,
                                            contentColor = White
                                        ),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) {
                                        Text("Ca Ngày", fontSize = 11.sp)
                                    }
                                    
                                    Button(
                                        onClick = {
                                            shiftAnchorType = "NIGHT"
                                            notificationPrefs.edit().putString("shift_anchor_type", "NIGHT").apply()
                                            
                                            userSession?.let { session ->
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    val inMs = com.example.notification.NotificationHelper.estimateHistoricalCheckInTime(context, session.uid)
                                                    estimatedInTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(inMs))
                                                    val mockEntry = com.example.data.model.TimeEntry(userId = session.uid, date = "", checkInTime = inMs)
                                                    val outMs = com.example.notification.NotificationHelper.estimateHistoricalCheckoutTime(context, session.uid, mockEntry)
                                                    estimatedOutTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(outMs))
                                                    
                                                    if (autoClockInOutEnabled) {
                                                        com.example.notification.NotificationHelper.scheduleAutoCheckIn(context, session.uid, inMs)
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isNight) PrimaryBlue else MediumGray,
                                            contentColor = White
                                        ),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) {
                                        Text("Ca Đêm", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        showNotificationConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Xong", color = White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showReminderDialog) {
        var minutesText by remember { mutableStateOf(reminderMinutes) }
        var isCustomError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.AlarmOn,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "NHẮC NHỞ CHẤM CÔNG",
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cài đặt thời gian gửi thông báo nhắc nhở trước khi vào ca làm việc.",
                        color = LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Thuật toán học ca làm việc (AI) đã được tự động bật kèm nhắc nhở.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Text(
                        text = "Chọn thời gian nhắc trước ca:",
                        color = White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("5", "10", "15", "30", "60").forEach { min ->
                            val isSelected = minutesText == min
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    minutesText = min
                                    isCustomError = false
                                },
                                label = { Text("${min}p", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = White,
                                    containerColor = Color(0xFF1E1E1E),
                                    labelColor = LightGray
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                                minutesText = newValue
                                val valInt = newValue.toIntOrNull()
                                isCustomError = valInt == null || valInt <= 0 || valInt > 240
                            }
                        },
                        label = { Text("Số phút (1 - 240)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = White, fontSize = 15.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = MediumGray,
                            cursorColor = PrimaryBlue
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    if (isCustomError && minutesText.isNotEmpty()) {
                        Text(
                            text = "Vui lòng nhập số phút từ 1 đến 240",
                            color = AccentOrange,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val minutesVal = minutesText.trim().toIntOrNull()
                        if (minutesVal != null && minutesVal in 1..240) {
                            reminderMinutes = minutesText.trim()
                            notificationsEnabled = true
                            notificationPrefs.edit()
                                .putBoolean("notifications_enabled", true)
                                .putBoolean("smart_learning_enabled", true)
                                .putString("reminder_minutes_before", reminderMinutes)
                                .apply()

                            userSession?.let { session ->
                                com.example.notification.NotificationHelper.scheduleNextCheckInReminder(context, session.uid)
                                activeEntry?.let { active ->
                                    if (active.isWorking) {
                                        com.example.notification.NotificationHelper.scheduleCheckOutReminderForActiveEntry(context, session.uid, active)
                                    }
                                }
                            }

                            Toast.makeText(context, "Đã lưu nhắc nhở trước $reminderMinutes phút", Toast.LENGTH_SHORT).show()
                            showReminderDialog = false
                        } else {
                            isCustomError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Lưu cài đặt", color = White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        notificationsEnabled = false
                        notificationPrefs.edit()
                            .putBoolean("notifications_enabled", false)
                            .apply()
                        userSession?.let { session ->
                            com.example.notification.NotificationHelper.cancelCheckOutReminder(context, session.uid)
                            androidx.work.WorkManager.getInstance(context).cancelUniqueWork("checkin_reminder_${session.uid}")
                        }
                        Toast.makeText(context, "Đã tắt nhắc nhở (Im lặng)", Toast.LENGTH_SHORT).show()
                        showReminderDialog = false
                    },
                    border = BorderStroke(1.dp, MediumGray)
                ) {
                    Text("Tắt (Im lặng)", color = LightGray)
                }
            },
            containerColor = Color(0xFF121212),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(115.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    color = LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subValue,
                    color = MediumGray,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

data class DayChartPoint(
    val label: String,
    val hours: Double,
    val dateStr: String,
    val isNightShift: Boolean = false
)

@Composable
fun TimeSnap7DayBarChart(data: List<DayChartPoint>) {
    val barColor = NeonBlue
    val gridLineColor = Color(0xFF2C2C2C)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val bottomPadding = 30f
            val topPadding = 20f
            val leftPadding = 40f
            val rightPadding = 20f

            val graphWidth = canvasWidth - leftPadding - rightPadding
            val graphHeight = canvasHeight - topPadding - bottomPadding

            val maxHours = 12.0

            drawLine(
                color = gridLineColor,
                start = Offset(leftPadding, canvasHeight - bottomPadding),
                end = Offset(canvasWidth - rightPadding, canvasHeight - bottomPadding),
                strokeWidth = 2f
            )

            val middleY1 = canvasHeight - bottomPadding - (graphHeight * 0.5f)
            drawLine(
                color = gridLineColor.copy(alpha = 0.5f),
                start = Offset(leftPadding, middleY1),
                end = Offset(canvasWidth - rightPadding, middleY1),
                strokeWidth = 1f
            )

            val barCount = data.size
            if (barCount > 0) {
                val columnWidth = graphWidth / barCount
                val barMaxWidth = columnWidth * 0.5f

                for (i in 0 until barCount) {
                    val pt = data[i]

                    val rawHours = pt.hours.coerceAtLeast(0.0)
                    val percent = (rawHours / maxHours).coerceAtMost(1.0).toFloat()

                    val barHeight = graphHeight * percent
                    val xCenter = leftPadding + (i * columnWidth) + (columnWidth / 2f)
                    val xLeft = xCenter - (barMaxWidth / 2f)
                    val yTop = canvasHeight - bottomPadding - barHeight

                    if (barHeight > 2f) {
                        drawRoundRect(
                            color = if (pt.isNightShift) NightPurple else if (pt.hours > 8.0) AccentGreen else barColor,
                            topLeft = Offset(xLeft, yTop),
                            size = Size(barMaxWidth, barHeight),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }

                    drawContext.canvas.nativeCanvas.apply {
                        val textPaint = android.text.TextPaint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawText(
                            pt.label,
                            xCenter,
                            canvasHeight - 5f,
                            textPaint
                        )

                        if (pt.hours > 0.0) {
                            val hrsText = if (pt.hours % 1.0 == 0.0) "${pt.hours.toInt()}h" else "${DecimalFormat("#.#").format(pt.hours)}h"
                            val valPaint = android.text.TextPaint().apply {
                                color = if (pt.isNightShift) {
                                    android.graphics.Color.parseColor("#BB6BD9") // Purple
                                } else if (pt.hours > 8.0) {
                                    android.graphics.Color.parseColor("#27AE60")
                                } else {
                                    android.graphics.Color.parseColor("#2F80ED")
                                }
                                textSize = 20f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                            }
                            drawText(
                                hrsText,
                                xCenter,
                                yTop - 6f,
                                valPaint
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calculateRecent7Days(entries: List<TimeEntry>): List<DayChartPoint> {
    val cal = Calendar.getInstance()
    val sdfValue = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val sdfDay = SimpleDateFormat("EEE", Locale("vi", "VN"))

    val dates = ArrayList<Pair<String, String>>()
    for (i in 6 downTo 0) {
        val clone = cal.clone() as Calendar
        clone.add(Calendar.DAY_OF_YEAR, -i)
        val dateStr = sdfValue.format(clone.time)
        var label = sdfDay.format(clone.time)
            .replace("Thứ ", "T")
            .replace("Chủ nhật", "CN")
            .replace("Chủ Nhật", "CN")
        dates.add(Pair(label, dateStr))
    }

    return dates.map { (label, dateStr) ->
        val entity = entries.find { it.date == dateStr }
        val hours = if (entity != null && entity.checkInTime != null) {
            if (entity.checkOutTime != null) {
                val elapsed = entity.checkOutTime - entity.checkInTime
                val actual = elapsed / 3600000.0
                if (actual <= 8.5) {
                    actual.coerceAtMost(8.0)
                } else {
                    actual
                }
            } else {
                val elapsed = System.currentTimeMillis() - entity.checkInTime
                val actual = (elapsed / 3600000.0).coerceAtLeast(0.0)
                if (actual <= 8.5) {
                    actual.coerceAtMost(8.0)
                } else {
                    actual
                }
            }
        } else {
            0.0
        }
        val isNight = if (entity != null) {
            entity.shiftType == "NIGHT" || entity.dayType == "NIGHT" || 
            (entity.checkInTime != null && com.example.data.SalaryCalculator.isNightShift(entity.checkInTime, entity.checkOutTime))
        } else false
        DayChartPoint(label, hours, dateStr, isNight)
    }
}

private fun calculateMonthlyChartData(selectedMonth: String, entries: List<TimeEntry>): List<DayChartPoint> {
    val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val date = try { sdfMonth.parse(selectedMonth) ?: Date() } catch(e: Exception) { Date() }
    val cal = Calendar.getInstance()
    cal.time = date
    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val list = ArrayList<DayChartPoint>()
    val parts = selectedMonth.split("-")
    val year = if (parts.size == 2) parts[0] else "2026"
    val month = if (parts.size == 2) parts[1] else "01"

    for (day in 1..maxDay) {
        val dateStr1 = String.format(Locale.US, "%02d/%s/%s", day, month, year)
        val dateStr2 = String.format(Locale.US, "%s-%s-%02d", year, month, day)

        val entity = entries.find { it.date == dateStr1 || it.date == dateStr2 }
        val hours = if (entity != null) {
            if (entity.checkInTime != null) {
                if (entity.checkOutTime != null) {
                    val elapsed = entity.checkOutTime - entity.checkInTime
                    val actual = elapsed / 3600000.0
                    if (actual <= 8.5) {
                        actual.coerceAtMost(8.0)
                    } else {
                        actual
                    }
                } else {
                    val elapsed = System.currentTimeMillis() - entity.checkInTime
                    val actual = (elapsed / 3600000.0).coerceAtLeast(0.0)
                    if (actual <= 8.5) {
                        actual.coerceAtMost(8.0)
                    } else {
                        actual
                    }
                }
            } else {
                (entity.workDay * 8.0) + entity.otHours
            }
        } else {
            0.0
        }
        val isNight = if (entity != null) {
            entity.shiftType == "NIGHT" || entity.dayType == "NIGHT" || 
            (entity.checkInTime != null && com.example.data.SalaryCalculator.isNightShift(entity.checkInTime, entity.checkOutTime))
        } else false
        list.add(DayChartPoint(day.toString(), hours, if (entity != null) entity.date else dateStr1, isNight))
    }
    return list
}

@Composable
fun MonthlyActivityChart(data: List<DayChartPoint>, selectedMonth: String = "") {
    val workedPoints = remember(data) {
        data.filter { it.hours > 0.0 }
    }

    if (workedPoints.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Không có dữ liệu giờ làm trong tháng.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            val hoursList = data.map { it.hours }
            val maxHours = (hoursList.maxOrNull() ?: 8.0).coerceAtLeast(1.0)
            
            val density = androidx.compose.ui.platform.LocalDensity.current
            val scrollState = rememberScrollState()
            
            val lastActiveIndex = remember(data, selectedMonth) {
                val idx = data.indexOfLast { it.hours > 0.0 }
                if (idx == -1) {
                    val todayCal = Calendar.getInstance()
                    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
                    val todayMonthStr = String.format(Locale.US, "%04d-%02d", todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH) + 1)
                    if (selectedMonth == todayMonthStr || selectedMonth.isEmpty()) {
                        (todayDay - 1).coerceIn(0, data.lastIndex)
                    } else {
                        data.lastIndex
                    }
                } else {
                    idx
                }
            }

            LaunchedEffect(scrollState.maxValue, lastActiveIndex) {
                if (scrollState.maxValue > 0) {
                    val itemWidthPx = with(density) { 32.dp.toPx() }
                    val spacingPx = with(density) { 12.dp.toPx() }
                    val stepPx = itemWidthPx + spacingPx
                    val firstVisibleIndex = (lastActiveIndex - 6).coerceAtLeast(0)
                    val targetScroll = (firstVisibleIndex * stepPx).toInt().coerceIn(0, scrollState.maxValue)
                    scrollState.scrollTo(targetScroll)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .horizontalScroll(scrollState)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { pt ->
                    val hrs = pt.hours
                    val barHeightFraction = (hrs / maxHours).coerceIn(0.01, 1.0).toFloat()

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(32.dp)
                    ) {
                        if (hrs > 0.0) {
                            Text(
                                text = String.format(Locale.US, "%.1f", hrs),
                                color = White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .weight(1f, fill = false)
                                .fillMaxHeight(barHeightFraction)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = if (pt.isNightShift) {
                                            listOf(NightPurple, NightPurple.copy(alpha = 0.2f))
                                        } else if (hrs > 8.0) {
                                            listOf(AccentGreen, AccentGreen.copy(alpha = 0.2f))
                                        } else {
                                            listOf(NeonBlue, NeonBlue.copy(alpha = 0.2f))
                                        }
                                    ),
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = pt.label,
                            color = if (hrs > 0.0) Color.LightGray else Color.Gray.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = if (hrs > 0.0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(8.dp))

            val dayHours = data.filter { !it.isNightShift }.sumOf { it.hours }
            val nightHours = data.filter { it.isNightShift }.sumOf { it.hours }
            val avg = if (workedPoints.isNotEmpty()) workedPoints.map { it.hours }.average() else 0.0
            val total = hoursList.sum()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Trung bình/ngày", color = Color.Gray, fontSize = 10.sp)
                    Text(String.format(Locale.US, "%.1f giờ", avg), color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Tổng số giờ làm", color = Color.Gray, fontSize = 10.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (dayHours > 0.0) {
                            Text(String.format(Locale.US, "Ca ngày: %.1fh", dayHours), color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (nightHours > 0.0) {
                            Text(String.format(Locale.US, "Ca đêm: %.1fh", nightHours), color = NightPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (dayHours == 0.0 && nightHours == 0.0) {
                            Text("0.0 giờ", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun calculateDayEarnings(entry: TimeEntry, config: com.example.data.model.UserConfig?): Double {
    if (config == null) return 0.0
    val processed = com.example.data.SalaryCalculator.calculateSingleEntry(entry, config)
    val hourlySalary = config.luongCoBan / 26.0 / 8.0
    
    if (processed.dayType == "PAID_LEAVE" || processed.dayType == "HOLIDAY_LEAVE") {
        return config.luongCoBan / 26.0
    }
    if (processed.dayType == "UNPAID_LEAVE" || processed.checkInTime == null) {
        return 0.0
    }
    
    val inCal = Calendar.getInstance()
    inCal.timeInMillis = processed.checkInTime!!
    val inHour = inCal.get(Calendar.HOUR_OF_DAY)
    val inMin = inCal.get(Calendar.MINUTE)
    val inTotalMin = inHour * 60 + inMin
    val isNightShift = (inTotalMin in (18 * 60)..(19 * 60 + 30)) || 
                       inHour >= 22 || inHour <= 6 || 
                       processed.dayType == "NIGHT"

    val isSunday = processed.dayType == "SUNDAY" || 
                   (run {
                       try {
                           val parser = if (processed.date.contains("/")) {
                               SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                           } else {
                               SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                           }
                           val dateVal = parser.parse(processed.date)
                           if (dateVal != null) {
                               val cal = Calendar.getInstance()
                               cal.time = dateVal
                               cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                           } else {
                               false
                           }
                       } catch (e: Exception) {
                           false
                       }
                   } && processed.dayType == "NIGHT")

    val isHoliday = com.example.data.SalaryCalculator.isHoliday(processed.date)

    if (processed.checkOutTime == null) {
        if (isSunday) {
            return 8.0 * hourlySalary * config.heSoOtChuNhat
        } else {
            return config.luongCoBan / 26.0
        }
    }

    val finalCheckIn = processed.normalizedCheckIn ?: processed.rawCheckIn ?: processed.checkInTime!!
    val finalCheckOut = processed.normalizedCheckOut ?: processed.rawCheckOut ?: processed.checkOutTime!!
    val rawDurationMs = (finalCheckOut - finalCheckIn).coerceAtLeast(0L)
    // Round duration to the nearest minute to avoid sub-minute floating point variance
    val durationMs = Math.round(rawDurationMs / 60000.0) * 60000L
    val rawHours = durationMs / 3600000.0
    val breakHours = if (config.tinhKhauTruNghi) config.soGioNghiGiaiLao else 0.0
    val actualHours = (rawHours - breakHours).coerceAtLeast(0.0)

    val finalStandardHours = processed.workDay * 8.0
    val finalOtHours = processed.otHours

    var earned = 0.0
    if (isSunday) {
        earned += actualHours * hourlySalary * config.heSoOtChuNhat
    } else if (isHoliday) {
        earned += actualHours * hourlySalary * config.heSoOtNgayLe
    } else {
        earned += finalStandardHours * hourlySalary
        if (finalOtHours > 0.0) {
            earned += finalOtHours * hourlySalary * config.heSoOtNgayThuong
        }
    }

    if (actualHours >= 4.0) {
        earned += config.pcComCa
    }

    if (finalOtHours >= 2.0) {
        earned += config.pcComCa
    }

    if (isNightShift) {
        earned += 100000.0
    }

    return earned
}

@Composable
fun QuickSummaryItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = DarkContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF2C2C2C))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label, 
                color = MediumGray, 
                fontSize = 9.sp, 
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value, 
                color = White, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetroactiveCheckInDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        dateStr: String,
        inH: Int,
        inM: Int,
        outH: Int?,
        outM: Int?,
        note: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val todaySdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val todayStr = remember { todaySdf.format(Date()) }
    val calYesterday = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) } }
    val yesterdayStr = remember { todaySdf.format(calYesterday.time) }

    var selectedDateStr by remember { mutableStateOf(todayStr) }
    var isFullShift by remember { mutableStateOf(false) } // false = Bấm Vào Ca Bù (đang trong ca), true = Bù Cả Ca (đã ra ca)

    var inHourText by remember { mutableStateOf(TextFieldValue("07")) }
    var inMinText by remember { mutableStateOf(TextFieldValue("30")) }

    var outHourText by remember { mutableStateOf(TextFieldValue("16")) }
    var outMinText by remember { mutableStateOf(TextFieldValue("30")) }

    var noteText by remember { mutableStateOf(TextFieldValue("Vào ca bù do quên bấm chấm công")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkContainer,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = NeonBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VÀO CA BÙ (BÙ CHẤM CÔNG)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }
        },
        text = {
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Dành cho trường hợp quên bấm vào ca. Bạn có thể chọn ngày và thời gian bắt đầu ca làm để bù vào hệ thống.",
                    fontSize = 12.sp,
                    color = LightGray
                )

                // 1. CHỌN NGÀY
                Text("1. Ngày chấm công bù:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonBlue)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedDateStr == todayStr,
                        onClick = { selectedDateStr = todayStr },
                        label = { Text("Hôm nay", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonBlue,
                            selectedLabelColor = White,
                            containerColor = DarkBackground,
                            labelColor = LightGray
                        )
                    )
                    FilterChip(
                        selected = selectedDateStr == yesterdayStr,
                        onClick = { selectedDateStr = yesterdayStr },
                        label = { Text("Hôm qua", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonBlue,
                            selectedLabelColor = White,
                            containerColor = DarkBackground,
                            labelColor = LightGray
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Ngày đã chọn: ",
                        fontSize = 12.sp,
                        color = White
                    )
                    Text(
                        text = selectedDateStr,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = {
                            val curCal = Calendar.getInstance()
                            try {
                                val p = todaySdf.parse(selectedDateStr)
                                if (p != null) curCal.time = p
                            } catch (e: Exception) {}
                            android.app.DatePickerDialog(
                                context,
                                { _, yr, mo, dy ->
                                    val c = Calendar.getInstance()
                                    c.set(yr, mo, dy)
                                    selectedDateStr = todaySdf.format(c.time)
                                },
                                curCal.get(Calendar.YEAR),
                                curCal.get(Calendar.MONTH),
                                curCal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("📅 Chọn ngày khác", fontSize = 11.sp, color = White)
                    }
                }

                HorizontalDivider(color = MediumGray.copy(alpha = 0.3f))

                // 2. LOẠI HÌNH CHẤM BÙ
                Text("2. Trạng thái ca:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonBlue)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isFullShift) NeonBlue.copy(alpha = 0.25f) else DarkBackground
                        ),
                        border = BorderStroke(1.dp, if (!isFullShift) NeonBlue else MediumGray),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isFullShift = false }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("🔵 VÀO CA BÙ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (!isFullShift) NeonBlue else White)
                            Text("Bắt đầu ca, đang trong ca", fontSize = 10.sp, color = LightGray)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFullShift) AccentGreen.copy(alpha = 0.25f) else DarkBackground
                        ),
                        border = BorderStroke(1.dp, if (isFullShift) AccentGreen else MediumGray),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isFullShift = true }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("🟢 BÙ CẢ CA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isFullShift) AccentGreen else White)
                            Text("Nhập giờ vào & giờ ra", fontSize = 10.sp, color = LightGray)
                        }
                    }
                }

                HorizontalDivider(color = MediumGray.copy(alpha = 0.3f))

                // 3. GIỜ VÀO CA BÙ
                Text("3. Giờ vào ca bù (Giờ bắt đầu):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonBlue)

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val presets = listOf("07:30", "08:00", "19:30", "20:00")
                    presets.forEach { time ->
                        AssistChip(
                            onClick = {
                                val p = time.split(":")
                                inHourText = TextFieldValue(p[0])
                                inMinText = TextFieldValue(p[1])
                            },
                            label = { Text(time, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inHourText,
                        onValueChange = { if (it.text.length <= 2) inHourText = it },
                        label = { Text("Giờ (0-23)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        )
                    )
                    Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = White)
                    OutlinedTextField(
                        value = inMinText,
                        onValueChange = { if (it.text.length <= 2) inMinText = it },
                        label = { Text("Phút (0-59)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        )
                    )
                }

                // 4. GIỜ RA CA (Only if isFullShift = true)
                if (isFullShift) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("4. Giờ ra ca bù (Giờ kết thúc):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentGreen)

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val outPresets = listOf("16:30", "17:30", "05:30", "06:00")
                        outPresets.forEach { time ->
                            AssistChip(
                                onClick = {
                                    val p = time.split(":")
                                    outHourText = TextFieldValue(p[0])
                                    outMinText = TextFieldValue(p[1])
                                },
                                label = { Text(time, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = outHourText,
                            onValueChange = { if (it.text.length <= 2) outHourText = it },
                            label = { Text("Giờ (0-23)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            )
                        )
                        Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = White)
                        OutlinedTextField(
                            value = outMinText,
                            onValueChange = { if (it.text.length <= 2) outMinText = it },
                            label = { Text("Phút (0-59)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            )
                        )
                    }
                }

                // 5. GHI CHÚ
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Ghi chú", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    )
                )
            }
        },
        confirmButton = {
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val inH = inHourText.text.toIntOrNull()
                    val inM = inMinText.text.toIntOrNull()
                    if (inH == null || inH !in 0..23 || inM == null || inM !in 0..59) {
                        Toast.makeText(context, "Giờ vào không hợp lệ (0-23h, 0-59p)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    var outH: Int? = null
                    var outM: Int? = null
                    if (isFullShift) {
                        outH = outHourText.text.toIntOrNull()
                        outM = outMinText.text.toIntOrNull()
                        if (outH == null || outH !in 0..23 || outM == null || outM !in 0..59) {
                            Toast.makeText(context, "Giờ ra không hợp lệ (0-23h, 0-59p)", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                    }

                    onSubmit(
                        selectedDateStr,
                        inH,
                        inM,
                        outH,
                        outM,
                        noteText.text.ifBlank { null }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
            ) {
                Text("Xác nhận Vào Ca Bù", color = White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = LightGray)
            }
        }
    )
}
