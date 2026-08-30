package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GeminiAiService
import com.example.ui.theme.*
import com.example.util.AiApiKeyManager
import com.example.viewmodel.TimeSnapViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.alpha
import java.text.DecimalFormat
import kotlin.math.roundToInt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import java.util.Locale

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalAiAssistantWidget(
    currentTab: String,
    viewModel: TimeSnapViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var isChatOpen by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showPricingCard by remember { mutableStateOf(false) }

    var apiKey by remember { mutableStateOf(AiApiKeyManager.getApiKey(context)) }
    val hasApiKey = apiKey.isNotBlank()

    // Auto display Key dialog when user opens AI assistant without API key
    LaunchedEffect(isChatOpen) {
        if (isChatOpen && !hasApiKey) {
            showApiKeyDialog = true
            showPricingCard = false
        }
    }

    // Drag offset for floating bubble
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Chat states
    val chatMessages = remember { mutableStateListOf<AiChatMessage>() }
    var userPromptText by remember { mutableStateOf("") }
    var isGeneratingResponse by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto-dimming logic when AI floating bubble is idle / unused
    var lastActivityTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isIdle by remember { mutableStateOf(false) }

    LaunchedEffect(isChatOpen, isGeneratingResponse, lastActivityTime, currentTab) {
        if (isChatOpen || isGeneratingResponse) {
            isIdle = false
        } else {
            isIdle = false
            delay(3000L) // Wait 3 seconds of inactivity before dimming
            isIdle = true
        }
    }

    val bubbleAlpha by animateFloatAsState(
        targetValue = if (isIdle && !isChatOpen) 0.35f else 1.0f,
        animationSpec = tween(durationMillis = 700),
        label = "bubbleAlpha"
    )

    // Observe App State for context
    val userConfig by viewModel.userConfig.collectAsStateWithLifecycle()
    val summaryState by viewModel.salarySummaryState.collectAsStateWithLifecycle()
    val timeEntries by viewModel.monthTimeEntries.collectAsStateWithLifecycle(emptyList())
    val salaryHistory by viewModel.salaryHistoryState.collectAsStateWithLifecycle()

    // Scroll to bottom when new message arrives or when soft keyboard opens
    val isImeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    LaunchedEffect(chatMessages.size, isGeneratingResponse, isImeVisible) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Prepare Context Information for Gemini AI
    val tabNameVi = when (currentTab) {
        "home" -> "Trang chủ Chấm công"
        "history" -> "Lịch sử Chấm công"
        "payslip" -> "Phiếu lương & Thu nhập"
        "settings" -> "Cài đặt ứng dụng"
        "admin" -> "Quản trị hệ thống"
        "notifications" -> "Trung tâm Thông báo"
        else -> "Màn hình ứng dụng"
    }

    val contextDataStr = remember(currentTab, userConfig, summaryState, timeEntries, salaryHistory) {
        val fmt = DecimalFormat("#,###")
        val configInfo = userConfig?.let { c ->
            val totalPhuCap = c.pcKyThuat + c.pcTrachNhiem + c.pcChucVu + c.pcHieuSuat +
                    c.pcSanPham + c.pcComCa + c.pcComOt + c.pcNhaO + c.pcDocHai +
                    c.pcDtDoanhThu + c.pcXangXe + c.pcThamNien + c.pcCaDem + c.pcKhac1
            """
            * CÀI ĐẶT LƯƠNG & QUY ĐỊNH CÔNG TY CỦA NHÂN VIÊN:
            - Họ tên: ${c.hoVaTen} | Mã NV: ${c.maNhanVien} | Chức vụ: ${c.roleName.ifBlank { "Nhân viên" }} | Bộ phận: ${c.boPhan.ifBlank { "Chưa phân bổ" }}
            - Công ty: ${c.companyName} | Ca/Lịch trình: ${c.lichTrinh} | Ngày vào làm: ${c.ngayVaoLam.ifBlank { "Chưa cập nhật" }}
            - Lương cơ bản (LCB): ${fmt.format(c.luongCoBan)}đ | Lương đóng BHXH: ${fmt.format(c.luongDongBaoHiem)}đ
            - Mức trừ BHXH: ${c.tiLeDongBaoHiem}% | Đoàn phí công đoàn: ${fmt.format(c.doanPhiCongDoan)}đ | Ngày chốt lương hàng tháng: Ngày ${c.ngayChotLuong}
            - Hệ số tăng ca OT: Ngày thường x${c.heSoOtNgayThuong}, Chủ nhật x${c.heSoOtChuNhat}, Ngày lễ x${c.heSoOtNgayLe}, Ca đêm x${c.heSoOtDem} (Giờ ca đêm: ${c.caDemStart} - ${c.caDemEnd})
            - Giờ giải lao mỗi ca: ${c.soGioNghiGiaiLao}g (${if (c.tinhKhauTruNghi) "Có trừ vào tổng giờ công" else "Không trừ vào tổng giờ công"})
            - Tiền chuyên cần gốc: ${fmt.format(c.tienChuyenCanGoc)}đ | Quỹ phép năm: ${c.soNgayPhepNam} ngày (Còn lại: ${c.phepNamConLai} ngày)
            - Chi tiết 12 phụ cấp & hỗ trợ:
              + Cơm ca: ${fmt.format(c.pcComCa)}đ/ngày công | Cơm OT: ${fmt.format(c.pcComOt)}đ/suất OT
              + Xăng xe: ${fmt.format(c.pcXangXe)}đ | Nhà ở: ${fmt.format(c.pcNhaO)}đ | Điện thoại: ${fmt.format(c.pcDtDoanhThu)}đ
              + Trách nhiệm: ${fmt.format(c.pcTrachNhiem)}đ | Kỹ thuật: ${fmt.format(c.pcKyThuat)}đ | Chức vụ: ${fmt.format(c.pcChucVu)}đ
              + Hiệu suất: ${fmt.format(c.pcHieuSuat)}đ | Sản phẩm: ${fmt.format(c.pcSanPham)}đ | Độc hại: ${fmt.format(c.pcDocHai)}đ
              + Thâm niên: ${fmt.format(c.pcThamNien)}đ | Ca đêm: ${fmt.format(c.pcCaDem)}đ | Phụ cấp khác: ${fmt.format(c.pcKhac1)}đ
              => Tổng phụ cấp: ${fmt.format(totalPhuCap)}đ
            """.trimIndent()
        } ?: "Thông tin & cài đặt nhân viên chưa được thiết lập"

        val summaryInfo = summaryState?.let { s ->
            val hourlyRate = if (s.standardWorkDays > 0 && s.baseBasicSalary > 0) s.baseBasicSalary / (s.standardWorkDays * 8.0) else 0.0
            """
            * THỐNG KÊ CHI TIẾT LƯƠNG & CÔNG THÁNG HIỆN TẠI:
            - Đơn giá 1 giờ công chuẩn (LCB / Công chuẩn / 8g): ${fmt.format(hourlyRate)}đ/giờ
            - Ngày công thực tế: ${s.workingDays}/${s.standardWorkDays} ngày công | Giờ làm chuẩn: ${s.standardHours}g | Số ca đêm: ${s.caDemCount} ca
            - Chi tiết các loại giờ & Tiền công tương ứng:
              + OT ngày thường: ${s.otDayHours}g => Tiền: ${fmt.format(s.tienOtNgay)}đ
              + OT ca đêm: ${s.otNightHours}g => Tiền: ${fmt.format(s.tienOtDem)}đ
              + Giờ Chủ nhật (Tổng): ${s.chuNhatHours}g (Ngày: ${s.chuNhatDayHours}g, Đêm: ${s.chuNhatNightHours}g) => Tổng tiền CN: ${fmt.format(s.tienChuNhat)}đ (CN Ngày: ${fmt.format(s.tienChuNhatNgay)}đ, CN Đêm: ${fmt.format(s.tienChuNhatDem)}đ)
              + OT Ngày lễ: ${s.otLeHours}g => Tiền: ${fmt.format(s.tienOtLe)}đ
            - Tổng tiền cơm: ${fmt.format(s.tongTienCom)}đ | Tổng phụ cấp: ${fmt.format(s.phuCap)}đ | Thưởng: ${fmt.format(s.thuong)}đ
            - Các khoản khấu trừ: BHXH (${fmt.format(s.tienBh)}đ), Đoàn phí (${fmt.format(s.doanPhi)}đ), Phạt/Nghỉ (${fmt.format(s.tienKhauTruNghi)}đ)
            - LƯƠNG THỰC NHẬN (NET): ${fmt.format(s.luongThucNhan)}đ
            """.trimIndent()
        } ?: "Chưa có tổng hợp công tháng này"

        val historyInfo = if (salaryHistory.isNotEmpty()) {
            val historyListStr = salaryHistory.joinToString("\n") { p ->
                "  + Tháng ${p.monthStr}: Lương NET = ${fmt.format(p.luongThucNhan)}đ, Ngày công = ${p.workingDays} ngày"
            }
            """
            * DỮ LIỆU LỊCH SỬ LƯƠNG & NGÀY CÔNG (6 THÁNG GẦN NHẤT ĐỂ SO SÁNH):
            $historyListStr
            """.trimIndent()
        } else {
            "* LỊCH SỬ LƯƠNG CÁC THÁNG TRƯỚC: Chưa ghi nhận dữ liệu tháng trước."
        }

        """
        - Màn hình người dùng đang mở: $tabNameVi
        $configInfo
        $summaryInfo
        $historyInfo
        """.trimIndent()
    }

    // TTS Voice Engine State
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsSpeaking by remember { mutableStateOf(false) }
    var currentSpeakingMsgId by remember { mutableStateOf<String?>(null) }
    var autoSpeakVoice by remember { mutableStateOf(true) }
    var isListeningVoice by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale("vi", "VN")
            }
        }
        ttsInstance = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    val stopSpeaking: () -> Unit = {
        ttsInstance?.stop()
        isTtsSpeaking = false
        currentSpeakingMsgId = null
    }

    val speakText: (String, String) -> Unit = { id, text ->
        if (currentSpeakingMsgId == id && isTtsSpeaking) {
            stopSpeaking()
        } else {
            ttsInstance?.stop()
            val clean = text.replace(Regex("[*_#`~]"), "")
            val speakResult = ttsInstance?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, id)
            if (speakResult != TextToSpeech.ERROR) {
                isTtsSpeaking = true
                currentSpeakingMsgId = id
            }
        }
    }

    // Function to handle sending message
    val handleSendMessageInternal: (String, Boolean) -> Unit = { inputPrompt, isFromVoice ->
        val prompt = inputPrompt.trim()
        if (prompt.isNotBlank() && !isGeneratingResponse) {
            if (!AiApiKeyManager.hasApiKey(context)) {
                showApiKeyDialog = true
            } else {
                stopSpeaking()
                val currentKey = AiApiKeyManager.getApiKey(context)
                chatMessages.add(AiChatMessage(sender = "user", text = prompt))
                userPromptText = ""
                isGeneratingResponse = true

                coroutineScope.launch {
                    val historyPairs = chatMessages.takeLast(6).filter { !it.isError }.map { 
                        Pair(if (it.sender == "user") "user" else "model", it.text) 
                    }

                    val result = GeminiAiService.generateContent(
                        apiKey = currentKey,
                        userPrompt = prompt,
                        contextData = contextDataStr,
                        chatHistory = historyPairs
                    )

                    isGeneratingResponse = false
                    result.onSuccess { responseText ->
                        val newAiMsg = AiChatMessage(sender = "ai", text = responseText)
                        chatMessages.add(newAiMsg)
                        if (isFromVoice || autoSpeakVoice) {
                            speakText(newAiMsg.id, responseText)
                        }
                    }.onFailure { error ->
                        val errText = error.message ?: "Có lỗi xảy ra khi gọi Gemini API."
                        chatMessages.add(AiChatMessage(sender = "ai", text = "⚠️ $errText", isError = true))
                        if (errText.contains("Key", ignoreCase = true)) {
                            showApiKeyDialog = true
                        }
                    }
                }
            }
        }
    }

    val handleSendMessage: (String) -> Unit = { prompt -> handleSendMessageInternal(prompt, false) }

    // Speech-to-Text Launcher
    val voiceSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListeningVoice = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenTextList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenTextList?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                userPromptText = spokenText
                handleSendMessageInternal(spokenText, true)
            }
        }
    }

    val startVoiceInput: () -> Unit = {
        stopSpeaking()
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "vi-VN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Đang lắng nghe... Hãy nói câu hỏi của bạn")
            }
            isListeningVoice = true
            voiceSpeechLauncher.launch(intent)
        } catch (e: Exception) {
            isListeningVoice = false
            Toast.makeText(context, "Thiết bị chưa cài dịch vụ nhận diện giọng nói Google", Toast.LENGTH_SHORT).show()
        }
    }

    // FLOATING BUBBLE UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 68.dp, end = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Floating Draggable AI Icon
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .alpha(bubbleAlpha)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { lastActivityTime = System.currentTimeMillis() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                            lastActivityTime = System.currentTimeMillis()
                        }
                    )
                }
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF7B2CBF),
                            Color(0xFF3A0CA3),
                            Color(0xFF4CC9F0)
                        )
                    )
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                .clickable {
                    lastActivityTime = System.currentTimeMillis()
                    isChatOpen = true
                }
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Trợ lý AI",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "AI",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Dot indicator if Key is set
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (hasApiKey) AccentGreen else AccentOrange)
                    .align(Alignment.TopEnd)
            )
        }
    }

    // BOTTOM SHEET CHAT DIALOG
    if (isChatOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val chatNestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    // Consume leftover vertical scroll delta so scrolling inside LazyColumn never drags or shifts the ModalBottomSheet
                    return available
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { isChatOpen = false },
            sheetState = sheetState,
            containerColor = Color(0xFF141A24),
            scrimColor = Color.Black.copy(alpha = 0.65f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Gray.copy(alpha = 0.5f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF7B2CBF), Color(0xFF4CC9F0))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Trợ lý AI TimeSnap",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Surface(
                                    color = if (hasApiKey) AccentGreen.copy(alpha = 0.2f) else AccentOrange.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (hasApiKey) "Sẵn sàng" else "Chưa có Key",
                                        color = if (hasApiKey) AccentGreen else AccentOrange,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Đang hỗ trợ tại: $tabNameVi",
                                color = LightGray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { 
                                if (isTtsSpeaking) {
                                    stopSpeaking()
                                } else {
                                    autoSpeakVoice = !autoSpeakVoice
                                    Toast.makeText(context, if (autoSpeakVoice) "Đã BẬT tự động phát giọng nói phản hồi" else "Đã TẮT phát giọng nói phản hồi", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isTtsSpeaking) Icons.Default.VolumeUp else if (autoSpeakVoice) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "Đọc giọng nói phản hồi",
                                tint = if (isTtsSpeaking) NeonBlue else if (autoSpeakVoice) Color(0xFF4CC9F0) else LightGray
                            )
                        }
                        IconButton(
                            onClick = { showPricingCard = !showPricingCard },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Bảng giá API",
                                tint = if (showPricingCard) Color(0xFFFFD166) else LightGray
                            )
                        }
                        IconButton(
                            onClick = { showApiKeyDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Cài đặt API Key",
                                tint = if (hasApiKey) NeonBlue else AccentOrange
                            )
                        }
                        if (chatMessages.isNotEmpty()) {
                            IconButton(
                                onClick = { chatMessages.clear() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Xóa trò chuyện",
                                    tint = LightGray
                                )
                            }
                        }
                        IconButton(
                            onClick = { isChatOpen = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = LightGray
                            )
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Gemini API Pricing Card (Show ONLY if user toggles Info button)
                if (showPricingCard) {
                    GeminiApiPricingCard(
                        onOpenGoogleStudio = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Mở trình duyệt thất bại", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSetupKeyClick = { showApiKeyDialog = true }
                    )
                }

                // Welcome / Instructions Card if empty
                if (chatMessages.isEmpty() && !showPricingCard) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2638)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF2C384E))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD166),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Xin chào! Tôi có thể giúp gì cho bạn?",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tôi đã tự động nắm thông tin $tabNameVi của bạn. Hãy chọn gợi ý bên dưới hoặc tự nhập câu hỏi nhé!",
                                color = LightGray,
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // Quick Suggestion Chips according to current Tab
                val quickChips = when (currentTab) {
                    "payslip" -> listOf(
                        "💡 So sánh lương tháng này với tháng trước",
                        "💡 Giải thích tổng lương tháng này",
                        "💡 Các khoản phụ cấp của tôi gồm những gì?",
                        "💡 Tỷ lệ trừ BHXH & Công đoàn phí"
                    )
                    "history" -> listOf(
                        "💡 So sánh lương & ngày công tháng này vs tháng trước",
                        "💡 Tổng công làm việc tháng này là bao nhiêu?",
                        "💡 Tháng này tôi làm bao nhiêu ca đêm?",
                        "💡 Tổng số giờ OT tăng ca tháng này?"
                    )
                    "home" -> listOf(
                        "💡 Giờ quy định vào ca ngày & ca đêm",
                        "💡 Quy định ngày chốt lương & giờ nghỉ giải lao",
                        "💡 Ngày công chuẩn tháng này là bao nhiêu?",
                        "💡 Các khoản phụ cấp của tôi"
                    )
                    "settings" -> listOf(
                        "💡 Kiểm tra toàn bộ cài đặt lương của tôi",
                        "💡 Hướng dẫn tạo Gemini API Key miễn phí",
                        "💡 Hướng dẫn xuất file phiếu lương PDF/PNG",
                        "💡 Đổi mật khẩu & Bảo mật tài khoản"
                    )
                    else -> listOf(
                        "💡 Chi tiết cài đặt lương của tôi",
                        "💡 Tỷ lệ trừ BHXH & các phụ cấp",
                        "💡 Hướng dẫn sử dụng ứng dụng"
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(quickChips) { chipText ->
                        Surface(
                            onClick = { handleSendMessage(chipText.replace("💡 ", "")) },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF222C3E),
                            border = BorderStroke(1.dp, Color(0xFF35445E))
                        ) {
                            Text(
                                text = chipText,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Chat Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .nestedScroll(chatNestedScrollConnection),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(chatMessages, key = { it.id }) { msg ->
                        val isUser = msg.sender == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isUser) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFF7B2CBF), Color(0xFF4CC9F0))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Column(
                                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 16.dp
                                    ),
                                    color = if (isUser) Color(0xFF2563EB) 
                                            else if (msg.isError) Color(0xFF7F1D1D) 
                                            else Color(0xFF1E293B),
                                    border = if (!isUser && !msg.isError) BorderStroke(1.dp, Color(0xFF334155)) else null
                                ) {
                                    Text(
                                        text = msg.text,
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        lineHeight = 19.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                    )
                                }

                                if (!isUser && !msg.isError) {
                                    val isSpeaking = currentSpeakingMsgId == msg.id && isTtsSpeaking
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 3.dp, start = 4.dp)
                                    ) {
                                        Text(
                                            text = "Sao chép",
                                            color = LightGray.copy(alpha = 0.7f),
                                            fontSize = 10.5.sp,
                                            modifier = Modifier.clickable {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("AI Response", msg.text))
                                                Toast.makeText(context, "Đã sao chép phản hồi!", Toast.LENGTH_SHORT).show()
                                            }
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            modifier = Modifier.clickable {
                                                speakText(msg.id, msg.text)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                                contentDescription = "Đọc bằng giọng nói",
                                                tint = if (isSpeaking) NeonBlue else LightGray.copy(alpha = 0.7f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = if (isSpeaking) "Đang phát..." else "Đọc giọng nói",
                                                color = if (isSpeaking) NeonBlue else LightGray.copy(alpha = 0.7f),
                                                fontSize = 10.5.sp,
                                                fontWeight = if (isSpeaking) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isGeneratingResponse) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = NeonBlue,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "AI đang suy nghĩ và phân tích dữ liệu...",
                                    color = LightGray,
                                    fontSize = 12.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Listening Status Banner
                if (isListeningVoice) {
                    Surface(
                        color = Color(0xFFEF4444).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "🎙️ Đang lắng nghe giọng nói của bạn...",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Bottom Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { startVoiceInput() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isListeningVoice) Color(0xFFEF4444) else Color(0xFF334155)
                            )
                    ) {
                        Icon(
                            imageVector = if (isListeningVoice) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Nói bằng giọng nói",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    OutlinedTextField(
                        value = userPromptText,
                        onValueChange = { userPromptText = it },
                        placeholder = { Text("Hỏi AI bất kỳ điều gì...", color = LightGray.copy(alpha = 0.6f), fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF182232),
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            keyboardController?.hide()
                            handleSendMessage(userPromptText)
                        })
                    )

                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            handleSendMessage(userPromptText)
                        },
                        enabled = userPromptText.isNotBlank() && !isGeneratingResponse,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (userPromptText.isNotBlank() && !isGeneratingResponse) NeonBlue else Color.Gray.copy(alpha = 0.3f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Gửi",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // DIALOG SETUP API KEY
    if (showApiKeyDialog) {
        var inputKey by remember { mutableStateOf(AiApiKeyManager.getApiKey(context)) }
        var isTestingKey by remember { mutableStateOf(false) }
        var testErrorText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = NeonBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Cấu hình Gemini API Key",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    GeminiApiPricingCard(
                        onOpenGoogleStudio = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Mở trình duyệt thất bại", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Text(
                        text = "Vui lòng dán mã Gemini API Key của bạn để sử dụng Trợ lý AI cá nhân. Key của bạn sẽ được lưu mã hóa an toàn trực tiếp trên điện thoại này.",
                        color = LightGray,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )

                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = { 
                            inputKey = it 
                            testErrorText = null
                        },
                        label = { Text("Gemini API Key", color = LightGray) },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    if (testErrorText != null) {
                        Text(
                            text = testErrorText!!,
                            color = AccentRed,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp
                        )
                    }

                    TextButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Mở trình duyệt thất bại", Toast.LENGTH_SHORT).show()
                            }
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "👉 Nhấn vào đây để lấy Gemini API Key miễn phí từ Google",
                            color = NeonBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val keyTrimmed = inputKey.trim()
                        if (keyTrimmed.isBlank()) {
                            AiApiKeyManager.clearApiKey(context)
                            apiKey = ""
                            showApiKeyDialog = false
                            Toast.makeText(context, "Đã xóa API Key.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isTestingKey = true
                        testErrorText = null

                        coroutineScope.launch {
                            val testRes = GeminiAiService.generateContent(
                                apiKey = keyTrimmed,
                                userPrompt = "Xin chào, hãy trả lời đúng từ OK",
                                contextData = "Kiểm tra API Key"
                            )
                            isTestingKey = false
                            testRes.onSuccess {
                                AiApiKeyManager.saveApiKey(context, keyTrimmed)
                                apiKey = keyTrimmed
                                showApiKeyDialog = false
                                Toast.makeText(context, "Đã lưu API Key thành công! Trợ lý AI đã sẵn sàng.", Toast.LENGTH_LONG).show()
                            }.onFailure { err ->
                                testErrorText = err.message ?: "Key không hợp lệ."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isTestingKey
                ) {
                    if (isTestingKey) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Đang kiểm tra...", fontSize = 12.sp)
                        }
                    } else {
                        Text("Lưu & Kích hoạt", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Đóng", color = LightGray)
                }
            },
            containerColor = Color(0xFF1E2638),
            tonalElevation = 6.dp
        )
    }
}

