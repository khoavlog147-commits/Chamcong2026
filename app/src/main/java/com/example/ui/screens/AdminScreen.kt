package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserConfig
import com.example.data.model.RoleConfig
import com.example.data.model.getRoles
import com.example.data.model.updateRoles
import com.example.data.model.CompanyConfig
import com.example.data.AttendanceRecord
import com.example.ui.theme.*
import com.example.viewmodel.AdminViewModel
import com.example.util.ThousandSeparatorVisualTransformation
import com.example.util.ExportUtils
import com.example.util.toTimeEntry
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.LocalTextStyle
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val adminViewModel: AdminViewModel = viewModel()
    val employees by adminViewModel.employees.collectAsStateWithLifecycle()
    val companies by adminViewModel.companies.collectAsStateWithLifecycle()
    val selectedCompanyId by adminViewModel.selectedCompanyId.collectAsStateWithLifecycle()
    val filteredEmployees by adminViewModel.filteredEmployees.collectAsStateWithLifecycle()
    val isLoading by adminViewModel.isLoading.collectAsStateWithLifecycle()
    val selectedEmployee by adminViewModel.selectedEmployee.collectAsStateWithLifecycle()
    val selectedIds by adminViewModel.selectedEmployeeIds.collectAsStateWithLifecycle()
    val isExportingByVM by adminViewModel.isExporting.collectAsStateWithLifecycle()
    val exportProgressByVM by adminViewModel.exportProgress.collectAsStateWithLifecycle()
    
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var showBatchExportDialog by remember { mutableStateOf(false) }
    var showBatchEditDialog by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showSingleDeleteConfirm by remember { mutableStateOf(false) }
    var showSendNotifDialog by remember { mutableStateOf(false) }
    var showCompanyManagementDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var longClickedEmployee by remember { mutableStateOf<UserConfig?>(null) }

    val exportSuccessCount by adminViewModel.exportSuccessCount.collectAsStateWithLifecycle()
    val todayAttendanceMap by adminViewModel.todayAttendanceMap.collectAsStateWithLifecycle()
    val isExportingSingle by adminViewModel.isExportingSingle.collectAsStateWithLifecycle()

    BackHandler {
        if (selectedIds.isNotEmpty()) {
            adminViewModel.clearSelection()
        } else if (selectedEmployee != null) {
            adminViewModel.selectEmployee(null)
        } else {
            onBack()
        }
    }

    LaunchedEffect(isExportingByVM) {
        if (!isExportingByVM && showBatchExportDialog) {
            if (employees.isNotEmpty()) {
                android.widget.Toast.makeText(context, "Đã xuất thành công $exportSuccessCount/${employees.size} phiếu lương vào thư mục Download/TimeSnapPro", android.widget.Toast.LENGTH_LONG).show()
            }
            showBatchExportDialog = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (selectedIds.isNotEmpty()) {
                        Text("${selectedIds.size}/${employees.size} đã chọn", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    } else {
                        Text("Quản Trị Hệ Thống", color = White, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = { adminViewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection", tint = White)
                        }
                    } else if (selectedEmployee != null) {
                        IconButton(onClick = { adminViewModel.selectEmployee(null) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to list", tint = White)
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                        }
                    }
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                if (selectedIds.size == employees.size) {
                                    adminViewModel.clearSelection()
                                } else {
                                    adminViewModel.selectAllEmployees(employees.map { it.userId })
                                }
                            }
                        ) {
                            Text(
                                text = if (selectedIds.size == employees.size) "Bỏ chọn" else "Chọn tất cả",
                                color = NeonBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        IconButton(onClick = { showBatchEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Batch Edit", tint = White)
                        }
                        IconButton(onClick = { showBatchDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Batch Delete", tint = AccentOrange)
                        }
                    } else {
                        if (selectedEmployee != null) {
                            IconButton(
                                onClick = {
                                    adminViewModel.exportSingleEmployeePayslip(context, selectedEmployee!!)
                                }
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Xuất phiếu lương cá nhân", tint = NeonBlue)
                            }
                        } else {
                            IconButton(onClick = { showSendNotifDialog = true }) {
                                Icon(Icons.Default.Campaign, contentDescription = "Gửi thông báo", tint = White)
                            }
                            IconButton(onClick = { showBatchExportDialog = true }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Xuất hàng loạt", tint = White)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (selectedEmployee == null && selectedIds.isEmpty()) {
                FloatingActionButton(
                    onClick = { showAddEmployeeDialog = true },
                    containerColor = NeonBlue,
                    contentColor = White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Employee")
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { adminViewModel.loadEmployees() },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (isExportingSingle) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = NeonBlue)
            } else {
                if (selectedEmployee != null) {
                    EmployeeDetailView(
                        employee = selectedEmployee!!,
                        records = adminViewModel.attendanceRecords.collectAsStateWithLifecycle().value,
                        onBack = { adminViewModel.selectEmployee(null) },
                        onClose = { adminViewModel.loadEmployees() },
                        adminViewModel = adminViewModel,
                        onDeleteRequest = { showSingleDeleteConfirm = true }
                    )
                } else {
                    EmployeeListView(
                        companies = companies,
                        selectedCompanyId = selectedCompanyId,
                        onSelectCompany = { adminViewModel.selectCompany(it) },
                        onOpenCompanyManagement = { showCompanyManagementDialog = true },
                        employees = filteredEmployees.filter { it.hoVaTen.contains(searchQuery, ignoreCase = true) || it.maNhanVien.contains(searchQuery, ignoreCase = true) }.sortedWith(compareByDescending<UserConfig> { it.isAdmin }.thenBy { it.hoVaTen }),
                        selectedIds = selectedIds,
                        todayAttendanceMap = todayAttendanceMap,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onEmployeeClick = { emp ->
                            if (selectedIds.isNotEmpty()) {
                                adminViewModel.toggleEmployeeSelection(emp.userId)
                            } else {
                                adminViewModel.selectEmployee(emp)
                            }
                        },
                        onEmployeeLongClick = { emp ->
                            longClickedEmployee = emp
                        }
                    )
                }
            }
        }
    }

    if (showCompanyManagementDialog) {
        CompanyManagementDialog(
            companies = companies,
            allEmployees = employees,
            initialCompanyId = selectedCompanyId ?: companies.firstOrNull()?.companyId ?: "default_company",
            adminViewModel = adminViewModel,
            onDismiss = { showCompanyManagementDialog = false }
        )
    }

    if (longClickedEmployee != null) {
        AlertDialog(
            onDismissRequest = { longClickedEmployee = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = NeonBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tùy chọn nhân viên", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${longClickedEmployee?.hoVaTen} (${longClickedEmployee?.maNhanVien})",
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text("Chọn chế độ chọn hoặc chấm công:", color = Color.Gray, fontSize = 12.sp)

                    // Option 1: Select All Employees
                    Button(
                        onClick = {
                            val allList = employees.map { it.userId }
                            adminViewModel.selectAllEmployees(allList)
                            longClickedEmployee = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkContainer),
                        border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.5f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.SelectAll, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Chọn tất cả nhân viên (${employees.size} NV)", color = White, fontSize = 13.sp)
                        }
                    }

                    // Option 2: Toggle select this individual employee
                    val isThisSelected = selectedIds.contains(longClickedEmployee?.userId)
                    Button(
                        onClick = {
                            longClickedEmployee?.let { adminViewModel.toggleEmployeeSelection(it.userId) }
                            longClickedEmployee = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkContainer),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                if (isThisSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (isThisSelected) AccentOrange else White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                if (isThisSelected) "Bỏ chọn nhân viên này" else "Chọn duy nhất / Thêm NV này",
                                color = White,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Option 3: Quick Batch Attendance Dialog
                    Button(
                        onClick = {
                            val emp = longClickedEmployee
                            if (emp != null && !selectedIds.contains(emp.userId)) {
                                adminViewModel.toggleEmployeeSelection(emp.userId)
                            }
                            longClickedEmployee = null
                            showBatchEditDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, NeonBlue)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Chấm công & Sửa hàng loạt ngay", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { longClickedEmployee = null }) {
                    Text("Đóng", color = Color.Gray)
                }
            },
            containerColor = DarkContainer
        )
    }

    if (showBatchEditDialog) {
        var selectedMode by remember { mutableStateOf(0) } // 0: Vào ca / Thêm ca, 1: Ra ca hàng loạt, 2: Sửa Lương, 3: Gán Chức Vụ

        var batchLcb by remember { mutableStateOf("") }
        var batchPcXangXe by remember { mutableStateOf("") }
        var batchChuyenCan by remember { mutableStateOf("") }

        val todayDdMmYyyy = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
        var batchDateInput by remember { mutableStateOf(todayDdMmYyyy.replace("/", "")) }
        var batchClockIn by remember { mutableStateOf("07:30") }
        var batchClockOut by remember { mutableStateOf("19:30") }
        var batchNote by remember { mutableStateOf("Admin chấm công hàng loạt") }
        var activeShiftPreset by remember { mutableStateOf("7:30-19:30") }

        var batchCompanyId by remember { mutableStateOf(selectedCompanyId ?: companies.firstOrNull()?.companyId ?: "default_company") }
        var batchRoleId by remember { mutableStateOf("") }

        val batchCurrentCompany = companies.find { it.companyId == batchCompanyId } ?: companies.firstOrNull() ?: CompanyConfig.DEFAULT_COMPANY
        val batchRoles = batchCurrentCompany.getRoles()

        AlertDialog(
            onDismissRequest = { showBatchEditDialog = false },
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (selectedMode) {
                                3 -> "Gán chức vụ hàng loạt (${selectedIds.size} NV)"
                                2 -> "Cấu hình lương hàng loạt (${selectedIds.size} NV)"
                                else -> "Chấm công hàng loạt (${selectedIds.size} NV)"
                            },
                            color = White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedMode == 0,
                            onClick = { selectedMode = 0 },
                            label = { Text("Vào / Thêm ca", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonBlue.copy(alpha = 0.3f),
                                selectedLabelColor = NeonBlue
                            )
                        )
                        FilterChip(
                            selected = selectedMode == 1,
                            onClick = { 
                                selectedMode = 1
                                if (batchNote == "Admin chấm công hàng loạt") {
                                    batchNote = "Admin ra ca hàng loạt"
                                }
                            },
                            label = { Text("Ra ca hàng loạt", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentOrange.copy(alpha = 0.3f),
                                selectedLabelColor = AccentOrange
                            )
                        )
                        FilterChip(
                            selected = selectedMode == 2,
                            onClick = { selectedMode = 2 },
                            label = { Text("Sửa Lương", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00E676).copy(alpha = 0.3f),
                                selectedLabelColor = Color(0xFF00E676)
                            )
                        )
                        FilterChip(
                            selected = selectedMode == 3,
                            onClick = { selectedMode = 3 },
                            label = { Text("Gán Chức Vụ", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonBlue.copy(alpha = 0.3f),
                                selectedLabelColor = NeonBlue
                            )
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedMode) {
                        0 -> {
                            // MODE 0: VÀO / THÊM CA HÀNG LOẠT
                            Surface(
                                color = Color.White.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Chọn mẫu ca chuẩn:", color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            FilterChip(
                                                selected = activeShiftPreset == "7:30-19:30",
                                                onClick = {
                                                    activeShiftPreset = "7:30-19:30"
                                                    batchClockIn = "07:30"
                                                    batchClockOut = "19:30"
                                                },
                                                label = { Text("7:30 - 19:30 (Ca ngày)", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = NeonBlue.copy(alpha = 0.25f),
                                                    selectedLabelColor = NeonBlue
                                                )
                                            )
                                            FilterChip(
                                                selected = activeShiftPreset == "7:30-20:00",
                                                onClick = {
                                                    activeShiftPreset = "7:30-20:00"
                                                    batchClockIn = "07:30"
                                                    batchClockOut = "20:00"
                                                },
                                                label = { Text("7:30 - 20:00 (Ca ngày)", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = AccentOrange.copy(alpha = 0.25f),
                                                    selectedLabelColor = AccentOrange
                                                )
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            FilterChip(
                                                selected = activeShiftPreset == "19:30-07:30",
                                                onClick = {
                                                    activeShiftPreset = "19:30-07:30"
                                                    batchClockIn = "19:30"
                                                    batchClockOut = "07:30"
                                                },
                                                label = { Text("19:30 - 07:30 (Ca đêm)", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFFAB47BC).copy(alpha = 0.25f),
                                                    selectedLabelColor = Color(0xFFAB47BC)
                                                )
                                            )
                                            FilterChip(
                                                selected = activeShiftPreset == "7:30-15:30",
                                                onClick = {
                                                    activeShiftPreset = "7:30-15:30"
                                                    batchClockIn = "07:30"
                                                    batchClockOut = "15:30"
                                                },
                                                label = { Text("7:30 - 15:30 (Hành chính)", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFF00E676).copy(alpha = 0.25f),
                                                    selectedLabelColor = Color(0xFF00E676)
                                                )
                                            )
                                        }
                                        FilterChip(
                                            selected = activeShiftPreset == "Đang làm",
                                            onClick = {
                                                activeShiftPreset = "Đang làm"
                                                batchClockIn = "07:30"
                                                batchClockOut = ""
                                            },
                                            label = { Text("Đang làm (Để trống giờ ra)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFFFD54F).copy(alpha = 0.25f),
                                                selectedLabelColor = Color(0xFFFFD54F)
                                            )
                                        )
                                    }

                                    AdminInputField(
                                        label = "Ngày chấm công (DD/MM/YYYY)",
                                        value = batchDateInput,
                                        onValueChange = { input ->
                                            val clean = input.filter { it.isDigit() }.take(8)
                                            batchDateInput = clean
                                        },
                                        keyboardType = KeyboardType.Number,
                                        visualTransformation = DateVisualTransformation()
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            AdminInputField(
                                                label = "Giờ vào ca",
                                                value = batchClockIn,
                                                onValueChange = { batchClockIn = autoFormatTimeInput(it) },
                                                keyboardType = KeyboardType.Number
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            AdminInputField(
                                                label = "Giờ ra ca (Trống = đang làm)",
                                                value = batchClockOut,
                                                onValueChange = { batchClockOut = autoFormatTimeInput(it) },
                                                keyboardType = KeyboardType.Number
                                            )
                                        }
                                    }

                                    if (batchClockOut.isEmpty()) {
                                        Text(
                                            "💡 Để trống giờ ra = Nhân viên đang trong ca làm (chưa ra ca).",
                                            color = Color(0xFFFFD54F),
                                            fontSize = 11.sp
                                        )
                                    }

                                    AdminInputField(
                                        label = "Ghi chú chấm công",
                                        value = batchNote,
                                        onValueChange = { batchNote = it }
                                    )

                                    Text(
                                        "⚡ Tự động nhận diện giờ khi gõ số (VD: gõ 0730 -> 07:30, 1930 -> 19:30, 2000 -> 20:00)",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        1 -> {
                            // MODE 1: RA CA HÀNG LOẠT
                            Surface(
                                color = Color.White.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, AccentOrange.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Cập nhật giờ RA CA hàng loạt cho NV đang làm:", color = AccentOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                    Text("Mẫu giờ ra ca nhanh:", color = Color.Gray, fontSize = 11.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        FilterChip(
                                            selected = batchClockOut == "19:30",
                                            onClick = { batchClockOut = "19:30" },
                                            label = { Text("19:30", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentOrange.copy(alpha = 0.3f), selectedLabelColor = AccentOrange)
                                        )
                                        FilterChip(
                                            selected = batchClockOut == "20:00",
                                            onClick = { batchClockOut = "20:00" },
                                            label = { Text("20:00", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentOrange.copy(alpha = 0.3f), selectedLabelColor = AccentOrange)
                                        )
                                        FilterChip(
                                            selected = batchClockOut == "07:30",
                                            onClick = { batchClockOut = "07:30" },
                                            label = { Text("07:30", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentOrange.copy(alpha = 0.3f), selectedLabelColor = AccentOrange)
                                        )
                                        FilterChip(
                                            selected = batchClockOut == "17:00",
                                            onClick = { batchClockOut = "17:00" },
                                            label = { Text("17:00", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentOrange.copy(alpha = 0.3f), selectedLabelColor = AccentOrange)
                                        )
                                    }

                                    AdminInputField(
                                        label = "Ngày ra ca (DD/MM/YYYY)",
                                        value = batchDateInput,
                                        onValueChange = { input ->
                                            val clean = input.filter { it.isDigit() }.take(8)
                                            batchDateInput = clean
                                        },
                                        keyboardType = KeyboardType.Number,
                                        visualTransformation = DateVisualTransformation()
                                    )

                                    AdminInputField(
                                        label = "Giờ ra ca thực tế",
                                        value = batchClockOut,
                                        onValueChange = { batchClockOut = autoFormatTimeInput(it) },
                                        keyboardType = KeyboardType.Number
                                    )

                                    AdminInputField(
                                        label = "Ghi chú ra ca",
                                        value = batchNote,
                                        onValueChange = { batchNote = it }
                                    )

                                    Text(
                                        "⚡ Sẽ cập nhật giờ ra ca cho tất cả ${selectedIds.size} NV đã chọn trong ngày này.",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        2 -> {
                            // MODE 2: CẤU HÌNH LƯƠNG HÀNG LOẠT
                            Surface(
                                color = Color.White.copy(alpha = 0.02f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Cập nhật lương & Phụ cấp hàng loạt:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    AdminInputField("Lương Cơ Bản mới", batchLcb, onValueChange = { batchLcb = it }, isNumeric = true)
                                    AdminInputField("Phụ cấp xăng xe mới", batchPcXangXe, onValueChange = { batchPcXangXe = it }, isNumeric = true)
                                    AdminInputField("Tiền chuyên cần mới", batchChuyenCan, onValueChange = { batchChuyenCan = it }, isNumeric = true)
                                }
                            }
                        }
                        3 -> {
                            // MODE 3: GÁN CHỨC VỤ HÀNG LOẠT
                            Surface(
                                color = Color.White.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Gán chức vụ cho ${selectedIds.size} NV đã chọn:", color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Chọn Công ty", color = Color.Gray, fontSize = 11.sp)
                                    var expandedComp by remember { mutableStateOf(false) }
                                    Box {
                                        androidx.compose.material3.OutlinedButton(
                                            onClick = { expandedComp = true }, 
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(batchCurrentCompany.companyName, color = White)
                                        }
                                        androidx.compose.material3.DropdownMenu(
                                            expanded = expandedComp, 
                                            onDismissRequest = { expandedComp = false }
                                        ) {
                                            companies.forEach { comp ->
                                                androidx.compose.material3.DropdownMenuItem(
                                                    text = { Text(comp.companyName) },
                                                    onClick = {
                                                        batchCompanyId = comp.companyId
                                                        batchRoleId = ""
                                                        expandedComp = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Chọn Chức vụ", color = Color.Gray, fontSize = 11.sp)
                                    val selectedRole = batchRoles.find { it.roleId == batchRoleId }
                                    var expandedRole by remember { mutableStateOf(false) }
                                    Box {
                                        androidx.compose.material3.OutlinedButton(
                                            onClick = { expandedRole = true }, 
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(selectedRole?.roleName ?: "Chưa phân công", color = White)
                                        }
                                        androidx.compose.material3.DropdownMenu(
                                            expanded = expandedRole, 
                                            onDismissRequest = { expandedRole = false }
                                        ) {
                                            androidx.compose.material3.DropdownMenuItem(
                                                text = { Text("Chưa phân công") },
                                                onClick = {
                                                    batchRoleId = ""
                                                    expandedRole = false
                                                }
                                            )
                                            batchRoles.forEach { role ->
                                                androidx.compose.material3.DropdownMenuItem(
                                                    text = { Text(role.roleName) },
                                                    onClick = {
                                                        batchRoleId = role.roleId
                                                        expandedRole = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val formattedDateStr = if (batchDateInput.length == 8) {
                            val d = batchDateInput.substring(0, 2)
                            val m = batchDateInput.substring(2, 4)
                            val y = batchDateInput.substring(4, 8)
                            "$d/$m/$y"
                        } else {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                        }

                        when (selectedMode) {
                            0 -> {
                                adminViewModel.batchAddAttendance(formattedDateStr, batchClockIn.trim(), batchClockOut.trim(), batchNote.trim())
                            }
                            1 -> {
                                adminViewModel.batchCheckout(formattedDateStr, batchClockOut.trim(), batchNote.trim())
                            }
                            2 -> {
                                if (batchLcb.isNotEmpty() || batchPcXangXe.isNotEmpty() || batchChuyenCan.isNotEmpty()) {
                                    adminViewModel.batchUpdateSalaryConfig { emp ->
                                        emp.copy(
                                            luongCoBan = batchLcb.toDoubleOrNull() ?: emp.luongCoBan,
                                            pcXangXe = batchPcXangXe.toDoubleOrNull() ?: emp.pcXangXe,
                                            tienChuyenCanGoc = batchChuyenCan.toDoubleOrNull() ?: emp.tienChuyenCanGoc
                                        )
                                    }
                                }
                            }
                            3 -> {
                                val selectedRole = batchRoles.find { it.roleId == batchRoleId }
                                adminViewModel.batchUpdateSalaryConfig { emp ->
                                    var updated = emp.copy(
                                        companyId = batchCurrentCompany.companyId,
                                        companyName = batchCurrentCompany.companyName,
                                        companyCode = batchCurrentCompany.companyCode,
                                        roleId = batchRoleId,
                                        roleName = selectedRole?.roleName ?: ""
                                    )
                                    if (selectedRole != null) {
                                        updated = updated.copy(
                                            luongCoBan = if (selectedRole.luongCoBan > 0.0) selectedRole.luongCoBan else batchCurrentCompany.luongCoBan,
                                            pcKyThuat = if (selectedRole.pcKyThuat > 0.0) selectedRole.pcKyThuat else batchCurrentCompany.pcKyThuat,
                                            pcTrachNhiem = if (selectedRole.pcTrachNhiem > 0.0) selectedRole.pcTrachNhiem else batchCurrentCompany.pcTrachNhiem,
                                            pcChucVu = if (selectedRole.pcChucVu > 0.0) selectedRole.pcChucVu else batchCurrentCompany.pcChucVu,
                                            pcHieuSuat = if (selectedRole.pcHieuSuat > 0.0) selectedRole.pcHieuSuat else batchCurrentCompany.pcHieuSuat,
                                            pcSanPham = if (selectedRole.pcSanPham > 0.0) selectedRole.pcSanPham else batchCurrentCompany.pcSanPham,
                                            pcComCa = if (selectedRole.pcComCa > 0.0) selectedRole.pcComCa else batchCurrentCompany.pcComCa,
                                            pcComOt = if (selectedRole.pcComOt > 0.0) selectedRole.pcComOt else batchCurrentCompany.pcComOt,
                                            pcNhaO = if (selectedRole.pcNhaO > 0.0) selectedRole.pcNhaO else batchCurrentCompany.pcNhaO,
                                            pcDocHai = if (selectedRole.pcDocHai > 0.0) selectedRole.pcDocHai else batchCurrentCompany.pcDocHai,
                                            pcDtDoanhThu = if (selectedRole.pcDtDoanhThu > 0.0) selectedRole.pcDtDoanhThu else batchCurrentCompany.pcDtDoanhThu,
                                            pcXangXe = if (selectedRole.pcXangXe > 0.0) selectedRole.pcXangXe else batchCurrentCompany.pcXangXe,
                                            pcThamNien = if (selectedRole.pcThamNien > 0.0) selectedRole.pcThamNien else batchCurrentCompany.pcThamNien,
                                            pcKhac1 = if (selectedRole.pcKhac1 > 0.0) selectedRole.pcKhac1 else batchCurrentCompany.pcKhac1,
                                            pcCaDem = if (selectedRole.pcCaDem > 0.0) selectedRole.pcCaDem else batchCurrentCompany.pcCaDem,
                                            tienChuyenCanGoc = if (selectedRole.tienChuyenCanGoc > 0.0) selectedRole.tienChuyenCanGoc else batchCurrentCompany.tienChuyenCanGoc,
                                            allowanceCalcTypes = if (selectedRole.allowanceCalcTypes.isNotBlank()) selectedRole.allowanceCalcTypes else updated.allowanceCalcTypes
                                        )
                                    }
                                    updated
                                }
                            }
                        }
                        showBatchEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (selectedMode) {
                            1 -> AccentOrange
                            2 -> Color(0xFF00E676)
                            3 -> NeonBlue
                            else -> NeonBlue
                        }
                    )
                ) {
                    Text(
                        text = when (selectedMode) {
                            1 -> "Xác nhận Ra Ca (${selectedIds.size} NV)"
                            2 -> "Cập nhật Lương (${selectedIds.size} NV)"
                            3 -> "Gán Chức Vụ (${selectedIds.size} NV)"
                            else -> "Chấm Công (${selectedIds.size} NV)"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchEditDialog = false }) { Text("Hủy", color = Color.Gray) }
            },
            containerColor = DarkContainer
        )
    }

    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("Xác nhận xóa", color = White) },
            text = { Text("Bạn có chắc chắn muốn xóa ${selectedIds.size} nhân viên đã chọn? Hành động này không thể hoàn tác.", color = Color.Gray) },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.batchDeleteEmployees()
                        showBatchDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Xóa tất cả")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) { Text("Hủy") }
            },
            containerColor = DarkContainer
        )
    }

    if (showSingleDeleteConfirm && selectedEmployee != null) {
        AlertDialog(
            onDismissRequest = { showSingleDeleteConfirm = false },
            title = { Text("Xác nhận xóa", color = White) },
            text = { Text("Bạn có chắc chắn muốn xóa nhân viên '${selectedEmployee?.hoVaTen}'? Tất cả dữ liệu chấm công và cấu hình sẽ bị mất.", color = Color.Gray) },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteEmployee(selectedEmployee!!.userId)
                        showSingleDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Xác nhận xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSingleDeleteConfirm = false }) { Text("Hủy") }
            },
            containerColor = DarkContainer
        )
    }

    if (showSendNotifDialog) {
        SendAdminNotificationDialog(
            employees = employees,
            adminViewModel = adminViewModel,
            onDismiss = { showSendNotifDialog = false },
            onSend = { targetUid, targetName, title, message, type, isPinned ->
                adminViewModel.sendNotificationToEmployee(
                    targetUid = targetUid,
                    targetName = targetName,
                    title = title,
                    message = message,
                    type = type,
                    isPinned = isPinned
                ) { success ->
                    if (success) {
                        android.widget.Toast.makeText(
                            context,
                            if (isPinned) "Đã gửi & ghim thông báo" else "Đã gửi thông báo",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "❌ Lỗi gửi thông báo. Vui lòng kiểm tra kết nối mạng.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    showSendNotifDialog = false
                }
            }
        )
    }

    if (showAddEmployeeDialog) {
        var newName by remember { mutableStateOf("") }
        var newMsnv by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddEmployeeDialog = false },
            title = { Text("Thêm Nhân Viên", color = White) },
            text = {
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Họ và Tên") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White)
                    )
                    OutlinedTextField(
                        value = newMsnv,
                        onValueChange = { newMsnv = it },
                        label = { Text("Mã Số Nhân Viên") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White)
                    )
                }
            },
            confirmButton = {
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current
                Button(onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    val uid = "offline_" + UUID.randomUUID().toString().take(8)
                    adminViewModel.saveEmployeeConfig(UserConfig(userId = uid, hoVaTen = newName, maNhanVien = newMsnv))
                    showAddEmployeeDialog = false
                }) {
                    Text("Thêm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEmployeeDialog = false }) { Text("Hủy") }
            },
            containerColor = DarkContainer
        )
    }

    if (showBatchExportDialog) {
        val defaultMonthStr = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date())
        var exportInput by remember { mutableStateOf(defaultMonthStr) }

        val normalizedYymm = remember(exportInput) { normalizeMonthYearInput(exportInput) }
        val displayMonthLabel = remember(normalizedYymm) {
            try {
                val parts = normalizedYymm.split("-")
                "Tháng ${parts[1]}/${parts[0]}"
            } catch (e: Exception) {
                "Tháng $normalizedYymm"
            }
        }

        AlertDialog(
            onDismissRequest = { if (!isExportingByVM) showBatchExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Xuất Phiếu Lương Hàng Loạt", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current

                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isExportingByVM) {
                        Text("Đang tổng hợp và xuất dữ liệu phiếu lương... ${(exportProgressByVM * 100).toInt()}%", color = White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { exportProgressByVM },
                            modifier = Modifier.fillMaxWidth(),
                            color = NeonBlue,
                            trackColor = DarkBackground
                        )
                        Text("Vui lòng chờ trong giây lát...", color = Color.Gray, fontSize = 11.sp)
                    } else {
                        Text("Nhập tháng và năm cần xuất phiếu lương:", color = White, fontSize = 13.sp)

                        OutlinedTextField(
                            value = exportInput,
                            onValueChange = { exportInput = it },
                            label = { Text("Nhập Tháng/Năm (VD: 08/2026, 8-2026, 082026)") },
                            placeholder = { Text("08/2026") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White,
                                unfocusedTextColor = White,
                                focusedBorderColor = NeonBlue,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                            )
                        )

                        // Smart Auto-Detection Indicator Card
                        Surface(
                            color = NeonBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Hệ thống tự động nhận diện:", color = Color.LightGray, fontSize = 11.sp)
                                    Text("$displayMonthLabel (Mã: $normalizedYymm)", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        // Quick selection buttons
                        Text("Chọn nhanh tháng:", color = Color.Gray, fontSize = 11.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    exportInput = String.format(Locale.US, "%02d/%04d", cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
                                },
                                label = { Text("Tháng này", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonBlue.copy(alpha = 0.2f), selectedLabelColor = NeonBlue)
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.MONTH, -1)
                                    exportInput = String.format(Locale.US, "%02d/%04d", cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
                                },
                                label = { Text("Tháng trước", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentOrange.copy(alpha = 0.2f), selectedLabelColor = AccentOrange)
                            )
                        }

                        Text("⚡ Bạn có thể nhập số liền nhau (VD: 082026), cách nhau gạch (8-2026), xược (8/2026), hoặc gõ chữ (Tháng 8 năm 2026) hệ thống đều tự động nhận dạng chính xác.", color = Color.Gray, fontSize = 10.sp)

                        Text("Hệ thống sẽ tổng hợp và xuất phiếu lương cho toàn bộ ${employees.size} nhân viên dưới dạng hình ảnh (.PNG) lưu vào thư mục Download/TimeSnapPro.", color = Color.LightGray, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                if (!isExportingByVM) {
                    Button(
                        onClick = { adminViewModel.performBatchExport(context, normalizedYymm) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                    ) {
                        Text("Xuất $displayMonthLabel (${employees.size} NV)", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isExportingByVM) {
                    TextButton(onClick = { showBatchExportDialog = false }) { Text("Đóng", color = Color.Gray) }
                }
            },
            containerColor = DarkContainer
        )
    }
}

data class ShiftStatusInfo(
    val label: String,
    val color: Color,
    val timeDetail: String = ""
)

fun getEmployeeShiftStatus(rec: AttendanceRecord?): ShiftStatusInfo {
    if (rec == null) {
        return ShiftStatusInfo("Chưa vào ca", Color(0xFF8F9BB3), "")
    }
    val statusLower = rec.status.lowercase()
    if (statusLower.contains("phep") || statusLower.contains("leave")) {
        return ShiftStatusInfo("Nghỉ phép", Color(0xFFFFB74D), "")
    }

    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val inStr = if (rec.clockInTime != 0L) sdf.format(Date(rec.clockInTime)) else ""
    val outStr = rec.clockOutTime?.let { if (it != 0L) sdf.format(Date(it)) else null }

    return if (rec.clockInTime != 0L) {
        if (outStr == null) {
            ShiftStatusInfo(inStr, Color(0xFF00E676), "")
        } else {
            ShiftStatusInfo(outStr, Color(0xFF4C84FF), "")
        }
    } else {
        ShiftStatusInfo("Chưa vào ca", Color(0xFF8F9BB3), "")
    }
}

@Composable
fun EmployeeListView(
    companies: List<CompanyConfig>,
    selectedCompanyId: String?,
    onSelectCompany: (String?) -> Unit,
    onOpenCompanyManagement: () -> Unit,
    employees: List<UserConfig>,
    selectedIds: Set<String> = emptySet(),
    todayAttendanceMap: Map<String, AttendanceRecord> = emptyMap(),
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onEmployeeClick: (UserConfig) -> Unit,
    onEmployeeLongClick: (UserConfig) -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Company Filter Bar & Management Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompanySelectorBox(
                companies = companies,
                selectedCompanyId = selectedCompanyId,
                onSelectCompany = onSelectCompany,
                allowAllOption = true,
                allEmployees = employees,
                onAddCompany = { onOpenCompanyManagement() },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onOpenCompanyManagement,
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, NeonBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(52.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Business, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Cty & Phụ cấp", 
                    color = NeonBlue, 
                    fontSize = 13.sp, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("Tìm tên hoặc mã nhân viên...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonBlue) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            ),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedBorderColor = NeonBlue,
                unfocusedBorderColor = DarkContainer,
                focusedContainerColor = DarkContainer,
                unfocusedContainerColor = DarkContainer
            )
        )

        if (employees.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PersonOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(if (searchQuery.isEmpty()) "Chưa có nhân viên nào" else "Không tìm thấy kết quả", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val inShiftCount = employees.count { 
                        val status = getEmployeeShiftStatus(todayAttendanceMap[it.userId])
                        status.color == Color(0xFF00E676)
                    }
                    val outShiftCount = employees.count { 
                        val status = getEmployeeShiftStatus(todayAttendanceMap[it.userId])
                        status.color == Color(0xFF4C84FF)
                    }
                    val notInCount = employees.size - inShiftCount - outShiftCount

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Tổng: ${employees.size} NV",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🟢 Vào ca: $inShiftCount", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("🔵 Ra ca: $outShiftCount", color = Color(0xFF4C84FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("⚪ Chưa: $notInCount", color = Color(0xFF8F9BB3), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                items(employees) { employee ->
                    EmployeeCard(
                        employee = employee, 
                        isSelected = selectedIds.contains(employee.userId),
                        todayAttendanceRecord = todayAttendanceMap[employee.userId],
                        onClick = { onEmployeeClick(employee) },
                        onLongClick = { onEmployeeLongClick(employee) }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EmployeeCard(
    employee: UserConfig, 
    isSelected: Boolean,
    todayAttendanceRecord: AttendanceRecord? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val shiftStatus = remember(todayAttendanceRecord) { getEmployeeShiftStatus(todayAttendanceRecord) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) NeonBlue.copy(alpha = 0.15f) else DarkContainer,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isSelected) NeonBlue else Color.White.copy(alpha = 0.06f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact Avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (isSelected) NeonBlue else NeonBlue.copy(alpha = 0.12f), 
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = White, modifier = Modifier.size(18.dp))
                } else {
                    Text(
                        text = employee.hoVaTen.take(1).uppercase(),
                        color = NeonBlue,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Line 1: Employee Name + ADMIN Badge + ID
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = employee.hoVaTen, 
                            color = White, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        if (employee.isAdmin) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = AccentOrange.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "ADMIN", 
                                    color = AccentOrange, 
                                    fontSize = 8.sp, 
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ID: ${employee.maNhanVien}", 
                        color = Color.Gray, 
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Line 2: Shift Status Tag + Time Details + Department
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = shiftStatus.color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, shiftStatus.color.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(shiftStatus.color, shape = androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = shiftStatus.label,
                                color = shiftStatus.color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (shiftStatus.timeDetail.isNotEmpty()) {
                        Text(
                            text = shiftStatus.timeDetail,
                            color = shiftStatus.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (employee.boPhan.isNotEmpty()) {
                        Text(text = "•", color = Color.DarkGray, fontSize = 10.sp)
                        Text(
                            text = employee.boPhan,
                            color = NeonBlue.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

data class AttendanceStats(
    val workDays: Int,
    val leaveDays: Int,
    val totalHours: Double,
    val lateArrivals: Int
)

@Composable
fun EmployeeDetailView(
    employee: UserConfig,
    records: List<AttendanceRecord>,
    onBack: () -> Unit,
    onClose: () -> Unit,
    adminViewModel: AdminViewModel,
    onDeleteRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val sdfYm = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val calCurrent = remember { Calendar.getInstance() }
    val currentMonthYm = remember { sdfYm.format(calCurrent.time) }

    var selectedMonthYm by remember { mutableStateOf(currentMonthYm) }
    var isAllMonths by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }

    fun isRecordInMonth(dateStr: String, monthYmd: String): Boolean {
        return com.example.util.ExportUtils.isRecordInMonth(dateStr, monthYmd)
    }

    val filteredRecords = remember(records, selectedMonthYm, isAllMonths) {
        if (isAllMonths) {
            records
        } else {
            records.filter { isRecordInMonth(it.dateString, selectedMonthYm) }
        }
    }

    val monthStats = remember(filteredRecords, employee) {
        val workDays = filteredRecords.count { it.clockOutTime != null }
        val lateCount = filteredRecords.count { r ->
            val isLeave = com.example.data.SalaryCalculator.isLeaveType(r.status)
            if (!isLeave && r.clockInTime > 0) {
                val tempEntry = r.toTimeEntry()
                com.example.data.SalaryCalculator.calculateSingleEntry(tempEntry, employee).lateMinutes > 0
            } else {
                false
            }
        }
        val totalHrs = filteredRecords.sumOf { r ->
            if (r.clockOutTime != null) (r.clockOutTime - r.clockInTime) / 3600000.0 else 0.0
        }
        val leaves = filteredRecords.count { com.example.data.SalaryCalculator.isLeaveType(it.status) }
        AttendanceStats(workDays, leaves, totalHrs, lateCount)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val cal = Calendar.getInstance()
        val todayDmy = String.format(Locale.US, "%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
        val todayRec = records.find { r ->
            com.example.data.SalaryCalculator.normalizeDateToDmy(r.dateString) == todayDmy
        }
        val shiftStatus = remember(todayRec) { getEmployeeShiftStatus(todayRec) }

        // Employee Info Card (Large, High-End Card)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkContainer),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Square rounded avatar
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = PrimaryBlue.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.35f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = employee.hoVaTen.take(1).uppercase(),
                                color = PrimaryBlue,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = employee.hoVaTen.uppercase(),
                            color = White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ID: ${employee.maNhanVien}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            if (employee.emailDangKy.isNotEmpty()) {
                                Text("•", color = Color.Gray, fontSize = 10.sp)
                                Text(
                                    text = employee.emailDangKy,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }

                            Surface(
                                color = if (employee.isAdmin) Color(0xFFF59E0B).copy(alpha = 0.18f) else PrimaryBlue.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (employee.isAdmin) Color(0xFFF59E0B).copy(alpha = 0.4f) else PrimaryBlue.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    if (employee.isAdmin) "ADMIN" else "NV",
                                    color = if (employee.isAdmin) Color(0xFFF59E0B) else PrimaryBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { /* Detail action */ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Chi tiết",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Shift Status Bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(shiftStatus.color, androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hôm nay: ${shiftStatus.label} ${if (shiftStatus.timeDetail.isNotEmpty()) "(${shiftStatus.timeDetail})" else ""}",
                            color = shiftStatus.color,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (todayRec == null || todayRec.clockInTime == 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val now = System.currentTimeMillis()
                                    adminViewModel.saveAttendanceRecord(
                                        AttendanceRecord(
                                            uid = employee.userId,
                                            dateString = todayDmy,
                                            clockInTime = now,
                                            status = "NORMAL"
                                        )
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                Text("Vào ca", color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    cal.set(Calendar.HOUR_OF_DAY, 8)
                                    cal.set(Calendar.MINUTE, 0)
                                    val leaveTime = cal.timeInMillis
                                    adminViewModel.saveAttendanceRecord(
                                        AttendanceRecord(
                                            uid = employee.userId,
                                            dateString = todayDmy,
                                            clockInTime = leaveTime,
                                            status = "PAID_LEAVE",
                                            notes = "Nghỉ phép có lương"
                                        )
                                    )
                                    if (employee.phepNamConLai > 0) {
                                        adminViewModel.saveEmployeeConfig(employee.copy(phepNamConLai = (employee.phepNamConLai - 1).coerceAtLeast(0)))
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2C94C))
                            ) {
                                Text("Nghỉ phép", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    cal.set(Calendar.HOUR_OF_DAY, 8)
                                    cal.set(Calendar.MINUTE, 0)
                                    val leaveTime = cal.timeInMillis
                                    adminViewModel.saveAttendanceRecord(
                                        AttendanceRecord(
                                            uid = employee.userId,
                                            dateString = todayDmy,
                                            clockInTime = leaveTime,
                                            status = "UNAUTHORIZED_LEAVE",
                                            notes = "Nghỉ không phép"
                                        )
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB5757))
                            ) {
                                Text("Không phép", color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (todayRec.clockOutTime == null || todayRec.clockOutTime == 0L) {
                        // Admin muốn ra ca thì xoá ngày công này và thêm giờ vào, giờ ra thủ công, chứ không được sửa giờ ra trực tiếp
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF312E81).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF4338CA), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "💡 Nhân viên đang trong ca làm việc. Để ra ca, vui lòng xóa ngày công này và bấm Thêm công mới.",
                                color = Color(0xFFC7D2FE),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = DarkBackground,
            contentColor = NeonBlue,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = NeonBlue,
                    height = 3.dp
                )
            },
            divider = {
                HorizontalDivider(color = DarkContainer, thickness = 1.dp)
            }
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text("Cấu Hình", fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text("Chấm Công", fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = pagerState.currentPage == 2,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                text = { Text("Phiếu Lương", fontWeight = if (pagerState.currentPage == 2) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        // MONTH NAVIGATION BAR FOR CHẤM CÔNG AND PHIẾU LƯƠNG
        if (pagerState.currentPage == 1 || pagerState.currentPage == 2) {
            Surface(
                color = DarkBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkContainer, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, NeonBlue.copy(alpha = 0.35f)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isAllMonths = false
                            try {
                                val d = sdfYm.parse(selectedMonthYm) ?: Date()
                                val c = Calendar.getInstance().apply { time = d }
                                c.add(Calendar.MONTH, -1)
                                selectedMonthYm = sdfYm.format(c.time)
                            } catch (e: Exception) {
                                selectedMonthYm = currentMonthYm
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Tháng trước",
                            tint = NeonBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    val displayMonthStr = remember(selectedMonthYm, isAllMonths) {
                        if (isAllMonths) "Tất cả các tháng"
                        else {
                            try {
                                val d = sdfYm.parse(selectedMonthYm) ?: Date()
                                val fmt = SimpleDateFormat("'Tháng' MM/yyyy", Locale("vi", "VN"))
                                fmt.format(d)
                            } catch (e: Exception) {
                                "Tháng $selectedMonthYm"
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showCalendarDialog = true
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Xem lịch chấm công",
                            tint = NeonBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = displayMonthStr,
                            color = White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            isAllMonths = false
                            try {
                                val d = sdfYm.parse(selectedMonthYm) ?: Date()
                                val c = Calendar.getInstance().apply { time = d }
                                c.add(Calendar.MONTH, 1)
                                selectedMonthYm = sdfYm.format(c.time)
                            } catch (e: Exception) {
                                selectedMonthYm = currentMonthYm
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Tháng sau",
                            tint = NeonBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        if (showCalendarDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showCalendarDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF121212)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Lịch chấm công - ${employee.hoVaTen}",
                                    color = White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Chấm công & lịch sử theo ngày",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(onClick = { showCalendarDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Đóng", tint = White)
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        Box(modifier = Modifier.weight(1f)) {
                            TabHistoryContent(
                                userId = employee.userId,
                                userConfig = employee,
                                attendanceLogs = records,
                                onRecordsChanged = {
                                    adminViewModel.loadTodayAttendance()
                                }
                            )
                        }
                    }
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            if (page == 0) {
                EmployeeConfigEdit(
                    employee = employee, 
                    onSave = { adminViewModel.saveEmployeeConfig(it) },
                    onDelete = onDeleteRequest
                )
            } else if (page == 1) {
                var showAddAttendanceDialog by remember { mutableStateOf(false) }
                @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                ) {
                    item {
                        // Attendance Summary Board
                        AttendanceSummaryBoard(
                            stats = monthStats,
                            totalRecordCount = filteredRecords.size,
                            filteredRecords = filteredRecords,
                            employee = employee,
                            onShowCalendar = { showCalendarDialog = true }
                        )
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dữ liệu chấm công", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            TextButton(
                                onClick = { showAddAttendanceDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = NeonBlue)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Thêm công", fontSize = 14.sp)
                            }
                        }
                    }
                    
                    if (filteredRecords.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.EventBusy,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Không có dữ liệu chấm công cho khoảng thời gian này",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredRecords.sortedByDescending { it.dateString }) { record ->
                            AttendanceRecordItem(record = record, employee = employee, onDelete = { adminViewModel.deleteAttendanceRecord(employee.userId, record.dateString) })
                        }
                    }
                }
                
                if (showAddAttendanceDialog) {
                    val todayCal = remember { Calendar.getInstance() }
                    var dayStr by remember { mutableStateOf(TextFieldValue(String.format("%02d", todayCal.get(Calendar.DAY_OF_MONTH)))) }
                    var monthStr by remember { mutableStateOf(TextFieldValue(String.format("%02d", todayCal.get(Calendar.MONTH) + 1))) }
                    var yearStr by remember { mutableStateOf(TextFieldValue(String.format("%04d", todayCal.get(Calendar.YEAR)))) }

                    var recordType by remember { mutableStateOf("NORMAL") } // "NORMAL", "PAID_LEAVE", "UNPAID_LEAVE", "HOLIDAY_LEAVE"
                    var noteText by remember { mutableStateOf("") }

                    var checkInHour by remember { mutableStateOf(TextFieldValue("08")) }
                    var checkInMin by remember { mutableStateOf(TextFieldValue("00")) }
                    var checkOutHour by remember { mutableStateOf(TextFieldValue("17")) }
                    var checkOutMin by remember { mutableStateOf(TextFieldValue("00")) }
                    
                    val focusRequesters = remember { List(7) { FocusRequester() } }

                    val dVal = dayStr.text.trim().padStart(2, '0')
                    val mVal = monthStr.text.trim().padStart(2, '0')
                    val yVal = yearStr.text.trim()
                    val dialogDateStr = "$dVal/$mVal/$yVal"
                    val isDayHoliday = com.example.data.SalaryCalculator.isHoliday(dialogDateStr)

                    LaunchedEffect(isDayHoliday) {
                        if (isDayHoliday) {
                            recordType = "HOLIDAY_LEAVE"
                        } else {
                            if (recordType == "HOLIDAY_LEAVE") {
                                recordType = "NORMAL"
                            }
                        }
                    }

                    AlertDialog(
                        onDismissRequest = { showAddAttendanceDialog = false },
                        title = { Text("Thêm / Nhập ngày công", color = White) },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                @Composable
                                fun AutoJumpField(
                                    value: TextFieldValue,
                                    onValueChange: (TextFieldValue) -> Unit,
                                    focusRequester: FocusRequester,
                                    nextFocusRequester: FocusRequester? = null,
                                    maxLength: Int = 2,
                                    modifier: Modifier = Modifier
                                ) {
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
                                            val filtered = it.text.filter { char -> char.isDigit() }
                                            if (filtered.length <= maxLength) {
                                                val newVal = it.copy(text = filtered)
                                                val textChanged = newVal.text != value.text
                                                onValueChange(newVal)
                                                if (textChanged && newVal.text.length == maxLength && nextFocusRequester != null) {
                                                    nextFocusRequester.requestFocus()
                                                }
                                            }
                                        },
                                        modifier = modifier.focusRequester(focusRequester),
                                        interactionSource = interactionSource,
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = LocalTextStyle.current.copy(
                                            textAlign = TextAlign.Center, 
                                            fontSize = 15.sp,
                                            color = White
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonBlue,
                                            unfocusedBorderColor = Color.Gray,
                                            focusedTextColor = White,
                                            unfocusedTextColor = White
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number, 
                                            imeAction = if (nextFocusRequester != null) ImeAction.Next else ImeAction.Done
                                        )
                                    )
                                }

                                // Record Type Selector
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Loại ngày:", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val types = remember(isDayHoliday) {
                                            val list = mutableListOf(
                                                "NORMAL" to "Đi làm",
                                                "PAID_LEAVE" to "Phép năm",
                                                "UNPAID_LEAVE" to "Nghỉ KL",
                                                "UNAUTHORIZED_LEAVE" to "Nghỉ KP"
                                            )
                                            if (isDayHoliday) {
                                                list.add("HOLIDAY_LEAVE" to "Nghỉ lễ")
                                            }
                                            list
                                        }
                                        types.forEach { (type, label) ->
                                            val isSelected = recordType == type
                                            val chipBg = when {
                                                !isSelected -> DarkContainer
                                                type == "PAID_LEAVE" -> Color(0xFFF2C94C)
                                                type == "UNPAID_LEAVE" -> Color(0xFFFF9800)
                                                type == "UNAUTHORIZED_LEAVE" -> Color(0xFFEB5757)
                                                type == "HOLIDAY_LEAVE" -> Color(0xFFBB86FC)
                                                else -> NeonBlue
                                            }
                                            val chipTextCol = if (isSelected && type == "PAID_LEAVE") Color.Black else White

                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { recordType = type },
                                                color = chipBg,
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, if (isSelected) chipBg else Color.Gray.copy(alpha = 0.5f))
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = chipTextCol,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Date input: Ngày (dd/MM/yyyy)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Ngày (dd/MM/yyyy):", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AutoJumpField(
                                            value = dayStr,
                                            onValueChange = { dayStr = it },
                                            focusRequester = focusRequesters[0],
                                            nextFocusRequester = focusRequesters[1],
                                            maxLength = 2,
                                            modifier = Modifier.width(55.dp)
                                        )
                                        Text("/", color = White, fontWeight = FontWeight.Bold)
                                        AutoJumpField(
                                            value = monthStr,
                                            onValueChange = { monthStr = it },
                                            focusRequester = focusRequesters[1],
                                            nextFocusRequester = focusRequesters[2],
                                            maxLength = 2,
                                            modifier = Modifier.width(55.dp)
                                        )
                                        Text("/", color = White, fontWeight = FontWeight.Bold)
                                        AutoJumpField(
                                            value = yearStr,
                                            onValueChange = { yearStr = it },
                                            focusRequester = focusRequesters[2],
                                            nextFocusRequester = if (recordType == "NORMAL") focusRequesters[3] else null,
                                            maxLength = 4,
                                            modifier = Modifier.width(75.dp)
                                        )
                                    }
                                }

                                // Hour input: Giờ vào & Giờ ra (ONLY IF NORMAL)
                                if (recordType == "NORMAL") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Giờ vào (HH:mm)
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("Giờ vào (HH:mm):", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                AutoJumpField(
                                                    value = checkInHour,
                                                    onValueChange = { checkInHour = it },
                                                    focusRequester = focusRequesters[3],
                                                    nextFocusRequester = focusRequesters[4],
                                                    maxLength = 2,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(":", color = White, fontWeight = FontWeight.Bold)
                                                AutoJumpField(
                                                    value = checkInMin,
                                                    onValueChange = { checkInMin = it },
                                                    focusRequester = focusRequesters[4],
                                                    nextFocusRequester = focusRequesters[5],
                                                    maxLength = 2,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        // Giờ ra (HH:mm)
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("Giờ ra (HH:mm):", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                AutoJumpField(
                                                    value = checkOutHour,
                                                    onValueChange = { checkOutHour = it },
                                                    focusRequester = focusRequesters[5],
                                                    nextFocusRequester = focusRequesters[6],
                                                    maxLength = 2,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(":", color = White, fontWeight = FontWeight.Bold)
                                                AutoJumpField(
                                                    value = checkOutMin,
                                                    onValueChange = { checkOutMin = it },
                                                    focusRequester = focusRequesters[6],
                                                    nextFocusRequester = null,
                                                    maxLength = 2,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Note for Leave
                                    OutlinedTextField(
                                        value = noteText,
                                        onValueChange = { noteText = it },
                                        label = { Text("Ghi chú nghỉ phép", fontSize = 12.sp) },
                                        placeholder = { Text(if (recordType == "PAID_LEAVE") "Nghỉ phép có lương" else "Lý do nghỉ...", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = White,
                                            unfocusedTextColor = White,
                                            focusedBorderColor = NeonBlue,
                                            unfocusedBorderColor = Color.Gray
                                        )
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                try {
                                    val d = dayStr.text.trim().padStart(2, '0')
                                    val m = monthStr.text.trim().padStart(2, '0')
                                    val y = yearStr.text.trim().padStart(4, '0')
                                    val dbDateStr = "$d/$m/$y"

                                    val inH = checkInHour.text.trim().padStart(2, '0')
                                    val inM = checkInMin.text.trim().padStart(2, '0')
                                    val outHStr = checkOutHour.text.trim()
                                    val outMStr = checkOutMin.text.trim()

                                    val isLeave = recordType == "PAID_LEAVE" || 
                                                  recordType == "UNPAID_LEAVE" || 
                                                  recordType == "UNAUTHORIZED_LEAVE" || 
                                                  recordType == "HOLIDAY_LEAVE"

                                    val fullIn = if (isLeave) {
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse("$dbDateStr 08:00")?.time ?: 0L
                                    } else if (recordType == "NORMAL" && inH.isNotBlank() && inM.isNotBlank()) {
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse("$dbDateStr $inH:$inM")?.time ?: 0L
                                    } else {
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse("$dbDateStr 08:00")?.time ?: System.currentTimeMillis()
                                    }
                                    
                                    var fullOut = if (isLeave) {
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse("$dbDateStr 17:00")?.time
                                    } else if (recordType == "NORMAL" && outHStr.isNotEmpty() && outMStr.isNotEmpty()) {
                                        val outH = outHStr.padStart(2, '0')
                                        val outM = outMStr.padStart(2, '0')
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse("$dbDateStr $outH:$outM")?.time
                                    } else {
                                        null
                                    }
                                    if (fullOut != null && fullOut <= fullIn) {
                                        fullOut += 24 * 3600 * 1000L
                                    }

                                    val defaultNote = when (recordType) {
                                        "PAID_LEAVE" -> "Nghỉ phép có lương"
                                        "UNPAID_LEAVE" -> "Nghỉ không lương"
                                        "UNAUTHORIZED_LEAVE" -> "Nghỉ không phép"
                                        "HOLIDAY_LEAVE" -> "Nghỉ lễ"
                                        else -> ""
                                    }
                                    val finalNote = if (noteText.isNotBlank()) noteText.trim() else defaultNote

                                    adminViewModel.saveAttendanceRecord(
                                        AttendanceRecord(
                                            uid = employee.userId, 
                                            dateString = dbDateStr, 
                                            clockInTime = fullIn, 
                                            clockOutTime = fullOut,
                                            status = recordType,
                                            notes = finalNote
                                        )
                                    )

                                    if (recordType == "PAID_LEAVE" && employee.phepNamConLai > 0) {
                                        adminViewModel.saveEmployeeConfig(employee.copy(phepNamConLai = (employee.phepNamConLai - 1).coerceAtLeast(0)))
                                    }
                                } catch (e: Exception) {}
                                showAddAttendanceDialog = false
                            }) { Text("Lưu") }
                        },
                        dismissButton = { TextButton(onClick = { showAddAttendanceDialog = false }) { Text("Hủy") } },
                        containerColor = DarkContainer
                    )
                }
            } else {
                EmployeePayslipView(
                    employee = employee,
                    records = filteredRecords,
                    selectedMonthYm = selectedMonthYm,
                    isAllMonths = isAllMonths
                )
            }
        }
    }
}

@Composable
fun AttendanceSummaryBoard(
    stats: AttendanceStats,
    totalRecordCount: Int = 0,
    filteredRecords: List<AttendanceRecord> = emptyList(),
    employee: UserConfig,
    onShowCalendar: () -> Unit
) {
    var showLeavesDetailDialog by remember { mutableStateOf(false) }
    var showHoursChartDialog by remember { mutableStateOf(false) }
    var showLateDetailsDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = DarkContainer),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(PrimaryBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TỔNG QUAN THÁNG",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    color = PrimaryBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "$totalRecordCount ngày ghi nhận",
                        color = PrimaryBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 4 Key Metrics Cards Grid (2x2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryItem(
                    label = "Ngày công",
                    value = "${stats.workDays}",
                    unit = "ngày",
                    icon = Icons.Default.WorkHistory,
                    accentColor = SuccessGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onShowCalendar
                )
                SummaryItem(
                    label = "Tổng giờ",
                    value = String.format(Locale.US, "%.1f", stats.totalHours),
                    unit = "giờ",
                    icon = Icons.Default.Timer,
                    accentColor = PrimaryBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { showHoursChartDialog = true }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryItem(
                    label = "Đi trễ",
                    value = "${stats.lateArrivals}",
                    unit = "lần",
                    icon = Icons.Default.Schedule,
                    accentColor = if (stats.lateArrivals > 0) Color(0xFFF59E0B) else TextSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { showLateDetailsDialog = true }
                )
                SummaryItem(
                    label = "Nghỉ phép",
                    value = "${stats.leaveDays}",
                    unit = "ngày",
                    icon = Icons.Default.EventBusy,
                    accentColor = Color(0xFFA855F7),
                    modifier = Modifier.weight(1f),
                    onClick = { showLeavesDetailDialog = true }
                )
            }

            // Monthly Progress Indicator (Standard 160h)
            val progress = (stats.totalHours / 160.0).coerceIn(0.0, 1.0).toFloat()
            Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tiến độ giờ chuẩn (160h/tháng)", color = TextSecondary, fontSize = 12.sp)
                    Text("${(progress * 100).toInt()}%", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = PrimaryBlue,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }

    // --- LEAVES DETAIL DIALOG ---
    if (showLeavesDetailDialog) {
        val leaveRecords = remember(filteredRecords) {
            filteredRecords.filter { com.example.data.SalaryCalculator.isLeaveType(it.status) }
                .sortedByDescending { 
                    try {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.dateString)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }
        }

        AlertDialog(
            onDismissRequest = { showLeavesDetailDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color(0xFFA855F7))
                    Text("Danh Sách Ngày Nghỉ Phép", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (leaveRecords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SentimentSatisfied, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Không có ngày nghỉ phép nào trong tháng.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(leaveRecords) { r ->
                            val statusUpper = r.status.uppercase(Locale.ROOT)
                            val typeLabel = when {
                                statusUpper == "PAID_LEAVE" || statusUpper == "PAID" || statusUpper == "NP" || statusUpper == "PHEP" -> "Phép năm"
                                statusUpper == "UNAUTHORIZED_LEAVE" || statusUpper == "KP" -> "Nghỉ không phép"
                                statusUpper == "UNPAID_LEAVE" || statusUpper == "UNPAID" -> "Nghỉ không lương"
                                statusUpper == "HOLIDAY_LEAVE" || statusUpper == "HOLIDAY" || statusUpper.contains("LỄ") -> "Nghỉ lễ"
                                else -> "Nghỉ phép"
                            }
                            val badgeColor = when {
                                typeLabel == "Phép năm" -> Color(0xFFF2C94C)
                                typeLabel == "Nghỉ không phép" -> Color(0xFFEB5757)
                                typeLabel == "Nghỉ không lương" -> Color(0xFFFF9800)
                                typeLabel == "Nghỉ lễ" -> Color(0xFFBB86FC)
                                else -> Color(0xFFA855F7)
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = r.dateString,
                                            color = White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        if (!r.notes.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Lý do: ${r.notes}",
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = typeLabel,
                                            color = badgeColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLeavesDetailDialog = false }) {
                    Text("Đóng", color = NeonBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkContainer
        )
    }

    // --- HOURS WORKED CHART DIALOG ---
    if (showHoursChartDialog) {
        AlertDialog(
            onDismissRequest = { showHoursChartDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = PrimaryBlue)
                    Text("Biểu Đồ Giờ Làm Trong Tháng", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                MonthlyWorkHoursChart(records = filteredRecords)
            },
            confirmButton = {
                TextButton(onClick = { showHoursChartDialog = false }) {
                    Text("Đóng", color = NeonBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkContainer
        )
    }

    // --- LATE DETAILS DIALOG ---
    if (showLateDetailsDialog) {
        val lateRecords = remember(filteredRecords, employee) {
            filteredRecords.filter { r ->
                val isLeave = com.example.data.SalaryCalculator.isLeaveType(r.status)
                if (!isLeave && r.clockInTime > 0) {
                    com.example.data.SalaryCalculator.calculateSingleEntry(r.toTimeEntry(), employee).lateMinutes > 0
                } else {
                    false
                }
            }.sortedByDescending { 
                try {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.dateString)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showLateDetailsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFF59E0B))
                    Text("Chi Tiết Đi Trễ", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (lateRecords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tuyệt vời! Nhân viên không đi trễ lần nào.", color = SuccessGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(lateRecords) { r ->
                            val calc = com.example.data.SalaryCalculator.calculateSingleEntry(r.toTimeEntry(), employee)
                            val lateMins = calc.lateMinutes
                            val inTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(r.clockInTime))

                            val formattedLate = if (lateMins < 60) {
                                "$lateMins phút"
                            } else {
                                val h = lateMins / 60
                                val m = lateMins % 60
                                if (m == 0) "$h giờ" else "$h giờ $m phút"
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = r.dateString,
                                            color = White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Giờ vào: $inTimeStr",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Surface(
                                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "Trễ $formattedLate",
                                            color = Color(0xFFF59E0B),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLateDetailsDialog = false }) {
                    Text("Đóng", color = NeonBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkContainer
        )
    }
}

@Composable
fun MonthlyWorkHoursChart(records: List<AttendanceRecord>) {
    val workedRecords = remember(records) {
        records.filter { it.clockInTime > 0 && it.clockOutTime != null }
            .sortedBy { 
                try {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.dateString)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
    }

    if (workedRecords.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Không có dữ liệu giờ làm trong tháng.", color = Color.Gray, fontSize = 13.sp)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            val hoursList = workedRecords.map { r ->
                (r.clockOutTime!! - r.clockInTime) / 3600000.0
            }
            val maxHours = (hoursList.maxOrNull() ?: 8.0).coerceAtLeast(1.0)

            Text("Giờ làm hàng ngày (Vuốt để xem toàn bộ):", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                workedRecords.forEach { record ->
                    val hrs = (record.clockOutTime!! - record.clockInTime) / 3600000.0
                    val barHeightFraction = (hrs / maxHours).coerceIn(0.01, 1.0).toFloat()
                    val isNight = com.example.data.SalaryCalculator.isNightShift(record.clockInTime, record.clockOutTime)
                    
                    val dayNum = try {
                        val parts = record.dateString.split("/")
                        parts.firstOrNull() ?: record.dateString
                    } catch (e: Exception) {
                        record.dateString
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(36.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", hrs),
                            color = if (isNight) NightPurple else White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .weight(1f, fill = false)
                                .fillMaxHeight(barHeightFraction)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = if (isNight) {
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
                            text = dayNum,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))
            
            val dayHours = workedRecords.filter { record ->
                !com.example.data.SalaryCalculator.isNightShift(record.clockInTime, record.clockOutTime)
            }.sumOf { (it.clockOutTime!! - it.clockInTime) / 3600000.0 }

            val nightHours = workedRecords.filter { record ->
                com.example.data.SalaryCalculator.isNightShift(record.clockInTime, record.clockOutTime)
            }.sumOf { (it.clockOutTime!! - it.clockInTime) / 3600000.0 }

            val total = dayHours + nightHours
            val avg = if (workedRecords.isNotEmpty()) total / workedRecords.size else 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Trung bình/ngày", color = Color.Gray, fontSize = 11.sp)
                    Text(String.format(Locale.US, "%.1f giờ", avg), color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Tổng số giờ làm", color = Color.Gray, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (dayHours > 0.0) {
                            Text(String.format(Locale.US, "Ca ngày: %.1fh", dayHours), color = NeonBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        if (nightHours > 0.0) {
                            Text(String.format(Locale.US, "Ca đêm: %.1fh", nightHours), color = NightPurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        if (dayHours == 0.0 && nightHours == 0.0) {
                            Text("0.0 giờ", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryItem(
    label: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(unit, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 1.dp))
                }
            }
        }
    }
}

@Composable
fun ConfigSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkContainer.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun EmployeeConfigEdit(
    adminViewModel: AdminViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    employee: UserConfig, 
    onSave: (UserConfig) -> Unit,
    onDelete: () -> Unit
) {
    val companies by adminViewModel.companies.collectAsStateWithLifecycle()
    
    var name by remember { mutableStateOf(employee.hoVaTen) }
    var msnv by remember { mutableStateOf(employee.maNhanVien) }
    var companyId by remember { mutableStateOf(employee.companyId) }
    var roleId by remember { mutableStateOf(employee.roleId) }
    
    val currentCompany = companies.find { it.companyId == companyId } ?: companies.firstOrNull() ?: CompanyConfig.DEFAULT_COMPANY
    val roles = currentCompany.getRoles()
    
    var expandedCompany by remember { mutableStateOf(false) }
    var expandedRole by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Chỉnh sửa thông tin nhân viên", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        AdminInputField("Họ và Tên", name, { name = it })
        AdminInputField("Mã Nhân Viên", msnv, { msnv = it })
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Công ty", color = Color.Gray, fontSize = 14.sp)
        Box {
            androidx.compose.material3.OutlinedButton(onClick = { expandedCompany = true }, modifier = Modifier.fillMaxWidth()) {
                Text(currentCompany.companyName, color = White)
            }
            androidx.compose.material3.DropdownMenu(expanded = expandedCompany, onDismissRequest = { expandedCompany = false }) {
                companies.forEach { comp ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(comp.companyName) },
                        onClick = {
                            companyId = comp.companyId
                            roleId = "" // reset role when company changes
                            expandedCompany = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Chức vụ", color = Color.Gray, fontSize = 14.sp)
        val selectedRole = roles.find { it.roleId == roleId }
        Box {
            androidx.compose.material3.OutlinedButton(onClick = { expandedRole = true }, modifier = Modifier.fillMaxWidth()) {
                Text(if (selectedRole != null) selectedRole.roleName else "Chưa phân công", color = White)
            }
            androidx.compose.material3.DropdownMenu(expanded = expandedRole, onDismissRequest = { expandedRole = false }) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("Chưa phân công") },
                    onClick = {
                        roleId = ""
                        expandedRole = false
                    }
                )
                roles.forEach { role ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(role.roleName) },
                        onClick = {
                            roleId = role.roleId
                            expandedRole = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            androidx.compose.material3.TextButton(onClick = onDelete) {
                Text("Xóa nhân viên", color = androidx.compose.ui.graphics.Color.Red)
            }
            androidx.compose.material3.Button(
                onClick = {
                    val updatedRole = roles.find { it.roleId == roleId }
                    var updatedUser = employee.copy(
                        hoVaTen = name, 
                        maNhanVien = msnv,
                        companyId = currentCompany.companyId,
                        companyName = currentCompany.companyName,
                        companyCode = currentCompany.companyCode,
                        roleId = roleId,
                        roleName = updatedRole?.roleName ?: ""
                    )
                    if (updatedRole != null) {
                        updatedUser = updatedUser.copy(
                            luongCoBan = if (updatedRole.luongCoBan > 0.0) updatedRole.luongCoBan else currentCompany.luongCoBan,
                            pcKyThuat = if (updatedRole.pcKyThuat > 0.0) updatedRole.pcKyThuat else currentCompany.pcKyThuat,
                            pcTrachNhiem = if (updatedRole.pcTrachNhiem > 0.0) updatedRole.pcTrachNhiem else currentCompany.pcTrachNhiem,
                            pcChucVu = if (updatedRole.pcChucVu > 0.0) updatedRole.pcChucVu else currentCompany.pcChucVu,
                            pcHieuSuat = if (updatedRole.pcHieuSuat > 0.0) updatedRole.pcHieuSuat else currentCompany.pcHieuSuat,
                            pcSanPham = if (updatedRole.pcSanPham > 0.0) updatedRole.pcSanPham else currentCompany.pcSanPham,
                            pcComCa = if (updatedRole.pcComCa > 0.0) updatedRole.pcComCa else currentCompany.pcComCa,
                            pcComOt = if (updatedRole.pcComOt > 0.0) updatedRole.pcComOt else currentCompany.pcComOt,
                            pcNhaO = if (updatedRole.pcNhaO > 0.0) updatedRole.pcNhaO else currentCompany.pcNhaO,
                            pcDocHai = if (updatedRole.pcDocHai > 0.0) updatedRole.pcDocHai else currentCompany.pcDocHai,
                            pcDtDoanhThu = if (updatedRole.pcDtDoanhThu > 0.0) updatedRole.pcDtDoanhThu else currentCompany.pcDtDoanhThu,
                            pcXangXe = if (updatedRole.pcXangXe > 0.0) updatedRole.pcXangXe else currentCompany.pcXangXe,
                            pcThamNien = if (updatedRole.pcThamNien > 0.0) updatedRole.pcThamNien else currentCompany.pcThamNien,
                            pcKhac1 = if (updatedRole.pcKhac1 > 0.0) updatedRole.pcKhac1 else currentCompany.pcKhac1,
                            pcCaDem = if (updatedRole.pcCaDem > 0.0) updatedRole.pcCaDem else currentCompany.pcCaDem,
                            tienChuyenCanGoc = if (updatedRole.tienChuyenCanGoc > 0.0) updatedRole.tienChuyenCanGoc else currentCompany.tienChuyenCanGoc,
                            allowanceCalcTypes = if (updatedRole.allowanceCalcTypes.isNotBlank()) updatedRole.allowanceCalcTypes else updatedUser.allowanceCalcTypes
                        )
                    } else {
                        updatedUser = updatedUser.copy(
                            luongCoBan = currentCompany.luongCoBan,
                            pcKyThuat = currentCompany.pcKyThuat,
                            pcTrachNhiem = currentCompany.pcTrachNhiem,
                            pcChucVu = currentCompany.pcChucVu,
                            pcHieuSuat = currentCompany.pcHieuSuat,
                            pcSanPham = currentCompany.pcSanPham,
                            pcComCa = currentCompany.pcComCa,
                            pcComOt = currentCompany.pcComOt,
                            pcNhaO = currentCompany.pcNhaO,
                            pcDocHai = currentCompany.pcDocHai,
                            pcDtDoanhThu = currentCompany.pcDtDoanhThu,
                            pcXangXe = currentCompany.pcXangXe,
                            pcThamNien = currentCompany.pcThamNien,
                            pcKhac1 = currentCompany.pcKhac1,
                            pcCaDem = currentCompany.pcCaDem,
                            tienChuyenCanGoc = currentCompany.tienChuyenCanGoc
                        )
                    }
                    onSave(updatedUser)
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = NeonBlue)
            ) {
                Text("Lưu", color = White)
            }
        }
    }
}

@Composable
fun CompanySelectorBox(
    companies: List<CompanyConfig>,
    selectedCompanyId: String?,
    onSelectCompany: (String?) -> Unit,
    allowAllOption: Boolean = true,
    allEmployees: List<UserConfig> = emptyList(),
    onAddCompany: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currentCompany = companies.find { it.companyId == selectedCompanyId }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable { expanded = true },
            color = DarkBackground.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Công ty", color = Color.Gray, fontSize = 10.sp)
                        Text(
                            text = if (selectedCompanyId == null) "Tất cả công ty" else currentCompany?.companyName ?: "Không xác định",
                            color = White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = LightGray)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(DarkContainer)
                .border(1.dp, NeonBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        ) {
            if (allowAllOption) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Tất cả công ty", color = if (selectedCompanyId == null) NeonBlue else White, fontWeight = if (selectedCompanyId == null) FontWeight.Bold else FontWeight.Normal)
                            Text("${allEmployees.size} NV", color = Color.Gray, fontSize = 11.sp)
                        }
                    },
                    onClick = {
                        onSelectCompany(null)
                        expanded = false
                    }
                )
                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
            }

            companies.forEach { comp ->
                val isSelected = comp.companyId == selectedCompanyId
                val count = allEmployees.count { it.companyId == comp.companyId || (comp.companyId == "default_company" && (it.companyId.isBlank() || it.companyId == "default_company")) }
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(comp.companyName, color = if (isSelected) NeonBlue else White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text("Mã: ${comp.companyCode}", color = Color.Gray, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = if (isSelected) NeonBlue.copy(alpha = 0.2f) else DarkBackground,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("$count NV", color = if (isSelected) NeonBlue else LightGray, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    },
                    onClick = {
                        onSelectCompany(comp.companyId)
                        expanded = false
                    }
                )
            }

            if (onAddCompany != null) {
                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AddBusiness, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("+ Thêm công ty mới", color = NeonBlue, fontWeight = FontWeight.Bold)
                        }
                    },
                    onClick = {
                        expanded = false
                        onAddCompany()
                    }
                )
            }
        }
    }
}

@Composable
fun AdminInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isNumeric: Boolean = false,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = if (isNumeric) androidx.compose.ui.text.input.KeyboardType.Number else androidx.compose.ui.text.input.KeyboardType.Text,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 6
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { androidx.compose.material3.Text(label) },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonBlue,
            unfocusedBorderColor = Color.DarkGray,
            focusedTextColor = White,
            unfocusedTextColor = LightGray
        )
    )
}

fun autoFormatTimeInput(input: String): String {
    val clean = input.filter { it.isDigit() }.take(4)
    if (clean.length > 2) {
        return clean.substring(0, 2) + ":" + clean.substring(2)
    }
    return clean
}

fun normalizeMonthYearInput(input: String): String {
    try {
        val parts = input.split("/")
        if (parts.size == 2) {
            val mm = parts[0].padStart(2, '0')
            val yyyy = parts[1]
            return "$yyyy-$mm"
        }
    } catch (e: Exception) {}
    return "2024-01"
}

@Composable
fun SendAdminNotificationDialog(
    employees: List<UserConfig>,
    adminViewModel: AdminViewModel,
    onDismiss: () -> Unit,
    onSend: (targetUid: String, targetName: String, title: String, message: String, type: String, isPinned: Boolean) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Gửi mới, 1: Quản lý & Xóa

    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }
    var expandedUserDropdown by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<UserConfig?>(null) } // null means "Tất cả nhân viên"

    val sentNotifications by adminViewModel.sentNotifications.collectAsStateWithLifecycle()
    val isLoadingNotifs by adminViewModel.isLoadingNotifications.collectAsStateWithLifecycle()
    var notifToDelete by remember { mutableStateOf<com.example.data.model.AdminNotification?>(null) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            adminViewModel.loadSentNotifications()
        }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = NeonBlue)
                    androidx.compose.material3.Text(
                        "Quản lý Thông báo Admin",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                // Tab Header
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkBackground,
                    contentColor = NeonBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NeonBlue
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Gửi thông báo",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) NeonBlue else TextSecondary
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            adminViewModel.loadSentNotifications()
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Đã gửi & Đã ghim",
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 1) NeonBlue else TextSecondary
                                )
                                if (sentNotifications.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        color = if (sentNotifications.any { it.isPinned }) AccentOrange else NeonBlue,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "${sentNotifications.size}",
                                            color = White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        },
        text = {
            if (selectedTab == 0) {
                // Tab 0: Gửi thông báo mới
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Dropdown Selector for Recipient
                    Column {
                        androidx.compose.material3.Text(
                            text = "Người nhận",
                            color = LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                                .clickable { expandedUserDropdown = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedUser?.let { "${it.hoVaTen} (${it.roleName})" } ?: "Tất cả nhân viên",
                                    color = White,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = NeonBlue
                                )
                            }
                            androidx.compose.material3.DropdownMenu(
                                expanded = expandedUserDropdown,
                                onDismissRequest = { expandedUserDropdown = false },
                                modifier = Modifier
                                    .background(DarkContainer)
                                    .width(300.dp)
                            ) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Tất cả nhân viên", color = White) },
                                    onClick = {
                                        selectedUser = null
                                        expandedUserDropdown = false
                                    }
                                )
                                employees.forEach { emp ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("${emp.hoVaTen} (${emp.roleName})", color = White, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        onClick = {
                                            selectedUser = emp
                                            expandedUserDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    AdminInputField(
                        label = "Tiêu đề",
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        maxLines = 1
                    )

                    AdminInputField(
                        label = "Nội dung",
                        value = message,
                        onValueChange = { message = it },
                        singleLine = false,
                        minLines = 4,
                        maxLines = 6
                    )

                    // Pinned Notification Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isPinned) AccentOrange.copy(alpha = 0.15f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { isPinned = !isPinned }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "📌",
                                fontSize = 18.sp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ghim thông báo quan trọng",
                                    color = if (isPinned) AccentOrange else White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Nhân viên không thể tự xóa (Admin có thể xóa sau)",
                                    color = LightGray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Switch(
                            checked = isPinned,
                            onCheckedChange = { isPinned = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = White,
                                checkedTrackColor = AccentOrange
                            )
                        )
                    }
                }
            } else {
                // Tab 1: Danh sách thông báo đã gửi & đã ghim (Xóa được)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Danh sách thông báo (${sentNotifications.size})",
                            color = LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = { adminViewModel.loadSentNotifications() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Làm mới", tint = NeonBlue, modifier = Modifier.size(18.dp))
                        }
                    }

                    if (isLoadingNotifs) {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NeonBlue, modifier = Modifier.size(28.dp))
                        }
                    } else if (sentNotifications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(DarkBackground, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Chưa có thông báo nào được lưu trên máy chủ.", color = TextSecondary, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sentNotifications, key = { it.id }) { notif ->
                                val timeStr = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()).format(Date(notif.createdAt))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DarkBackground,
                                    border = BorderStroke(1.dp, if (notif.isPinned) AccentOrange.copy(alpha = 0.5f) else CardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                if (notif.isPinned) {
                                                    Surface(
                                                        color = AccentOrange.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "📌 ĐÃ GHIM",
                                                            color = AccentOrange,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = notif.title.ifBlank { "Thông báo" },
                                                    color = White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            IconButton(
                                                onClick = { notifToDelete = notif },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "Xóa thông báo",
                                                    tint = DangerRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = notif.message,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Tới: ${if (notif.targetUid == "ALL") "Tất cả NV" else notif.targetName}",
                                                color = NeonBlue,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = timeStr,
                                                color = TextSecondary,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 0) {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val uid = selectedUser?.userId ?: "ALL"
                        val name = selectedUser?.hoVaTen ?: "Tất cả nhân viên"
                        onSend(uid, name, title, message, "general", isPinned)
                    },
                    enabled = title.isNotBlank() && message.isNotBlank()
                ) {
                    androidx.compose.material3.Text("Gửi", color = if (title.isNotBlank() && message.isNotBlank()) NeonBlue else Color.Gray)
                }
            } else {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    androidx.compose.material3.Text("Đóng", color = NeonBlue)
                }
            }
        },
        dismissButton = {
            if (selectedTab == 0) {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    androidx.compose.material3.Text("Hủy", color = Color.Gray)
                }
            }
        },
        containerColor = DarkContainer
    )

    // Delete Confirmation Dialog for Admin
    if (notifToDelete != null) {
        val target = notifToDelete!!
        AlertDialog(
            onDismissRequest = { notifToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DangerRed)
                    Text("Xác nhận xóa thông báo", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Bạn có chắc chắn muốn xóa thông báo này?",
                        color = White,
                        fontSize = 13.5.sp
                    )
                    Surface(
                        color = DarkBackground,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = target.title,
                                color = White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (target.isPinned) {
                                Text(
                                    text = "📌 Thông báo đã ghim",
                                    color = AccentOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = target.message,
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = "Thông báo sẽ được gỡ bỏ và xóa hoàn toàn khỏi máy chủ cũng như thiết bị của mọi nhân viên.",
                        color = AccentOrange,
                        fontSize = 11.5.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.id
                        val targetUid = target.targetUid
                        notifToDelete = null
                        adminViewModel.deleteNotificationByAdmin(id, targetUid) { success ->
                            if (success) {
                                android.widget.Toast.makeText(context, "Đã xóa thông báo thành công", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "Lỗi khi xóa thông báo", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Xác nhận xóa", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { notifToDelete = null }) {
                    Text("Hủy", color = TextSecondary)
                }
            },
            containerColor = DarkContainer,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun AttendanceRecordItem(record: AttendanceRecord, employee: UserConfig, onDelete: () -> Unit) {
    val inStr = if (record.clockInTime > 0) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.clockInTime)) else "N/A"
    val outStr = if (record.clockOutTime != null && record.clockOutTime > 0) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.clockOutTime)) else "N/A"
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = DarkBackground,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(record.dateString, color = White, fontWeight = FontWeight.Bold)
                Text("In: $inStr - Out: $outStr", color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentOrange)
            }
        }
    }
}

@Composable
fun EmployeePayslipView(
    employee: UserConfig,
    records: List<AttendanceRecord>,
    selectedMonthYm: String,
    isAllMonths: Boolean,
    adminViewModel: AdminViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Thực tế, 1: Dự kiến

    // Date calculations
    var targetYear = 2026
    var targetMonth = 8
    try {
        val parts = selectedMonthYm.split("-")
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
        val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
        if (com.example.data.SalaryCalculator.isHoliday(dateStr)) {
            if (!isCurrentSelectedMonth || dateStr <= todayStr) {
                holidayDatesInMonth.add(dateStr)
            }
        }
    }

    val effectiveJoinDate: String? = if (employee.ngayVaoLam.isNotBlank()) {
        employee.ngayVaoLam.trim()
    } else {
        null
    }

    var expectedWorkDaysSoFar = 0
    var totalWorkDaysInMonth = 0

    // Calculate total work days in month first
    for (day in 1..maxDaysInMo) {
        val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
        val cal = Calendar.getInstance()
        cal.set(targetYear, targetMonth - 1, day)
        val isSunday = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
        val isHoliday = com.example.data.SalaryCalculator.isHoliday(dateStr)
        if (!isSunday && !isHoliday) {
            totalWorkDaysInMonth++
        }
    }

    if (isCurrentSelectedMonth) {
        for (day in 1 until todayDayOfMonth) {
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, day)
            if (effectiveJoinDate != null && dateStr < effectiveJoinDate) {
                continue
            }
            val cal = Calendar.getInstance()
            cal.set(currentYear, currentMonth - 1, day)
            val isSunday = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            val isHoliday = com.example.data.SalaryCalculator.isHoliday(dateStr)
            if (!isSunday && !isHoliday) {
                expectedWorkDaysSoFar++
            }
        }
    } else {
        if (effectiveJoinDate != null && effectiveJoinDate.startsWith(selectedMonthYm)) {
            for (day in 1..maxDaysInMo) {
                val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
                if (dateStr < effectiveJoinDate) {
                    continue
                }
                val cal = Calendar.getInstance()
                cal.set(targetYear, targetMonth - 1, day)
                val isSunday = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                val isHoliday = com.example.data.SalaryCalculator.isHoliday(dateStr)
                if (!isSunday && !isHoliday) {
                    expectedWorkDaysSoFar++
                }
            }
        } else {
            expectedWorkDaysSoFar = totalWorkDaysInMonth
        }
    }

    fun normalizeToYmd(dateStr: String): String {
        return try {
            if (dateStr.contains("/")) {
                val parts = dateStr.split("/")
                if (parts.size == 3) {
                    "${parts[2].padStart(4, '0')}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}"
                } else {
                    dateStr
                }
            } else {
                dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    val timeEntries = records.map { log ->
        val entry = log.toTimeEntry()
        entry.copy(date = normalizeToYmd(entry.date))
    }

    val summary = com.example.data.SalaryCalculator.calculateMonthlySalary(
        entries = timeEntries,
        config = employee,
        scheduledDaysSoFar = expectedWorkDaysSoFar,
        totalScheduledDaysInMonth = totalWorkDaysInMonth,
        earliestDate = effectiveJoinDate,
        selectedMonth = selectedMonthYm,
        todayStr = todayStr,
        isCurrentSelectedMonth = isCurrentSelectedMonth,
        holidayDatesInMonth = holidayDatesInMonth
    )

    val hasTodayLogged = timeEntries.any { e ->
        val day = try {
            val parts = if (e.date.contains("/")) e.date.split("/") else e.date.split("-")
            if (e.date.contains("/")) parts.getOrNull(0)?.toIntOrNull() else parts.getOrNull(2)?.toIntOrNull()
        } catch (ex: Exception) { null }
        day == todayDayOfMonth && (e.checkInTime != null || e.isWorking || com.example.data.SalaryCalculator.isPaidLeaveType(e.dayType) || com.example.data.SalaryCalculator.isUnpaidLeaveType(e.dayType) || e.dayType == "HOLIDAY_LEAVE")
    }

    // Projected state calculations
    val startProjectionDay = if (!isCurrentSelectedMonth) 1 else if (hasTodayLogged) (todayDayOfMonth + 1).coerceAtMost(maxDaysInMo + 1) else todayDayOfMonth.coerceAtMost(maxDaysInMo + 1)
    
    val remainingWeekdays = if (!isCurrentSelectedMonth) 0 else {
        var count = 0
        val cal = Calendar.getInstance()
        for (day in startProjectionDay..maxDaysInMo) {
            cal.set(targetYear, targetMonth - 1, day)
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
            val isHoliday = com.example.data.SalaryCalculator.isHoliday(dateStr)
            val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            if (!isSunday && !isHoliday) {
                count++
            }
        }
        count
    }

    val remainingSundays = if (!isCurrentSelectedMonth) 0 else {
        var count = 0
        val cal = Calendar.getInstance()
        for (day in startProjectionDay..maxDaysInMo) {
            cal.set(targetYear, targetMonth - 1, day)
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
            val isHoliday = com.example.data.SalaryCalculator.isHoliday(dateStr)
            val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            if (isSunday && !isHoliday) {
                count++
            }
        }
        count
    }

    val dailySalary = employee.luongCoBan / 26.0
    val hourlySalary = dailySalary / 8.0

    val hasWorkedSunday = timeEntries.any { e ->
        com.example.data.SalaryCalculator.isSunday(e.date) && e.checkInTime != null
    }

    var includeSundayInProjection by remember(hasWorkedSunday) { mutableStateOf(hasWorkedSunday) }

    val additionalWeekdaysPay = remainingWeekdays * dailySalary
    val additionalSundaysPay = if (includeSundayInProjection) {
        remainingSundays * dailySalary * employee.heSoOtChuNhat
    } else {
        0.0
    }

    val unpaidDaysCount = remember(timeEntries) {
        timeEntries.count { e -> com.example.data.SalaryCalculator.isUnpaidLeaveType(e.dayType) || e.dayType == "UNAUTHORIZED_LEAVE" }
    }
    val hasLoggedUnpaidOrAbsent = unpaidDaysCount > 0

    val projectedRemainingWorkdays = remainingWeekdays.toDouble() + (if (includeSundayInProjection) remainingSundays.toDouble() else 0.0)
    val standardTargetDays = (if (isCurrentSelectedMonth) summary.expectedWorkDays else summary.standardWorkDays).toDouble().coerceAtLeast(26.0)

    val soNgayCongDuKien = if (isCurrentSelectedMonth) {
        val rawProjected = summary.workingDays + projectedRemainingWorkdays
        if (unpaidDaysCount > 0) {
            (standardTargetDays - unpaidDaysCount).coerceAtLeast(rawProjected)
        } else {
            maxOf(rawProjected, standardTargetDays)
        }
    } else {
        maxOf(summary.workingDays, summary.standardWorkDays.toDouble())
    }
    val soNgayCongDuKienDouble = soNgayCongDuKien

    fun calcPr(fieldName: String, valRaw: Double): Double {
        return com.example.data.SalaryCalculator.calculateAllowanceValue(
            fieldName = fieldName,
            allowanceValue = valRaw,
            calcType = employee.getCalcTypeFor(fieldName),
            totalWorkDays = soNgayCongDuKienDouble,
            comCaCount = soNgayCongDuKienDouble.toInt(),
            comOtCount = 0,
            nightShiftsCount = summary.caDemCount,
            scheduledDaysSoFar = soNgayCongDuKienDouble.toInt(),
            totalScheduledDaysInMonth = 26
        )
    }

    val pcKyThuatShow = if (selectedTab == 1) calcPr("pcKyThuat", employee.pcKyThuat) else summary.pcKyThuatVal
    val pcTrachNhiemShow = if (selectedTab == 1) calcPr("pcTrachNhiem", employee.pcTrachNhiem) else summary.pcTrachNhiemVal
    val pcChucVuShow = if (selectedTab == 1) calcPr("pcChucVu", employee.pcChucVu) else summary.pcChucVuVal
    val pcHieuSuatShow = if (selectedTab == 1) calcPr("pcHieuSuat", employee.pcHieuSuat) else summary.pcHieuSuatVal
    val pcSanPhamShow = if (selectedTab == 1) calcPr("pcSanPham", employee.pcSanPham) else summary.pcSanPhamVal

    val pcComCaShow = if (selectedTab == 1) {
        if (isCurrentSelectedMonth) {
            soNgayCongDuKienDouble * employee.pcComCa
        } else {
            summary.pcComCaVal
        }
    } else {
        summary.pcComCaVal
    }

    val pcComOtShow = summary.pcComOtVal

    val pcNhaOShow = if (selectedTab == 1) calcPr("pcNhaO", employee.pcNhaO) else summary.pcNhaOVal
    val pcDocHaiShow = if (selectedTab == 1) calcPr("pcDocHai", employee.pcDocHai) else summary.pcDocHaiVal
    val pcDtDoanhThuShow = if (selectedTab == 1) calcPr("pcDtDoanhThu", employee.pcDtDoanhThu) else summary.pcDtDoanhThuVal
    val pcXangXeShow = if (selectedTab == 1) calcPr("pcXangXe", employee.pcXangXe) else summary.pcXangXeVal
    val pcKhacShow = if (selectedTab == 1) calcPr("pcCaDem", employee.pcCaDem) else summary.pcCaDemVal
    val pcKhac1Show = if (selectedTab == 1) calcPr("pcKhac1", employee.pcKhac1) else summary.pcKhac1Val
    val pcThamNienShow = if (selectedTab == 1) calcPr("pcThamNien", employee.pcThamNien) else summary.pcThamNienVal

    val pcChuyenCanShow = if (selectedTab == 1) {
        if (hasLoggedUnpaidOrAbsent) 0.0 else calcPr("tienChuyenCanGoc", employee.tienChuyenCanGoc)
    } else {
        summary.phuCapChuyenCan
    }

    val luongDuKienBaseSalary = Math.round((employee.luongCoBan / 26.0) * soNgayCongDuKienDouble).toDouble()

    val currentProratedAllowancesSum = summary.pcKyThuatVal + summary.pcTrachNhiemVal + summary.pcChucVuVal + summary.pcHieuSuatVal +
            summary.pcSanPhamVal + summary.pcComCaVal + summary.pcComOtVal + summary.pcNhaOVal + summary.pcDocHaiVal + 
            summary.pcDtDoanhThuVal + summary.pcXangXeVal + summary.pcKhac1Val + summary.pcThamNienVal + summary.phuCapChuyenCan +
            summary.pcCaDemVal

    val fullProjectedAllowancesSum = pcKyThuatShow + pcTrachNhiemShow + pcChucVuShow + pcHieuSuatShow +
            pcSanPhamShow + pcComCaShow + pcComOtShow + pcNhaOShow + pcDocHaiShow + 
            pcDtDoanhThuShow + pcXangXeShow + pcKhac1Show + pcThamNienShow + pcChuyenCanShow +
            summary.pcCaDemVal

    val allowanceAdjustment = fullProjectedAllowancesSum - currentProratedAllowancesSum
    val baseSalaryAdjustment = if (isCurrentSelectedMonth) (luongDuKienBaseSalary - summary.baseBasicSalary) else 0.0

    val finalLuongHienThi = if (selectedTab == 1) {
        summary.luongThucNhan + baseSalaryAdjustment + additionalSundaysPay + allowanceAdjustment
    } else {
        summary.luongThucNhan
    }

    val monthLabel = remember(selectedMonthYm) {
        try {
            val parser = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val d = parser.parse(selectedMonthYm) ?: Date()
            val formatter = SimpleDateFormat("MMMM / yyyy", Locale("vi", "VN"))
            formatter.format(d).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            selectedMonthYm
        }
    }

    val fmt = remember { DecimalFormat("#,###") }
    val df = remember { DecimalFormat("#.#") }

    @Composable
    fun LocalPayslipMoneyRow(
        label: String,
        value: Double,
        isAddition: Boolean,
        isAccent: Boolean = false
    ) {
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isAddition) "+${fmt.format(value)}đ" else "-${fmt.format(value)}đ",
                color = if (isAddition) AccentGreen else AccentRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Toggle Switch Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkContainer,
            contentColor = NeonBlue,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = NeonBlue,
                    height = 2.dp
                )
            },
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Thực Tế", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Dự Kiến", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) }
            )
        }

        // Main Invoice Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkContainer),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Ticket Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PHIẾU LƯƠNG NHÂN VIÊN",
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = monthLabel,
                            color = LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Surface(
                        color = (if (selectedTab == 1) AccentOrange else NeonBlue).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (selectedTab == 1) "DỰ KIẾN" else "THỰC TẾ",
                            color = if (selectedTab == 1) AccentOrange else NeonBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Employee Metadata
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Nhân viên:", color = LightGray, fontSize = 13.sp)
                        Text(employee.hoVaTen, color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Chức vụ / Vai trò:", color = LightGray, fontSize = 13.sp)
                        Text(employee.roleName, color = White, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Lương cơ bản (26 ngày):", color = LightGray, fontSize = 13.sp)
                        Text("${fmt.format(employee.luongCoBan)}đ", color = White, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ngày công chuẩn:", color = LightGray, fontSize = 13.sp)
                        Text("${summary.standardWorkDays} ngày", color = White, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ngày công thực tế tính luơng:", color = LightGray, fontSize = 13.sp)
                        val daysShow = if (selectedTab == 1) soNgayCongDuKienDouble else summary.workingDays.toDouble()
                        Text("${df.format(daysShow)} ngày", color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Earnings & Allowances Details
                Text("CÁC KHOẢN THU NHẬP", color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                // 1. Base Salary
                if (selectedTab == 1) {
                    LocalPayslipMoneyRow(label = "Lương cơ bản thực nhận (dự kiến)", value = luongDuKienBaseSalary, isAddition = true)
                } else {
                    LocalPayslipMoneyRow(label = "Lương cơ bản thực nhận", value = summary.baseBasicSalary, isAddition = true)
                }

                // 2. Allowances
                if (pcChuyenCanShow > 0.0) {
                    LocalPayslipMoneyRow(label = "Phụ cấp chuyên cần", value = pcChuyenCanShow, isAddition = true)
                }
                if (employee.pcTrachNhiem > 0.0) {
                    LocalPayslipMoneyRow(label = "Phụ cấp trách nhiệm", value = pcTrachNhiemShow, isAddition = true)
                }
                if (employee.pcKyThuat > 0.0) {
                    LocalPayslipMoneyRow(label = "Phụ cấp kỹ thuật", value = pcKyThuatShow, isAddition = true)
                }
                if (employee.pcHieuSuat > 0.0) {
                    LocalPayslipMoneyRow(label = "Phụ cấp hiệu suất", value = pcHieuSuatShow, isAddition = true)
                }
                if (employee.pcSanPham > 0.0) {
                    LocalPayslipMoneyRow(label = "Phụ cấp sản phẩm", value = pcSanPhamShow, isAddition = true)
                }
                if (employee.pcChucVu > 0.0) {
                    LocalPayslipMoneyRow(label = "Phụ cấp chức vụ", value = pcChucVuShow, isAddition = true)
                }
                if (employee.pcDocHai > 0.0) {
                    LocalPayslipMoneyRow(label = "Phụ cấp độc hại", value = pcDocHaiShow, isAddition = true)
                }
                if (employee.pcDtDoanhThu > 0.0) {
                    LocalPayslipMoneyRow(label = "Phụ cấp doanh thu", value = pcDtDoanhThuShow, isAddition = true)
                }
                if (employee.pcThamNien > 0.0) {
                    LocalPayslipMoneyRow(label = "Phụ cấp thâm niên", value = pcThamNienShow, isAddition = true)
                }
                if (pcComCaShow > 0.0) {
                    LocalPayslipMoneyRow(label = "Phụ cấp cơm ca", value = pcComCaShow, isAddition = true)
                }
                if (pcComOtShow > 0.0) {
                    LocalPayslipMoneyRow(label = "Phụ cấp cơm OT", value = pcComOtShow, isAddition = true)
                }
                if (pcXangXeShow > 0.0) {
                    LocalPayslipMoneyRow(label = "Phụ cấp xăng xe", value = pcXangXeShow, isAddition = true)
                }
                if (pcNhaOShow > 0.0) {
                    LocalPayslipMoneyRow(label = "Phụ cấp nhà ở", value = pcNhaOShow, isAddition = true)
                }
                if (pcKhacShow > 0.0) {
                    LocalPayslipMoneyRow(label = "Phụ cấp ca đêm", value = pcKhacShow, isAddition = true)
                }
                if (pcKhac1Show > 0.0) {
                    LocalPayslipMoneyRow(label = "Phụ cấp khác", value = pcKhac1Show, isAddition = true)
                }

                // 3. Overtime Pay
                val totalOtNgayHienThi = summary.otDayHours + (if (selectedTab == 1 && isCurrentSelectedMonth) remainingWeekdays * 0.0 else 0.0)
                val totalOtNgayTienHienThi = summary.tienOtNgay
                if (totalOtNgayHienThi > 0.0 || totalOtNgayTienHienThi > 0.0) {
                    LocalPayslipMoneyRow(label = "OT ngày ${df.format(employee.heSoOtNgayThuong)} (${df.format(totalOtNgayHienThi)}h)", value = totalOtNgayTienHienThi, isAddition = true, isAccent = true)
                }
                if (summary.tienOtDem > 0.0) {
                    LocalPayslipMoneyRow(label = "OT đêm ${df.format(employee.heSoOtDem)} (${df.format(summary.otNightHours)}h)", value = summary.tienOtDem, isAddition = true, isAccent = true)
                }
                if (summary.tienOtLe > 0.0) {
                    LocalPayslipMoneyRow(label = "OT lễ ${df.format(employee.heSoOtNgayLe)} (${df.format(summary.otLeHours)}h)", value = summary.tienOtLe, isAddition = true, isAccent = true)
                }
                if (summary.tienChuNhatNgay > 0.0) {
                    LocalPayslipMoneyRow(label = "OT CN - Ca ngày ${df.format(employee.heSoOtChuNhat)} (${df.format(summary.chuNhatDayHours)}h)", value = summary.tienChuNhatNgay, isAddition = true, isAccent = true)
                }
                if (summary.tienChuNhatDem > 0.0) {
                    LocalPayslipMoneyRow(label = "OT CN - Ca đêm ${df.format(employee.heSoOtChuNhat)} (${df.format(summary.chuNhatNightHours)}h)", value = summary.tienChuNhatDem, isAddition = true, isAccent = true)
                }
                if (summary.tienChuNhatNgay == 0.0 && summary.tienChuNhatDem == 0.0 && summary.tienChuNhat > 0.0) {
                    LocalPayslipMoneyRow(label = "OT chủ nhật ${df.format(employee.heSoOtChuNhat)} (${df.format(summary.chuNhatHours)}h)", value = summary.tienChuNhat, isAddition = true, isAccent = true)
                }

                // Projected Sunday Pay
                if (selectedTab == 1 && isCurrentSelectedMonth && includeSundayInProjection && remainingSundays > 0) {
                    LocalPayslipMoneyRow(label = "OT chủ nhật ${df.format(employee.heSoOtChuNhat)} ($remainingSundays ngày)", value = additionalSundaysPay, isAddition = true, isAccent = true)
                }

                // Bonus
                if (summary.thuong > 0.0) {
                    LocalPayslipMoneyRow(label = "Tiền thưởng / Thưởng dự án", value = summary.thuong, isAddition = true)
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Deductions
                Text("CÁC KHOẢN GIẢM TRỪ", color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                if (summary.tienBh > 0.0) {
                    LocalPayslipMoneyRow(label = "Khấu trừ Bảo hiểm xã hội", value = summary.tienBh, isAddition = false)
                }
                if (summary.doanPhi > 0.0) {
                    LocalPayslipMoneyRow(label = "Khấu trừ Đoàn phí công đoàn", value = summary.doanPhi, isAddition = false)
                }
                if (summary.tienKhauTruNghi > 0.0) {
                    LocalPayslipMoneyRow(label = "Khấu trừ nghỉ giữa giờ / nghỉ lễ", value = summary.tienKhauTruNghi, isAddition = false)
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Final Pay (Thực nhận)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "THỰC NHẬN:",
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${fmt.format(finalLuongHienThi)}đ",
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            }
        }

        // Action Buttons (Export PDF / Resync)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    adminViewModel.exportSingleEmployeePayslip(context, employee, selectedMonthYm)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Xuất PDF", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
