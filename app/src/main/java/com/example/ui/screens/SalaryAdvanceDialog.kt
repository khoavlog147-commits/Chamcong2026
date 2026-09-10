package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SalaryAdvance
import com.example.viewmodel.SalarySummary
import com.example.viewmodel.TimeSnapViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

private val DarkBg = Color(0xFF121214)
private val SurfaceBg = Color(0xFF1E1E24)
private val CardBg = Color(0xFF26262E)
private val PrimaryBlue = Color(0xFF007AFF)
private val SuccessGreen = Color(0xFF10B981)
private val WarningOrange = Color(0xFFF59E0B)
private val DangerRed = Color(0xFFEF4444)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF9E9E9E)
private val BorderDark = Color(0xFF33333E)

@Composable
fun SalaryAdvanceDialog(
    viewModel: TimeSnapViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentMonth by viewModel.currentSelectedMonth.collectAsState()
    val advances by viewModel.monthSalaryAdvances.collectAsState()
    val salarySummary by viewModel.salarySummaryState.collectAsState()

    val currencyFmt = remember { DecimalFormat("#,###") }
    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    val totalAlreadyAdvanced = remember(advances) {
        advances.sumOf { it.amount }
    }

    // Actual earned salary till now in the selected month
    // If salarySummary already deducted tamUng, earned before advance is:
    val earnedSalaryTillDate = remember(salarySummary, totalAlreadyAdvanced) {
        val baseNet = salarySummary?.luongThucNhan ?: 0.0
        baseNet + totalAlreadyAdvanced
    }

    // Maximum allowable advance is earnedSalaryTillDate minus what was already taken
    val maxAllowableAdvance = remember(earnedSalaryTillDate, totalAlreadyAdvanced) {
        (earnedSalaryTillDate - totalAlreadyAdvanced).coerceAtLeast(0.0)
    }

    var advanceAmountText by remember { mutableStateOf("") }
    var advanceDateText by remember { mutableStateOf(todayDateStr) }
    var noteText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val enteredAmount = remember(advanceAmountText) {
        val clean = advanceAmountText.replace(".", "").replace(",", "").trim()
        clean.toDoubleOrNull() ?: 0.0
    }

    val isExceeding = enteredAmount > maxAllowableAdvance && maxAllowableAdvance >= 0.0

    // Month label formatting (e.g. 09/2026)
    val formattedMonth = remember(currentMonth) {
        try {
            val parts = currentMonth.split("-")
            if (parts.size >= 2) "${parts[1]}/${parts[0]}" else currentMonth
        } catch (e: Exception) {
            currentMonth
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .background(DarkBg)
                .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
                .testTag("salary_advance_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tạm Ứng Lương",
                                color = TextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Kỳ lương tháng: $formattedMonth",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Limits / Earnings Summary Card
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "HẠN MỨC TẠM ỨNG THÁNG $formattedMonth",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Lương ngày thực tế làm việc:",
                                        color = TextWhite.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${currencyFmt.format(earnedSalaryTillDate)}đ",
                                        color = TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Đã tạm ứng trong tháng:",
                                        color = TextWhite.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "-${currencyFmt.format(totalAlreadyAdvanced)}đ",
                                        color = DangerRed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                HorizontalDivider(
                                    color = BorderDark,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Tối đa có thể ứng:",
                                            color = SuccessGreen,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "(Không vượt quá lương thực tế)",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(
                                        text = "${currencyFmt.format(maxAllowableAdvance)}đ",
                                        color = SuccessGreen,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    // Advance Request Form
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "YÊU CẦU TẠM ỨNG",
                                    color = PrimaryBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Amount Input Field
                                OutlinedTextField(
                                    value = advanceAmountText,
                                    onValueChange = { input ->
                                        errorMessage = null
                                        successMessage = null
                                        val digitsOnly = input.filter { it.isDigit() }
                                        if (digitsOnly.isEmpty()) {
                                            advanceAmountText = ""
                                        } else {
                                            val num = digitsOnly.toDoubleOrNull() ?: 0.0
                                            advanceAmountText = currencyFmt.format(num)
                                        }
                                    },
                                    label = { Text("Số tiền muốn tạm ứng (VNĐ)", color = TextMuted, fontSize = 12.sp) },
                                    placeholder = { Text("Ví dụ: 3,000,000", color = TextMuted.copy(alpha = 0.5f)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = null,
                                            tint = if (isExceeding) DangerRed else PrimaryBlue
                                        )
                                    },
                                    trailingIcon = {
                                        Text(
                                            text = "VNĐ",
                                            color = TextMuted,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(end = 12.dp)
                                        )
                                    },
                                    isError = isExceeding,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedBorderColor = if (isExceeding) DangerRed else PrimaryBlue,
                                        unfocusedBorderColor = if (isExceeding) DangerRed else BorderDark,
                                        focusedContainerColor = CardBg,
                                        unfocusedContainerColor = CardBg
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("advance_amount_input")
                                )

                                // Preset Percentage Buttons
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "25%" to 0.25,
                                        "50%" to 0.50,
                                        "75%" to 0.75,
                                        "Tối đa" to 1.00
                                    ).forEach { (label, ratio) ->
                                        val presetVal = (maxAllowableAdvance * ratio).toLong()
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (ratio == 1.0) PrimaryBlue.copy(alpha = 0.2f) else CardBg)
                                                .border(
                                                    1.dp,
                                                    if (ratio == 1.0) PrimaryBlue else BorderDark,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable(enabled = maxAllowableAdvance > 0) {
                                                    if (maxAllowableAdvance > 0) {
                                                        advanceAmountText = currencyFmt.format(presetVal)
                                                        errorMessage = null
                                                    }
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (ratio == 1.0) PrimaryBlue else TextWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // Warning if exceeding
                                if (isExceeding) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DangerRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = DangerRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Không được vượt quá lương thực tế (${currencyFmt.format(maxAllowableAdvance)}đ)",
                                            color = DangerRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Advance Date Field
                                OutlinedTextField(
                                    value = advanceDateText,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Ngày tạm ứng", color = TextMuted, fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = PrimaryBlue
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val cal = Calendar.getInstance()
                                            DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    advanceDateText = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.EditCalendar,
                                                contentDescription = "Chọn ngày",
                                                tint = PrimaryBlue
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedBorderColor = BorderDark,
                                        unfocusedBorderColor = BorderDark,
                                        focusedContainerColor = CardBg,
                                        unfocusedContainerColor = CardBg
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Note / Reason
                                OutlinedTextField(
                                    value = noteText,
                                    onValueChange = { noteText = it },
                                    label = { Text("Lý do tạm ứng / Ghi chú (Tuỳ chọn)", color = TextMuted, fontSize = 12.sp) },
                                    placeholder = { Text("Ví dụ: Chi tiêu gia đình, việc cá nhân...", color = TextMuted.copy(alpha = 0.5f)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Notes,
                                            contentDescription = null,
                                            tint = PrimaryBlue
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedBorderColor = PrimaryBlue,
                                        unfocusedBorderColor = BorderDark,
                                        focusedContainerColor = CardBg,
                                        unfocusedContainerColor = CardBg
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Error or Success Banner
                                if (errorMessage != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = errorMessage!!,
                                        color = DangerRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                if (successMessage != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = successMessage!!,
                                        color = SuccessGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Submit Button
                                val canSubmit = enteredAmount > 0 && !isExceeding && maxAllowableAdvance > 0
                                Button(
                                    onClick = {
                                        viewModel.addSalaryAdvance(
                                            amount = enteredAmount,
                                            date = advanceDateText,
                                            note = noteText,
                                            onSuccess = {
                                                advanceAmountText = ""
                                                noteText = ""
                                                errorMessage = null
                                                successMessage = "Tạm ứng ${currencyFmt.format(enteredAmount)}đ thành công!"
                                            },
                                            onError = { err ->
                                                errorMessage = err
                                                successMessage = null
                                            }
                                        )
                                    },
                                    enabled = canSubmit,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryBlue,
                                        disabledContainerColor = PrimaryBlue.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("submit_salary_advance_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (canSubmit) TextWhite else TextWhite.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "XÁC NHẬN TẠM ỨNG",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (canSubmit) TextWhite else TextWhite.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }

                    // Monthly Advances History List
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "LỊCH SỬ TẠM ỨNG (${advances.size})",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (advances.isNotEmpty()) {
                                        Text(
                                            text = "Tổng: ${currencyFmt.format(totalAlreadyAdvanced)}đ",
                                            color = DangerRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (advances.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Chưa có khoản tạm ứng nào trong tháng $formattedMonth",
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        advances.forEach { advance ->
                                            AdvanceItemRow(
                                                advance = advance,
                                                onDelete = {
                                                    viewModel.deleteSalaryAdvance(
                                                        advance = advance,
                                                        onSuccess = {
                                                            successMessage = "Đã xoá khoản tạm ứng"
                                                        },
                                                        onError = { err ->
                                                            errorMessage = err
                                                        }
                                                    )
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
        }
    }
}

@Composable
private fun AdvanceItemRow(
    advance: SalaryAdvance,
    onDelete: () -> Unit
) {
    val currencyFmt = remember { DecimalFormat("#,###") }
    var showConfirmDelete by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardBg)
            .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "-${currencyFmt.format(advance.amount)}đ",
                    color = DangerRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(PrimaryBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = advance.date,
                        color = PrimaryBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!advance.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = advance.note,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        if (showConfirmDelete) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        onDelete()
                        showConfirmDelete = false
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Xác nhận xoá",
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { showConfirmDelete = false },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Huỷ",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            IconButton(
                onClick = { showConfirmDelete = true },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Xoá",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
