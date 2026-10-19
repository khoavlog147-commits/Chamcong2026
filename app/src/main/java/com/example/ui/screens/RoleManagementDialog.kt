package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CompanyConfig
import com.example.data.model.RoleConfig
import com.example.data.model.getRoles
import com.example.data.model.updateRoles
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleManagementDialog(
    company: CompanyConfig,
    onSave: (CompanyConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var roles by remember { mutableStateOf(company.getRoles()) }
    var editingRole by remember { mutableStateOf<RoleConfig?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    if (editingRole != null || showAddDialog) {
        val initialRole = editingRole ?: RoleConfig()
        RoleEditDialog(
            role = initialRole,
            onSave = { updatedRole ->
                roles = if (showAddDialog) {
                    roles + updatedRole
                } else {
                    roles.map { if (it.roleId == updatedRole.roleId) updatedRole else it }
                }
                showAddDialog = false
                editingRole = null
                onSave(company.updateRoles(roles))
            },
            onDismiss = {
                showAddDialog = false
                editingRole = null
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .widthIn(max = 560.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = DarkContainer,
            border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground.copy(alpha = 0.8f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Quản lý Chức Vụ (${company.companyName})", color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = LightGray)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("THÊM CHỨC VỤ MỚI", color = White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (roles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(DarkBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Chưa có chức vụ nào. Hãy thêm chức vụ để cấu hình mức lương & phụ cấp đặc thù cho từng vị trí làm việc.",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(roles) { role ->
                                RoleCardItem(
                                    role = role,
                                    onEdit = { editingRole = role },
                                    onDelete = {
                                        roles = roles.filter { it.roleId != role.roleId }
                                        onSave(company.updateRoles(roles))
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Đóng", color = LightGray, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleEditDialog(
    role: RoleConfig,
    onSave: (RoleConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var localRole by remember { mutableStateOf(role) }
    var roleName by remember { mutableStateOf(localRole.roleName) }
    var lcb by remember { mutableStateOf(if (localRole.luongCoBan == 0.0) "" else localRole.luongCoBan.toLong().toString()) }
    var tinhKhauTruNghi by remember { mutableStateOf(localRole.tinhKhauTruNghi) }
    var soGioNghiGiaiLao by remember { mutableStateOf(if (localRole.soGioNghiGiaiLao == 0.0) "1.5" else localRole.soGioNghiGiaiLao.toString()) }
    var lichTrinh by remember { mutableStateOf(localRole.lichTrinh.ifBlank { "07:30 - 19:30" }) }
    
    // Professional Allowances
    var pcChucVu by remember { mutableStateOf(if (localRole.pcChucVu == 0.0) "" else localRole.pcChucVu.toLong().toString()) }
    var pcTrachNhiem by remember { mutableStateOf(if (localRole.pcTrachNhiem == 0.0) "" else localRole.pcTrachNhiem.toLong().toString()) }
    var pcKyThuat by remember { mutableStateOf(if (localRole.pcKyThuat == 0.0) "" else localRole.pcKyThuat.toLong().toString()) }
    var pcHieuSuat by remember { mutableStateOf(if (localRole.pcHieuSuat == 0.0) "" else localRole.pcHieuSuat.toLong().toString()) }
    var pcSanPham by remember { mutableStateOf(if (localRole.pcSanPham == 0.0) "" else localRole.pcSanPham.toLong().toString()) }
    
    // Living / Welfare Allowances
    var pcComCa by remember { mutableStateOf(if (localRole.pcComCa == 0.0) "" else localRole.pcComCa.toLong().toString()) }
    var pcComOt by remember { mutableStateOf(if (localRole.pcComOt == 0.0) "" else localRole.pcComOt.toLong().toString()) }
    var pcNhaO by remember { mutableStateOf(if (localRole.pcNhaO == 0.0) "" else localRole.pcNhaO.toLong().toString()) }
    var pcXangXe by remember { mutableStateOf(if (localRole.pcXangXe == 0.0) "" else localRole.pcXangXe.toLong().toString()) }
    var pcDocHai by remember { mutableStateOf(if (localRole.pcDocHai == 0.0) "" else localRole.pcDocHai.toLong().toString()) }
    var pcCaDem by remember { mutableStateOf(if (localRole.pcCaDem == 0.0) "" else localRole.pcCaDem.toLong().toString()) }
    var pcThamNien by remember { mutableStateOf(if (localRole.pcThamNien == 0.0) "" else localRole.pcThamNien.toLong().toString()) }
    var pcDtDoanhThu by remember { mutableStateOf(if (localRole.pcDtDoanhThu == 0.0) "" else localRole.pcDtDoanhThu.toLong().toString()) }
    var pcKhac by remember { mutableStateOf(if (localRole.pcKhac1 == 0.0) "" else localRole.pcKhac1.toLong().toString()) }
    var chuyenCan by remember { mutableStateOf(if (localRole.tienChuyenCanGoc == 0.0) "" else localRole.tienChuyenCanGoc.toLong().toString()) }

    var activeEditingAllowanceField by remember { mutableStateOf<String?>(null) }
    var activeEditingAllowanceName by remember { mutableStateOf("") }
    var activeEditingAllowanceValue by remember { mutableStateOf("") }
    var activeEditingAllowanceType by remember { mutableStateOf("") }

    val allowanceCalcTypesMap = remember(localRole.allowanceCalcTypes) {
        if (localRole.allowanceCalcTypes.isBlank()) emptyMap()
        else localRole.allowanceCalcTypes.split(";").filter { it.contains(":") }.associate {
            val parts = it.split(":")
            parts[0] to parts[1]
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .widthIn(max = 540.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = DarkContainer,
            border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground.copy(alpha = 0.8f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (role.roleName.isEmpty()) "Thêm Chức Vụ Mới" else "Chỉnh Sửa Chức Vụ",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = LightGray)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Group 1: Basic Info & Salary
                    CompanyCardSection(title = "1. Tên chức vụ & Lương cơ bản", icon = Icons.Default.Badge) {
                        OutlinedTextField(
                            value = roleName,
                            onValueChange = { roleName = it },
                            label = { Text("Tên chức vụ (VD: Tổ trưởng, Kỹ sư...)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = lcb,
                            onValueChange = { lcb = it.filter { c -> c.isDigit() } },
                            label = { Text("Lương cơ bản mặc định (VNĐ)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = lichTrinh,
                            onValueChange = { lichTrinh = it },
                            label = { Text("Lịch làm việc của chức vụ (VD: 07:30 - 19:30)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Text(
                        text = "Chạm vào từng khoản phụ cấp để cấu hình số tiền mặc định & cách tính (Theo ngày / Theo giờ / Cố định) cho chức vụ này.",
                        color = LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Group 2: Professional Allowances
                    CompanyCardSection(title = "2. Phụ cấp chuyên môn & Trách nhiệm", icon = Icons.Default.Engineering) {
                        AllowanceRowItem(
                            name = "1. Kỹ thuật",
                            value = pcKyThuat,
                            fieldName = "pcKyThuat",
                            calcTypeMap = allowanceCalcTypesMap,
                            onClick = {
                                activeEditingAllowanceField = "pcKyThuat"
                                activeEditingAllowanceName = "Phụ cấp Kỹ thuật"
                                activeEditingAllowanceValue = pcKyThuat
                                activeEditingAllowanceType = localRole.getCalcTypeFor("pcKyThuat")
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
                                activeEditingAllowanceType = localRole.getCalcTypeFor("pcTrachNhiem")
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
                                activeEditingAllowanceType = localRole.getCalcTypeFor("pcChucVu")
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
                                activeEditingAllowanceType = localRole.getCalcTypeFor("pcHieuSuat")
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
                                activeEditingAllowanceType = localRole.getCalcTypeFor("pcSanPham")
                            }
                        )
                    }

                    // Group 3: Living & Welfare Allowances
                    CompanyCardSection(title = "3. Phụ cấp đời sống & Sinh hoạt", icon = Icons.Default.Restaurant) {
                        AllowanceRowItem(
                            name = "6. Cơm ca",
                            value = pcComCa,
                            fieldName = "pcComCa",
                            calcTypeMap = allowanceCalcTypesMap,
                            onClick = {
                                activeEditingAllowanceField = "pcComCa"
                                activeEditingAllowanceName = "Phụ cấp Cơm ca"
                                activeEditingAllowanceValue = pcComCa
                                activeEditingAllowanceType = localRole.getCalcTypeFor("pcComCa")
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
                                activeEditingAllowanceType = localRole.getCalcTypeFor("pcComOt")
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
                                activeEditingAllowanceType = localRole.getCalcTypeFor("pcNhaO")
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
                                activeEditingAllowanceType = localRole.getCalcTypeFor("pcXangXe")
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
                                activeEditingAllowanceType = localRole.getCalcTypeFor("pcCaDem")
                            }
                        )
                    }

                    // Group 4: Special Allowances
                    CompanyCardSection(title = "4. Phụ cấp đặc thù & Phúc lợi", icon = Icons.Default.CardGiftcard) {
                        AllowanceRowItem(
                            name = "11. Độc hại",
                            value = pcDocHai,
                            fieldName = "pcDocHai",
                            calcTypeMap = allowanceCalcTypesMap,
                            onClick = {
                                activeEditingAllowanceField = "pcDocHai"
                                activeEditingAllowanceName = "Phụ cấp Độc hại"
                                activeEditingAllowanceValue = pcDocHai
                                activeEditingAllowanceType = localRole.getCalcTypeFor("pcDocHai")
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
                                activeEditingAllowanceType = localRole.getCalcTypeFor("pcDtDoanhThu")
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
                                activeEditingAllowanceType = localRole.getCalcTypeFor("pcThamNien")
                            }
                        )
                        AllowanceRowItem(
                            name = "14. Khác",
                            value = pcKhac,
                            fieldName = "pcKhac1",
                            calcTypeMap = allowanceCalcTypesMap,
                            onClick = {
                                activeEditingAllowanceField = "pcKhac1"
                                activeEditingAllowanceName = "Phụ cấp Khác"
                                activeEditingAllowanceValue = pcKhac
                                activeEditingAllowanceType = localRole.getCalcTypeFor("pcKhac1")
                            }
                        )
                        AllowanceRowItem(
                            name = "15. Chuyên cần",
                            value = chuyenCan,
                            fieldName = "tienChuyenCanGoc",
                            calcTypeMap = allowanceCalcTypesMap,
                            onClick = {
                                activeEditingAllowanceField = "tienChuyenCanGoc"
                                activeEditingAllowanceName = "Tiền Chuyên cần"
                                activeEditingAllowanceValue = chuyenCan
                                activeEditingAllowanceType = localRole.getCalcTypeFor("tienChuyenCanGoc")
                            }
                        )
                    }

                    // Group 5: Break Time Deduction (Khấu trừ giờ nghỉ)
                    CompanyCardSection(title = "5. Cấu hình khấu trừ giờ nghỉ", icon = Icons.Default.Timer) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Khấu trừ giờ nghỉ giữa ca", color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Tự động trừ thời gian nghỉ giải lao khỏi tổng giờ làm", color = LightGray, fontSize = 11.sp)
                            }
                            Switch(
                                checked = tinhKhauTruNghi,
                                onCheckedChange = { tinhKhauTruNghi = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = NeonBlue)
                            )
                        }
                        if (tinhKhauTruNghi) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = soGioNghiGiaiLao,
                                onValueChange = { soGioNghiGiaiLao = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Số giờ nghỉ giải lao (giờ)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                // Bottom Actions
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground.copy(alpha = 0.9f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LightGray)
                    ) {
                        Text("Hủy", color = LightGray)
                    }

                    Button(
                        onClick = {
                            val updated = localRole.copy(
                                roleName = roleName.trim(),
                                luongCoBan = lcb.toDoubleOrNull() ?: 0.0,
                                pcChucVu = pcChucVu.toDoubleOrNull() ?: 0.0,
                                pcTrachNhiem = pcTrachNhiem.toDoubleOrNull() ?: 0.0,
                                pcKyThuat = pcKyThuat.toDoubleOrNull() ?: 0.0,
                                pcHieuSuat = pcHieuSuat.toDoubleOrNull() ?: 0.0,
                                pcSanPham = pcSanPham.toDoubleOrNull() ?: 0.0,
                                pcComCa = pcComCa.toDoubleOrNull() ?: 0.0,
                                pcComOt = pcComOt.toDoubleOrNull() ?: 0.0,
                                pcNhaO = pcNhaO.toDoubleOrNull() ?: 0.0,
                                pcXangXe = pcXangXe.toDoubleOrNull() ?: 0.0,
                                pcDocHai = pcDocHai.toDoubleOrNull() ?: 0.0,
                                pcCaDem = pcCaDem.toDoubleOrNull() ?: 0.0,
                                pcThamNien = pcThamNien.toDoubleOrNull() ?: 0.0,
                                pcDtDoanhThu = pcDtDoanhThu.toDoubleOrNull() ?: 0.0,
                                pcKhac1 = pcKhac.toDoubleOrNull() ?: 0.0,
                                tienChuyenCanGoc = chuyenCan.toDoubleOrNull() ?: 0.0,
                                tinhKhauTruNghi = tinhKhauTruNghi,
                                soGioNghiGiaiLao = soGioNghiGiaiLao.toDoubleOrNull() ?: 1.5,
                                lichTrinh = lichTrinh.ifBlank { "07:30 - 19:30" }
                            )
                            onSave(updated)
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                    ) {
                        Text("LƯU", color = White, fontWeight = FontWeight.Bold)
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
                    "tienChuyenCanGoc" -> chuyenCan = cleanVal
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
                    "pcKhac1" -> pcKhac = cleanVal
                    "pcThamNien" -> pcThamNien = cleanVal
                }
                localRole = localRole.copyWithCalcType(activeEditingAllowanceField!!, newType)
                activeEditingAllowanceField = null
            }
        )
    }
}
