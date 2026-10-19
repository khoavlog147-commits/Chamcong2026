package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NeonBlue
import com.example.viewmodel.TimeSnapViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: TimeSnapViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var name by remember { mutableStateFlowOf("") }
    var email by remember { mutableStateFlowOf("") }
    var maNhanVien by remember { mutableStateFlowOf("") }
    var companyCode by remember { mutableStateFlowOf("") }
    var password by remember { mutableStateFlowOf("") }
    var confirmPassword by remember { mutableStateFlowOf("") }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "App Logo",
                tint = NeonBlue,
                modifier = Modifier
                    .size(60.dp)
                    .padding(bottom = 8.dp)
            )

            Text(
                text = "Tạo tài khoản mới",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "TimeSnap Pro - Cá nhân hóa lương & chấm công",
                color = MaterialTheme.colorScheme.onSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { 
                    name = it
                    errorMessage = null
                },
                label = { Text("Họ và tên") },
                leadingIcon = { Icon(Icons.Default.Person, "User Icon", tint = NeonBlue) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("name_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = NeonBlue,
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.3f),
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            // Mã nhân viên
            OutlinedTextField(
                value = maNhanVien,
                onValueChange = { 
                    maNhanVien = it
                    errorMessage = null
                },
                label = { Text("Mã nhân viên (Dùng làm tên đăng nhập)") },
                leadingIcon = { Icon(Icons.Default.Person, "Badge Icon", tint = NeonBlue) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("ma_nhan_vien_register_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = NeonBlue,
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.3f),
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            // Mã công ty (Tùy chọn)
            OutlinedTextField(
                value = companyCode,
                onValueChange = { 
                    companyCode = it
                    errorMessage = null
                },
                label = { Text("Mã công ty (Để trống nếu dùng mặc định)") },
                leadingIcon = { Icon(Icons.Default.Work, "Company Icon", tint = NeonBlue) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("company_code_register_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = NeonBlue,
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.3f),
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    errorMessage = null
                },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, "Email Icon", tint = NeonBlue) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("email_register_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = NeonBlue,
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.3f),
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    errorMessage = null
                },
                label = { Text("Mật khẩu (tối thiểu 6 ký tự)") },
                leadingIcon = { Icon(Icons.Default.Lock, "Lock Icon", tint = NeonBlue) },
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(icon, "Toggle Password")
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("password_register_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = NeonBlue,
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.3f),
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            // Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                    errorMessage = null
                },
                label = { Text("Nhập lại mật khẩu") },
                leadingIcon = { Icon(Icons.Default.Lock, "Lock Icon", tint = NeonBlue) },
                trailingIcon = {
                    val icon = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(icon, "Toggle Confirm Password")
                    }
                },
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .testTag("confirm_password_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = NeonBlue,
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.3f),
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            // Submit
            Button(
                onClick = {
                    when {
                        name.isBlank() || email.isBlank() || maNhanVien.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                            errorMessage = "Vui lòng nhập đầy đủ tất cả các trường dữ liệu!"
                        }
                        !email.trim().lowercase().endsWith("@gmail.com") -> {
                            errorMessage = "Đăng ký bắt buộc sử dụng địa chỉ Gmail (@gmail.com)!"
                        }
                        password.length < 6 -> {
                            errorMessage = "Mật khẩu phải chứa ít nhất 6 ký tự!"
                        }
                        password != confirmPassword -> {
                            errorMessage = "Mật khẩu xác nhận không trùng khớp!"
                        }
                        else -> {
                            keyboardController?.hide()
                            isLoading = true
                            coroutineScope.launch {
                                viewModel.authController.registerWithEmail(
                                    email = email,
                                    password = password,
                                    name = name,
                                    maNhanVien = maNhanVien,
                                    onSuccess = {
                                        isLoading = false
                                        viewModel.triggerSync()
                                        onRegisterSuccess()
                                    },
                                    onError = { err ->
                                        isLoading = false
                                        errorMessage = err
                                    }
                                )
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("TẠO TÀI KHOẢN", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("Đã có tài khoản? Đăng nhập ngay", color = NeonBlue, fontSize = 13.sp)
            }
        }
    }
}
