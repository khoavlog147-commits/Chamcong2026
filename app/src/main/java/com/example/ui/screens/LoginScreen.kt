package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NeonBlue
import com.example.viewmodel.TimeSnapViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: TimeSnapViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("timesnap_auth", android.content.Context.MODE_PRIVATE) }
    
    val savedRememberMe = remember { sharedPrefs.getBoolean("remember_me", false) }
    val savedEmail = remember { if (savedRememberMe) sharedPrefs.getString("saved_email", "") ?: "" else "" }
    val savedPassword = remember { if (savedRememberMe) sharedPrefs.getString("saved_password", "") ?: "" else "" }

    val appVersionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "2.2"
        } catch (e: Exception) {
            "2.2"
        }
    }

    var email by remember { mutableStateFlowOf(savedEmail) }
    var password by remember { mutableStateFlowOf(savedPassword) }
    var rememberMe by remember { mutableStateOf(savedRememberMe) }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isEmailFocused by remember { mutableStateOf(false) }
    var isPasswordFocused by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Full screen dusk building background image
        Image(
            painter = painterResource(id = R.drawable.img_login_bg),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark gradient overlay for readability and premium tone
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B0F17).copy(alpha = 0.65f),
                            Color(0xFF070A0F).copy(alpha = 0.92f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Spacer to push the main login box to center
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Nested concentric rings logo (matches screenshot)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.25f), shape = CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.5f), shape = CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF2563EB).copy(alpha = 0.15f), shape = CircleShape)
                                .border(2.dp, Color(0xFF2563EB), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "App Logo",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Title & Subtitle with increased whitespace
                Text(
                    text = "TimeSnap Pro",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Quản lý chấm công & tiền lương",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Main semi-transparent glassmorphic login card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF1F2937).copy(alpha = 0.6f)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF111827).copy(alpha = 0.85f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (errorMessage != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (successMessage != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Text(
                                    text = successMessage ?: "",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Username Text Field with normal/focused borders
                        OutlinedTextField(
                            value = email,
                            onValueChange = { 
                                email = it
                                errorMessage = null
                                successMessage = null
                            },
                            label = { Text("Mã nhân viên / Email") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Person, 
                                    contentDescription = "User Icon", 
                                    tint = if (isEmailFocused) Color(0xFF4F8EF7) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                ) 
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .onFocusChanged { isEmailFocused = it.isFocused }
                                .testTag("email_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedLabelColor = Color(0xFF4F8EF7),
                                unfocusedLabelColor = Color(0xFF94A3B8),
                                focusedBorderColor = Color(0xFF4F8EF7),
                                unfocusedBorderColor = Color(0xFF4B5563),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f),
                                unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f)
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )

                        // Password Text Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { 
                                password = it
                                errorMessage = null
                                successMessage = null
                            },
                            label = { Text("Mật khẩu") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Lock, 
                                    contentDescription = "Lock Icon", 
                                    tint = if (isPasswordFocused) Color(0xFF4F8EF7) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                ) 
                            },
                            trailingIcon = {
                                val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(icon, "Toggle Password", tint = Color(0xFF6B7280), modifier = Modifier.size(18.dp))
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .onFocusChanged { isPasswordFocused = it.isFocused }
                                .testTag("password_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedLabelColor = Color(0xFF4F8EF7),
                                unfocusedLabelColor = Color(0xFF94A3B8),
                                focusedBorderColor = Color(0xFF4F8EF7),
                                unfocusedBorderColor = Color(0xFF4B5563),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f),
                                unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f)
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        // Remember Account checkbox
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF2563EB),
                                    uncheckedColor = Color(0xFF4B5563),
                                    checkmarkColor = Color.White
                                ),
                                modifier = Modifier.testTag("remember_me_checkbox")
                            )
                            Text(
                                text = "Ghi nhớ tài khoản",
                                color = Color(0xFFE2E8F0),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        // Gradient Login Button
                        val buttonGradient = if (isLoading) {
                            Brush.linearGradient(colors = listOf(Color(0xFF4B5563), Color(0xFF374151)))
                        } else {
                            Brush.linearGradient(colors = listOf(Color(0xFF4F8EF7), Color(0xFF2D6CDF)))
                        }

                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!"
                                    return@Button
                                }
                                keyboardController?.hide()
                                isLoading = true
                                coroutineScope.launch {
                                    // Professional UX transition delay as requested
                                    delay(800)
                                    viewModel.authController.loginWithEmail(
                                        usernameOrEmail = email,
                                        password = password,
                                        onSuccess = {
                                            sharedPrefs.edit().apply {
                                                putBoolean("remember_me", rememberMe)
                                                if (rememberMe) {
                                                    putString("saved_email", email)
                                                    putString("saved_password", password)
                                                } else {
                                                    remove("saved_email")
                                                    remove("saved_password")
                                                }
                                                apply()
                                            }
                                            isLoading = false
                                            onLoginSuccess()
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        }
                                    )
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .background(brush = buttonGradient, shape = RoundedCornerShape(16.dp))
                                .testTag("login_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text(
                                        text = "ĐĂNG NHẬP",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Elegant links with lock/person icon and divider (matches screenshot)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quên mật khẩu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(enabled = !isLoading) {
                                val targetEmailOrUsername = email.trim()
                                if (targetEmailOrUsername.isEmpty()) {
                                    errorMessage = "Vui lòng nhập Mã nhân viên hoặc Email vào ô trên, sau đó bấm 'Quên mật khẩu?' để nhận thư khôi phục!"
                                    successMessage = null
                                    return@clickable
                                }
                                errorMessage = null
                                successMessage = null
                                isLoading = true
                                coroutineScope.launch {
                                    delay(600)
                                    viewModel.authController.sendPasswordResetEmail(
                                        usernameOrEmail = targetEmailOrUsername,
                                        onSuccess = {
                                            isLoading = false
                                            successMessage = "Hệ thống đã gửi một Email khôi phục mật khẩu thành công. Vui lòng kiểm tra hộp thư của bạn!"
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        }
                                    )
                                }
                            }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Forgot Password Icon",
                            tint = Color(0xFF4F8EF7),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Quên mật khẩu?",
                            color = Color(0xFF4F8EF7),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .width(1.dp)
                            .background(Color(0xFF374151))
                            .padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Đăng ký ngay
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onNavigateToRegister() }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Register Icon",
                            tint = Color(0xFF4F8EF7),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Đăng ký ngay",
                            color = Color(0xFF4F8EF7),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Footer with copyright and version info (matches screenshot)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = "© 2026 TimeSnap Pro",
                    color = Color(0xFF94A3B8).copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Phiên bản $appVersionName",
                    color = Color(0xFF6B7280).copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

// HELPER FOR COMPOSE MUTABLESTATEFLOW STATE COUPLING
fun <T> rememberStateFlow(initial: T): MutableState<T> {
    return mutableStateOf(initial)
}

fun mutableStateFlowOf(value: String): MutableState<String> {
    return mutableStateOf(value)
}
