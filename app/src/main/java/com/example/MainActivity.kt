package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.DatabaseHelper
import com.example.notification.NotificationHelper
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.GlobalAiAssistantWidget
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NotificationCenterScreen
import com.example.ui.screens.PayslipScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkContainer
import com.example.ui.theme.MediumGray
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.TimeSnapTheme
import com.example.ui.theme.White
import com.example.viewmodel.TimeSnapViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DatabaseHelper.init(applicationContext)
        NotificationHelper.createNotificationChannel(applicationContext)
        setContent {
            TimeSnapTheme {
                TimeSnapApp()
            }
        }
    }
}

private data class MainNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
private fun TimeSnapApp(
    viewModel: TimeSnapViewModel = viewModel()
) {
    val context = LocalContext.current
    val userSession by viewModel.currentUserSession.collectAsStateWithLifecycle()

    var showSplash by rememberSaveable { mutableStateOf(true) }
    var authScreen by rememberSaveable { mutableStateOf("login") }
    var currentTab by rememberSaveable { mutableStateOf("home") }
    var overlayScreen by rememberSaveable { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(userSession) {
        if (userSession != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val tabs = listOf(
        MainNavTab(
            route = "home",
            label = "Trang chủ",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "nav_home"
        ),
        MainNavTab(
            route = "history",
            label = "Lịch sử",
            selectedIcon = Icons.Filled.CalendarMonth,
            unselectedIcon = Icons.Outlined.CalendarMonth,
            testTag = "nav_history"
        ),
        MainNavTab(
            route = "payslip",
            label = "Phiếu lương",
            selectedIcon = Icons.Filled.ReceiptLong,
            unselectedIcon = Icons.Outlined.ReceiptLong,
            testTag = "nav_payslip"
        ),
        MainNavTab(
            route = "settings",
            label = "Cài đặt",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            testTag = "nav_settings"
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        when {
            showSplash -> {
                SplashScreen(
                    onSplashFinished = { showSplash = false }
                )
            }

            userSession == null -> {
                if (authScreen == "register") {
                    BackHandler { authScreen = "login" }
                    RegisterScreen(
                        viewModel = viewModel,
                        onNavigateToLogin = { authScreen = "login" },
                        onRegisterSuccess = {
                            authScreen = "login"
                            currentTab = "home"
                            overlayScreen = null
                        }
                    )
                } else {
                    LoginScreen(
                        viewModel = viewModel,
                        onNavigateToRegister = { authScreen = "register" },
                        onLoginSuccess = {
                            currentTab = "home"
                            overlayScreen = null
                        }
                    )
                }
            }

            overlayScreen == "admin" -> {
                AdminScreen(
                    onBack = { overlayScreen = null }
                )
            }

            overlayScreen == "notifications" -> {
                BackHandler { overlayScreen = null }
                NotificationCenterScreen(
                    viewModel = viewModel,
                    onBack = { overlayScreen = null }
                )
            }

            else -> {
                if (currentTab != "home") {
                    BackHandler { currentTab = "home" }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkContainer,
                            windowInsets = NavigationBarDefaults.windowInsets
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentTab == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab.route },
                                    modifier = Modifier.testTag(tab.testTag),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NeonBlue,
                                        selectedTextColor = NeonBlue,
                                        indicatorColor = NeonBlue.copy(alpha = 0.16f),
                                        unselectedIconColor = MediumGray,
                                        unselectedTextColor = MediumGray
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            "home" -> HomeScreen(
                                viewModel = viewModel,
                                onNavigateToLogin = {
                                    overlayScreen = null
                                    authScreen = "login"
                                },
                                onNavigateToNotifications = {
                                    overlayScreen = "notifications"
                                }
                            )
                            "history" -> HistoryScreen(viewModel = viewModel)
                            "payslip" -> PayslipScreen(viewModel = viewModel)
                            "settings" -> com.example.ui.screens.SettingsScreen(
                                viewModel = viewModel,
                                onNavigateToAdmin = {
                                    overlayScreen = "admin"
                                }
                            )
                        }
                    }
                }

                GlobalAiAssistantWidget(
                    currentTab = currentTab,
                    viewModel = viewModel,
                    onNavigateTab = { target ->
                        if (target in listOf("home", "history", "payslip", "settings")) {
                            currentTab = target
                        }
                    }
                )
            }
        }
    }
}
