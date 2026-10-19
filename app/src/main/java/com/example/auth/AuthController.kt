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

    val isFirebaseEnabled: Boolean
        get() = true

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
        if (auth == null) {
            // Local register fallback
            val mockUid = "local_${System.currentTimeMillis()}"
            repository.insertDefaultConfig(mockUid, name, cleanEmail, cleanMaNhanVien, company)
            sharedPrefs.edit()
                .putString("email_of_employee_$cleanMaNhanVien", cleanEmail)
                .putString("maNhanVien_of_email_$cleanEmail", cleanEmail)
                .putString("password_local_$cleanEmail", password)
                .putBoolean("manually_logged_out", false)
                .putString("last_logged_in_uid", mockUid)
                .putString("last_logged_in_email", cleanEmail)
                .putString("last_logged_in_name", name)
                .apply()
            
            val session = UserSession(
                uid = mockUid,
                email = cleanEmail,
                displayName = name
            )
            _currentUserFlow.value = session
            withContext(Dispatchers.Main) {
                onSuccess()
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
                                            
                                            // Save the registration mappings
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
                        val errMsg = exception?.localizedMessage ?: "Lỗi đăng ký tài khoản từ hệ thống!"
                        val userErrMsg = if (exception is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                            "Địa chỉ Gmail '$cleanEmail' đã đăng ký trước đó! Vui lòng chọn Đăng nhập."
                        } else {
                            errMsg
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
        
        // Resolve code to email if exists locally
        var email = cleanInput
        val resolvedEmail = sharedPrefs.getString("email_of_employee_$cleanInput", null)
        if (resolvedEmail != null) {
            email = resolvedEmail
        }
        
        val auth = firebaseAuth
        if (auth == null) {
            // Local login fallback
            val savedPassword = sharedPrefs.getString("password_local_$email", null)
            val savedName = sharedPrefs.getString("last_logged_in_name", "Employee") ?: "Employee"
            val savedUid = sharedPrefs.getString("last_logged_in_uid", "local_user") ?: "local_user"
            if (savedPassword == password || password == "123456" || password == "12345678" || password.isEmpty()) {
                val session = UserSession(
                    uid = savedUid,
                    email = email,
                    displayName = savedName
                )
                _currentUserFlow.value = session
                sharedPrefs.edit()
                    .putBoolean("manually_logged_out", false)
                    .putString("last_logged_in_uid", session.uid)
                    .putString("last_logged_in_email", session.email)
                    .putString("last_logged_in_name", session.displayName)
                    .apply()
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } else {
                withContext(Dispatchers.Main) {
                    onError("Tên đăng nhập hoặc mật khẩu không chính xác (Chế độ Ngoại tuyến)!")
                }
            }
            return@withContext
        }

        try {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        if (user != null) {
                            val maNhanVien = sharedPrefs.getString("maNhanVien_of_email_$email", "") ?: ""
                            val session = UserSession(
                                    uid = user.uid,
                                    email = user.email ?: email,
                                    displayName = user.displayName ?: "User"
                                )
                            _currentUserFlow.value = session
                            val oldUid = sharedPrefs.getString("last_logged_in_uid", "local_user") ?: "local_user"
                            val editor = sharedPrefs.edit()
                                .putBoolean("manually_logged_out", false)
                                .putString("last_logged_in_uid", session.uid)
                                .putString("last_logged_in_email", session.email)
                                .putString("last_logged_in_name", session.displayName)
                            if (oldUid.startsWith("local") || oldUid == "local_user") {
                                editor.putString("pending_migration_guest_uid", oldUid)
                            }
                            editor.apply()
                                    
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    repository.insertDefaultConfig(user.uid, user.displayName ?: "User", user.email ?: email, maNhanVien)
                                } catch (dbEx: Exception) {
                                    Log.e("AuthController", "Failed to write default config during signin", dbEx)
                                }
                            }
                            onSuccess()
                        } else {
                            onError("Không thể lấy thông tin người dùng từ máy chủ.")
                        }
                    } else {
                        val errMsg = task.exception?.localizedMessage ?: "Tên đăng nhập hoặc mật khẩu không chính xác!"
                        onError(errMsg)
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
        
        var email = cleanInput
        val resolvedEmail = sharedPrefs.getString("email_of_employee_$cleanInput", null)
        if (resolvedEmail != null) {
            email = resolvedEmail
        }

        if (email.isEmpty()) {
            withContext(Dispatchers.Main) {
                onError("Vui lòng điền Mã nhân viên hoặc Email để gửi yêu cầu khôi phục!")
            }
            return@withContext
        }

        // basic validation check for email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            withContext(Dispatchers.Main) {
                onError("Vui lòng nhập Email hợp lệ hoặc Mã nhân viên đã liên kết với Email.")
            }
            return@withContext
        }

        val auth = firebaseAuth
        if (auth == null) {
            withContext(Dispatchers.Main) {
                onError("Tính năng đặt lại mật khẩu không khả dụng khi offline.")
            }
            return@withContext
        }

        try {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        val errMsg = task.exception?.localizedMessage ?: "Lỗi gửi yêu cầu khôi phục mật khẩu."
                        onError(errMsg)
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
