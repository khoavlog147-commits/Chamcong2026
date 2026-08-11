package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.screens.AdminScreen // Ensure it is imported if not covered by *
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.White
import com.example.ui.theme.AccentGreen
import com.example.viewmodel.TimeSnapViewModel
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Info
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.data.DatabaseHelper.init(applicationContext)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                // Yêu cầu quyền thông báo trên Android 13+ (Tiramisu)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            android.util.Log.d("MainActivity", "Quyền thông báo đã được cấp")
                        } else {
                            android.util.Log.d("MainActivity", "Quyền thông báo bị từ chối")
                        }
                    }
                    
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != 
                            android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val viewModel: TimeSnapViewModel = viewModel()
                val sessionState by viewModel.currentUserSession.collectAsStateWithLifecycle()
                
                val rootNavController = rememberNavController()

                val startDest = remember {
                    if (viewModel.currentUserSession.value != null) "main" else "login"
                }

                val context = androidx.compose.ui.platform.LocalContext.current
                var updateInfo by remember { mutableStateOf<com.example.data.AppVersionControl?>(null) }

                 val currentLocalCode = remember {
                    try {
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
                }

                val currentLocalName = remember {
                    try {
                        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        packageInfo.versionName ?: "1.4"
                    } catch (e: Exception) {
                        "1.4"
                    }
                }

                var showDevPublishPrompt by remember { mutableStateOf(false) }
                var devPublishCloudUrl by remember { mutableStateOf("") }
                var devCloudCode by remember { mutableStateOf(0L) }

                LaunchedEffect(sessionState) {
                    sessionState?.uid?.let { uid ->
                        com.example.notification.NotificationHelper.scheduleAdminNotificationSync(context, uid)
                    }
                    val email = sessionState?.email ?: ""
                    if (email.trim().lowercase() == "khoatubexxx@gmail.com" && !com.example.data.FirestoreService.isEmulator()) {
                        try {
                            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            firestore.collection("app_config").document("version_control")
                                .get().addOnSuccessListener { doc ->
                                    if (doc != null && doc.exists()) {
                                        val cloudCode = doc.getLong("current_version_code") ?: 0L
                                        val cloudName = doc.getString("current_version_name") ?: ""
                                        val cloudUrl = doc.getString("download_url") ?: ""
                                        devPublishCloudUrl = cloudUrl
                                        devCloudCode = cloudCode
                                        
                                        val isLocalNewer = com.example.data.FirestoreService.isVersionNewer(
                                            currentLocalCode, currentLocalName,
                                            cloudCode, cloudName
                                        )
                                        if (isLocalNewer) {
                                            showDevPublishPrompt = true
                                        }
                                    }
                                }
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Error checking dev publish: ${e.message}")
                        }
                    }
                }

                LaunchedEffect(sessionState) {
                    try {
                        kotlinx.coroutines.delay(2000)
                        val info = com.example.data.FirestoreService.checkAppVersion(context)
                        if (info != null && info.isNewVersionAvailable) {
                            updateInfo = info
                        }
                    } catch (e: Throwable) {
                        android.util.Log.e("MainActivity", "Error checking update version", e)
                    }
                }

                if (updateInfo != null) {
                    val info = updateInfo!!
                    AlertDialog(
                        onDismissRequest = { /* Dismissable */ },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "New Version Icon",
                                    tint = com.example.ui.theme.AccentOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Bản Cập Nhật Mới!",
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.White
                                )
                            }
                        },
                        text = {
                            Text(
                                text = "Đã có phiên bản mới, vui lòng cập nhật để sử dụng tính năng mới nhất!",
                                color = com.example.ui.theme.White.copy(alpha = 0.85f),
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
                                    containerColor = com.example.ui.theme.NeonBlue,
                                    contentColor = com.example.ui.theme.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cập nhật ngay", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { updateInfo = null }
                            ) {
                                Text("Để sau", color = com.example.ui.theme.White.copy(alpha = 0.6f))
                            }
                        },
                        containerColor = Color(0xFF1E1E1E),
                        tonalElevation = 6.dp,
                        modifier = Modifier.testTag("app_update_dialog")
                    )
                }

                if (showDevPublishPrompt) {
                    AlertDialog(
                        onDismissRequest = { showDevPublishPrompt = false },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Admin Icon",
                                    tint = AccentGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Công Cụ Quản Trị Viên 🚀",
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.White
                                )
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Hi Developer! Phát hiện bạn đang cài đặt bản dựng mới có mã v$currentLocalCode (cao hơn v$devCloudCode đang phát sóng trên cloud).",
                                    color = com.example.ui.theme.White.copy(alpha = 0.85f),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Bạn có muốn cập nhật mã phiên bản v$currentLocalCode này lên cloud ngay bây giờ để những người dùng khác có thể nhận hướng dẫn tự động nâng cấp?",
                                    color = com.example.ui.theme.LightGray,
                                    fontSize = 13.sp
                                )
                                OutlinedTextField(
                                    value = devPublishCloudUrl,
                                    onValueChange = { devPublishCloudUrl = it },
                                    label = { Text("Đường dẫn tải APK trực tiếp", color = com.example.ui.theme.AccentOrange) },
                                    placeholder = { Text("Dán link tải APK (Google Drive, v.v)") },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = com.example.ui.theme.White,
                                        unfocusedTextColor = com.example.ui.theme.White,
                                        focusedBorderColor = com.example.ui.theme.AccentOrange,
                                        unfocusedBorderColor = Color.Gray,
                                        cursorColor = com.example.ui.theme.AccentOrange
                                    )
                                )
                            }
                        },
                        confirmButton = {
                            val coroutineScope = rememberCoroutineScope()
                            var isPrompterPublishing by remember { mutableStateOf(false) }
                            Button(
                                onClick = {
                                    if (devPublishCloudUrl.isBlank()) {
                                        android.widget.Toast.makeText(context, "Vui lòng nhập đường dẫn tải APK!", android.widget.Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isPrompterPublishing = true
                                    coroutineScope.launch {
                                        val success = com.example.data.FirestoreService.publishNewAppVersion(currentLocalCode, devPublishCloudUrl.trim())
                                        isPrompterPublishing = false
                                        showDevPublishPrompt = false
                                        if (success) {
                                            android.widget.Toast.makeText(context, "Đã tự động phát hành bản cập nhật v$currentLocalCode thành công!", android.widget.Toast.LENGTH_LONG).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Lỗi xảy ra khi phát hành cập nhật!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentGreen,
                                    contentColor = com.example.ui.theme.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isPrompterPublishing
                            ) {
                                if (isPrompterPublishing) {
                                    CircularProgressIndicator(color = com.example.ui.theme.White, modifier = Modifier.size(18.dp))
                                } else {
                                    Text("Đồng ý phát hành", fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showDevPublishPrompt = false }
                            ) {
                                Text("Khép bỏ", color = com.example.ui.theme.White.copy(alpha = 0.6f))
                            }
                        },
                        containerColor = Color(0xFF1E1E1E),
                        tonalElevation = 6.dp
                    )
                }

                // Security Redirection Rule: Handled centrally using standard dynamic startDestination to prevent Navigation crashes
                LaunchedEffect(sessionState) {
                    val currentDest = rootNavController.currentBackStackEntry?.destination?.route
                    if (sessionState == null) {
                        if (currentDest != "login" && currentDest != "register") {
                            rootNavController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    } else {
                        if (currentDest == "login" || currentDest == "register") {
                            rootNavController.navigate("main") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Full screen dusk building background image matching login screen
                        Image(
                            painter = painterResource(id = R.drawable.img_login_bg),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Dark gradient overlay for optimal readability and high contrast
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF0B0F17).copy(alpha = 0.72f),
                                            Color(0xFF070A0F).copy(alpha = 0.93f)
                                        )
                                    )
                                )
                        )

                        NavHost(
                            navController = rootNavController,
                            startDestination = startDest
                        ) {
                            composable("login") {
                                LoginScreen(
                                    viewModel = viewModel,
                                    onNavigateToRegister = { rootNavController.navigate("register") },
                                    onLoginSuccess = {
                                        rootNavController.navigate("main") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("register") {
                                RegisterScreen(
                                    viewModel = viewModel,
                                    onNavigateToLogin = { rootNavController.navigate("login") },
                                    onRegisterSuccess = {
                                        rootNavController.navigate("main") {
                                            popUpTo("register") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("main") {
                                MainTabScreenContainer(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

// NESTED SCOPE MATERIAL 3 BOTTOM NAVIGATION CONTAINER
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabScreenContainer(viewModel: TimeSnapViewModel) {
    val tabNavController = rememberNavController()
    
    // Track selected destination for the bottom bar active states
    var currentTab by remember { mutableStateOf("home") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = com.example.ui.theme.DarkContainer.copy(alpha = 0.88f),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .testTag("bottom_nav_bar")
                    .border(androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.CardBorder))
            ) {
                // Item 1: Trang chu
                NavigationBarItem(
                    selected = currentTab == "home",
                    onClick = {
                        currentTab = "home"
                        tabNavController.navigate("home") {
                            popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Trang chủ", modifier = Modifier.size(22.dp)) },
                    label = { Text("Trang chủ", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = White,
                        selectedTextColor = com.example.ui.theme.PrimaryBlue,
                        indicatorColor = com.example.ui.theme.PrimaryBlue,
                        unselectedIconColor = com.example.ui.theme.TextSecondary,
                        unselectedTextColor = com.example.ui.theme.TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_home_tab")
                )

                // Item 2: Lịch sử Chấm công
                NavigationBarItem(
                    selected = currentTab == "history",
                    onClick = {
                        currentTab = "history"
                        tabNavController.navigate("history") {
                            popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Lịch sử", modifier = Modifier.size(22.dp)) },
                    label = { Text("Lịch sử", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = White,
                        selectedTextColor = com.example.ui.theme.PrimaryBlue,
                        indicatorColor = com.example.ui.theme.PrimaryBlue,
                        unselectedIconColor = com.example.ui.theme.TextSecondary,
                        unselectedTextColor = com.example.ui.theme.TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_history_tab")
                )

                // Item 3: Phieu luong
                NavigationBarItem(
                    selected = currentTab == "payslip",
                    onClick = {
                        currentTab = "payslip"
                        tabNavController.navigate("payslip") {
                            popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Payments, contentDescription = "Phiếu lương", modifier = Modifier.size(22.dp)) },
                    label = { Text("Phiếu lương", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = White,
                        selectedTextColor = com.example.ui.theme.PrimaryBlue,
                        indicatorColor = com.example.ui.theme.PrimaryBlue,
                        unselectedIconColor = com.example.ui.theme.TextSecondary,
                        unselectedTextColor = com.example.ui.theme.TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_payslip_tab")
                )

                // Item 4: Cai dat
                NavigationBarItem(
                    selected = currentTab == "settings",
                    onClick = {
                        currentTab = "settings"
                        tabNavController.navigate("settings") {
                            popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Cài đặt", modifier = Modifier.size(22.dp)) },
                    label = { Text("Cài đặt", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = White,
                        selectedTextColor = com.example.ui.theme.PrimaryBlue,
                        indicatorColor = com.example.ui.theme.PrimaryBlue,
                        unselectedIconColor = com.example.ui.theme.TextSecondary,
                        unselectedTextColor = com.example.ui.theme.TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_settings_tab")
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = {},
                    onNavigateToNotifications = {
                        tabNavController.navigate("notifications")
                    }
                )
            }
            composable("history") {
                HistoryScreen(viewModel = viewModel)
            }
            composable("payslip") {
                PayslipScreen(viewModel = viewModel)
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToAdmin = {
                        currentTab = "admin"
                        tabNavController.navigate("admin")
                    }
                )
            }
            composable("admin") {
                AdminScreen(onBack = {
                    currentTab = "settings"
                    val popped = tabNavController.popBackStack("settings", false)
                    if (!popped) {
                        tabNavController.popBackStack()
                    }
                })
            }
            composable("notifications") {
                NotificationCenterScreen(
                    viewModel = viewModel,
                    onBack = {
                        tabNavController.popBackStack()
                    }
                )
            }
        }
    }
}
