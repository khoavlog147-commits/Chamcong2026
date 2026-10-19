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

/**
 * Helper to sanitize and format AI generated text for clean display and natural Vietnamese speech.
 * Eliminates erratic whitespace, trailing punctuation spaces, raw markdown artifacts,
 * and normalizes currency/time/abbreviations for smooth Text-to-Speech reading.
 */
object AiTextFormatter {
    fun cleanForDisplay(rawText: String): String {
        var text = rawText.replace(Regex("\\[\\[?\\s*ACTION:[^\\]\\}]+\\]?\\]"), "").trim()
        
        // Fix weird space before punctuation (e.g. "chữ , dấu cách" -> "chữ, dấu cách", "được . " -> "được. ")
        text = text.replace(Regex("[ \\t]+([,.:;?!])"), "$1")
        
        // Fix space after opening bracket and before closing bracket
        text = text.replace(Regex("\\(\\s+"), "(").replace(Regex("\\s+\\)"), ")")
        
        // Clean markdown bold syntax if broken: e.g. "** từ **" -> "**từ**"
        text = text.replace(Regex("\\*\\*\\s+"), "**").replace(Regex("\\s+\\*\\*"), "**")
        
        // Replace multiple consecutive spaces with a single space
        text = text.replace(Regex("[ \\t]+"), " ")
        
        // Replace 3+ consecutive newlines with 2 newlines
        text = text.replace(Regex("\\n{3,}"), "\n\n")
        
        return text.trim()
    }

