package com.example.auth

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.example.data.model.CompanyConfig
import com.example.data.FirestoreService
import com.example.data.repository.TimeRepository
import com.example.data.repository.CloudSyncManager
import com.example.data.repository.RegisteredUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UserSession(
    val uid: String,
    val email: String,
    val displayName: String
)

class AuthController(
    private val context: Context,
    private val repository: TimeRepository
) {
    private val firebaseAuth: com.google.firebase.auth.FirebaseAuth? = try {
        val app = try {
            FirebaseApp.getInstance()
        } catch (e: Exception) {
            FirebaseApp.initializeApp(context)
        }
        if (app != null) com.google.firebase.auth.FirebaseAuth.getInstance(app) else null
    } catch (e: Throwable) {
        Log.w("AuthController", "Firebase Auth not active or initialized: ${e.message}")
        null
    }
    private val cloudSyncManager = CloudSyncManager(context)
    
    private val _currentUserFlow = MutableStateFlow<UserSession?>(null)
    val currentUserFlow: StateFlow<UserSession?> = _currentUserFlow

    init {
        initializeSession()
    }

    private fun initializeSession() {
        val sharedPrefs = context.getSharedPreferences("timesnap_auth", Context.MODE_PRIVATE)
        val isLoggedOut = sharedPrefs.getBoolean("manually_logged_out", false)

        val user = firebaseAuth?.currentUser
        if (user != null) {
            _currentUserFlow.value = UserSession(
                uid = user.uid,
                email = user.email ?: "",
                displayName = user.displayName ?: "Employee"
            )
        } else if (!isLoggedOut) {
            val lastUid = sharedPrefs.getString("last_logged_in_uid", null)
            val lastEmail = sharedPrefs.getString("last_logged_in_email", null)
            val lastName = sharedPrefs.getString("last_logged_in_name", null)

            if (lastUid != null && lastEmail != null && lastName != null) {
                _currentUserFlow.value = UserSession(
                    uid = lastUid,
                    email = lastEmail,
                    displayName = lastName
                )
            } else {
                _currentUserFlow.value = null
            }
        } else {
            _currentUserFlow.value = null
        }
    }

    val currentUid: String?
        get() = _currentUserFlow.value?.uid

    private fun isPlaceholderFirebaseConfig(): Boolean {
        return try {
            val options = FirebaseApp.getInstance().options
            val pid = options.projectId.orEmpty().lowercase()
            val key = options.apiKey.lowercase()
            pid.contains("placeholder") || key.contains("placeholder")
        } catch (e: Exception) {
            true
        }
    }

    val isFirebaseEnabled: Boolean
        get() = firebaseAuth != null && !isPlaceholderFirebaseConfig()

    private fun resolveLocalEmailForInput(sharedPrefs: android.content.SharedPreferences, cleanInput: String): String {
        sharedPrefs.getString("email_of_employee_$cleanInput", null)?.let { return it.trim().lowercase() }
        val lowerInput = cleanInput.lowercase()
        for ((key, value) in sharedPrefs.all) {
            if (key.startsWith("email_of_employee_") && key.removePrefix("email_of_employee_").equals(cleanInput, ignoreCase = true)) {
                (value as? String)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it.lowercase() }
            }
        }
        return if (cleanInput.contains("@")) lowerInput else cleanInput
    }

    private fun formatFirebaseAuthError(exception: Exception?, email: String): String {
        val raw = exception?.localizedMessage ?: ""
        return when {
            raw.contains("API key not valid", ignoreCase = true) ||
                raw.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                "APK đang dùng cấu hình Firebase chưa hợp lệ (thiếu Secret GOOGLE_SERVICES_JSON đúng của server trên GitHub). Vui lòng gắn đúng google-services.json khi build lại APK."
            exception is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
                "Tài khoản '$email' không tồn tại trên server Firebase hiện tại!"
            exception is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                "Email hoặc mật khẩu không chính xác!"
            exception is com.google.firebase.FirebaseNetworkException ->
                "Lỗi kết nối mạng tới server. Vui lòng kiểm tra Wi-Fi/4G và thử lại!"
            raw.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) ||
                raw.contains("blocked all requests", ignoreCase = true) ->
                "Bạn đã thử đăng nhập sai quá nhiều lần. Vui lòng đợi ít phút rồi thử lại!"
            raw.isNotBlank() -> raw
            else -> "Tên đăng nhập hoặc mật khẩu không chính xác!"
        }
    }

    suspend fun registerWithEmail(
        email: String,
        password: String,
        name: String,
        maNhanVien: String,
        companyCode: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanMaNhanVien = maNhanVien.trim()
        val cleanCompanyCode = companyCode.trim()
        val sharedPrefs = context.getSharedPreferences("timesnap_auth", Context.MODE_PRIVATE)

        val company = if (cleanCompanyCode.isNotEmpty()) {
            FirestoreService.getCompanyByCode(cleanCompanyCode) ?: CompanyConfig.DEFAULT_COMPANY
        } else {
            CompanyConfig.DEFAULT_COMPANY
        }

        val auth = firebaseAuth
        if (auth == null || isPlaceholderFirebaseConfig()) {
            withContext(Dispatchers.Main) {
                onError("APK chưa được cấu hình Firebase server hợp lệ (thiếu google-services.json). Không thể đăng ký tài khoản lên server!")
            }
            return@withContext
        }

        try {
            auth.createUserWithEmailAndPassword(cleanEmail, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        if (user != null) {
                            user.updateProfile(
                                com.google.firebase.auth.userProfileChangeRequest {
                                    displayName = name
                                }
                            ).addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            repository.insertDefaultConfig(user.uid, name, cleanEmail, cleanMaNhanVien, company)

                                            val oldUid = sharedPrefs.getString("last_logged_in_uid", "local_user") ?: "local_user"
                                            val editor = sharedPrefs.edit()
                                                .putString("email_of_employee_$cleanMaNhanVien", cleanEmail)
                                                .putString("maNhanVien_of_email_$cleanEmail", cleanMaNhanVien)
                                                .putBoolean("manually_logged_out", false)
                                                .putString("last_logged_in_uid", user.uid)
                                                .putString("last_logged_in_email", user.email ?: cleanEmail)
                                                .putString("last_logged_in_name", name)
                                            if (oldUid.startsWith("local") || oldUid == "local_user") {
                                                editor.putString("pending_migration_guest_uid", oldUid)
                                            }
                                            editor.apply()

                                            val session = UserSession(
                                                uid = user.uid,
                                                email = user.email ?: cleanEmail,
                                                displayName = name
                                            )
                                            _currentUserFlow.value = session

                                            withContext(Dispatchers.Main) {
                                                onSuccess()
                                            }
                                        } catch (ex: Exception) {
                                            withContext(Dispatchers.Main) {
                                                onError("Lỗi lưu cấu hình: ${ex.localizedMessage ?: "Không xác định"}")
                                            }
                                        }
                                    }
                                } else {
                                    onError(profileTask.exception?.localizedMessage ?: "Không thể cập nhật tên hiển thị.")
                                }
                            }
                        } else {
                            onError("Không thể khởi tạo phiên làm việc của user.")
                        }
                    } else {
                        val exception = task.exception
                        val userErrMsg = if (exception is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                            "Địa chỉ Gmail '$cleanEmail' đã đăng ký trước đó! Vui lòng chọn Đăng nhập."
                        } else {
                            formatFirebaseAuthError(exception, cleanEmail)
                        }
                        onError(userErrMsg)
                    }
                }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e.localizedMessage ?: "Xảy ra ngoại lệ khi đăng ký tài khoản!")
            }
        }
    }

    suspend fun loginWithEmail(
        usernameOrEmail: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val cleanInput = usernameOrEmail.trim()
        val sharedPrefs = context.getSharedPreferences("timesnap_auth", Context.MODE_PRIVATE)

        val auth = firebaseAuth
        if (auth == null) {
            withContext(Dispatchers.Main) {
                onError("APK chưa được tích hợp cấu hình Firebase (thiếu google-services.json khi build), nên không thể kết nối tới tài khoản server!")
            }
            return@withContext
        }
        if (isPlaceholderFirebaseConfig()) {
            withContext(Dispatchers.Main) {
                onError("APK đang dùng google-services.json mẫu (chưa gắn Secret GOOGLE_SERVICES_JSON thật của server trên GitHub), nên không thể đăng nhập tài khoản cũ!")
            }
            return@withContext
        }

        var email = resolveLocalEmailForInput(sharedPrefs, cleanInput)
        if (!email.contains("@")) {
            val remoteResolved = FirestoreService.resolveEmailByEmployeeCode(cleanInput)
            if (!remoteResolved.isNullOrBlank()) {
                email = remoteResolved
            }
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            withContext(Dispatchers.Main) {
                onError("Không tìm thấy mã nhân viên '$cleanInput' trên thiết bị này. Lần đầu đăng nhập trên máy mới vui lòng nhập Email đã đăng ký trên server!")
            }
            return@withContext
        }

        try {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        if (user != null) {
                            val normalizedEmail = (user.email ?: email).trim().lowercase()
                            val maNhanVien = sharedPrefs.getString("maNhanVien_of_email_$normalizedEmail", "")
                                ?: sharedPrefs.getString("maNhanVien_of_email_$email", "")
                                ?: ""
                            val session = UserSession(
                                uid = user.uid,
                                email = normalizedEmail,
                                displayName = user.displayName ?: "User"
                            )
                            _currentUserFlow.value = session
                            val oldUid = sharedPrefs.getString("last_logged_in_uid", "local_user") ?: "local_user"
                            val editor = sharedPrefs.edit()
                                .putBoolean("manually_logged_out", false)
                                .putString("last_logged_in_uid", session.uid)
                                .putString("last_logged_in_email", session.email)
                                .putString("last_logged_in_name", session.displayName)
                            if (maNhanVien.isNotBlank()) {
                                editor.putString("email_of_employee_$maNhanVien", normalizedEmail)
                                editor.putString("maNhanVien_of_email_$normalizedEmail", maNhanVien)
                            }
                            if (oldUid.startsWith("local") || oldUid == "local_user") {
                                editor.putString("pending_migration_guest_uid", oldUid)
                            }
                            editor.apply()

                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    repository.insertDefaultConfig(user.uid, user.displayName ?: "User", normalizedEmail, maNhanVien)
                                } catch (dbEx: Exception) {
                                    Log.e("AuthController", "Failed to write default config during signin", dbEx)
                                }
                            }
                            onSuccess()
                        } else {
                            onError("Không thể lấy thông tin người dùng từ máy chủ.")
                        }
                    } else {
                        onError(formatFirebaseAuthError(task.exception, email))
                    }
                }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e.localizedMessage ?: "Ngoại lệ kết nối máy chủ Google Firebase!")
            }
        }
    }

    suspend fun sendPasswordResetEmail(
        usernameOrEmail: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val cleanInput = usernameOrEmail.trim()
        val sharedPrefs = context.getSharedPreferences("timesnap_auth", Context.MODE_PRIVATE)

        val auth = firebaseAuth
        if (auth == null || isPlaceholderFirebaseConfig()) {
            withContext(Dispatchers.Main) {
                onError("Tính năng đặt lại mật khẩu không khả dụng vì APK chưa gắn cấu hình Firebase server hợp lệ.")
            }
            return@withContext
        }

        var email = resolveLocalEmailForInput(sharedPrefs, cleanInput)
        if (!email.contains("@") && cleanInput.isNotEmpty()) {
            val remoteResolved = FirestoreService.resolveEmailByEmployeeCode(cleanInput)
            if (!remoteResolved.isNullOrBlank()) {
                email = remoteResolved
            }
        }

        if (email.isEmpty()) {
            withContext(Dispatchers.Main) {
                onError("Vui lòng điền Mã nhân viên hoặc Email để gửi yêu cầu khôi phục!")
            }
            return@withContext
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            withContext(Dispatchers.Main) {
                onError("Vui lòng nhập Email đã đăng ký trên server để khôi phục mật khẩu.")
            }
            return@withContext
        }

        try {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        onError(formatFirebaseAuthError(task.exception, email))
                    }
                }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e.localizedMessage ?: "Ngoại lệ khôi phục mật khẩu!")
            }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.e("AuthController", "Error during FirebaseAuth signOut", e)
        }
        
        _currentUserFlow.value = null
        context.getSharedPreferences("timesnap_auth", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("manually_logged_out", true)
            .remove("last_logged_in_uid")
            .remove("last_logged_in_email")
            .remove("last_logged_in_name")
            .apply()
        onComplete()
    }
}
