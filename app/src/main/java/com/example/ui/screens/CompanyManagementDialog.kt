package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CompanyConfig
import com.example.data.model.RoleConfig
import com.example.data.model.UserConfig
import com.example.data.model.getRoles
import com.example.data.model.updateRoles
import com.example.ui.theme.*
import com.example.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyManagementDialog(
    companies: List<CompanyConfig>,
    allEmployees: List<UserConfig>,
    initialCompanyId: String,
    adminViewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val updateLocalUserSessionConfig = { updatedCompany: CompanyConfig ->
        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val sharedPrefs = context.getSharedPreferences("timesnap_auth", android.content.Context.MODE_PRIVATE)
                val currentUid = sharedPrefs.getString("last_logged_in_uid", "local_user") ?: "local_user"
                val db = com.example.data.db.AppDatabase.getInstance(context)
                val userConfigDao = db.userConfigDao()
                val localUserConfig = userConfigDao.getConfigForUser(currentUid)
                if (localUserConfig != null) {
                    if (localUserConfig.companyId == updatedCompany.companyId || 
                        (updatedCompany.companyId == "default_company" && (localUserConfig.companyId.isBlank() || localUserConfig.companyId == "default_company"))
                    ) {
                        val updatedUserConfig = updatedCompany.applyToUserConfig(localUserConfig, overwriteCustomBaseSalary = false)
                        userConfigDao.saveConfig(updatedUserConfig)
                        android.util.Log.d("CompanyManagementDialog", "Successfully updated local UserConfig in Room for $currentUid to match updated CompanyConfig.")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CompanyManagementDialog", "Error updating local user session config", e)
            }
        }
    }

    var selectedCompanyId by remember { mutableStateOf(initialCompanyId) }
    val currentCompany = companies.find { it.companyId == selectedCompanyId } 
        ?: companies.firstOrNull() 
        ?: CompanyConfig.DEFAULT_COMPANY

    var localCompanyConfig by remember(currentCompany.companyId) { mutableStateOf(currentCompany) }

    var companyName by remember(currentCompany.companyId) { mutableStateOf(currentCompany.companyName) }
    var companyCode by remember(currentCompany.companyId) { mutableStateOf(currentCompany.companyCode) }
    var description by remember(currentCompany.companyId) { mutableStateOf(currentCompany.description) }
    var address by remember(currentCompany.companyId) { mutableStateOf(currentCompany.address) }
    
    // Core Salary & Insurance
    var luongCoBan by remember(currentCompany.companyId) { mutableStateOf(currentCompany.luongCoBan.toLong().toString()) }
    var luongDongBaoHiem by remember(currentCompany.companyId) { mutableStateOf(currentCompany.luongDongBaoHiem.toLong().toString()) }
    var tiLeDongBaoHiem by remember(currentCompany.companyId) { mutableStateOf(currentCompany.tiLeDongBaoHiem.toString()) }
    var doanPhiCongDoan by remember(currentCompany.companyId) { mutableStateOf(currentCompany.doanPhiCongDoan.toLong().toString()) }
    
    // Schedules & OT
    var schedule by remember(currentCompany.companyId) { mutableStateOf(currentCompany.lichTrinh) }
    var soGioNghiGiaiLao by remember(currentCompany.companyId) { mutableStateOf(currentCompany.soGioNghiGiaiLao.toString()) }
    var tinhKhauTruNghi by remember(currentCompany.companyId) { mutableStateOf(currentCompany.tinhKhauTruNghi) }
    var hsOtThuong by remember(currentCompany.companyId) { mutableStateOf(currentCompany.heSoOtNgayThuong.toString()) }
    var hsOtChuNhat by remember(currentCompany.companyId) { mutableStateOf(currentCompany.heSoOtChuNhat.toString()) }
    var hsOtNgayLe by remember(currentCompany.companyId) { mutableStateOf(currentCompany.heSoOtNgayLe.toString()) }
    var hsOtDem by remember(currentCompany.companyId) { mutableStateOf(currentCompany.heSoOtDem.toString()) }

    // Allowances
    var pcXangXe by remember(currentCompany.companyId) { mutableStateOf(currentCompany.pcXangXe.toLong().toString()) }
    var pcTrachNhiem by remember(currentCompany.companyId) { mutableStateOf(currentCompany.pcTrachNhiem.toLong().toString()) }
    var pcKyThuat by remember(currentCompany.companyId) { mutableStateOf(currentCompany.pcKyThuat.toLong().toString()) }
    var pcChucVu by remember(currentCompany.companyId) { mutableStateOf(currentCompany.pcChucVu.toLong().toString()) }
    var pcHieuSuat by remember(currentCompany.companyId) { mutableStateOf(currentCompany.pcHieuSuat.toLong().toString()) }
    var pcSanPham by remember(currentCompany.companyId) { mutableStateOf(currentCompany.pcSanPham.toLong().toString()) }
    var pcComCa by remember(currentCompany.companyId) { mutableStateOf(currentCompany.pcComCa.toLong().toString()) }
    var pcComOt by remember(currentCompany.companyId) { mutableStateOf(currentCompany.pcComOt.toLong().toString()) }
    var pcNhaO by remember(currentCompany.companyId) { mutableStateOf(currentCompany.pcNhaO.toLong().toString()) }
    var pcDocHai by remember(currentCompany.companyId) { mutableStateOf(currentCompany.pcDocHai.toLong().toString()) }
    var pcDtDoanhThu by remember(currentCompany.companyId) { mutableStateOf(currentCompany.pcDtDoanhThu.toLong().toString()) }
    var pcThamNien by remember(currentCompany.companyId) { mutableStateOf(currentCompany.pcThamNien.toLong().toString()) }
    var pcCaDem by remember(currentCompany.companyId) { mutableStateOf(currentCompany.pcCaDem.toLong().toString()) }
    var pcKhac1 by remember(currentCompany.companyId) { mutableStateOf(currentCompany.pcKhac1.toLong().toString()) }
    var tienChuyenCanGoc by remember(currentCompany.companyId) { mutableStateOf(currentCompany.tienChuyenCanGoc.toLong().toString()) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "🏢 Chung" to Icons.Default.Business,
        "💼 Chức vụ" to Icons.Default.Badge,
        "👥 Nhân viên" to Icons.Default.People
    )

    var activeEditingAllowanceField by remember { mutableStateOf<String?>(null) }
    var activeEditingAllowanceName by remember { mutableStateOf("") }
    var activeEditingAllowanceValue by remember { mutableStateOf("") }
    var activeEditingAllowanceType by remember { mutableStateOf("") }

    var showAddCompanyDialog by remember { mutableStateOf(false) }
    var showDeleteCompanyDialog by remember { mutableStateOf(false) }
    var showAddRoleDialog by remember { mutableStateOf(false) }
    var editingRole by remember { mutableStateOf<RoleConfig?>(null) }
    var empSearchQuery by remember { mutableStateOf("") }

    val allowanceCalcTypesMap = remember(localCompanyConfig.allowanceCalcTypes) {
        if (localCompanyConfig.allowanceCalcTypes.isBlank()) emptyMap()
        else localCompanyConfig.allowanceCalcTypes.split(";").filter { it.contains(":") }.associate {
            val parts = it.split(":")
            parts[0] to parts[1]
        }
    }

    val companyEmployees = allEmployees.filter { 
        it.companyId == currentCompany.companyId || 
        (currentCompany.companyId == "default_company" && (it.companyId.isBlank() || it.companyId == "default_company"))
    }

    val currentRoles = localCompanyConfig.getRoles()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .widthIn(max = 600.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = DarkContainer,
            border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground.copy(alpha = 0.8f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(NeonBlue.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Apartment, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Quản Lý Doanh Nghiệp", color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(currentCompany.companyName, color = NeonBlue, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    IconButton(
                        onClick = { showAddCompanyDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.AddBusiness, contentDescription = "Thêm công ty mới", tint = NeonBlue)
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = LightGray)
                    }
                }

                // Modern Tab Bar
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = DarkBackground.copy(alpha = 0.4f),
                    contentColor = NeonBlue,
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = NeonBlue,
                                height = 3.dp
                            )
                        }
                    },
                    divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.08f)) }
                ) {
                    tabs.forEachIndexed { index, pair ->
                        val isSelected = selectedTabIndex == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    pair.first,
                                    color = if (isSelected) NeonBlue else LightGray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            icon = {
                                Icon(
                                    pair.second, 
                                    contentDescription = null, 
                                    tint = if (isSelected) NeonBlue else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                // Tab Content Area (Scrollable)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTabIndex) {
                        // TAB 1: THÔNG TIN CHUNG & LƯƠNG CƠ BẢN
                        0 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Company Selector & Delete Bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    var compDropdownExpanded by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { compDropdownExpanded = true },
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkBackground.copy(alpha = 0.5f), contentColor = White),
                                            border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Icon(Icons.Default.Business, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(currentCompany.companyName, color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = LightGray)
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = compDropdownExpanded,
                                            onDismissRequest = { compDropdownExpanded = false },
                                            modifier = Modifier.background(DarkContainer).border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        ) {
                                            companies.forEach { comp ->
                                                DropdownMenuItem(
                                                    text = { Text(comp.companyName, color = if (comp.companyId == selectedCompanyId) NeonBlue else White, fontWeight = if (comp.companyId == selectedCompanyId) FontWeight.Bold else FontWeight.Normal) },
                                                    onClick = {
                                                        selectedCompanyId = comp.companyId
                                                        compDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    if (currentCompany.companyId != "default_company") {
                                        Button(
                                            onClick = { showDeleteCompanyDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange.copy(alpha = 0.2f)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.height(48.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Xóa công ty", tint = AccentOrange, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Xóa", color = AccentOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }

                                CompanyCardSection(title = "Thông tin cơ bản", icon = Icons.Default.Info) {
                                    OutlinedTextField(
                                        value = companyName,
                                        onValueChange = { companyName = it },
                                        label = { Text("Tên Công ty") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = White,
                                            unfocusedTextColor = White,
                                            focusedBorderColor = NeonBlue,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                            focusedLabelColor = NeonBlue,
                                            unfocusedLabelColor = LightGray
                                        )
                                    )
                                    OutlinedTextField(
                                        value = companyCode,
                                        onValueChange = { companyCode = it.uppercase() },
                                        label = { Text("Mã Cty (CTY_A...)") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = White,
                                            unfocusedTextColor = White,
                                            focusedBorderColor = NeonBlue,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                            focusedLabelColor = NeonBlue,
                                            unfocusedLabelColor = LightGray
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = address,
                                        onValueChange = { address = it },
                                        label = { Text("Địa chỉ trụ sở") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = White,
                                            unfocusedTextColor = White,
                                            focusedBorderColor = NeonBlue,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                            focusedLabelColor = NeonBlue,
                                            unfocusedLabelColor = LightGray
                                        )
                                    )
                                }

                                CompanyCardSection(title = "Hệ số tăng ca", icon = Icons.Default.TrendingUp) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = hsOtThuong,
                                            onValueChange = { hsOtThuong = it },
                                            label = { Text("OT Ngày thường") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = White,
                                                unfocusedTextColor = White,
                                                focusedBorderColor = NeonBlue,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                                focusedLabelColor = NeonBlue,
                                                unfocusedLabelColor = LightGray
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = hsOtChuNhat,
                                            onValueChange = { hsOtChuNhat = it },
                                            label = { Text("OT Chủ nhật") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = White,
                                                unfocusedTextColor = White,
                                                focusedBorderColor = NeonBlue,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                                focusedLabelColor = NeonBlue,
                                                unfocusedLabelColor = LightGray
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = hsOtNgayLe,
                                            onValueChange = { hsOtNgayLe = it },
                                            label = { Text("OT Ngày lễ") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = White,
                                                unfocusedTextColor = White,
                                                focusedBorderColor = NeonBlue,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                                focusedLabelColor = NeonBlue,
                                                unfocusedLabelColor = LightGray
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = hsOtDem,
                                            onValueChange = { hsOtDem = it },
                                            label = { Text("OT Ca đêm") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = White,
                                                unfocusedTextColor = White,
                                                focusedBorderColor = NeonBlue,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                                focusedLabelColor = NeonBlue,
                                                unfocusedLabelColor = LightGray
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // TAB 2: QUẢN LÝ CHỨC VỤ TRONG CÔNG TY
                        1 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                Button(
                                    onClick = {
                                        editingRole = RoleConfig()
                                        showAddRoleDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("THÊM CHỨC VỤ MỚI", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (currentRoles.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .background(DarkBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Badge, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                "Chưa có chức vụ nào được cấu hình.",
                                                color = White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Thêm các chức vụ (Giám đốc, Quản lý, Tổ trưởng, Công nhân...) để tự động gán lương và phụ cấp chuẩn cho nhân viên.",
                                                color = LightGray,
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(currentRoles) { role ->
                                            RoleCardItem(
                                                role = role,
                                                onEdit = {
                                                    editingRole = role
                                                    showAddRoleDialog = true
                                                },
                                                onDelete = {
                                                    val updatedRoles = currentRoles.filter { it.roleId != role.roleId }
                                                    val updatedComp = localCompanyConfig.updateRoles(updatedRoles)
                                                    localCompanyConfig = updatedComp
                                                    adminViewModel.saveCompany(updatedComp)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // TAB 3: DANH MỤC 15 PHỤ CẤP CÔNG TY (ĐÃ XÓA)
                        99 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    "Chạm vào từng khoản phụ cấp để chỉnh sửa mức tiền mặc định & cách tính (Theo ngày / Theo giờ / Cố định).",
                                    color = LightGray,
                                    fontSize = 12.sp
                                )

                                CompanyCardSection(title = "1. Phụ cấp chuyên môn & Trách nhiệm", icon = Icons.Default.Engineering) {
                                    AllowanceRowItem(
                                        name = "1. Kỹ thuật",
                                        value = pcKyThuat,
                                        fieldName = "pcKyThuat",
                                        calcTypeMap = allowanceCalcTypesMap,
                                        onClick = {
                                            activeEditingAllowanceField = "pcKyThuat"
                                            activeEditingAllowanceName = "Phụ cấp Kỹ thuật"
                                            activeEditingAllowanceValue = pcKyThuat
                                            activeEditingAllowanceType = localCompanyConfig.getCalcTypeFor("pcKyThuat")
                                        }
                                    )
                                    AllowanceRowItem(
                                        name = "2. Trách nhiệm",
                                        value = pcTrachNhiem,
                                        fieldName = "pcTrachNhiem",
                                        calcTypeMap = allowanceCalcTypesMap,
                                        onClick = {
                                            activeEditingAllowanceField = "pcTrachNhiem"
                                            activeEditingAllowanceName = "Phụ cấp Trách nhiệm"
                                            activeEditingAllowanceValue = pcTrachNhiem
                                            activeEditingAllowanceType = localCompanyConfig.getCalcTypeFor("pcTrachNhiem")
                                        }
                                    )
                                    AllowanceRowItem(
                                        name = "3. Chức vụ",
                                        value = pcChucVu,
                                        fieldName = "pcChucVu",
                                        calcTypeMap = allowanceCalcTypesMap,
                                        onClick = {
                                            activeEditingAllowanceField = "pcChucVu"
                                            activeEditingAllowanceName = "Phụ cấp Chức vụ"
                                            activeEditingAllowanceValue = pcChucVu
                                            activeEditingAllowanceType = localCompanyConfig.getCalcTypeFor("pcChucVu")
                                        }
                                    )
                                    AllowanceRowItem(
                                        name = "4. Hiệu suất",
                                        value = pcHieuSuat,
                                        fieldName = "pcHieuSuat",
                                        calcTypeMap = allowanceCalcTypesMap,
                                        onClick = {
                                            activeEditingAllowanceField = "pcHieuSuat"
                                            activeEditingAllowanceName = "Phụ cấp Hiệu suất"
                                            activeEditingAllowanceValue = pcHieuSuat
                                            activeEditingAllowanceType = localCompanyConfig.getCalcTypeFor("pcHieuSuat")
                                        }
                                    )
                                    AllowanceRowItem(
                                        name = "5. Sản phẩm",
                                        value = pcSanPham,
                                        fieldName = "pcSanPham",
                                        calcTypeMap = allowanceCalcTypesMap,
                                        onClick = {
                                            activeEditingAllowanceField = "pcSanPham"
                                            activeEditingAllowanceName = "Phụ cấp Sản phẩm"
                                            activeEditingAllowanceValue = pcSanPham
                                            activeEditingAllowanceType = localCompanyConfig.getCalcTypeFor("pcSanPham")
                                        }
                                    )
                                }

                                CompanyCardSection(title = "2. Phụ cấp đời sống & Sinh hoạt", icon = Icons.Default.Restaurant) {
                                    AllowanceRowItem(
                                        name = "6. Cơm ca",
                                        value = pcComCa,
                                        fieldName = "pcComCa",
                                        calcTypeMap = allowanceCalcTypesMap,
                                        onClick = {
                                            activeEditingAllowanceField = "pcComCa"
                                            activeEditingAllowanceName = "Phụ cấp Cơm ca"
                                            activeEditingAllowanceValue = pcComCa
                                            activeEditingAllowanceType = localCompanyConfig.getCalcTypeFor("pcComCa")
                                        }
                                    )
                                    AllowanceRowItem(
                                        name = "7. Cơm OT",
                                        value = pcComOt,
                                        fieldName = "pcComOt",
                                        calcTypeMap = allowanceCalcTypesMap,
                                        onClick = {
                                            activeEditingAllowanceField = "pcComOt"
                                            activeEditingAllowanceName = "Phụ cấp Cơm OT"
                                            activeEditingAllowanceValue = pcComOt
                                            activeEditingAllowanceType = localCompanyConfig.getCalcTypeFor("pcComOt")
                                        }
                                    )
                                    AllowanceRowItem(
                                        name = "8. Nhà ở",
                                        value = pcNhaO,
                                        fieldName = "pcNhaO",
                                        calcTypeMap = allowanceCalcTypesMap,
                                        onClick = {
                                            activeEditingAllowanceField = "pcNhaO"
                                            activeEditingAllowanceName = "Phụ cấp Nhà ở"
                                            activeEditingAllowanceValue = pcNhaO
                                            activeEditingAllowanceType = localCompanyConfig.getCalcTypeFor("pcNhaO")
                                        }
                                    )
                                    AllowanceRowItem(
                                        name = "9. Xăng xe",
                                        value = pcXangXe,
                                        fieldName = "pcXangXe",
                                        calcTypeMap = allowanceCalcTypesMap,
                                        onClick = {
                                            activeEditingAllowanceField = "pcXangXe"
                                            activeEditingAllowanceName = "Phụ cấp Xăng xe"
                                            activeEditingAllowanceValue = pcXangXe
                                            activeEditingAllowanceType = localCompanyConfig.getCalcTypeFor("pcXangXe")
                                        }
                                    )
                                    AllowanceRowItem(
                                        name = "10. Ca đêm",
                                        value = pcCaDem,
                                        fieldName = "pcCaDem",
                                        calcTypeMap = allowanceCalcTypesMap,
                                        onClick = {
                                            activeEditingAllowanceField = "pcCaDem"
                                            activeEditingAllowanceName = "Phụ cấp Ca đêm"
                                            activeEditingAllowanceValue = pcCaDem
                                            activeEditingAllowanceType = localCompanyConfig.getCalcTypeFor("pcCaDem")
                                        }
                                    )
                                }

                                CompanyCardSection(title = "3. Phụ cấp đặc thù & Phúc lợi", icon = Icons.Default.CardGiftcard) {
                                    AllowanceRowItem(
                                        name = "11. Độc hại",
                                        value = pcDocHai,
                                        fieldName = "pcDocHai",
                                        calcTypeMap = allowanceCalcTypesMap,
                                        onClick = {
                                            activeEditingAllowanceField = "pcDocHai"
                                            activeEditingAllowanceName = "Phụ cấp Độc hại"
                                            activeEditingAllowanceValue = pcDocHai
                                            activeEditingAllowanceType = localCompanyConfig.getCalcTypeFor("pcDocHai")
                                        }
                                    )
                                    AllowanceRowItem(
                                        name = "12. Doanh thu",
                                        value = pcDtDoanhThu,
                                        fieldName = "pcDtDoanhThu",
                                        calcTypeMap = allowanceCalcTypesMap,
                                        onClick = {
                                            activeEditingAllowanceField = "pcDtDoanhThu"
                                            activeEditingAllowanceName = "Phụ cấp Doanh thu"
                                            activeEditingAllowanceValue = pcDtDoanhThu
                                            activeEditingAllowanceType = localCompanyConfig.getCalcTypeFor("pcDtDoanhThu")
                                        }
                                    )
                                    AllowanceRowItem(
                                        name = "13. Thâm niên",
                                        value = pcThamNien,
                                        fieldName = "pcThamNien",
                                        calcTypeMap = allowanceCalcTypesMap,
                                        onClick = {
                                            activeEditingAllowanceField = "pcThamNien"
                                            activeEditingAllowanceName = "Phụ cấp Thâm niên"
                                            activeEditingAllowanceValue = pcThamNien
                                            activeEditingAllowanceType = localCompanyConfig.getCalcTypeFor("pcThamNien")
                                        }
                                    )
                                    AllowanceRowItem(
                                        name = "14. Khác",
                                        value = pcKhac1,
                                        fieldName = "pcKhac1",
                                        calcTypeMap = allowanceCalcTypesMap,
                                        onClick = {
                                            activeEditingAllowanceField = "pcKhac1"
                                            activeEditingAllowanceName = "Phụ cấp Khác"
                                            activeEditingAllowanceValue = pcKhac1
                                            activeEditingAllowanceType = localCompanyConfig.getCalcTypeFor("pcKhac1")
                                        }
                                    )
                                    AllowanceRowItem(
                                        name = "15. Chuyên cần",
                                        value = tienChuyenCanGoc,
                                        fieldName = "tienChuyenCanGoc",
                                        calcTypeMap = allowanceCalcTypesMap,
                                        onClick = {
                                            activeEditingAllowanceField = "tienChuyenCanGoc"
                                            activeEditingAllowanceName = "Tiền Chuyên cần"
                                            activeEditingAllowanceValue = tienChuyenCanGoc
                                            activeEditingAllowanceType = localCompanyConfig.getCalcTypeFor("tienChuyenCanGoc")
                                        }
                                    )
                                }
                            }
                        }

                        // TAB 3: DANH SÁCH NHÂN VIÊN THUỘC CTY
                        2 -> {
                            val filteredCompanyEmps = remember(companyEmployees, empSearchQuery) {
                                if (empSearchQuery.isBlank()) companyEmployees
                                else companyEmployees.filter { 
                                    it.hoVaTen.contains(empSearchQuery, ignoreCase = true) || 
                                    it.maNhanVien.contains(empSearchQuery, ignoreCase = true) ||
                                    it.roleName.contains(empSearchQuery, ignoreCase = true)
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = empSearchQuery,
                                    onValueChange = { empSearchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Tìm nhân viên trong công ty...", color = Color.Gray, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Nhân viên trực thuộc: ${companyEmployees.size} NV",
                                        color = White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (filteredCompanyEmps.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .background(DarkBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (companyEmployees.isEmpty()) 
                                                "Chưa có nhân viên nào thuộc công ty này.\nNhân viên có thể nhập mã '${currentCompany.companyCode}' khi đăng ký." 
                                            else "Không tìm thấy kết quả",
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(filteredCompanyEmps) { emp ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = DarkBackground,
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(emp.hoVaTen, color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                            if (emp.roleName.isNotBlank()) {
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Surface(
                                                                    color = NeonBlue.copy(alpha = 0.15f),
                                                                    shape = RoundedCornerShape(4.dp)
                                                                ) {
                                                                    Text(
                                                                        emp.roleName,
                                                                        color = NeonBlue,
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Text(
                                                            "Mã NV: ${emp.maNhanVien} | Bộ phận: ${emp.boPhan.ifBlank { "Mặc định" }}",
                                                            color = Color.Gray,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                    if (emp.isAdmin) {
                                                        Surface(
                                                            color = AccentOrange.copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text("Admin", color = AccentOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Sticky Bottom Action Bar
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground.copy(alpha = 0.9f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val updated = localCompanyConfig.copy(
                                companyName = companyName.ifBlank { localCompanyConfig.companyName },
                                companyCode = companyCode.ifBlank { localCompanyConfig.companyCode },
                                description = description,
                                address = address,
                                lichTrinh = schedule.ifBlank { "07:30 - 19:30" },
                                luongCoBan = luongCoBan.toDoubleOrNull() ?: localCompanyConfig.luongCoBan,
                                luongDongBaoHiem = luongDongBaoHiem.toDoubleOrNull() ?: localCompanyConfig.luongDongBaoHiem,
                                tiLeDongBaoHiem = tiLeDongBaoHiem.toDoubleOrNull() ?: localCompanyConfig.tiLeDongBaoHiem,
                                doanPhiCongDoan = doanPhiCongDoan.toDoubleOrNull() ?: localCompanyConfig.doanPhiCongDoan,
                                heSoOtNgayThuong = hsOtThuong.toDoubleOrNull() ?: localCompanyConfig.heSoOtNgayThuong,
                                heSoOtChuNhat = hsOtChuNhat.toDoubleOrNull() ?: localCompanyConfig.heSoOtChuNhat,
                                heSoOtNgayLe = hsOtNgayLe.toDoubleOrNull() ?: localCompanyConfig.heSoOtNgayLe,
                                heSoOtDem = hsOtDem.toDoubleOrNull() ?: localCompanyConfig.heSoOtDem,
                                soGioNghiGiaiLao = soGioNghiGiaiLao.toDoubleOrNull() ?: localCompanyConfig.soGioNghiGiaiLao,
                                tinhKhauTruNghi = tinhKhauTruNghi,
                                pcXangXe = pcXangXe.toDoubleOrNull() ?: 0.0,
                                pcTrachNhiem = pcTrachNhiem.toDoubleOrNull() ?: 0.0,
                                pcKyThuat = pcKyThuat.toDoubleOrNull() ?: 0.0,
                                pcChucVu = pcChucVu.toDoubleOrNull() ?: 0.0,
                                pcHieuSuat = pcHieuSuat.toDoubleOrNull() ?: 0.0,
                                pcSanPham = pcSanPham.toDoubleOrNull() ?: 0.0,
                                pcComCa = pcComCa.toDoubleOrNull() ?: 0.0,
                                pcComOt = pcComOt.toDoubleOrNull() ?: 0.0,
                                pcNhaO = pcNhaO.toDoubleOrNull() ?: 0.0,
                                pcDocHai = pcDocHai.toDoubleOrNull() ?: 0.0,
                                pcDtDoanhThu = pcDtDoanhThu.toDoubleOrNull() ?: 0.0,
                                pcThamNien = pcThamNien.toDoubleOrNull() ?: 0.0,
                                pcCaDem = pcCaDem.toDoubleOrNull() ?: 0.0,
                                pcKhac1 = pcKhac1.toDoubleOrNull() ?: 0.0,
                                tienChuyenCanGoc = tienChuyenCanGoc.toDoubleOrNull() ?: 0.0
                            )
                            adminViewModel.saveCompany(updated) { success ->
                                if (success) {
                                    updateLocalUserSessionConfig(updated)
                                    android.widget.Toast.makeText(context, "Đã lưu cấu hình công ty thành công!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LƯU CẤU HÌNH", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val updated = localCompanyConfig.copy(
                                companyName = companyName.ifBlank { localCompanyConfig.companyName },
                                companyCode = companyCode.ifBlank { localCompanyConfig.companyCode },
                                description = description,
                                address = address,
                                lichTrinh = schedule.ifBlank { "07:30 - 19:30" },
                                luongCoBan = luongCoBan.toDoubleOrNull() ?: localCompanyConfig.luongCoBan,
                                luongDongBaoHiem = luongDongBaoHiem.toDoubleOrNull() ?: localCompanyConfig.luongDongBaoHiem,
                                tiLeDongBaoHiem = tiLeDongBaoHiem.toDoubleOrNull() ?: localCompanyConfig.tiLeDongBaoHiem,
                                doanPhiCongDoan = doanPhiCongDoan.toDoubleOrNull() ?: localCompanyConfig.doanPhiCongDoan,
                                heSoOtNgayThuong = hsOtThuong.toDoubleOrNull() ?: localCompanyConfig.heSoOtNgayThuong,
                                heSoOtChuNhat = hsOtChuNhat.toDoubleOrNull() ?: localCompanyConfig.heSoOtChuNhat,
                                heSoOtNgayLe = hsOtNgayLe.toDoubleOrNull() ?: localCompanyConfig.heSoOtNgayLe,
                                heSoOtDem = hsOtDem.toDoubleOrNull() ?: localCompanyConfig.heSoOtDem,
                                soGioNghiGiaiLao = soGioNghiGiaiLao.toDoubleOrNull() ?: localCompanyConfig.soGioNghiGiaiLao,
                                tinhKhauTruNghi = tinhKhauTruNghi,
                                pcXangXe = pcXangXe.toDoubleOrNull() ?: 0.0,
                                pcTrachNhiem = pcTrachNhiem.toDoubleOrNull() ?: 0.0,
                                pcKyThuat = pcKyThuat.toDoubleOrNull() ?: 0.0,
                                pcChucVu = pcChucVu.toDoubleOrNull() ?: 0.0,
                                pcHieuSuat = pcHieuSuat.toDoubleOrNull() ?: 0.0,
                                pcComCa = pcComCa.toDoubleOrNull() ?: 0.0,
                                pcComOt = pcComOt.toDoubleOrNull() ?: 0.0,
                                pcNhaO = pcNhaO.toDoubleOrNull() ?: 0.0,
                                pcDocHai = pcDocHai.toDoubleOrNull() ?: 0.0,
                                pcDtDoanhThu = pcDtDoanhThu.toDoubleOrNull() ?: 0.0,
                                pcThamNien = pcThamNien.toDoubleOrNull() ?: 0.0,
                                pcCaDem = pcCaDem.toDoubleOrNull() ?: 0.0,
                                pcKhac1 = pcKhac1.toDoubleOrNull() ?: 0.0,
                                tienChuyenCanGoc = tienChuyenCanGoc.toDoubleOrNull() ?: 0.0
                            )
                            adminViewModel.syncCompanyConfigToEmployees(updated) { count ->
                                updateLocalUserSessionConfig(updated)
                                android.widget.Toast.makeText(context, "Đã đồng bộ phụ cấp cho $count nhân viên!", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ĐỒNG BỘ NV", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (activeEditingAllowanceField != null) {
        AllowanceEditDialog(
            name = activeEditingAllowanceName,
            initialValue = activeEditingAllowanceValue,
            initialType = activeEditingAllowanceType,
            onDismiss = { activeEditingAllowanceField = null },
            onSave = { newValue, newType ->
                val cleanVal = newValue.filter { it.isDigit() }
                when (activeEditingAllowanceField) {
                    "tienChuyenCanGoc" -> tienChuyenCanGoc = cleanVal
                    "pcKyThuat" -> pcKyThuat = cleanVal
                    "pcTrachNhiem" -> pcTrachNhiem = cleanVal
                    "pcChucVu" -> pcChucVu = cleanVal
                    "pcHieuSuat" -> pcHieuSuat = cleanVal
                    "pcSanPham" -> pcSanPham = cleanVal
                    "pcComCa" -> pcComCa = cleanVal
                    "pcComOt" -> pcComOt = cleanVal
                    "pcNhaO" -> pcNhaO = cleanVal
                    "pcDocHai" -> pcDocHai = cleanVal
                    "pcDtDoanhThu" -> pcDtDoanhThu = cleanVal
                    "pcXangXe" -> pcXangXe = cleanVal
                    "pcCaDem" -> pcCaDem = cleanVal
                    "pcKhac1" -> pcKhac1 = cleanVal
                    "pcThamNien" -> pcThamNien = cleanVal
                }
                localCompanyConfig = localCompanyConfig.copyWithCalcType(activeEditingAllowanceField!!, newType)
                activeEditingAllowanceField = null
            }
        )
    }

    if (showAddRoleDialog && editingRole != null) {
        RoleEditDialog(
            role = editingRole!!,
            onSave = { updatedRole ->
                val existing = currentRoles.any { it.roleId == updatedRole.roleId }
                val newRoles = if (existing) {
                    currentRoles.map { if (it.roleId == updatedRole.roleId) updatedRole else it }
                } else {
                    currentRoles + updatedRole
                }
                val updatedComp = localCompanyConfig.updateRoles(newRoles)
                localCompanyConfig = updatedComp
                adminViewModel.saveCompany(updatedComp)
                showAddRoleDialog = false
                editingRole = null
            },
            onDismiss = {
                showAddRoleDialog = false
                editingRole = null
            }
        )
    }

    if (showAddCompanyDialog) {
        var newName by remember { mutableStateOf("") }
        var newCode by remember { mutableStateOf("") }
        var newSchedule by remember { mutableStateOf("08:00 - 17:00") }
        AlertDialog(
            onDismissRequest = { showAddCompanyDialog = false },
            containerColor = DarkContainer,
            title = { Text("Thêm Doanh Nghiệp Mới", color = White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Tên công ty (VD: Công ty A)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedLabelColor = NeonBlue,
                            unfocusedLabelColor = LightGray
                        )
                    )
                    OutlinedTextField(
                        value = newCode,
                        onValueChange = { newCode = it.uppercase() },
                        label = { Text("Mã công ty (VD: CTY_A)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedLabelColor = NeonBlue,
                            unfocusedLabelColor = LightGray
                        )
                    )
                    OutlinedTextField(
                        value = newSchedule,
                        onValueChange = { newSchedule = it },
                        label = { Text("Lịch trình chuẩn (08:00 - 17:00)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedLabelColor = NeonBlue,
                            unfocusedLabelColor = LightGray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newCode.isNotBlank()) {
                            val newComp = CompanyConfig(
                                companyId = "comp_${System.currentTimeMillis()}",
                                companyName = newName.trim(),
                                companyCode = newCode.trim(),
                                description = "Công ty $newName",
                                lichTrinh = newSchedule.ifBlank { "08:00 - 17:00" }
                            )
                            adminViewModel.saveCompany(newComp) { success ->
                                if (success) {
                                    adminViewModel.selectCompany(newComp.companyId)
                                    showAddCompanyDialog = false
                                    android.widget.Toast.makeText(context, "Đã tạo công ty mới thành công!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                ) {
                    Text("Tạo mới", color = White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCompanyDialog = false }) {
                    Text("Hủy", color = LightGray)
                }
            }
        )
    }

    if (showDeleteCompanyDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCompanyDialog = false },
            title = { Text("Xác nhận xóa công ty") },
            text = { Text("Bạn có chắc chắn muốn xóa công ty '${currentCompany.companyName}' không? Các nhân viên thuộc công ty này sẽ được chuyển về công ty mặc định.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteCompanyDialog = false
                        adminViewModel.deleteCompany(currentCompany.companyId) { success ->
                            if (success) {
                                android.widget.Toast.makeText(context, "Đã xóa công ty thành công!", android.widget.Toast.LENGTH_SHORT).show()
                                selectedCompanyId = "default_company"
                            } else {
                                android.widget.Toast.makeText(context, "Xóa công ty thất bại", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Xóa", color = AccentOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCompanyDialog = false }) {
                    Text("Hủy", color = LightGray)
                }
            },
            containerColor = DarkContainer
        )
    }
}

@Composable
fun CompanyCardSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkBackground.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            content()
        }
    }
}

@Composable
fun RoleCardItem(
    role: RoleConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        color = DarkBackground,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        role.roleName.ifBlank { "Chưa đặt tên" },
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = NeonBlue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "LCB: ${formatNumberWithDots(role.luongCoBan.toLong().toString())}đ",
                            color = NeonBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val allowancesCount = listOf(
                    role.pcChucVu, role.pcTrachNhiem, role.pcKyThuat, role.pcKhac1, 
                    role.pcSanPham, role.pcComCa, role.pcComOt, role.pcNhaO, role.pcDocHai, 
                    role.pcDtDoanhThu, role.pcXangXe, role.pcThamNien, role.pcCaDem, role.tienChuyenCanGoc
                ).count { it > 0.0 }
                
                Text(
                    "Có $allowancesCount khoản phụ cấp cấu hình riêng",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = NeonBlue, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = AccentOrange, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