@Composable
fun GeminiApiPricingCard(
    onOpenGoogleStudio: () -> Unit,
    onSetupKeyClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2436)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF334566))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFFD166),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "BẢNG GIÁ GEMINI API (GOOGLE)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
                Surface(
                    color = AccentGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "MIỄN PHÍ 0Đ",
                        color = AccentGreen,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Hạn mức sử dụng Gemini 1.5 Flash API trực tiếp từ Google AI Studio:",
                color = LightGray,
                fontSize = 11.5.sp,
                lineHeight = 15.sp
            )

            // Pricing Plans Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Free Plan (Personal)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2B)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, AccentGreen)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Gói Cá Nhân", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(13.dp))
                        }
                        Text("0 VNĐ / tháng", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 2.dp))
                        Text("• 15 câu hỏi / phút", color = LightGray, fontSize = 10.5.sp)
                        Text("• 1.500 câu / ngày", color = LightGray, fontSize = 10.5.sp)
                        Text("• 1M token / phút", color = LightGray, fontSize = 10.5.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            color = NeonBlue.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Khuyên dùng cho NV", color = NeonBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }

                // Paid Plan (Enterprise)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2B)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF2C3B54))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("Gói Tổ Chức", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        Text("~2,5đ / 1K từ", color = Color(0xFFFFD166), fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 2.dp))
                        Text("• Không giới hạn RPM", color = LightGray, fontSize = 10.5.sp)
                        Text("• Ưu tiên tốc độ cao", color = LightGray, fontSize = 10.5.sp)
                        Text("• Dành cho tổ chức lớn", color = LightGray, fontSize = 10.5.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            color = Color.Gray.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Pay-as-you-go", color = LightGray, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onOpenGoogleStudio,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🔗 Đăng ký lấy API Key 0đ", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(12.dp))
                    }
                }

                if (onSetupKeyClick != null) {
                    Button(
                        onClick = onSetupKeyClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("🔑 Kích hoạt Key", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
