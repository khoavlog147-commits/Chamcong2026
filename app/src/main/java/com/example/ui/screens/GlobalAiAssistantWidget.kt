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
import java.text.DecimalFormat
import kotlin.math.roundToInt

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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

    // Auto display API Pricing / Key dialog when user opens AI assistant for the first time without API key
    LaunchedEffect(isChatOpen) {
        if (isChatOpen && !hasApiKey) {
            showApiKeyDialog = true
            showPricingCard = true
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

    // Observe App State for context
    val userConfig by viewModel.userConfig.collectAsStateWithLifecycle()
    val summaryState by viewModel.salarySummaryState.collectAsStateWithLifecycle()
    val timeEntries by viewModel.monthTimeEntries.collectAsStateWithLifecycle(emptyList())

    // Scroll to bottom when new message arrives
    LaunchedEffect(chatMessages.size, isGeneratingResponse) {
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

    val contextDataStr = remember(currentTab, userConfig, summaryState, timeEntries) {
        val fmt = DecimalFormat("#,###")
        val configInfo = userConfig?.let {
            "Họ tên: ${it.hoVaTen}, Mã NV: ${it.maNhanVien}, Chức vụ: ${it.roleName}, Lương cơ bản: ${fmt.format(it.luongCoBan)}đ"
        } ?: "Thông tin nhân viên chưa cập nhật"

        val summaryInfo = summaryState?.let { s ->
            "Công thực tế: ${s.workingDays}/${s.standardWorkDays} ngày. OT ngày: ${s.otDayHours}g, OT đêm: ${s.otNightHours}g, Lương thực nhận: ${fmt.format(s.luongThucNhan)}đ"
        } ?: "Chưa có tổng hợp công tháng này"

        """
        - Màn hình hiện tại: $tabNameVi
        - Nhân viên: $configInfo
        - Thống kê tháng: $summaryInfo
        """.trimIndent()
    }

    // Function to handle sending message
    val handleSendMessage: (String) -> Unit = { inputPrompt ->
        val prompt = inputPrompt.trim()
        if (prompt.isNotBlank() && !isGeneratingResponse) {
            if (!AiApiKeyManager.hasApiKey(context)) {
                showApiKeyDialog = true
            } else {
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
                        chatMessages.add(AiChatMessage(sender = "ai", text = responseText))
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
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
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
                .clickable { isChatOpen = true }
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
        ModalBottomSheet(
            onDismissRequest = { isChatOpen = false },
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
                    .fillMaxHeight(0.85f)
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
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Gemini API Pricing Card (Show automatically if no key or toggled)
                if (showPricingCard || !hasApiKey) {
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
                if (chatMessages.isEmpty() && hasApiKey && !showPricingCard) {
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
                        "💡 Giải thích tổng lương tháng này",
                        "💡 Cách tính tiền tăng ca chủ nhật 2.0",
                        "💡 Tiền chuyên cần & phụ cấp tính sao?",
                        "💡 Các khoản khấu trừ gồm những gì?"
                    )
                    "history" -> listOf(
                        "💡 Tổng công làm việc tháng này là bao nhiêu?",
                        "💡 Tháng này tôi làm bao nhiêu ca đêm?",
                        "💡 Kiểm tra xem tôi có đi trễ buổi nào không?",
                        "💡 Tổng số giờ OT tăng ca tháng này?"
                    )
                    "home" -> listOf(
                        "💡 Giờ quy định vào ca ngày & ca đêm",
                        "💡 Hướng dẫn cách chấm công nhanh",
                        "💡 Ngày công chuẩn tháng này là bao nhiêu?",
                        "💡 Quy định tính tăng ca OT công ty"
                    )
                    "settings" -> listOf(
                        "💡 Hướng dẫn tạo Gemini API Key miễn phí",
                        "💡 Key của tôi được lưu bảo mật ở đâu?",
                        "💡 Hướng dẫn xuất file phiếu lương PDF/PNG",
                        "💡 Đổi mật khẩu & Bảo mật tài khoản"
                    )
                    else -> listOf(
                        "💡 Giải đáp bảng lương & chấm công",
                        "💡 Hướng dẫn sử dụng ứng dụng",
                        "💡 Lợi ích của Gemini AI Key cá nhân"
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
                        .fillMaxWidth(),
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
                                    Text(
                                        text = "Sao chép",
                                        color = LightGray.copy(alpha = 0.7f),
                                        fontSize = 10.5.sp,
                                        modifier = Modifier
                                            .padding(top = 2.dp, start = 4.dp)
                                            .clickable {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("AI Response", msg.text))
                                                Toast.makeText(context, "Đã sao chép phản hồi!", Toast.LENGTH_SHORT).show()
                                            }
                                    )
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

                // Bottom Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