    fun prepareForVietnameseTts(rawText: String): String {
        var text = cleanForDisplay(rawText)
        
        // Remove markdown formatting
        text = text.replace(Regex("[*_#`~]"), "")
        text = text.replace(Regex("^[-•*]\\s+", RegexOption.MULTILINE), "")
        
        // Remove emojis and special symbol glyphs that cause TTS to spell out English emoji names
        text = text.replace(Regex("[\\p{So}\\p{Cn}\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), " ")
        
        // Expand common work abbreviations to natural spoken Vietnamese
        text = text.replace(Regex("\\bOT\\b", RegexOption.IGNORE_CASE), "tăng ca")
        text = text.replace(Regex("\\bBHXH\\b", RegexOption.IGNORE_CASE), "bảo hiểm xã hội")
        text = text.replace(Regex("\\bBHYT\\b", RegexOption.IGNORE_CASE), "bảo hiểm y tế")
        text = text.replace(Regex("\\bBHTN\\b", RegexOption.IGNORE_CASE), "bảo hiểm thất nghiệp")
        text = text.replace(Regex("\\bLCB\\b", RegexOption.IGNORE_CASE), "lương cơ bản")
        text = text.replace(Regex("\\bLBH\\b", RegexOption.IGNORE_CASE), "lương đóng bảo hiểm")
        text = text.replace(Regex("\\bCN\\b"), "Chủ nhật")
        text = text.replace(Regex("\\bvs\\b", RegexOption.IGNORE_CASE), "so với")
        text = text.replace(Regex("\\bapprox\\b", RegexOption.IGNORE_CASE), "khoảng")
        
        // Convert Vietnamese currency numbers (e.g. 12.000.000đ, 500.000đ, 25.000đ, 25k, etc.) into natural spoken Vietnamese
        text = text.replace(Regex("(\\d+)\\.000\\.000\\s*(?:đ|₫|vnđ|vnd|đồng)?", RegexOption.IGNORE_CASE), "$1 triệu đồng")
        text = text.replace(Regex("(\\d+)\\.([1-9])00\\.000\\s*(?:đ|₫|vnđ|vnd|đồng)?", RegexOption.IGNORE_CASE), "$1 phẩy $2 triệu đồng")
        text = text.replace(Regex("(\\d+)\\.000\\s*(?:đ|₫|vnđ|vnd|đồng)?", RegexOption.IGNORE_CASE), "$1 nghìn đồng")
        text = text.replace(Regex("(\\d+)\\s*(?:k|K)\\s*(?:đ|₫|vnđ|vnd|đồng)?", RegexOption.IGNORE_CASE), "$1 nghìn đồng")
        text = text.replace(Regex("(\\d+)\\s*(?:đ|₫|vnđ|vnd)\\b", RegexOption.IGNORE_CASE), "$1 đồng")
        text = text.replace(Regex("(\\d+)\\s*₫"), "$1 đồng")
        
        // Replace "%" with " phần trăm"
        text = text.replace("%", " phần trăm")

        // Replace "/" between dates (e.g. 15/08/2026 -> ngày 15 tháng 8 năm 2026)
        text = text.replace(Regex("(\\d{1,2})/(\\d{1,2})/(\\d{4})"), "ngày $1 tháng $2 năm $3")
        text = text.replace(Regex("(\\d{1,2})/(\\d{1,2})"), "ngày $1 tháng $2")

        // Replace "/" in ratios/progress (e.g. "25/26" or "25 / 26" -> "25 trên 26")
        text = text.replace(Regex("(\\d+(?:[.,]\\d+)?)\\s*/\\s*(\\d+(?:[.,]\\d+)?)"), "$1 trên $2")
        text = text.replace("/", " trên ")
        
        // Replace ":" in time format (e.g. 08:00 -> 8 giờ, 17:30 -> 17 giờ 30)
        text = text.replace(Regex("(\\d{1,2}):00\\b"), "$1 giờ")
        text = text.replace(Regex("(\\d{1,2}):(\\d{2})\\b"), "$1 giờ $2")

        // Replace "g" or "h" as hours (e.g. "12g", "12 g", "12h", "12 h" -> "12 giờ", "12g30" -> "12 giờ 30")
        text = text.replace(Regex("(\\d{1,2})\\s*[ghGH](\\d{2})\\b"), "$1 giờ $2")
        text = text.replace(Regex("(\\d{1,2})\\s*[ghGH]\\b"), "$1 giờ")
        text = text.replace(Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:giờ|tiếng|h|g)\\b", RegexOption.IGNORE_CASE), "$1 giờ")
        
        // Clean leftover dots or double spaces
        text = text.replace(Regex("\\s+([,.:;?!])"), "$1")
        text = text.replace(Regex("[ \\t]+"), " ")
        text = text.replace(Regex("\\n+"), ". ")
        
        return text.trim()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalAiAssistantWidget(
    currentTab: String,
    viewModel: TimeSnapViewModel,
    onNavigateTab: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var isChatOpen by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showPricingCard by remember { mutableStateOf(false) }
    var showActivityLogsDialog by remember { mutableStateOf(false) }
    var logsTrigger by remember { mutableStateOf(0) }

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
    val inlineLogs = remember(chatMessages.size, isGeneratingResponse, showActivityLogsDialog, logsTrigger) {
        com.example.util.AiActivityLogManager.getLogs(context).take(3)
    }
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

    // Active Screen Name for UI display and AI context
    val tabNameVi = when (currentTab) {
        "home" -> "Trang chủ Chấm công"
        "history" -> "Lịch sử Chấm công"
        "payslip" -> "Phiếu lương & Thu nhập"
        "settings" -> "Cài đặt ứng dụng"
        "admin" -> "Quản trị hệ thống"
        "notifications" -> "Trung tâm Thông báo"
        else -> "Màn hình ứng dụng"
    }

    // Prepare Context Information for AI via real-time Screen Context Buffer
    val contextDataStr = remember(currentTab, userConfig, summaryState, timeEntries, salaryHistory) {
        com.example.util.AiContextBuffer.buildScreenContextBuffer(
            currentTab = currentTab,
            userConfig = userConfig,
            summaryState = summaryState,
            timeEntries = timeEntries,
            salaryHistory = salaryHistory
        )
    }

    // TTS Voice Engine State & SharedPreferences Preferences
    val prefs = remember { context.getSharedPreferences("ai_voice_prefs", Context.MODE_PRIVATE) }
    var voiceGender by remember { mutableStateOf(prefs.getString("gender", "female") ?: "female") }
    var voiceSpeed by remember { mutableStateOf(prefs.getFloat("speed", 1.0f)) }
    var showVoiceSettingsDialog by remember { mutableStateOf(false) }

    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsSpeaking by remember { mutableStateOf(false) }
    var currentSpeakingMsgId by remember { mutableStateOf<String?>(null) }
    var autoSpeakVoice by remember { mutableStateOf(true) }
    var isListeningVoice by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var ttsRef: TextToSpeech? = null
        ttsRef = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsRef?.language = Locale("vi", "VN")
            }
        }
        try {
            ttsRef.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isTtsSpeaking = true
                }
                override fun onDone(utteranceId: String?) {
                    isTtsSpeaking = false
                    currentSpeakingMsgId = null
                }
                override fun onError(utteranceId: String?) {
                    isTtsSpeaking = false
                    currentSpeakingMsgId = null
                }
            })
        } catch (e: Exception) {
            // Ignore if engine does not support listener
        }
        ttsInstance = ttsRef
        onDispose {
            ttsRef?.stop()
            ttsRef?.shutdown()
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
            
            val isFemaleTarget = (voiceGender == "female")
            
            // Adjust pitch to complement selected gender (Female higher 1.15, Male deeper 0.85)
            ttsInstance?.setPitch(if (isFemaleTarget) 1.15f else 0.85f)
            ttsInstance?.setSpeechRate(voiceSpeed)

            // Select matching female or male Voice object from Android TextToSpeech engine
            try {
                val voices = ttsInstance?.voices
                if (!voices.isNullOrEmpty()) {
                    val viVoices = voices.filter { 
                        it.locale.language == "vi" || 
                        it.locale.country == "VN" || 
                        it.locale.toString().lowercase().contains("vi") 
                    }.ifEmpty { voices.toList() }

                    val targetVoice = viVoices.find { v ->
                        val vName = v.name.lowercase()
                        val features = try { v.features } catch (e: Exception) { null }
                        val isFemale = features?.any { it.contains("gender=female") } == true ||
                                vName.contains("female") ||
                                vName.contains("woman") ||
                                vName.contains("vic") || // Google TTS vi-VN-x-vic = Female
                                vName.contains("vie") || // Google TTS vi-VN-x-vie = Female
                                vName.contains("gfm") || // Google Female Model
                                vName.contains("vdf") ||
                                vName.contains("wavenet-a") ||
                                vName.contains("wavenet-c") ||
                                vName.contains("standard-a") ||
                                vName.contains("standard-c") ||
                                vName.contains("f0") ||
                                vName.contains("f1") ||
                                vName.contains("f2") ||
                                vName.contains("-f-")

                        val isMale = features?.any { it.contains("gender=male") } == true ||
                                vName.contains("male") ||
                                vName.contains("man") ||
                                vName.contains("vif") || // Google TTS vi-VN-x-vif = Male
                                vName.contains("vid") || // Google TTS vi-VN-x-vid = Male
                                vName.contains("gmm") || // Google Male Model
                                vName.contains("vdm") ||
                                vName.contains("vib") ||
                                vName.contains("wavenet-b") ||
                                vName.contains("wavenet-d") ||
                                vName.contains("standard-b") ||
                                vName.contains("standard-d") ||
                                vName.contains("m0") ||
                                vName.contains("m1") ||
                                vName.contains("-m-")

                        if (isFemaleTarget) {
                            isFemale && !isMale
                        } else {
                            isMale && !isFemale
                        }
                    } ?: viVoices.find { v ->
                        val vName = v.name.lowercase()
                        if (isFemaleTarget) {
                            !vName.contains("male") && !vName.contains("vif") && !vName.contains("vid") && !vName.contains("gmm") && !vName.contains("m0")
                        } else {
                            !vName.contains("female") && !vName.contains("vic") && !vName.contains("vie") && !vName.contains("gfm") && !vName.contains("f0")
                        }
                    } ?: viVoices.firstOrNull()

                    if (targetVoice != null) {
                        ttsInstance?.voice = targetVoice
                    }
                }
            } catch (e: Exception) {
                // Fallback gracefully on TTS engines that don't support voice enumeration
            }

            val speechSanitized = AiTextFormatter.prepareForVietnameseTts(text)
            val speakResult = ttsInstance?.speak(speechSanitized, TextToSpeech.QUEUE_FLUSH, null, id)
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
                val primaryKey = AiApiKeyManager.getApiKey(context)
                val backupKey = AiApiKeyManager.getBackupApiKey(context)
                chatMessages.add(AiChatMessage(sender = "user", text = prompt))
                userPromptText = ""
                isGeneratingResponse = true

                coroutineScope.launch {
                    val historyPairs = chatMessages.takeLast(6).filter { !it.isError }.map { 
                        Pair(if (it.sender == "user") "user" else "model", it.text) 
                    }

                    val result = com.example.data.AiServiceManager.generateContent(
                        context = context,
                        userPrompt = prompt,
                        contextData = contextDataStr,
                        chatHistory = historyPairs
                    )

                    isGeneratingResponse = false
                    result.onSuccess { responseText ->
                        // Execute all explicit and implicit AI Action Commands via AiActionEngine
                        com.example.util.AiActionEngine.executeActions(
                            context = context,
                            userPrompt = prompt,
                            aiResponseText = responseText,
                            viewModel = viewModel,
                            userConfig = userConfig,
                            onNavigateTab = onNavigateTab
                        )
                        logsTrigger++

                        val cleanText = AiTextFormatter.cleanForDisplay(responseText)

                        val newAiMsg = AiChatMessage(sender = "ai", text = cleanText)
                        chatMessages.add(newAiMsg)
                        if (isFromVoice || autoSpeakVoice) {
                            speakText(newAiMsg.id, cleanText)
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
                            onClick = { showVoiceSettingsDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SettingsVoice,
                                contentDescription = "Cài đặt Giọng nói AI",
                                tint = if (voiceGender == "male") NeonBlue else Color(0xFFFF4081)
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
                            onClick = { showActivityLogsDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Lịch sử hoạt động AI",
                                tint = LightGray
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
                                text = "Tôi đã được cấp FULL QUYỀN TRỢ LÝ: Đọc bảng lương thực tế/dự kiến, tự động thêm/xóa ngày công, chấm công vào/ra ca, đổi lương/phụ cấp và chuyển màn hình theo lệnh của bạn!",
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
                        "⚡ Phân biệt Lương Thực tế vs Dự kiến",
                        "⚡ So sánh lương tháng này với tháng trước",
                        "⚡ Công thức tính tiền OT ca đêm & Chủ nhật",
                        "⚡ Tỷ lệ trừ BHXH & 12 khoản phụ cấp",
                        "⚡ Thuật toán tính lương của hệ thống"
                    )
                    "history" -> listOf(
                        "⚡ Xóa công ngày hôm qua",
                        "⚡ Thêm công làm bù từ 07:30 đến 19:30",
                        "⚡ Tổng công làm việc tháng này là bao nhiêu?",
                        "⚡ Tháng này tôi làm bao nhiêu ca đêm?",
                        "⚡ Tổng số giờ OT tăng ca tháng này?"
                    )
                    "home" -> listOf(
                        "⚡ Chấm công vào ca cho tôi",
                        "⚡ Chấm công ra ca cho tôi",
                        "⚡ Thêm ngày nghỉ phép năm cho tôi",
                        "⚡ Ngày công chuẩn tháng này là bao nhiêu?",
                        "⚡ Thuật toán tính lương của hệ thống"
                    )
                    "settings" -> listOf(
                        "⚡ Đổi lương cơ bản thành 12 triệu",
                        "⚡ Kiểm tra toàn bộ cài đặt lương của tôi",
                        "⚡ Hướng dẫn tạo Gemini API Key miễn phí",
                        "⚡ Hướng dẫn xuất file phiếu lương PDF/PNG"
                    )
                    else -> listOf(
                        "⚡ Thuật toán tính lương của hệ thống",
                        "⚡ Phân biệt Lương Thực tế vs Dự kiến",
                        "⚡ Chi tiết cài đặt lương của tôi",
                        "⚡ Tỷ lệ trừ BHXH & các phụ cấp"
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(quickChips) { chipText ->
                        Surface(
                            onClick = { handleSendMessage(chipText.replace("⚡ ", "").replace("💡 ", "").trim()) },
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
                                modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(0.82f)
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

                // Inline AI Activity Logs
                if (inlineLogs.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = NeonBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "AI vừa tự động chỉnh sửa:",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Xóa lịch sử",
                                        color = Color(0xFFEF476F),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            com.example.util.AiActivityLogManager.clearLogs(context)
                                            logsTrigger++
                                            Toast.makeText(context, "Đã xóa lịch sử hoạt động!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    Text(
                                        text = "Xem tất cả",
                                        color = NeonBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable { showActivityLogsDialog = true }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                inlineLogs.forEach { log ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val (icon, tint) = when (log.actionType) {
                                            "ATTENDANCE" -> Pair(Icons.Default.CheckCircle, AccentGreen)
                                            "SALARY_CONFIG" -> Pair(Icons.Default.MonetizationOn, Color(0xFFFFD166))
                                            "TIMESHEET" -> Pair(Icons.Default.CalendarMonth, NeonBlue)
                                            else -> Pair(Icons.Default.Bolt, Color(0xFF4CC9F0))
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = tint,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = log.description,
                                                color = Color.White,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (log.userPrompt.isNotBlank()) {
                                                Text(
                                                    text = "Yêu cầu: \"${log.userPrompt}\"",
                                                    color = LightGray.copy(alpha = 0.7f),
                                                    fontSize = 9.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

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
        var selectedProvider by remember { mutableStateOf(AiApiKeyManager.getProvider(context)) }
        var inputKey by remember { mutableStateOf(AiApiKeyManager.getApiKey(context)) }
        var inputBackupKey by remember { mutableStateOf(AiApiKeyManager.getBackupApiKey(context)) }
        var openRouterKey by remember { mutableStateOf(AiApiKeyManager.getOpenRouterKey(context)) }
        var openRouterModel by remember { mutableStateOf(AiApiKeyManager.getOpenRouterModel(context)) }

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
                        text = "Cấu hình Trợ lý AI",
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
                    // Provider Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = {
                                selectedProvider = "gemini"
                                testErrorText = null
                            },
                            color = if (selectedProvider == "gemini") NeonBlue.copy(alpha = 0.25f) else Color(0xFF2C384E),
                            border = BorderStroke(1.5.dp, if (selectedProvider == "gemini") NeonBlue else Color.Transparent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "♊ Gemini",
                                color = if (selectedProvider == "gemini") NeonBlue else LightGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                selectedProvider = "openrouter"
                                testErrorText = null
                            },
                            color = if (selectedProvider == "openrouter") Color(0xFFFF9800).copy(alpha = 0.25f) else Color(0xFF2C384E),
                            border = BorderStroke(1.5.dp, if (selectedProvider == "openrouter") Color(0xFFFF9800) else Color.Transparent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "🌐 OpenRouter Free",
                                color = if (selectedProvider == "openrouter") Color(0xFFFF9800) else LightGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    if (selectedProvider == "gemini") {
                        Text(
                            text = "Nhập API Key Gemini chính và dự phòng của bạn:",
                            color = LightGray,
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = inputKey,
                            onValueChange = { 
                                inputKey = it 
                                testErrorText = null
                            },
                            label = { Text("Gemini API Key chính", color = LightGray) },
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

                        OutlinedTextField(
                            value = inputBackupKey,
                            onValueChange = { 
                                inputBackupKey = it 
                                testErrorText = null
                            },
                            label = { Text("Gemini API Key dự phòng (Fallback)", color = LightGray) },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF9800),
                                unfocusedBorderColor = Color.Gray
                            )
                        )

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
                                text = "👉 Lấy Gemini API Key miễn phí từ Google",
                                color = NeonBlue,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // OpenRouter Form
                        Text(
                            text = "Mô hình AI Miễn phí:",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )

                        val models = listOf(
                            Pair("🦙 Llama 3.3 70B (Siêu Nhanh)", "meta-llama/llama-3.3-70b-instruct:free"),
                            Pair("🧠 DeepSeek R1 (Tư duy sâu)", "deepseek/deepseek-r1:free"),
                            Pair("💎 Gemini 2.0 Flash Lite", "google/gemini-2.0-flash-lite-001:free"),
                            Pair("🤖 Auto (Tự chọn AI tốt nhất)", "openrouter/auto")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            models.forEach { (label, modelKey) ->
                                val isSelected = openRouterModel == modelKey
                                Surface(
                                    onClick = { openRouterModel = modelKey },
                                    color = if (isSelected) Color(0xFFFF9800).copy(alpha = 0.2f) else Color(0xFF1E2838),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFFF9800) else Color.Transparent),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { openRouterModel = modelKey },
                                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF9800))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = openRouterKey,
                            onValueChange = { 
                                openRouterKey = it 
                                testErrorText = null
                            },
                            label = { Text("OpenRouter API Key", color = LightGray) },
                            placeholder = { Text("sk-or-v1-...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF9800),
                                unfocusedBorderColor = Color.Gray
                            )
                        )

                        TextButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouter.ai/keys"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Mở trình duyệt thất bại", Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "👉 Lấy OpenRouter API Key miễn phí",
                                color = Color(0xFFFF9800),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (testErrorText != null) {
                        Text(
                            text = testErrorText!!,
                            color = AccentRed,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AiApiKeyManager.saveProvider(context, selectedProvider)
                        if (selectedProvider == "gemini") {
                            val keyTrimmed = inputKey.trim()
                            val backupTrimmed = inputBackupKey.trim()

                            if (keyTrimmed.isBlank() && backupTrimmed.isBlank()) {
                                AiApiKeyManager.clearApiKey(context)
                                AiApiKeyManager.clearBackupApiKey(context)
                                apiKey = ""
                                showApiKeyDialog = false
                                Toast.makeText(context, "Đã xóa toàn bộ Gemini API Key.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isTestingKey = true
                            testErrorText = null

                            coroutineScope.launch {
                                val activeTestKey = if (keyTrimmed.isNotBlank()) keyTrimmed else backupTrimmed
                                val testRes = GeminiAiService.generateContent(
                                    apiKey = activeTestKey,
                                    userPrompt = "Xin chào, hãy trả lời đúng từ OK",
                                    contextData = "Kiểm tra API Key"
                                )
                                isTestingKey = false
                                testRes.onSuccess {
                                    if (keyTrimmed.isNotBlank()) AiApiKeyManager.saveApiKey(context, keyTrimmed) else AiApiKeyManager.clearApiKey(context)
                                    if (backupTrimmed.isNotBlank()) AiApiKeyManager.saveBackupApiKey(context, backupTrimmed) else AiApiKeyManager.clearBackupApiKey(context)
                                    apiKey = activeTestKey
                                    showApiKeyDialog = false
                                    Toast.makeText(context, "Đã lưu Gemini API Key thành công!", Toast.LENGTH_LONG).show()
                                }.onFailure { err ->
                                    testErrorText = err.message ?: "Key không hợp lệ."
                                }
                            }
                        } else {
                            val orKeyTrimmed = openRouterKey.trim()
                            if (orKeyTrimmed.isBlank()) {
                                AiApiKeyManager.clearOpenRouterKey(context)
                                apiKey = ""
                                showApiKeyDialog = false
                                Toast.makeText(context, "Đã xóa OpenRouter API Key.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isTestingKey = true
                            testErrorText = null

                            coroutineScope.launch {
                                val testRes = com.example.data.OpenRouterAiService.generateContent(
                                    apiKey = orKeyTrimmed,
                                    model = openRouterModel,
                                    userPrompt = "Xin chào, hãy trả lời OK",
                                    contextData = "Kiểm tra API Key"
                                )
                                isTestingKey = false
                                testRes.onSuccess {
                                    AiApiKeyManager.saveOpenRouterKey(context, orKeyTrimmed)
                                    AiApiKeyManager.saveOpenRouterModel(context, openRouterModel)
                                    apiKey = orKeyTrimmed
                                    showApiKeyDialog = false
                                    Toast.makeText(context, "Đã lưu OpenRouter API Key thành công!", Toast.LENGTH_LONG).show()
                                }.onFailure { err ->
                                    testErrorText = err.message ?: "OpenRouter Key không hợp lệ."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedProvider == "openrouter") Color(0xFFFF9800) else AccentGreen,
                        contentColor = Color.White
                    ),
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

    // DIALOG CÀI ĐẶT GIỌNG NÓI (MALE / FEMALE & SPEED)
    if (showVoiceSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceSettingsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsVoice,
                        contentDescription = null,
                        tint = NeonBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Cài đặt Giọng nói AI",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tùy chỉnh giọng đọc phản hồi của Trợ lý AI:",
                        color = LightGray,
                        fontSize = 13.sp
                    )

                    // Gender Selection
                    Text(
                        text = "Chọn Giọng đọc:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            onClick = {
                                voiceGender = "female"
                                prefs.edit().putString("gender", "female").apply()
                                Toast.makeText(context, "Đã chọn Giọng Nữ (Thanh thoát)", Toast.LENGTH_SHORT).show()
                            },
                            color = if (voiceGender == "female") Color(0xFFE91E63).copy(alpha = 0.25f) else Color(0xFF2C384E),
                            border = BorderStroke(
                                1.5.dp,
                                if (voiceGender == "female") Color(0xFFFF4081) else Color.Transparent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "👧 Giọng Nữ",
                                    color = if (voiceGender == "female") Color(0xFFFF4081) else LightGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                voiceGender = "male"
                                prefs.edit().putString("gender", "male").apply()
                                Toast.makeText(context, "Đã chọn Giọng Nam (Trầm ấm)", Toast.LENGTH_SHORT).show()
                            },
                            color = if (voiceGender == "male") NeonBlue.copy(alpha = 0.25f) else Color(0xFF2C384E),
                            border = BorderStroke(
                                1.5.dp,
                                if (voiceGender == "male") NeonBlue else Color.Transparent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "👦 Giọng Nam",
                                    color = if (voiceGender == "male") NeonBlue else LightGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Speed Selection
                    Text(
                        text = "Tốc độ nói:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            Triple(0.85f, "Chậm", "🐢"),
                            Triple(1.0f, "Bình thường", "🚶"),
                            Triple(1.25f, "Nhanh", "🐇")
                        ).forEach { (speedVal, speedLabel, emoji) ->
                            val isSelected = (voiceSpeed == speedVal)
                            Surface(
                                onClick = {
                                    voiceSpeed = speedVal
                                    prefs.edit().putFloat("speed", speedVal).apply()
                                },
                                color = if (isSelected) AccentGreen.copy(alpha = 0.25f) else Color(0xFF2C384E),
                                border = BorderStroke(
                                    1.5.dp,
                                    if (isSelected) AccentGreen else Color.Transparent
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Text(text = emoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = speedLabel,
                                        color = if (isSelected) AccentGreen else LightGray,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Test Voice button
                    Button(
                        onClick = {
                            val sampleText = if (voiceGender == "female") {
                                "Xin chào! Em là Trợ lý AI TimeSnap Pro giọng Nữ, luôn sẵn sàng hỗ trợ anh tính lương."
                            } else {
                                "Xin chào! Tôi là Trợ lý AI TimeSnap Pro giọng Nam, sẵn sàng đồng hành cùng bạn."
                            }
                            speakText("sample_voice_test", sampleText)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = NeonBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(text = "🔊 Nghe thử giọng đã chọn", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        stopSpeaking()
                        showVoiceSettingsDialog = false 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Đóng & Áp dụng", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            },
            containerColor = Color(0xFF1E2638),
            tonalElevation = 6.dp
        )
    }

    if (showActivityLogsDialog) {
        val logs = remember(showActivityLogsDialog) { com.example.util.AiActivityLogManager.getLogs(context) }
        var logsList by remember { mutableStateOf(logs) }

        AlertDialog(
            onDismissRequest = { showActivityLogsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = NeonBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Lịch sử hoạt động AI",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (logsList.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                com.example.util.AiActivityLogManager.clearLogs(context)
                                logsList = emptyList()
                                Toast.makeText(context, "Đã xóa lịch sử hoạt động!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Xóa tất cả",
                                tint = Color(0xFFEF476F)
                            )
                        }
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    if (logsList.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = LightGray.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Chưa có hoạt động nào được ghi nhận",
                                color = LightGray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Khi bạn yêu cầu Trợ lý thực hiện các tác vụ (như chấm công, đổi lương, ghi nhận ngày công...), lịch sử hoạt động của AI sẽ hiển thị tại đây.",
                                color = LightGray.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(logsList) { log ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF242F41)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Action icon
                                        val (icon, tint) = when (log.actionType) {
                                            "ATTENDANCE" -> Pair(Icons.Default.CheckCircle, AccentGreen)
                                            "SALARY_CONFIG" -> Pair(Icons.Default.MonetizationOn, Color(0xFFFFD166))
                                            "TIMESHEET" -> Pair(Icons.Default.CalendarMonth, NeonBlue)
                                            else -> Pair(Icons.Default.Bolt, Color(0xFF4CC9F0))
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = tint,
                                            modifier = Modifier.size(24.dp)
                                        )

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = "AI đã: " + log.description,
                                                color = Color.White,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (log.userPrompt.isNotBlank()) {
                                                Text(
                                                    text = "Yêu cầu: \"${log.userPrompt}\"",
                                                    color = LightGray,
                                                    fontSize = 11.5.sp,
                                                    style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                                )
                                            }
                                            val timeStr = java.text.SimpleDateFormat("HH:mm - dd/MM/yyyy", java.util.Locale.getDefault())
                                                .format(java.util.Date(log.timestamp))
                                            Text(
                                                text = timeStr,
                                                color = LightGray.copy(alpha = 0.5f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showActivityLogsDialog = false }) {
                    Text("Đóng", color = NeonBlue, fontWeight = FontWeight.Bold)
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
