
package com.example.ui.screens
import java.util.Locale
import androidx.compose.foundation.interaction.collectIsFocusedAsState

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserConfig
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkContainer
import com.example.ui.theme.LightGray
import com.example.ui.theme.MediumGray
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentRed
import com.example.ui.theme.White
import com.example.viewmodel.TimeSnapViewModel
import java.text.DecimalFormat

import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import com.example.util.ThousandSeparatorVisualTransformation
import androidx.compose.material.icons.filled.Group
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.onFocusChanged


fun formatNumberWithDots(rawInput: String): String {
    val clean = rawInput.replace(".", "").filter { it.isDigit() }
    if (clean.isEmpty()) return ""
    return try {
        val number = clean.toDouble()
        DecimalFormat("#,###").format(number).replace(",", ".")
    } catch (e: Exception) {
        clean
    }
}

fun interpretBreakHours(input: String, fallback: Double = 1.5): Triple<String, String, Double> {
    val clean = input.trim()
    if (clean.isEmpty()) return Triple("", "giờ", fallback)
    
    val normalized = clean.replace(",", ".")
    val parsedDouble = normalized.toDoubleOrNull()
    
    // Check if contains dot
    if (clean.contains(".") || clean.contains(",")) {
        val d = parsedDouble ?: fallback
        return Triple(clean, "giờ", d)
    }
    
    val parsedInt = clean.toIntOrNull()
    if (parsedInt != null) {
        return if (parsedInt <= 8) {
            Triple(clean, "giờ", parsedInt.toDouble())
        } else {
            Triple(clean, "phút", parsedInt.toDouble() / 60.0)
        }
    }
    
    val d = parsedDouble ?: fallback
    return Triple(clean, "giờ", d)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TimeSnapViewModel,
    onNavigateToAdmin: () -> Unit = {}
) {
    val context = LocalContext.current
    val configState by viewModel.userConfig.collectAsStateWithLifecycle()
    val syncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()
    val sessionState by viewModel.currentUserSession.collectAsStateWithLifecycle()

    val isAdmin = configState?.isAdmin == true || sessionState?.email?.lowercase() == "khoatubexxx@gmail.com"

    var latestVersionText by remember { mutableStateOf("") }
    var downloadUrlText by remember { mutableStateOf("") }
    var isPublishing by remember { mutableStateOf(false) }

    var publishedVersionCode by remember { mutableStateOf(0L) }
    var publishedVersionName by remember { mutableStateOf("") }
    var publishedDownloadUrl by remember { mutableStateOf("") }
    var loadErrorDetail by remember { mutableStateOf("") }

    val currentAppVersionCode = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    } catch (e: Exception) {
        1L
    }

    LaunchedEffect(Unit) {
        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.example.data.FirestoreService.fetchPublishedVersion()
        }
        publishedVersionCode = result.versionCode
        publishedVersionName = result.versionName
        publishedDownloadUrl = result.downloadUrl
        if (result.isSuccess) {
            if (latestVersionText.isEmpty()) {
                latestVersionText = if (result.versionName.isNotEmpty()) result.versionName else (result.versionCode + 1).toString()
            }
            if (downloadUrlText.isEmpty()) {
                downloadUrlText = result.downloadUrl
            }
        } else {
            loadErrorDetail = result.errorMessage
        }
    }

    var isInitialized by remember { mutableStateOf(false) }

    val currentAppVersionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.4"
        } catch (e: Exception) {
            "1.4"
        }
    }
    var checkingUpdate by remember { mutableStateOf(false) }
    var manualUpdateInfo by remember { mutableStateOf<com.example.data.AppVersionControl?>(null) }

    fun convertYyyyMmDdToDdMmYyyy(input: String): String {
        if (input.isBlank()) return ""
        val parts = input.split("-")
        if (parts.size == 3) {
            return "${parts[2]}${parts[1]}${parts[0]}"
        }
        return input
    }

    fun sanitizeAndCheckFutureDate(rawDigits: String): String {
        if (rawDigits.length != 8) return rawDigits
        try {
            val day = rawDigits.substring(0, 2).toIntOrNull() ?: 1
            val month = rawDigits.substring(2, 4).toIntOrNull() ?: 1
            val year = rawDigits.substring(4, 8).toIntOrNull() ?: 2024
            
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            
            val inputCal = java.util.Calendar.getInstance()
            inputCal.set(java.util.Calendar.YEAR, year)
            inputCal.set(java.util.Calendar.MONTH, month - 1)
            inputCal.set(java.util.Calendar.DAY_OF_MONTH, day)
            
            if (inputCal.after(cal)) {
                val newDay = "01"
                val newMonth = String.format("%02d", month)
                val newYear = String.format("%04d", year)
                return "$newDay$newMonth$newYear"
            }
        } catch (e: Exception) {}
        return rawDigits
    }

    fun convertDdMmYyyyToYyyyMmDd(rawDigits: String): String {
        if (rawDigits.length != 8) return ""
        val day = rawDigits.substring(0, 2)
        val month = rawDigits.substring(2, 4)
        val year = rawDigits.substring(4, 8)
        return "$year-$month-$day"
    }

    var ngayVaoLamInput by remember { mutableStateOf("") }

    var hoVaTen by remember { mutableStateOf("") }
    var maNhanVien by remember { mutableStateOf("") }
    var emailDangKy by remember { mutableStateOf("") }
    var ngayVaoLam by remember { mutableStateOf("") }

    var luongCoBan by remember { mutableStateOf("") }
    var luongDongBaoHiem by remember { mutableStateOf("") }
    var tiLeDongBaoHiem by remember { mutableStateOf("") }
    var doanPhiCongDoan by remember { mutableStateOf("") }

    var heSoOtNgayThuong by remember { mutableStateOf("") }
    var heSoOtChuNhat by remember { mutableStateOf("") }
    var heSoOtNgayLe by remember { mutableStateOf("") }
    var heSoOtDem by remember { mutableStateOf("") }

    var tienChuyenCanGoc by remember { mutableStateOf("") }
    var soNgayPhepNam by remember { mutableStateOf("") }
    var phepNamConLai by remember { mutableStateOf("") }

    // 12 Allowances
    var pcKyThuat by remember { mutableStateOf("") }
    var pcTrachNhiem by remember { mutableStateOf("") }
    var pcChucVu by remember { mutableStateOf("") }
    var pcHieuSuat by remember { mutableStateOf("") }
    var pcSanPham by remember { mutableStateOf("") }
    var pcComCa by remember { mutableStateOf("") }
    var pcComOt by remember { mutableStateOf("") }
    var pcNhaO by remember { mutableStateOf("") }
    var pcDocHai by remember { mutableStateOf("") }
    var pcDtDoanhThu by remember { mutableStateOf("") }
    var pcXangXe by remember { mutableStateOf("") }
    var pcCaDem by remember { mutableStateOf("") }
    var pcKhac1 by remember { mutableStateOf("") }
    var pcThamNien by remember { mutableStateOf("") }
    var allowanceCalcTypesMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var activeEditingAllowanceField by remember { mutableStateOf<String?>(null) }
    var activeEditingAllowanceName by remember { mutableStateOf("") }
    var activeEditingAllowanceValue by remember { mutableStateOf("") }
    var activeEditingAllowanceType by remember { mutableStateOf("") }
    var tinhKhauTruNghi by remember { mutableStateOf(false) }
    var soGioNghiGiaiLao by remember { mutableStateOf("") }

    // Sync database configuration state once initialized
    LaunchedEffect(configState) {
        if (configState != null && !isInitialized) {
            val c = configState!!
            hoVaTen = c.hoVaTen
            maNhanVien = c.maNhanVien
            emailDangKy = c.emailDangKy
            ngayVaoLam = c.ngayVaoLam
            ngayVaoLamInput = convertYyyyMmDdToDdMmYyyy(c.ngayVaoLam)

            luongCoBan = c.luongCoBan.toLong().toString()
            luongDongBaoHiem = c.luongDongBaoHiem.toLong().toString()
            tiLeDongBaoHiem = c.tiLeDongBaoHiem.toString()
            doanPhiCongDoan = c.doanPhiCongDoan.toLong().toString()

            heSoOtNgayThuong = c.heSoOtNgayThuong.toString()
            heSoOtChuNhat = c.heSoOtChuNhat.toString()
            heSoOtNgayLe = c.heSoOtNgayLe.toString()
            heSoOtDem = c.heSoOtDem.toString()

            tienChuyenCanGoc = c.tienChuyenCanGoc.toLong().toString()
            soNgayPhepNam = c.soNgayPhepNam.toString()
            phepNamConLai = c.phepNamConLai.toString()

            pcKyThuat = c.pcKyThuat.toLong().toString()
            pcTrachNhiem = c.pcTrachNhiem.toLong().toString()
            pcChucVu = c.pcChucVu.toLong().toString()
            pcHieuSuat = c.pcHieuSuat.toLong().toString()
            pcSanPham = c.pcSanPham.toLong().toString()
            pcComCa = c.pcComCa.toLong().toString()
            pcComOt = c.pcComOt.toLong().toString()
            pcNhaO = c.pcNhaO.toLong().toString()
            pcDocHai = c.pcDocHai.toLong().toString()
            pcDtDoanhThu = c.pcDtDoanhThu.toLong().toString()
            pcXangXe = c.pcXangXe.toLong().toString()
            pcCaDem = c.pcCaDem.toLong().toString()
            pcKhac1 = c.pcKhac1.toLong().toString()
            pcThamNien = c.pcThamNien.toLong().toString()
            allowanceCalcTypesMap = c.allowanceCalcTypes.split(";").filter { it.contains(":") }.associate {
                val parts = it.split(":")
                parts[0] to parts[1]
            }
            tinhKhauTruNghi = c.tinhKhauTruNghi
            soGioNghiGiaiLao = c.soGioNghiGiaiLao.toString()

            isInitialized = true
        }
    }

    // Live calculations formatting
    val lcbVal = luongCoBan.replace(".", "").toDoubleOrNull() ?: 0.0
    val hourlyPrice = lcbVal / 26.0 / 8.0
    val fmtPrice = DecimalFormat("#,###").format(hourlyPrice)

    val otThuongCoeff = heSoOtNgayThuong.toDoubleOrNull() ?: 1.5
    val otCnCoeff = heSoOtChuNhat.toDoubleOrNull() ?: 2.0
    val otLeCoeff = heSoOtNgayLe.toDoubleOrNull() ?: 3.0
    val otDemCoeff = heSoOtDem.toDoubleOrNull() ?: 1.75

    val otThuongPrice = hourlyPrice * otThuongCoeff
    val otCnPrice = hourlyPrice * otCnCoeff
    val otLePrice = hourlyPrice * otLeCoeff
    val otDemPrice = hourlyPrice * otDemCoeff

    // Direct save trigger function
    val saveChanges = {
        val current = configState
        if (current != null) {
            val sLcb = luongCoBan.replace(".", "")
            val sLbh = luongDongBaoHiem.replace(".", "")
            val sDp = doanPhiCongDoan.replace(".", "")
            val sCc = tienChuyenCanGoc.replace(".", "")

            if (current.maNhanVien != maNhanVien && maNhanVien.isNotBlank()) {
                val sharedPrefs = context.getSharedPreferences("timesnap_auth", android.content.Context.MODE_PRIVATE)
                val email = current.emailDangKy.ifEmpty { viewModel.authController.currentUserFlow.value?.email ?: "" }
                if (email.isNotEmpty()) {
                    sharedPrefs.edit()
                        .remove("email_of_employee_${current.maNhanVien}")
                        .putString("email_of_employee_$maNhanVien", email)
                        .putString("maNhanVien_of_email_$email", maNhanVien)
                        .apply()
                }
            }

            val updated = current.copy(
                hoVaTen = hoVaTen,
                maNhanVien = maNhanVien,
                emailDangKy = emailDangKy,
                ngayVaoLam = ngayVaoLam,
                luongCoBan = sLcb.toDoubleOrNull() ?: 0.0,
                luongDongBaoHiem = sLbh.toDoubleOrNull() ?: 0.0,
                tiLeDongBaoHiem = tiLeDongBaoHiem.toDoubleOrNull() ?: 0.0,
                doanPhiCongDoan = sDp.toDoubleOrNull() ?: 0.0,
                heSoOtNgayThuong = heSoOtNgayThuong.toDoubleOrNull() ?: 0.0,
                heSoOtChuNhat = heSoOtChuNhat.toDoubleOrNull() ?: 0.0,
                heSoOtNgayLe = heSoOtNgayLe.toDoubleOrNull() ?: 0.0,
                heSoOtDem = heSoOtDem.toDoubleOrNull() ?: 0.0,
                tienChuyenCanGoc = sCc.toDoubleOrNull() ?: 0.0,
                soNgayPhepNam = soNgayPhepNam.toIntOrNull() ?: 0,
                phepNamConLai = phepNamConLai.toIntOrNull() ?: 0,
                pcKyThuat = pcKyThuat.replace(".", "").toDoubleOrNull() ?: 0.0,
                pcTrachNhiem = pcTrachNhiem.replace(".", "").toDoubleOrNull() ?: 0.0,
                pcChucVu = pcChucVu.replace(".", "").toDoubleOrNull() ?: 0.0,
                pcHieuSuat = pcHieuSuat.replace(".", "").toDoubleOrNull() ?: 0.0,
                pcSanPham = pcSanPham.replace(".", "").toDoubleOrNull() ?: 0.0,
                pcComCa = pcComCa.replace(".", "").toDoubleOrNull() ?: 0.0,
                pcComOt = pcComOt.replace(".", "").toDoubleOrNull() ?: 0.0,
                pcNhaO = pcNhaO.replace(".", "").toDoubleOrNull() ?: 0.0,
                pcDocHai = pcDocHai.replace(".", "").toDoubleOrNull() ?: 0.0,
                pcDtDoanhThu = pcDtDoanhThu.replace(".", "").toDoubleOrNull() ?: 0.0,
                pcXangXe = pcXangXe.replace(".", "").toDoubleOrNull() ?: 0.0,
                pcCaDem = pcCaDem.replace(".", "").toDoubleOrNull() ?: 0.0,
                pcKhac1 = pcKhac1.replace(".", "").toDoubleOrNull() ?: 0.0,
                pcThamNien = pcThamNien.replace(".", "").toDoubleOrNull() ?: 0.0,
                allowanceCalcTypes = allowanceCalcTypesMap.entries.joinToString(";") { "${it.key}:${it.value}" },
                tinhKhauTruNghi = tinhKhauTruNghi,
                soGioNghiGiaiLao = interpretBreakHours(soGioNghiGiaiLao, 0.0).third
            )
            viewModel.updateSalaryConfig(updated)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HỆ SỐ LƯƠNG THEO HỢP ĐỒNG", fontWeight = FontWeight.Bold, color = White, fontSize = 16.sp) },
                actions = {
                    // Hidden as requested: "ở trên có chữ đã đồng bộ m cũng xoá luôn thông báo đó đi"
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            if (isAdmin) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAdmin() },
                    color = NeonBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(NeonBlue.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = NeonBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "QUẢN TRỊ HỆ THỐNG",
                                color = White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // CATEGORY 0: HỒ SƠ NHÂN VIÊN
            CategoryLayout(title = "HỒ SƠ CÁ NHÂN NHÂN VIÊN", icon = Icons.Default.VerifiedUser) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ConfigInputField(
                        label = "Họ và tên",
                        value = hoVaTen,
                        onValueChange = { hoVaTen = it; saveChanges() },
                        keyboardType = KeyboardType.Text
                    )
                    ConfigInputField(
                        label = "Mã nhân viên công ty (UID)",
                        value = maNhanVien,
                        onValueChange = { maNhanVien = it; saveChanges() },
                        keyboardType = KeyboardType.Text
                    )
                    ConfigInputField(
                        label = "Email đăng ký tài khoản (Gmail - Không thể sửa)",
                        value = emailDangKy.ifEmpty { viewModel.authController.currentUserFlow.value?.email ?: "Chưa có thiết lập" },
                        onValueChange = {},
                        keyboardType = KeyboardType.Email,
                        enabled = false
                    )
                    ConfigInputField(
                        label = "Ngày nhận việc",
                        value = ngayVaoLamInput,
                        onValueChange = { input ->
                            var clean = input.filter { it.isDigit() }.take(8)
                            if (clean.length == 8) {
                                clean = sanitizeAndCheckFutureDate(clean)
                                val isoDate = convertDdMmYyyyToYyyyMmDd(clean)
                                ngayVaoLam = isoDate
                                saveChanges()
                            } else if (clean.isEmpty()) {
                                ngayVaoLam = ""
                                saveChanges()
                            }
                            ngayVaoLamInput = clean
                        },
                        keyboardType = KeyboardType.Number,
                        visualTransformation = DateVisualTransformation()
                    )
                    Text(
                        text = "Thiết lập này giúp tự động tính toán lại chuyên cần, không trừ chuyên cần của nhân viên mới gia nhập giữa tháng.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // CATEGORY 1: HỢP ĐỒNG LƯƠNG & BẢO HIỂM
            CategoryLayout(title = "HỢP ĐỒNG LƯƠNG & BẢO HIỂM", icon = Icons.Default.Payments) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ConfigInputField(
                        label = "Mức lương cơ bản hàng tháng (LCB)",
                        value = luongCoBan,
                        onValueChange = { luongCoBan = it.filter { c -> c.isDigit() }; saveChanges() },
                        visualTransformation = ThousandSeparatorVisualTransformation()
                    )

                    ConfigInputField(
                        label = "Lương đóng bảo hiểm xã hội (LBH)",
                        value = luongDongBaoHiem,
                        onValueChange = { luongDongBaoHiem = it.filter { c -> c.isDigit() }; saveChanges() },
                        visualTransformation = ThousandSeparatorVisualTransformation()
                    )

                    ConfigInputField(
                        label = "Tỉ lệ đóng BH (%)",
                        value = tiLeDongBaoHiem,
                        onValueChange = { tiLeDongBaoHiem = it; saveChanges() },
                        keyboardType = KeyboardType.Decimal
                    )

                    ConfigInputField(
                        label = "Đoàn phí công đoàn",
                        value = doanPhiCongDoan,
                        onValueChange = { doanPhiCongDoan = it.filter { c -> c.isDigit() }; saveChanges() },
                        visualTransformation = ThousandSeparatorVisualTransformation()
                    )

                    // Dynamic calculated cost
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentGreen.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .border(1.dp, AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Lương mỗi giờ = LCB / 26 / 8 = $fmtPrice đ/giờ\nDùng làm căn cứ tính tăng ca chính xác.",
                            color = AccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // CATEGORY 2: HỆ SỐ TĂNG CA (OT)
            CategoryLayout(title = "HỆ SỐ TĂNG CA (OT)", icon = Icons.Default.Percent) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ConfigInputField(
                            label = "Ngày thường",
                            value = heSoOtNgayThuong,
                            onValueChange = { heSoOtNgayThuong = it; saveChanges() },
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        ConfigInputField(
                            label = "Chủ nhật",
                            value = heSoOtChuNhat,
                            onValueChange = { heSoOtChuNhat = it; saveChanges() },
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ConfigInputField(
                            label = "Ngày Lễ",
                            value = heSoOtNgayLe,
                            onValueChange = { heSoOtNgayLe = it; saveChanges() },
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        ConfigInputField(
                            label = "OT đêm",
                            value = heSoOtDem,
                            onValueChange = { heSoOtDem = it; saveChanges() },
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Simulated live estimate
                    val dec = DecimalFormat("#,###")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "• Tiền OT Ngày thường (${heSoOtNgayThuong}x): ${dec.format(otThuongPrice)} đ/giờ",
                            color = LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Tiền OT Chủ nhật (${heSoOtChuNhat}x): ${dec.format(otCnPrice)} đ/giờ",
                            color = AccentRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Tiền OT Ngày Lễ (${heSoOtNgayLe}x): ${dec.format(otLePrice)} đ/giờ",
                            color = AccentOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Tiền OT Đêm (${heSoOtDem}x): ${dec.format(otDemPrice)} đ/giờ",
                            color = NeonBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // CATEGORY 2.5: THỜI GIAN NGHỈ TRONG CA
            CategoryLayout(title = "THỜI GIAN NGHỈ TRONG CA", icon = Icons.Default.Settings) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bật khấu trừ thời gian nghỉ",
                                color = White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tự động trừ thời gian nghỉ ra khỏi ca làm việc. Hãy tắt đi nếu bạn làm xuyên suốt không nghỉ trưa/chiều (ví dụ làm thông ca) để được tính đủ công và tăng ca.",
                                color = LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Switch(
                            checked = tinhKhauTruNghi,
                            onCheckedChange = { 
                                tinhKhauTruNghi = it
                                saveChanges()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = White,
                                checkedTrackColor = NeonBlue,
                                uncheckedThumbColor = MediumGray,
                                uncheckedTrackColor = Color(0xFF1E1E1E)
                            )
                        )
                    }

                    if (tinhKhauTruNghi) {
                        ConfigInputField(
                            label = "Khấu trừ thời gian nghỉ (nhập sút phút vd: 30, hoặc số giờ vd: 1.5)",
                            value = soGioNghiGiaiLao,
                            onValueChange = { 
                                soGioNghiGiaiLao = it
                                saveChanges()
                            },
                            keyboardType = KeyboardType.Decimal
                        )

                        val interpretation = interpretBreakHours(soGioNghiGiaiLao)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NeonBlue.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            val computedHrs = interpretation.third
                            Text(
                                text = "Ví dụ: Nếu chấm công lúc 7h30 sáng:\n" +
                                       "• Nghỉ trưa từ 11h30 - 12h30 (1 tiếng)\n" +
                                       "• Nghỉ chiều từ 16h30 - 17h00 (30 phút)\n" +
                                       "• Tổng cộng nghỉ: $computedHrs giờ.\n" +
                                       "Hệ thống sẽ tự động trừ $computedHrs giờ ra khỏi thời gian làm việc và tự động tính đẩy giờ về (Ví dụ: đến 17h00 mới đủ 8 tiếng).",
                                color = NeonBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // CATEGORY 3: CHUYÊN CẦN & PHÉP NĂM
            CategoryLayout(title = "CHUYÊN CẦN & PHÉP NĂM", icon = Icons.Default.DateRange) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AllowanceRowItem(
                        name = "Tiền chuyên cần gốc",
                        value = tienChuyenCanGoc,
                        fieldName = "tienChuyenCanGoc",
                        calcTypeMap = allowanceCalcTypesMap,
                        onClick = {
                            activeEditingAllowanceField = "tienChuyenCanGoc"
                            activeEditingAllowanceName = "Chuyên cần gốc"
                            activeEditingAllowanceValue = tienChuyenCanGoc
                            activeEditingAllowanceType = allowanceCalcTypesMap["tienChuyenCanGoc"] ?: "MONTHLY_FLAT"
                        }
                    )
                    ConfigInputField(
                        label = "Số ngày phép cho phép/năm",
                        value = soNgayPhepNam,
                        onValueChange = { soNgayPhepNam = it.filter { c -> c.isDigit() }; saveChanges() }
                    )
                    ConfigInputField(
                        label = "Số ngày phép còn lại (Hiện có)",
                        value = phepNamConLai,
                        onValueChange = { phepNamConLai = it.filter { c -> c.isDigit() }; saveChanges() }
                    )
                }
            }

            // CATEGORY 4: CÁC KHOẢN PHỤ CẤP
            CategoryLayout(title = "CÁC KHOẢN PHỤ CẤP", icon = Icons.Default.Star) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Chạm vào từng mục để chỉnh sửa số tiền và tính chất tính lương của khoản phụ cấp đó.",
                        color = LightGray,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic
                    )

                    // Sub-group 1: 📌 Phụ cấp theo tháng
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📌 ", fontSize = 14.sp)
                            Text("PHỤ CẤP THEO THÁNG", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        AllowanceRowItem(
                            name = "1. Kỹ thuật",
                            value = pcKyThuat,
                            fieldName = "pcKyThuat",
                            calcTypeMap = allowanceCalcTypesMap,
                            onClick = {
                                activeEditingAllowanceField = "pcKyThuat"
                                activeEditingAllowanceName = "Phụ cấp Kỹ thuật"
                                activeEditingAllowanceValue = pcKyThuat
                                activeEditingAllowanceType = allowanceCalcTypesMap["pcKyThuat"] ?: com.example.data.model.UserConfig.getDefaultCalcType("pcKyThuat")
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
                                activeEditingAllowanceType = allowanceCalcTypesMap["pcTrachNhiem"] ?: com.example.data.model.UserConfig.getDefaultCalcType("pcTrachNhiem")
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
                                activeEditingAllowanceType = allowanceCalcTypesMap["pcChucVu"] ?: com.example.data.model.UserConfig.getDefaultCalcType("pcChucVu")
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
                                activeEditingAllowanceType = allowanceCalcTypesMap["pcHieuSuat"] ?: com.example.data.model.UserConfig.getDefaultCalcType("pcHieuSuat")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.Gray.copy(alpha = 0.2f)))

                    // Sub-group 2: 🍱 Phụ cấp theo ca
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🍱 ", fontSize = 14.sp)
                            Text("PHỤ CẤP THEO CA", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        AllowanceRowItem(
                            name = "5. Cơm / Ca làm việc",
                            value = pcComCa,
                            fieldName = "pcComCa",
                            calcTypeMap = allowanceCalcTypesMap,
                            onClick = {
                                activeEditingAllowanceField = "pcComCa"
                                activeEditingAllowanceName = "Phụ cấp Cơm/CA"
                                activeEditingAllowanceValue = pcComCa
                                activeEditingAllowanceType = allowanceCalcTypesMap["pcComCa"] ?: com.example.data.model.UserConfig.getDefaultCalcType("pcComCa")
                            }
                        )
                        AllowanceRowItem(
                            name = "6. Cơm tăng ca (OT)",
                            value = pcComOt,
                            fieldName = "pcComOt",
                            calcTypeMap = allowanceCalcTypesMap,
                            onClick = {
                                activeEditingAllowanceField = "pcComOt"
                                activeEditingAllowanceName = "Phụ cấp Cơm OT"
                                activeEditingAllowanceValue = pcComOt
                                activeEditingAllowanceType = allowanceCalcTypesMap["pcComOt"] ?: com.example.data.model.UserConfig.getDefaultCalcType("pcComOt")
                            }
                        )
                        AllowanceRowItem(
                            name = "7. Phụ cấp ca đêm (mỗi ca)",
                            value = pcCaDem,
                            fieldName = "pcCaDem",
                            calcTypeMap = allowanceCalcTypesMap,
                            onClick = {
                                activeEditingAllowanceField = "pcCaDem"
                                activeEditingAllowanceName = "Phụ cấp Ca đêm"
                                activeEditingAllowanceValue = pcCaDem
                                activeEditingAllowanceType = allowanceCalcTypesMap["pcCaDem"] ?: com.example.data.model.UserConfig.getDefaultCalcType("pcCaDem")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.Gray.copy(alpha = 0.2f)))

                    // Sub-group 3: 🎁 Phụ cấp khác
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎁 ", fontSize = 14.sp)
                            Text("PHỤ CẤP KHÁC", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        AllowanceRowItem(
                            name = "8. Nhà ở",
                            value = pcNhaO,
                            fieldName = "pcNhaO",
                            calcTypeMap = allowanceCalcTypesMap,
                            onClick = {
                                activeEditingAllowanceField = "pcNhaO"
                                activeEditingAllowanceName = "Phụ cấp Nhà ở"
                                activeEditingAllowanceValue = pcNhaO
                                activeEditingAllowanceType = allowanceCalcTypesMap["pcNhaO"] ?: com.example.data.model.UserConfig.getDefaultCalcType("pcNhaO")
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
                                activeEditingAllowanceType = allowanceCalcTypesMap["pcXangXe"] ?: com.example.data.model.UserConfig.getDefaultCalcType("pcXangXe")
                            }
                        )
                        AllowanceRowItem(
                            name = "10. Độc hại",
                            value = pcDocHai,
                            fieldName = "pcDocHai",
                            calcTypeMap = allowanceCalcTypesMap,
                            onClick = {
                                activeEditingAllowanceField = "pcDocHai"
                                activeEditingAllowanceName = "Phụ cấp Độc hại"
                                activeEditingAllowanceValue = pcDocHai
                                activeEditingAllowanceType = allowanceCalcTypesMap["pcDocHai"] ?: com.example.data.model.UserConfig.getDefaultCalcType("pcDocHai")
                            }
                        )
                        AllowanceRowItem(
                            name = "11. Doanh thu",
                            value = pcDtDoanhThu,
                            fieldName = "pcDtDoanhThu",
                            calcTypeMap = allowanceCalcTypesMap,
                            onClick = {
                                activeEditingAllowanceField = "pcDtDoanhThu"
                                activeEditingAllowanceName = "Phụ cấp Doanh thu"
                                activeEditingAllowanceValue = pcDtDoanhThu
                                activeEditingAllowanceType = allowanceCalcTypesMap["pcDtDoanhThu"] ?: com.example.data.model.UserConfig.getDefaultCalcType("pcDtDoanhThu")
                            }
                        )
                        AllowanceRowItem(
                            name = "12. Thâm niên",
                            value = pcThamNien,
                            fieldName = "pcThamNien",
                            calcTypeMap = allowanceCalcTypesMap,
                            onClick = {
                                activeEditingAllowanceField = "pcThamNien"
                                activeEditingAllowanceName = "Phụ cấp Thâm niên"
                                activeEditingAllowanceValue = pcThamNien
                                activeEditingAllowanceType = allowanceCalcTypesMap["pcThamNien"] ?: com.example.data.model.UserConfig.getDefaultCalcType("pcThamNien")
                            }
                        )
                        AllowanceRowItem(
                            name = "13. Sản phẩm",
                            value = pcSanPham,
                            fieldName = "pcSanPham",
                            calcTypeMap = allowanceCalcTypesMap,
                            onClick = {
                                activeEditingAllowanceField = "pcSanPham"
                                activeEditingAllowanceName = "Phụ cấp Sản phẩm"
                                activeEditingAllowanceValue = pcSanPham
                                activeEditingAllowanceType = allowanceCalcTypesMap["pcSanPham"] ?: com.example.data.model.UserConfig.getDefaultCalcType("pcSanPham")
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
                                activeEditingAllowanceType = allowanceCalcTypesMap["pcKhac1"] ?: com.example.data.model.UserConfig.getDefaultCalcType("pcKhac1")
                            }
                        )
                    }
                }
            }

            // Dialog for editing allowance details
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
                        allowanceCalcTypesMap = allowanceCalcTypesMap + (activeEditingAllowanceField!! to newType)
                        activeEditingAllowanceField = null
                        saveChanges()
                    }
                )
            }

            // CATEGORY 5: CẤU HÌNH NHẮC NHỞ CHẤM CÔNG (Notification Config)
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current
            val currentUid = sessionState?.uid
            val notificationPrefs = remember(context, currentUid) { com.example.notification.NotificationHelper.getPrefs(context, currentUid) }
            var notificationsEnabled by remember(currentUid) { mutableStateOf(notificationPrefs.getBoolean("notifications_enabled", true)) }
            var smartLearningEnabled by remember(currentUid) { mutableStateOf(notificationPrefs.getBoolean("smart_learning_enabled", true)) }
            var reminderMinutes by remember(currentUid) { mutableStateOf(notificationPrefs.getString("reminder_minutes_before", "15") ?: "15") }
            var showMinutesPickerDialog by remember { mutableStateOf(false) }

            var autoClockInOutEnabled by remember(currentUid) { mutableStateOf(notificationPrefs.getBoolean("auto_clock_in_out_enabled", false)) }
            var customCheckInTime by remember(currentUid) { mutableStateOf(notificationPrefs.getString("custom_check_in_time", "") ?: "") }
            var customCheckoutTime by remember(currentUid) { mutableStateOf(notificationPrefs.getString("custom_checkout_time", "") ?: "") }

            var estimatedInTime by remember(currentUid) { mutableStateOf("07:30") }
            var estimatedOutTime by remember(currentUid) { mutableStateOf("17:30") }

            var shiftRotationWeeks by remember(currentUid) { mutableStateOf(notificationPrefs.getInt("shift_rotation_weeks", 2)) }
            var rotationWeeksTf by remember(currentUid, shiftRotationWeeks) { mutableStateOf(TextFieldValue(shiftRotationWeeks.toString())) }
            var shiftAnchorTime by remember(currentUid) { mutableStateOf(notificationPrefs.getLong("shift_anchor_time", System.currentTimeMillis())) }
            var shiftAnchorType by remember(currentUid) { mutableStateOf(notificationPrefs.getString("shift_anchor_type", "DAY") ?: "DAY") }

            LaunchedEffect(currentUid) {
                sessionState?.let { session ->
                    val inMs = com.example.notification.NotificationHelper.estimateHistoricalCheckInTime(context, session.uid)
                    estimatedInTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(inMs))
                    
                    val mockEntry = com.example.data.model.TimeEntry(userId = session.uid, date = "", checkInTime = inMs)
                    val outMs = com.example.notification.NotificationHelper.estimateHistoricalCheckoutTime(context, session.uid, mockEntry)
                    estimatedOutTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(outMs))
                }
            }

            CategoryLayout(title = "NHẮC NHỞ CHẤM CÔNG", icon = Icons.Default.AlarmOn) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 1. Bật thông báo nhắc nhở
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Thông báo nhắc nhở",
                            color = White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { isEnabled ->
                                notificationsEnabled = isEnabled
                                smartLearningEnabled = isEnabled
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
                                
                                val session = sessionState
                                if (session != null) {
                                    if (isEnabled) {
                                        com.example.notification.NotificationHelper.scheduleNextCheckInReminder(context, session.uid)
                                        com.example.notification.NotificationHelper.cancelAutoCheckIn(context, session.uid)
                                    } else {
                                        com.example.notification.NotificationHelper.cancelCheckOutReminder(context, session.uid)
                                        androidx.work.WorkManager.getInstance(context).cancelUniqueWork("checkin_reminder_${session.uid}")
                                    }
                                }
                                Toast.makeText(context, if (isEnabled) "Đã bật nhắc nhở (Đã tắt tự động)" else "Đã tắt nhắc nhở", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = White,
                                checkedTrackColor = NeonBlue,
                                uncheckedThumbColor = MediumGray,
                                uncheckedTrackColor = Color(0xFF1E1E1E)
                            )
                        )
                    }

                    if (notificationsEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showMinutesPickerDialog = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Số phút nhắc nhở trước ca",
                                color = LightGray,
                                fontSize = 13.sp
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "$reminderMinutes phút",
                                    color = NeonBlue,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = LightGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.Gray.copy(alpha = 0.1f)))

                    // 2. Tự động Vào/Ra Ca
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🤖 Tự động vào/ra ca (Hẹn giờ)",
                            color = White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                                    val session = sessionState
                                    if (session != null) {
                                        com.example.notification.NotificationHelper.cancelCheckOutReminder(context, session.uid)
                                        androidx.work.WorkManager.getInstance(context).cancelUniqueWork("checkin_reminder_${session.uid}")
                                        
                                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                            val targetMs = com.example.notification.NotificationHelper.estimateHistoricalCheckInTime(context, session.uid)
                                            com.example.notification.NotificationHelper.scheduleAutoCheckIn(context, session.uid, targetMs)

                                            // Nếu đang làm việc, đặt lịch ra ca tự động luôn
                                            val currentActive = viewModel.repository.getActiveEntry(session.uid)
                                            if (currentActive != null && currentActive.isWorking) {
                                                val outMs = com.example.notification.NotificationHelper.estimateHistoricalCheckoutTime(context, session.uid, currentActive)
                                                com.example.notification.NotificationHelper.scheduleAutoCheckOut(context, session.uid, outMs)
                                            }
                                        }
                                    }
                                } else {
                                    val session = sessionState
                                    if (session != null) {
                                        com.example.notification.NotificationHelper.cancelAutoCheckIn(context, session.uid)
                                        com.example.notification.NotificationHelper.cancelAutoCheckOut(context, session.uid)
                                    }
                                }
                                editor.apply()
                                Toast.makeText(context, if (isEnabled) "Đã bật tự động (Đã tắt nhắc nhở)" else "Đã tắt tự động", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = White,
                                checkedTrackColor = NeonBlue,
                                uncheckedThumbColor = MediumGray,
                                uncheckedTrackColor = Color(0xFF1E1E1E)
                            )
                        )
                    }

                    if (autoClockInOutEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Input Giờ Vào
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
                                    val session = sessionState
                                    if (session != null) {
                                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                            val targetMs = com.example.notification.NotificationHelper.estimateHistoricalCheckInTime(context, session.uid)
                                            com.example.notification.NotificationHelper.scheduleAutoCheckIn(context, session.uid, targetMs)
                                        }
                                    }
                                },
                                label = { Text("Giờ vào ca", fontSize = 12.sp, color = LightGray) },
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
                                },
                                label = { Text("Giờ ra ca", fontSize = 12.sp, color = LightGray) },
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

                                    sessionState?.let { session ->
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val inMs = com.example.notification.NotificationHelper.estimateHistoricalCheckInTime(context, session.uid)
                                            estimatedInTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(inMs))
                                            val mockEntry = com.example.data.model.TimeEntry(userId = session.uid, date = "", checkInTime = inMs)
                                            val outMs = com.example.notification.NotificationHelper.estimateHistoricalCheckoutTime(context, session.uid, mockEntry)
                                            estimatedOutTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(outMs))
                                            
                                            if (autoClockInOutEnabled) {
                                                com.example.notification.NotificationHelper.scheduleAutoCheckIn(context, session.uid, inMs)
                                            }
                                        }
                                    }
                                },
                                label = { Text("Chu kỳ đổi ca (tuần)", fontSize = 12.sp, color = LightGray) },
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ngày mốc bắt đầu:", fontSize = 12.sp, color = LightGray, fontWeight = FontWeight.Medium)
                                val anchorDateStr = java.text.SimpleDateFormat("EEEE, dd/MM/yyyy", java.util.Locale("vi", "VN")).format(java.util.Date(shiftAnchorTime))
                                OutlinedButton(
                                    onClick = {
                                        val curCal = java.util.Calendar.getInstance().apply { timeInMillis = shiftAnchorTime }
                                        android.app.DatePickerDialog(
                                            context,
                                            { _, yr, mo, dy ->
                                                val c = java.util.Calendar.getInstance()
                                                c.set(yr, mo, dy)
                                                val newTime = c.timeInMillis
                                                shiftAnchorTime = newTime
                                                notificationPrefs.edit().putLong("shift_anchor_time", newTime).apply()
                                                
                                                sessionState?.let { session ->
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        val inMs = com.example.notification.NotificationHelper.estimateHistoricalCheckInTime(context, session.uid)
                                                        estimatedInTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(inMs))
                                                        val mockEntry = com.example.data.model.TimeEntry(userId = session.uid, date = "", checkInTime = inMs)
                                                        val outMs = com.example.notification.NotificationHelper.estimateHistoricalCheckoutTime(context, session.uid, mockEntry)
                                                        estimatedOutTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(outMs))
                                                        
                                                        if (autoClockInOutEnabled) {
                                                            com.example.notification.NotificationHelper.scheduleAutoCheckIn(context, session.uid, inMs)
                                                        }
                                                    }
                                                }
                                            },
                                            curCal.get(java.util.Calendar.YEAR),
                                            curCal.get(java.util.Calendar.MONTH),
                                            curCal.get(java.util.Calendar.DAY_OF_MONTH)
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
                                            
                                            sessionState?.let { session ->
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    val inMs = com.example.notification.NotificationHelper.estimateHistoricalCheckInTime(context, session.uid)
                                                    estimatedInTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(inMs))
                                                    val mockEntry = com.example.data.model.TimeEntry(userId = session.uid, date = "", checkInTime = inMs)
                                                    val outMs = com.example.notification.NotificationHelper.estimateHistoricalCheckoutTime(context, session.uid, mockEntry)
                                                    estimatedOutTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(outMs))
                                                    
                                                    if (autoClockInOutEnabled) {
                                                        com.example.notification.NotificationHelper.scheduleAutoCheckIn(context, session.uid, inMs)
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!isNight) NeonBlue else MediumGray,
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
                                            
                                            sessionState?.let { session ->
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    val inMs = com.example.notification.NotificationHelper.estimateHistoricalCheckInTime(context, session.uid)
                                                    estimatedInTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(inMs))
                                                    val mockEntry = com.example.data.model.TimeEntry(userId = session.uid, date = "", checkInTime = inMs)
                                                    val outMs = com.example.notification.NotificationHelper.estimateHistoricalCheckoutTime(context, session.uid, mockEntry)
                                                    estimatedOutTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(outMs))
                                                    
                                                    if (autoClockInOutEnabled) {
                                                        com.example.notification.NotificationHelper.scheduleAutoCheckIn(context, session.uid, inMs)
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isNight) NeonBlue else MediumGray,
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
            }

            if (showMinutesPickerDialog) {
                MinutesReminderPickerDialog(
                    title = "Cài đặt số phút nhắc nhở",
                    initialMinutes = reminderMinutes,
                    onDismiss = { showMinutesPickerDialog = false },
                    onSave = { newMinutes ->
                        reminderMinutes = newMinutes
                        notificationPrefs.edit().putString("reminder_minutes_before", newMinutes).apply()
                        
                        val session = sessionState
                        if (session != null) {
                            com.example.notification.NotificationHelper.scheduleNextCheckInReminder(context, session.uid)
                        }
                        
                        Toast.makeText(context, "Đã cập nhật thời gian nhắc nhở thành $newMinutes phút trước khi vào ca", Toast.LENGTH_SHORT).show()
                        showMinutesPickerDialog = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            CategoryLayout(title = "THÔNG TIN PHIÊN BẢN", icon = Icons.Default.Info) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Phiên bản hiện tại", color = LightGray, fontSize = 12.sp)
                            Text("v$currentAppVersionName (Build $currentAppVersionCode)", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        val coroutineScope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                if (!checkingUpdate) {
                                    checkingUpdate = true
                                    coroutineScope.launch {
                                        try {
                                            val info = com.example.data.FirestoreService.checkAppVersion(context)
                                            checkingUpdate = false
                                            if (info != null && info.isNewVersionAvailable) {
                                                manualUpdateInfo = info
                                            } else {
                                                Toast.makeText(context, "Ứng dụng của bạn đã là phiên bản mới nhất!", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Throwable) {
                                            checkingUpdate = false
                                            Toast.makeText(context, "Không thể kiểm tra lúc này: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (checkingUpdate) MediumGray else NeonBlue,
                                contentColor = White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp).testTag("check_update_button")
                        ) {
                            if (checkingUpdate) {
                                CircularProgressIndicator(color = White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Kiểm tra cập nhật", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (manualUpdateInfo != null) {
                val info = manualUpdateInfo!!
                AlertDialog(
                    onDismissRequest = { manualUpdateInfo = null },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "New Version Icon",
                                tint = AccentOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Bản Cập Nhật Mới!",
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }
                    },
                    text = {
                        Text(
                            text = "Đã có phiên bản mới v${info.latestVersionName}, vui lòng cập nhật để sử dụng tính năng mới nhất!",
                            color = White.copy(alpha = 0.85f),
                            fontSize = 15.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(info.downloadUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // fallback
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonBlue,
                                contentColor = White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cập nhật ngay", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { manualUpdateInfo = null }
                        ) {
                            Text("Để sau", color = White.copy(alpha = 0.6f))
                        }
                    },
                    containerColor = Color(0xFF1E1E1E),
                    tonalElevation = 6.dp
                )
            }

            val userEmail = sessionState?.email ?: ""
            val isDeveloper = userEmail.trim().lowercase() == "khoatubexxx@gmail.com" || userEmail.contains("admin")

            if (isDeveloper) {
                Spacer(modifier = Modifier.height(16.dp))
                CategoryLayout(title = "CÔNG CỤ PHÁT HÀNH CẬP NHẬT (CHỈ QUẢN TRỊ VIÊN)", icon = Icons.Default.VerifiedUser) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Hệ thống tự động đồng nhất phiên bản APK. Khi bạn cập nhật ở đây, tất cả người dùng khác khi mở app sẽ ngay lập tức được yêu cầu cập nhật lên phiên bản này.",
                            color = LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Bản hiện tại trên máy", color = LightGray, fontSize = 10.sp)
                                    Text("v$currentAppVersionCode", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Bản phát hành trên Cloud", color = LightGray, fontSize = 10.sp)
                                    Text(if (publishedVersionName.isNotEmpty()) publishedVersionName else if (publishedVersionCode > 0) "v$publishedVersionCode" else "Đang tải...", color = NeonBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (loadErrorDetail.isNotEmpty()) {
                            var showTroubleshootGuide by remember { mutableStateOf(false) }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1E1E)),
                                border = BorderStroke(1.dp, Color(0xFFE57373))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "⚠️ Chi tiết lỗi Firebase: ${loadErrorDetail.take(35)}...",
                                            color = Color(0xFFFFD2D2),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(
                                            onClick = { showTroubleshootGuide = !showTroubleshootGuide },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(if (showTroubleshootGuide) "Thu gọn 🔼" else "Cách sửa 🛠️", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    if (showTroubleshootGuide) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Để sửa triệt để lỗi này, bạn cần vào Firebase Console chỉnh lại Rules (Luật bảo mật) của Firestore Database như sau:\n\n" +
                                                    "1️⃣ Truy cập console.firebase.google.com -> Vào dự án của bạn.\n" +
                                                    "2️⃣ Chọn Firestore Database ở menu bên trái -> Vào tab Rules.\n" +
                                                    "3️⃣ Thay thế toàn bộ Rules hiện có bằng đoạn mã sau:\n\n" +
                                                    "rules_version = '2';\n" +
                                                    "service cloud.firestore {\n" +
                                                    "  match /databases/{database}/documents {\n" +
                                                    "    match /app_config/version_control {\n" +
                                                    "      allow read, write: if true;\n" +
                                                    "    }\n" +
                                                    "    match /users/{userId}/{document=**} {\n" +
                                                    "      allow read, write: if request.auth != null && request.auth.uid == userId;\n" +
                                                    "    }\n" +
                                                    "  }\n" +
                                                    "}\n\n" +
                                                    "4️⃣ Nhấn nút 'Publish' (Xuất bản) để lưu. Sau đó, tắt ứng dụng đi bật lại sẽ kết nối thành công!",
                                            color = Color(0xFFECEFF1),
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        ConfigInputField(
                            label = "Mã Phiên Bản Phát Hành Mới (Ví dụ: 1.3 hoặc 5)",
                            value = latestVersionText,
                            onValueChange = { latestVersionText = it.filter { c -> c.isDigit() || c == '.' } },
                            keyboardType = KeyboardType.Text,
                            labelColor = AccentOrange
                        )

                        ConfigInputField(
                            label = "Đường dẫn tải APK trực tiếp (Google Drive, v.v)",
                            value = downloadUrlText,
                            onValueChange = { downloadUrlText = it },
                            keyboardType = KeyboardType.Uri,
                            labelColor = AccentOrange
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val coroutineScope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                val inputStr = latestVersionText.trim()
                                if (inputStr.isBlank()) {
                                    Toast.makeText(context, "Mã phiên bản không được để trống!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val code = if (inputStr.contains(".")) {
                                    com.example.data.FirestoreService.parseVersionToCode(inputStr)
                                } else {
                                    inputStr.toLongOrNull() ?: 0L
                                }
                                if (code <= 0L) {
                                    Toast.makeText(context, "Mã phiên bản không hợp lệ!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (downloadUrlText.isBlank()) {
                                    Toast.makeText(context, "Đường dẫn APK không được để trống!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isPublishing = true
                                coroutineScope.launch {
                                    val success = com.example.data.FirestoreService.publishNewAppVersion(code, downloadUrlText.trim(), inputStr)
                                    isPublishing = false
                                    if (success) {
                                        publishedVersionCode = code
                                        publishedVersionName = inputStr
                                        publishedDownloadUrl = downloadUrlText.trim()
                                        Toast.makeText(context, "Đã phát hành bản cập nhật mớii: $inputStr thành công!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Lỗi xảy ra khi phát hành cập nhật!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                            enabled = !isPublishing
                        ) {
                            if (isPublishing) {
                                CircularProgressIndicator(color = White, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Phát Hành Bản Mới Ngay 🚀", color = White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Giải pháp tốt nhất cho người lao động",
                color = LightGray.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CategoryLayout(
    title: String,
    icon: ImageVector,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp)
            ) {
                Icon(icon, contentDescription = title, tint = NeonBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Thu gọn" else "Mở rộng",
                    tint = LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun ConfigInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Number,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    labelColor: Color = LightGray,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(value)) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    LaunchedEffect(isFocused) {
        if (isFocused && enabled) {
            kotlinx.coroutines.delay(50) // Small delay to override the cursor placement from tap
            textFieldValueState = textFieldValueState.copy(
                selection = TextRange(0, textFieldValueState.text.length)
            )
        }
    }

    LaunchedEffect(value) {
        if (textFieldValueState.text != value) {
            textFieldValueState = textFieldValueState.copy(text = value)
        }
    }

    OutlinedTextField(
        value = textFieldValueState,
        onValueChange = { newValue ->
            textFieldValueState = newValue
            onValueChange(newValue.text)
        },
        label = { Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = labelColor) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = androidx.compose.ui.text.input.ImeAction.Done
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onDone = { focusManager.clearFocus() }
        ),
        visualTransformation = visualTransformation,
        enabled = enabled,
        interactionSource = interactionSource,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = labelColor,
            focusedTextColor = White,
            unfocusedTextColor = White,
            unfocusedBorderColor = Color(0xFF2C2C2C),
            focusedLabelColor = labelColor,
            disabledTextColor = White.copy(alpha = 0.6f),
            disabledBorderColor = Color(0xFF2C2C2C).copy(alpha = 0.5f),
            disabledLabelColor = labelColor.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
    )
}

fun formatTimeToStandard(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    if (digits.isEmpty()) return "00:00"
    
    var h = 0
    var m = 0
    
    if (digits.length == 3) {
        h = digits.substring(0, 1).toIntOrNull() ?: 0
        m = digits.substring(1).toIntOrNull() ?: 0
    } else if (digits.length >= 4) {
        h = digits.substring(0, 2).toIntOrNull() ?: 0
        m = digits.substring(2, 4).toIntOrNull() ?: 0
    } else {
        h = digits.toIntOrNull() ?: 0
    }
    
    if (h > 24 || (h == 24 && m > 0)) {
        // Just cap it at 24:00 or let it be for validation to handle?
        // Let's cap at 24:00 for standard formatting safety
        h = 24
        m = 0
    }
    
    if (m > 59) m = 59
    
    val hStr = h.toString().padStart(2, '0')
    val mStr = m.toString().padStart(2, '0')
    return "$hStr:$mStr"
}

@Composable
fun AllowanceRowItem(
    name: String,
    value: String,
    fieldName: String,
    calcTypeMap: Map<String, String>,
    onClick: () -> Unit
) {
    val currentType = calcTypeMap[fieldName] ?: com.example.data.model.UserConfig.getDefaultCalcType(fieldName)
    val displayType = when (currentType) {
        "MONTHLY_PRO_RATED" -> "Theo tháng (/26)"
        "MONTHLY_FLAT" -> "Tháng cố định"
        "PER_WORK_DAY" -> "Theo ngày công"
        "OT_MEAL_GE_2H", "OT_MEAL_GE_1H" -> "Cơm OT ≥ 1h"
        "PER_NIGHT_SHIFT" -> "Số ca đêm"
        else -> "Theo tháng (/26)"
    }
    
    val formattedValue = try {
        val parsed = value.replace(".", "").toDoubleOrNull() ?: 0.0
        DecimalFormat("#,###").format(parsed) + " đ"
    } catch (e: Exception) {
        "$value đ"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Tính chất: $displayType",
                color = LightGray,
                fontSize = 10.sp
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formattedValue,
                color = AccentGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Chỉnh sửa",
                tint = LightGray,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowanceEditDialog(
    name: String,
    initialValue: String,
    initialType: String,
    onDismiss: () -> Unit,
    onSave: (newValue: String, newType: String) -> Unit
) {
    var textValue by remember { mutableStateOf(initialValue) }
    var selectedType by remember { mutableStateOf(initialType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSave(textValue, selectedType) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
            ) {
                Text("Xác nhận", color = White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = LightGray)
            }
        },
        title = {
            Text(
                text = "Cấu hình $name",
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Input field for amount
                ConfigInputField(
                    label = "Số tiền (VNĐ)",
                    value = textValue,
                    onValueChange = { textValue = it.filter { c -> c.isDigit() } },
                    visualTransformation = ThousandSeparatorVisualTransformation(),
                    labelColor = NeonBlue
                )

                Text(
                    text = "Tính chất tính lương (Loại tính):",
                    color = White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    listOf(
                        "MONTHLY_PRO_RATED" to ("Theo tháng (/26)" to "Chia 26 ngày công chuẩn và nhân ngày làm thực tế"),
                        "MONTHLY_FLAT" to ("Tháng cố định" to "Hưởng đủ 100% cố định không tính theo ngày công"),
                        "PER_WORK_DAY" to ("Theo ngày công" to "Cộng thêm theo số ngày đi làm thực tế"),
                        "OT_MEAL_GE_1H" to ("Cơm OT ≥ 1h" to "Nhân với số ngày tăng ca từ 1 giờ trở lên"),
                        "PER_NIGHT_SHIFT" to ("Theo số ca đêm" to "Nhân trực tiếp với số ca làm việc ban đêm")
                    ).forEach { (typeKey, info) ->
                        val (typeLabel, description) = info
                        val isSelected = selectedType == typeKey || (typeKey == "OT_MEAL_GE_1H" && selectedType == "OT_MEAL_GE_2H")
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF1E1E1E))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NeonBlue else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedType = typeKey }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = typeLabel,
                                        color = if (isSelected) NeonBlue else White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = NeonBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = description,
                                    color = LightGray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF121212),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun MinutesReminderPickerDialog(
    title: String,
    initialMinutes: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(initialMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = textValue.trim()
                    val minutesVal = trimmed.toIntOrNull()
                    if (minutesVal != null && minutesVal in 1..240) {
                        onSave(trimmed)
                    }
                },
                enabled = textValue.trim().toIntOrNull() != null && textValue.trim().toInt() in 1..240,
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
            ) {
                Text("Xác nhận", color = White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = LightGray)
            }
        },
        title = {
            Text(
                text = title,
                color = White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Nhập số phút nhắc nhở trước giờ vào ca:",
                    color = LightGray,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                            textValue = newValue
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = White, fontSize = 16.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = MediumGray,
                        cursorColor = NeonBlue
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                val isError = textValue.trim().toIntOrNull() == null || textValue.trim().toInt() <= 0 || textValue.trim().toInt() > 240
                if (isError && textValue.isNotEmpty()) {
                    Text(
                        text = "Vui lòng nhập từ 1 đến 240 phút",
                        color = AccentOrange,
                        fontSize = 11.sp
                    )
                }
            }
        },
        containerColor = Color(0xFF121212),
        shape = RoundedCornerShape(16.dp)
    )
}


