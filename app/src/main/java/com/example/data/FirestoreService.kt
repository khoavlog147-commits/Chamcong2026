package com.example.data

import android.util.Log
import com.example.data.model.CompanyConfig
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Helper extension function to safely await Google Task API in suspend coroutines
suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTaskFirestore(): T? = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (continuation.isActive) {
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: Exception("Unknown Firestore system error"))
            }
        }
    }
}

object FirestoreService {
    private const val TAG = "FirestoreService"

    fun isEmulator(): Boolean {
        val model = android.os.Build.MODEL
        val fingerprint = android.os.Build.FINGERPRINT
        val brand = android.os.Build.BRAND
        val device = android.os.Build.DEVICE
        val product = android.os.Build.PRODUCT
        val hardware = android.os.Build.HARDWARE
        return fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || (brand.startsWith("generic") && device.startsWith("generic"))
                || "google_sdk" == product
    }

    private fun getDb(): FirebaseFirestore? {
        if (isEmulator()) {
            return null
        }
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.e(TAG, "Firebase Firestore is not available in this configuration: ${e.message}")
            null
        }
    }

    private fun isDemoUser(uid: String): Boolean {
        return uid.startsWith("demo") || uid.contains("demo")
    }

    fun getUserConfigFlow(uid: String): Flow<com.example.data.model.UserConfig?> = callbackFlow {
        if (isDemoUser(uid)) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val firestore = getDb()
        if (firestore == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener1 = firestore.collection("users").document(uid).collection("salary_config").document("settings")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val config = snapshot.toUserSalaryConfig(uid)
                    trySend(config)
                }
            }
        val listener2 = firestore.collection("users").document(uid).collection("config").document("salary_settings")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val config = snapshot.toUserSalaryConfig(uid)
                    trySend(config)
                }
            }
        awaitClose {
            listener1.remove()
            listener2.remove()
        }
    }

    suspend fun saveUserConfig(config: UserConfig) {
        if (isDemoUser(config.uid)) return
        val firestore = getDb() ?: return
        val map = mapOf(
            "fullName" to config.fullName,
            "hourlyRate" to config.hourlyRate,
            "currency" to config.currency,
            "dailyTargetHours" to config.dailyTargetHours,
            "he_so_ot_dem" to config.he_so_ot_dem,
            "thoi_gian_ca_dem" to config.thoi_gian_ca_dem,
            "luong_co_ban" to config.luong_co_ban,
            "luong_bao_hiem" to config.luong_bao_hiem,
            "tile_bao_hiem" to config.tile_bao_hiem,
            "doan_phi_40k" to config.doan_phi_40k,
            "ngay_chot_luong" to config.ngay_chot_luong,
            "he_so_ot_normal" to config.he_so_ot_normal,
            "he_so_ot_sunday" to config.he_so_ot_sunday,
            "he_so_ot_holiday" to config.he_so_ot_holiday,
            "tien_chuyen_can" to config.tien_chuyen_can,
            "so_ngay_nghi_phep" to config.so_ngay_nghi_phep,
            "phu_cap_ky_thuat" to config.phu_cap_ky_thuat,
            "phu_cap_trach_nhiem" to config.phu_cap_trach_nhiem,
            "phu_cap_chuc_vu" to config.phu_cap_chuc_vu,
            "phu_cap_hieu_suat" to config.phu_cap_hieu_suat,
            "phu_cap_san_pham" to config.phu_cap_san_pham,
            "phu_cap_com_ca" to config.phu_cap_com_ca,
            "phu_cap_com_ot" to config.phu_cap_com_ot,
            "phu_cap_tham_nien" to config.phu_cap_tham_nien,
            "phu_cap_nha_o" to config.phu_cap_nha_o,
            "phu_cap_doc_hai" to config.phu_cap_doc_hai,
            "phu_cap_dien_thoai" to config.phu_cap_dien_thoai,
            "phu_cap_xang_xe" to config.phu_cap_xang_xe,
            "phu_cap_khac" to config.phu_cap_khac,
            "tong_tien_com" to config.tong_tien_com,
            "phu_cap" to config.phu_cap,
            "thuong" to config.thuong,
            "tien_khau_tru_nghi" to config.tien_khau_tru_nghi,
            "last_accumulated_month" to config.last_accumulated_month
        )
        firestore.collection("users").document(config.uid)
            .collection("config").document("salary_settings")
            .set(map, SetOptions.merge())
            .awaitTaskFirestore()
    }

    // Convert date string "dd/MM/yyyy" -> "yyyy-MM-dd" for secure chronological ordering & document IDs
    fun formatDateForDocId(dateStr: String): String {
        return try {
            val normalized = com.example.data.SalaryCalculator.normalizeDateToDmy(dateStr)
            val parts = normalized.split("/")
            if (parts.size == 3) {
                val dd = parts[0]
                val mm = parts[1]
                val yyyy = parts[2]
                "$yyyy-$mm-$dd"
            } else {
                normalized.replace("/", "-")
            }
        } catch (e: Exception) {
            dateStr.replace("/", "-")
        }
    }

    fun getAttendanceLogsFlow(uid: String): Flow<List<AttendanceRecord>> = callbackFlow {
        if (isDemoUser(uid)) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val firestore = getDb()
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val colRef = firestore.collection("users").document(uid).collection("attendance_logs")
        val listener = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to AttendanceLogs changes", error)
                trySend(emptyList())
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toAttendanceRecord(uid)
                }.sortedByDescending { it.clockInTime }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveAttendanceRecord(record: AttendanceRecord) {
        if (isDemoUser(record.uid)) return
        val firestore = getDb() ?: return
        val normalizedDateStr = com.example.data.SalaryCalculator.normalizeDateToDmy(record.dateString)
        val docId = formatDateForDocId(normalizedDateStr)
        val map = mapOf(
            "id" to record.id,
            "uid" to record.uid,
            "dateString" to normalizedDateStr,
            "clockInTime" to record.clockInTime,
            "clockOutTime" to record.clockOutTime,
            "status" to record.status,
            "notes" to record.notes
        )
        firestore.collection("users").document(record.uid)
            .collection("attendance_logs").document(docId)
            .set(map, SetOptions.merge())
            .awaitTaskFirestore()
    }

    suspend fun deleteAttendanceRecord(uid: String, dateString: String) {
        if (isDemoUser(uid)) return
        val firestore = getDb() ?: return
        val docId = formatDateForDocId(dateString)
        firestore.collection("users").document(uid)
            .collection("attendance_logs").document(docId)
            .delete()
            .awaitTaskFirestore()
    }

    suspend fun getAttendanceRecordForDate(uid: String, dateString: String): AttendanceRecord? {
        if (isDemoUser(uid)) return null
        val firestore = getDb() ?: return null
        return try {
            val docId = formatDateForDocId(dateString)
            val doc = firestore.collection("users").document(uid)
                .collection("attendance_logs").document(docId)
                .get()
                .awaitTaskFirestore()
            if (doc != null && doc.exists()) {
                doc.toAttendanceRecord(uid)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching record for date $dateString of user $uid: ${e.message}")
            null
        }
    }

    suspend fun getAllUserConfigs(): List<com.example.data.model.UserConfig> {
        val firestore = getDb() ?: return emptyList()
        val allConfigs = mutableListOf<com.example.data.model.UserConfig>()
        val processedIds = mutableSetOf<String>()

        // 1. Try New Structure: Collection group query for 'salary_config'
        try {
            val snapshot = firestore.collectionGroup("salary_config")
                .get()
                .awaitTaskFirestore()
            
            snapshot?.documents?.forEach { doc ->
                val userId = doc.reference.parent.parent?.id ?: return@forEach
                if (!processedIds.contains(userId)) {
                    val config = doc.toUserSalaryConfig(userId)
                    allConfigs.add(config)
                    processedIds.add(userId)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching collectionGroup salary_config: ${e.message}")
        }

        // 2. Try Old Structure: 'users_salary' collection
        try {
            val oldSnapshot = firestore.collection("users_salary")
                .get()
                .awaitTaskFirestore()
            
            oldSnapshot?.documents?.forEach { doc ->
                val userId = doc.id
                if (!processedIds.contains(userId)) {
                    val config = doc.toUserSalaryConfig(userId)
                    allConfigs.add(config)
                    processedIds.add(userId)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching old users_salary: ${e.message}")
        }
        
        return allConfigs
    }

    fun getAllCompaniesFlow(): Flow<List<CompanyConfig>> = callbackFlow {
        val firestore = getDb()
        if (firestore == null) {
            trySend(listOf(CompanyConfig.DEFAULT_COMPANY))
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("companies")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to companies", error)
                    trySend(listOf(CompanyConfig.DEFAULT_COMPANY))
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toCompanyConfig()
                    }.sortedBy { it.companyName }
                    if (list.none { it.companyId == "default_company" }) {
                        trySend(listOf(CompanyConfig.DEFAULT_COMPANY) + list)
                    } else {
                        trySend(list)
                    }
                } else {
                    trySend(listOf(CompanyConfig.DEFAULT_COMPANY))
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getAllCompanies(): List<CompanyConfig> {
        val firestore = getDb() ?: return listOf(CompanyConfig.DEFAULT_COMPANY)
        return try {
            val snapshot = firestore.collection("companies").get().awaitTaskFirestore()
            if (snapshot != null && !snapshot.isEmpty) {
                val list = snapshot.documents.mapNotNull { it.toCompanyConfig() }.sortedBy { it.companyName }
                if (list.none { it.companyId == "default_company" }) {
                    listOf(CompanyConfig.DEFAULT_COMPANY) + list
                } else {
                    list
                }
            } else {
                listOf(CompanyConfig.DEFAULT_COMPANY)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all companies: ${e.message}")
            listOf(CompanyConfig.DEFAULT_COMPANY)
        }
    }

    suspend fun getCompanyByCode(code: String): CompanyConfig? {
        val firestore = getDb() ?: return null
        return try {
            val snapshot = firestore.collection("companies")
                .whereEqualTo("companyCode", code.uppercase(Locale.ROOT))
                .get()
                .awaitTaskFirestore()
            if (snapshot != null && !snapshot.isEmpty) {
                snapshot.documents.firstOrNull()?.toCompanyConfig()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting company by code $code: ${e.message}")
            null
        }
    }

    suspend fun saveCompany(company: CompanyConfig): Boolean {
        val firestore = getDb() ?: return false
        val map = mapOf(
            "companyId" to company.companyId,
            "companyName" to company.companyName,
            "companyCode" to company.companyCode.uppercase(Locale.ROOT),
            "description" to company.description,
            "address" to company.address,
            "luongCoBan" to company.luongCoBan,
            "luongDongBaoHiem" to company.luongDongBaoHiem,
            "tiLeDongBaoHiem" to company.tiLeDongBaoHiem,
            "ngayChotLuong" to company.ngayChotLuong,
            "doanPhiCongDoan" to company.doanPhiCongDoan,
            "heSoOtNgayThuong" to company.heSoOtNgayThuong,
            "heSoOtChuNhat" to company.heSoOtChuNhat,
            "heSoOtNgayLe" to company.heSoOtNgayLe,
            "heSoOtDem" to company.heSoOtDem,
            "caDemStart" to company.caDemStart,
            "caDemEnd" to company.caDemEnd,
            "tienChuyenCanGoc" to company.tienChuyenCanGoc,
            "soNgayPhepNam" to company.soNgayPhepNam,
            "pcKyThuat" to company.pcKyThuat,
            "pcTrachNhiem" to company.pcTrachNhiem,
            "pcChucVu" to company.pcChucVu,
            "pcHieuSuat" to company.pcHieuSuat,
            "pcSanPham" to company.pcSanPham,
            "pcComCa" to company.pcComCa,
            "pcComOt" to company.pcComOt,
            "pcNhaO" to company.pcNhaO,
            "pcDocHai" to company.pcDocHai,
            "pcDtDoanhThu" to company.pcDtDoanhThu,
            "pcXangXe" to company.pcXangXe,
            "pcThamNien" to company.pcThamNien,
            "pcKhac1" to company.pcKhac1,
            "pcCaDem" to company.pcCaDem,
            "allowanceCalcTypes" to company.allowanceCalcTypes,
            "soGioNghiGiaiLao" to company.soGioNghiGiaiLao,
            "tinhKhauTruNghi" to company.tinhKhauTruNghi,
            "lichTrinh" to company.lichTrinh,
            "rolesData" to company.rolesData,
            "createdAt" to company.createdAt,
            "updatedAt" to System.currentTimeMillis()
        )
        return try {
            firestore.collection("companies").document(company.companyId)
                .set(map, SetOptions.merge())
                .awaitTaskFirestore()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving company ${company.companyId}: ${e.message}")
            false
        }
    }

    suspend fun deleteCompany(companyId: String): Boolean {
        if (companyId == "default_company") return false
        val firestore = getDb() ?: return false
        return try {
            firestore.collection("companies").document(companyId).delete().awaitTaskFirestore()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting company $companyId: ${e.message}")
            false
        }
    }

    suspend fun syncCompanyConfigToEmployees(company: CompanyConfig): Int {
        val allEmployees = getAllUserConfigs()
        val targetEmployees = allEmployees.filter { it.companyId == company.companyId || (company.companyId == "default_company" && (it.companyId.isBlank() || it.companyId == "default_company")) }
        var updatedCount = 0
        targetEmployees.forEach { emp ->
            try {
                val updatedConfig = company.applyToUserConfig(emp, overwriteCustomBaseSalary = false)
                saveUserSalaryConfigToFirestore(updatedConfig)
                updatedCount++
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing company config to employee ${emp.userId}: ${e.message}")
            }
        }
        return updatedCount
    }

    suspend fun getAttendanceLogsForUser(uid: String): List<AttendanceRecord> {
        val firestore = getDb() ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users").document(uid).collection("attendance_logs")
                .get()
                .awaitTaskFirestore()
            snapshot?.documents?.mapNotNull { doc ->
                val record = doc.toAttendanceRecord(uid)
                val correctDocId = formatDateForDocId(record.dateString)
                if (doc.id != correctDocId) {
                    // Delete the duplicate/legacy document ID so it is cleaned up from Firestore forever
                    Log.d(TAG, "Deleting legacy incorrect Firestore document ID: ${doc.id} (correct ID is $correctDocId)")
                    try {
                        firestore.collection("users").document(uid).collection("attendance_logs").document(doc.id)
                            .delete()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to delete legacy document ID ${doc.id}: ${e.message}")
                    }
                }
                record
            }?.sortedByDescending { it.clockInTime } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching attendance logs for $uid: ${e.message}")
            throw e
        }
    }

    suspend fun getAllAttendanceLogsInMonth(monthStr: String): List<AttendanceRecord> {
        val firestore = getDb() ?: return emptyList()
        return try {
            val snapshot = firestore.collectionGroup("attendance_logs")
                .get()
                .awaitTaskFirestore()
            snapshot?.documents?.mapNotNull { doc ->
                val uid = doc.reference.parent.parent?.id ?: return@mapNotNull null
                val record = doc.toAttendanceRecord(uid)
                val formattedDocDate = formatDateForDocId(record.dateString)
                val clockInMonth = if (record.clockInTime > 0) SimpleDateFormat("yyyy-MM", Locale.US).format(Date(record.clockInTime)) else ""
                
                if (record.dateString.contains(monthStr) || 
                    formattedDocDate.contains(monthStr) || 
                    doc.id.contains(monthStr) || 
                    clockInMonth == monthStr) {
                    record
                } else null
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all attendance logs in month $monthStr: ${e.message}")
            emptyList()
        }
    }

    fun getTodayAttendanceLogsFlow(): Flow<Map<String, AttendanceRecord>> = callbackFlow {
        val firestore = getDb()
        if (firestore == null) {
            trySend(emptyMap())
            close()
            return@callbackFlow
        }
        val listener = firestore.collectionGroup("attendance_logs")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to collectionGroup attendance_logs", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val cal = Calendar.getInstance()
                    val year = cal.get(Calendar.YEAR)
                    val month = cal.get(Calendar.MONTH) + 1
                    val day = cal.get(Calendar.DAY_OF_MONTH)

                    val todayYmd = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
                    val todayDmy = String.format(Locale.US, "%02d/%02d/%04d", day, month, year)
                    val todayShortDmy = String.format(Locale.US, "%d/%d/%04d", day, month, year)
                    val todayShortYmd = String.format(Locale.US, "%04d-%d-%d", year, month, day)
                    val todayDocId = formatDateForDocId(todayDmy)

                    val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
                    val yYear = calYesterday.get(Calendar.YEAR)
                    val yMonth = calYesterday.get(Calendar.MONTH) + 1
                    val yDay = calYesterday.get(Calendar.DAY_OF_MONTH)

                    val yesterdayYmd = String.format(Locale.US, "%04d-%02d-%02d", yYear, yMonth, yDay)
                    val yesterdayDmy = String.format(Locale.US, "%02d/%02d/%04d", yDay, yMonth, yYear)
                    val yesterdayShortDmy = String.format(Locale.US, "%d/%d/%04d", yDay, yMonth, yYear)
                    val yesterdayShortYmd = String.format(Locale.US, "%04d-%d-%d", yYear, yMonth, yDay)
                    val yesterdayDocId = formatDateForDocId(yesterdayDmy)

                    val todayRecords = mutableMapOf<String, AttendanceRecord>()
                    val yesterdayRecords = mutableMapOf<String, AttendanceRecord>()
                    for (doc in snapshot.documents) {
                        val uid = doc.reference.parent.parent?.id ?: continue
                        val rec = doc.toAttendanceRecord(uid)
                        
                        val ds = rec.dateString.trim()
                        val docId = doc.id
                        val clockInDmy = if (rec.clockInTime > 0) SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(rec.clockInTime)) else ""

                        val normalizedDs = com.example.data.SalaryCalculator.normalizeDateToDmy(ds)
                        val isToday = normalizedDs == todayDmy || docId == todayDocId || clockInDmy == todayDmy
                        val isYesterday = normalizedDs == yesterdayDmy || docId == yesterdayDocId || clockInDmy == yesterdayDmy

                        if (isToday) {
                            val existing = todayRecords[uid]
                            if (existing == null || rec.clockInTime > existing.clockInTime || (existing.clockInTime == 0L && rec.clockInTime > 0L)) {
                                todayRecords[uid] = rec
                            }
                        } else if (isYesterday) {
                            val existing = yesterdayRecords[uid]
                            if (existing == null || rec.clockInTime > existing.clockInTime || (existing.clockInTime == 0L && rec.clockInTime > 0L)) {
                                yesterdayRecords[uid] = rec
                            }
                        }
                    }

                    val map = mutableMapOf<String, AttendanceRecord>()
                    val allUids = todayRecords.keys + yesterdayRecords.keys
                    for (uid in allUids) {
                        val todayRec = todayRecords[uid]
                        val yesterdayRec = yesterdayRecords[uid]

                        if (yesterdayRec != null && yesterdayRec.clockInTime > 0L && (yesterdayRec.clockOutTime == null || yesterdayRec.clockOutTime == 0L)) {
                            if (todayRec != null && todayRec.clockInTime > 0L) {
                                map[uid] = todayRec
                            } else {
                                map[uid] = yesterdayRec
                            }
                        } else {
                            if (todayRec != null) {
                                map[uid] = todayRec
                            } else if (yesterdayRec != null) {
                                map[uid] = yesterdayRec
                            }
                        }
                    }
                    trySend(map)
                }
            }
        awaitClose { listener.remove() }
    }

    fun parseVersionToCode(versionStr: String): Long {
        val clean = versionStr.trim()
        if (clean.isEmpty()) return 0L
        
        // Remove any non-alphanumeric/dot characters just in case
        val filtered = clean.filter { it.isDigit() || it == '.' }
        val parts = filtered.split(".")
        if (parts.size == 1) {
            return filtered.toLongOrNull() ?: 0L
        }
        
        return try {
            val major = parts.getOrNull(0)?.toLongOrNull() ?: 0L
            val minor = parts.getOrNull(1)?.toLongOrNull() ?: 0L
            val patch = parts.getOrNull(2)?.toLongOrNull() ?: 0L
            major * 10000L + minor * 100L + patch
        } catch (e: java.lang.Exception) {
            0L
        }
    }

    fun isVersionNewer(codeA: Long, nameA: String, codeB: Long, nameB: String): Boolean {
        val cleanA = nameA.trim()
        val cleanB = nameB.trim()
        
        if (cleanA.isNotEmpty() && cleanB.isNotEmpty()) {
            val parsedA = parseVersionToCode(cleanA)
            val parsedB = parseVersionToCode(cleanB)
            if (parsedA > 0 && parsedB > 0) {
                return parsedA > parsedB
            }
        }
        
        return codeA > codeB
    }

    suspend fun checkAppVersion(context: android.content.Context): AppVersionControl? {
        if (isEmulator()) {
            return null
        }
        val currentCode = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            2L // fallback current version code
        }

        val currentVersionName = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.4"
        } catch (e: Exception) {
            "1.4"
        }

        // Method 1: Check Firestore (Primary, configured directly in Admin Settings)
        try {
            val firestore = getDb()
            if (firestore != null) {
                val document = kotlinx.coroutines.withTimeoutOrNull(4000) {
                    firestore.collection("app_config").document("version_control")
                        .get().awaitTaskFirestore()
                }
                if (document != null && document.exists()) {
                    val dbCode = document.getLong("current_version_code") ?: 0L
                    val dbName = document.getString("current_version_name") ?: ""
                    val downloadUrl = document.getString("download_url") ?: ""
                    
                    val isNewAvailable = isVersionNewer(dbCode, dbName, currentCode, currentVersionName)
                    if (isNewAvailable && downloadUrl.isNotEmpty()) {
                        Log.d(TAG, "New version detected via Firestore (Primary): $dbName (Code $dbCode) > $currentVersionName")
                        return AppVersionControl(
                            latestVersionCode = dbCode,
                            downloadUrl = downloadUrl,
                            isNewVersionAvailable = true,
                            latestVersionName = dbName.ifEmpty { "v$dbCode" }
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Firestore check failed: ${e.message}")
        }

        // Method 2: Check Realtime Database (Backup)
        try {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val ref = database.getReference("app_version")
            val dataSnapshot = kotlinx.coroutines.withTimeoutOrNull(4000) {
                ref.get().awaitTaskFirestore()
            }
            if (dataSnapshot != null && dataSnapshot.exists()) {
                val dbCode = dataSnapshot.child("current_version_code").getValue(Long::class.java) ?: 0L
                val dbName = dataSnapshot.child("current_version_name").getValue(String::class.java) ?: ""
                val downloadUrl = dataSnapshot.child("download_url").getValue(String::class.java) ?: ""
                
                val isNewAvailable = isVersionNewer(dbCode, dbName, currentCode, currentVersionName)
                if (isNewAvailable && downloadUrl.isNotEmpty()) {
                    Log.d(TAG, "New version detected via Realtime DB: $dbName (Code $dbCode) > $currentVersionName")
                    return AppVersionControl(
                        latestVersionCode = dbCode,
                        downloadUrl = downloadUrl,
                        isNewVersionAvailable = true,
                        latestVersionName = dbName.ifEmpty { "v$dbCode" }
                    )
                }
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Realtime Database check failed or not setup: ${e.message}")
        }

        // Method 3: Check Remote Config (Backup)
        try {
            val remoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
            val configSettings = com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // fetch check interval
                .build()
            
            val remoteFetched = kotlinx.coroutines.withTimeoutOrNull(4000) {
                remoteConfig.setConfigSettingsAsync(configSettings).awaitTaskFirestore()
                remoteConfig.fetchAndActivate().awaitTaskFirestore() ?: false
            } ?: false
            
            if (remoteFetched) {
                val remoteCode = remoteConfig.getLong("current_version_code")
                val remoteName = remoteConfig.getString("current_version_name")
                val downloadUrl = remoteConfig.getString("download_url")
                
                val isNewAvailable = isVersionNewer(remoteCode, remoteName, currentCode, currentVersionName)
                if (isNewAvailable && downloadUrl.isNotEmpty()) {
                    Log.d(TAG, "New version detected via Remote Config: $remoteName (Code $remoteCode) > $currentVersionName")
                    return AppVersionControl(
                        latestVersionCode = remoteCode,
                        downloadUrl = downloadUrl,
                        isNewVersionAvailable = true,
                        latestVersionName = remoteName.ifEmpty { "v$remoteCode" }
                    )
                }
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Remote Config check failed or not setup: ${e.message}")
        }

        return null
    }

    suspend fun publishNewAppVersion(versionCode: Long, downloadUrl: String, versionName: String = ""): Boolean {
        var firestoreSuccess = false
        var rtDbSuccess = false

        // Update Firestore
        try {
            val firestore = getDb()
            if (firestore != null) {
                kotlinx.coroutines.withTimeoutOrNull(4000) {
                    firestore.collection("app_config").document("version_control")
                        .set(mapOf(
                            "current_version_code" to versionCode,
                            "current_version_name" to versionName,
                            "download_url" to downloadUrl
                        ))
                        .awaitTaskFirestore()
                }
                firestoreSuccess = true
                Log.d(TAG, "Successfully updated version in Firestore: $versionCode ($versionName)")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to update version in Firestore: ${e.message}")
        }

        // Update Realtime Database
        try {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val ref = database.getReference("app_version")
            val updates = mapOf(
                "current_version_code" to versionCode,
                "current_version_name" to versionName,
                "download_url" to downloadUrl
            )
            val task = ref.updateChildren(updates)
            val dbSuccess = kotlinx.coroutines.withTimeoutOrNull(4000) {
                try {
                    task.awaitTaskFirestore()
                    true
                } catch (e: Exception) {
                    false
                }
            } ?: false
            rtDbSuccess = dbSuccess
            Log.d(TAG, "Successfully updated version in Realtime DB: $versionCode ($versionName)")
        } catch (e: Throwable) {
            Log.d(TAG, "Failed to update version in Realtime DB (maybe not configured): ${e.message}")
        }

        return firestoreSuccess || rtDbSuccess
    }

    suspend fun fetchPublishedVersion(): PublishedVersionResult {
        val errors = mutableListOf<String>()
        // 1. Try Firestore with robust timeout
        try {
            val firestore = getDb()
            if (firestore != null) {
                val doc = kotlinx.coroutines.withTimeoutOrNull(3000) {
                    try {
                        firestore.collection("app_config").document("version_control")
                            .get().awaitTaskFirestore()
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        val exMessage = e.message ?: "Unknown Firestore error"
                        errors.add("Firestore: $exMessage")
                        null
                    }
                }
                if (doc != null && doc.exists()) {
                    val dbCode = doc.getLong("current_version_code") ?: 0L
                    val dbName = doc.getString("current_version_name") ?: ""
                    val dbUrl = doc.getString("download_url") ?: ""
                    return PublishedVersionResult(dbCode, dbName, dbUrl, true)
                } else if (doc != null && !doc.exists()) {
                    return PublishedVersionResult(0L, "Chưa có bản", "", true)
                }
            } else {
                errors.add("Firestore instance null")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            errors.add("Firestore exception: ${e.message}")
            Log.e(TAG, "fetchPublishedVersion (Firestore) failed: ${e.message}")
        }

        // 2. Try Realtime Database with robust timeout
        try {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            val ref = database.getReference("app_version")
            val snap = kotlinx.coroutines.withTimeoutOrNull(3000) {
                try {
                    ref.get().awaitTaskFirestore()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val exMessage = e.message ?: "Unknown RTDB error"
                    errors.add("RTDB: $exMessage")
                    null
                }
            }
            if (snap != null && snap.exists()) {
                val dbCode = snap.child("current_version_code").getValue(Long::class.java) ?: 0L
                val dbName = snap.child("current_version_name").getValue(String::class.java) ?: ""
                val dbUrl = snap.child("download_url").getValue(String::class.java) ?: ""
                return PublishedVersionResult(dbCode, dbName, dbUrl, true)
            } else if (snap != null && !snap.exists()) {
                errors.add("RTDB: node empty")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            errors.add("RTDB exception: ${e.message}")
            Log.e(TAG, "fetchPublishedVersion (RTDB) failed: ${e.message}")
        }

        val finalError = if (errors.isEmpty()) "Không thể tải thông tin" else errors.joinToString("; ")
        return PublishedVersionResult(0L, "Lỗi tải bản", "", false, finalError)
    }

    suspend fun cleanupExpiredNotifications(uid: String = "") {
        try {
            val firestore = getDb() ?: return
            val cutoff = System.currentTimeMillis() - (30L * 24 * 3600 * 1000L) // 30 ngày để tài khoản mới vào vẫn xem được thông báo hệ thống

            // 1. Chỉ dọn dẹp các thông báo Admin không ghim và đã quá 30 ngày
            val rootOldDocs = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                firestore.collection("admin_notifications")
                    .whereLessThan("createdAt", cutoff)
                    .get().awaitTaskFirestore()
            }
            rootOldDocs?.documents?.forEach { doc ->
                try {
                    val isPinned = doc.getBoolean("isPinned") ?: false
                    if (!isPinned) {
                        firestore.collection("admin_notifications").document(doc.id).delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi dọn dẹp thông báo cũ: ${e.message}")
                }
            }

            // 2. Dọn dẹp subcollection users/{uid}/notifications cũ hơn 30 ngày nếu không ghim
            if (uid.isNotBlank() && !isDemoUser(uid)) {
                val userOldDocs = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                    firestore.collection("users").document(uid)
                        .collection("notifications")
                        .whereLessThan("createdAt", cutoff)
                        .get().awaitTaskFirestore()
                }
                userOldDocs?.documents?.forEach { doc ->
                    try {
                        val isPinned = doc.getBoolean("isPinned") ?: false
                        if (!isPinned) {
                            firestore.collection("users").document(uid)
                                .collection("notifications").document(doc.id).delete()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi dọn dẹp thông báo user: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Bỏ qua lỗi dọn dẹp thông báo: ${e.message}")
        }
    }

    suspend fun sendAdminNotification(notif: com.example.data.model.AdminNotification): Boolean {
        // Tự động dọn dẹp các thông báo cũ trong Background Coroutine để không chặn/làm chậm quá trình gửi
        try {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                cleanupExpiredNotifications(notif.targetUid)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi kích hoạt dọn dẹp ngầm: ${e.message}")
        }

        var anySuccess = false
        val firestore = getDb() ?: return false
        val notifId = if (notif.id.isNotBlank()) notif.id else firestore.collection("admin_notifications").document().id

        val map = mapOf(
            "id" to notifId,
            "targetUid" to notif.targetUid,
            "targetName" to notif.targetName,
            "title" to notif.title,
            "message" to notif.message,
            "type" to notif.type,
            "createdAt" to notif.createdAt,
            "sentBy" to notif.sentBy,
            "isPinned" to notif.isPinned
        )

        // 1. Lưu vào kho chung admin_notifications/{notifId} (Tăng timeout lên 10s)
        try {
            val res = kotlinx.coroutines.withTimeoutOrNull(10000L) {
                firestore.collection("admin_notifications").document(notifId)
                    .set(map).awaitTaskFirestore()
                true
            }
            if (res == true) anySuccess = true
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi lưu admin_notifications: ${e.message}")
        }

        // 2. Nếu gửi cho cá nhân 1 nhân viên -> Lưu vào subcollection của nhân viên đó
        if (notif.targetUid != "ALL" && notif.targetUid.isNotBlank()) {
            try {
                val resUser = kotlinx.coroutines.withTimeoutOrNull(10000L) {
                    firestore.collection("users").document(notif.targetUid)
                        .collection("notifications").document(notifId)
                        .set(map).awaitTaskFirestore()
                    true
                }
                if (resUser == true) anySuccess = true
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi lưu thông báo cho nhân viên: ${e.message}")
            }
        } else if (notif.targetUid == "ALL") {
            // Nếu gửi cho tất cả nhân viên -> Lưu vào subcollection của từng nhân viên để chắc chắn họ nhận được (do quy định bảo mật Firestore chặn đọc root)
            try {
                val allUsers = getAllUserConfigs()
                allUsers.forEach { user ->
                    if (user.userId.isNotBlank() && !isDemoUser(user.userId)) {
                        try {
                            firestore.collection("users").document(user.userId)
                                .collection("notifications").document(notifId)
                                .set(map) // Ghi bất đồng bộ không chặn luồng, tránh timeout vòng lặp
                        } catch (ex: Exception) {
                            Log.e(TAG, "Lỗi lưu thông báo cho nhân viên ${user.userId}: ${ex.message}")
                        }
                    }
                }
                anySuccess = true
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi phân phối thông báo cho tất cả nhân viên: ${e.message}")
            }
        }

        // 3. Đường dẫn dự phòng 3: app_config/admin_notifications/items/{notifId}
        try {
            val resAppConfig = kotlinx.coroutines.withTimeoutOrNull(10000L) {
                firestore.collection("app_config").document("admin_notifications")
                    .collection("items").document(notifId)
                    .set(map).awaitTaskFirestore()
                true
            }
            if (resAppConfig == true) anySuccess = true
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi lưu app_config backup: ${e.message}")
        }

        // Nếu Firestore khả dụng, ghi dữ liệu offline/stage thành công
        if (!anySuccess) {
            try {
                firestore.collection("admin_notifications").document(notifId).set(map)
                anySuccess = true
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi lưu offline fallback: ${e.message}")
            }
        }

        return anySuccess
    }

    suspend fun getUnreadAdminNotifications(uid: String, lastTimestamp: Long): List<com.example.data.model.AdminNotification> {
        // Tự động dọn dẹp các thông báo đã hết hạn
        cleanupExpiredNotifications(uid)

        val firestore = getDb() ?: return emptyList()
        val notifMap = mutableMapOf<String, com.example.data.model.AdminNotification>()

        fun parseDoc(doc: DocumentSnapshot) {
            val targetUid = doc.getString("targetUid") ?: "ALL"
            if (targetUid == "ALL" || targetUid == uid) {
                val id = doc.id
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                if (createdAt > lastTimestamp) {
                    val notif = com.example.data.model.AdminNotification(
                        id = id,
                        targetUid = targetUid,
                        targetName = doc.getString("targetName") ?: "Tất cả",
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        type = doc.getString("type") ?: "GENERAL",
                        createdAt = createdAt,
                        sentBy = doc.getString("sentBy") ?: "Admin",
                        isPinned = doc.getBoolean("isPinned") ?: false
                    )
                    notifMap[id] = notif
                }
            }
        }

        // Query Path 1: Subcollection users/{uid}/notifications
        if (uid.isNotBlank() && !isDemoUser(uid)) {
            try {
                val snap = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                    firestore.collection("users").document(uid)
                        .collection("notifications")
                        .whereGreaterThan("createdAt", lastTimestamp)
                        .get().awaitTaskFirestore()
                }
                snap?.documents?.forEach { parseDoc(it) }
            } catch (e: Exception) {
                Log.w(TAG, "Bỏ qua lỗi đọc notifications từ user")
            }
        }

        // Query Path 2: Root collection admin_notifications
        try {
            val snap = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                firestore.collection("admin_notifications")
                    .whereGreaterThan("createdAt", lastTimestamp)
                    .get().awaitTaskFirestore()
            }
            snap?.documents?.forEach { parseDoc(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Bỏ qua lỗi đọc root collection admin_notifications (có thể do quyền Firebase)")
        }

        // Query Path 3: app_config/admin_notifications/items
        try {
            val snap = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                firestore.collection("app_config").document("admin_notifications")
                    .collection("items")
                    .whereGreaterThan("createdAt", lastTimestamp)
                    .get().awaitTaskFirestore()
            }
            snap?.documents?.forEach { parseDoc(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Bỏ qua lỗi đọc app_config notifications")
        }

        return notifMap.values.sortedBy { it.createdAt }
    }

    suspend fun recordUserDeletedNotification(uid: String, notificationId: String): Boolean {
        if (uid.isBlank() || isDemoUser(uid)) return true
        val firestore = getDb() ?: return false
        return try {
            firestore.collection("users").document(uid)
                .collection("deleted_notifications").document(notificationId)
                .set(mapOf("deletedAt" to System.currentTimeMillis(), "notificationId" to notificationId))
                .awaitTaskFirestore()
            try {
                firestore.collection("users").document(uid)
                    .collection("notifications").document(notificationId).delete()
            } catch (e: Exception) {}
            true
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi ghi nhận user xóa thông báo: ${e.message}")
            false
        }
    }

    suspend fun getUserDeletedNotificationIds(uid: String): Set<String> {
        val firestore = getDb() ?: return emptySet()
        val deletedSet = mutableSetOf<String>()

        // 1. Đọc danh sách xóa cá nhân của user
        if (uid.isNotBlank() && !isDemoUser(uid)) {
            try {
                val snap = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                    firestore.collection("users").document(uid)
                        .collection("deleted_notifications")
                        .get().awaitTaskFirestore()
                }
                snap?.documents?.forEach { deletedSet.add(it.id) }
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi đọc deleted_notifications cá nhân: ${e.message}")
            }
        }

        // 2. Đọc danh sách thông báo đã bị Admin xóa toàn cục (global deleted)
        try {
            val globalSnap = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                firestore.collection("app_config").document("global_deleted_notifications")
                    .collection("items")
                    .get().awaitTaskFirestore()
            }
            globalSnap?.documents?.forEach { deletedSet.add(it.id) }
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi đọc global_deleted_notifications: ${e.message}")
        }

        return deletedSet
    }

    suspend fun getAllAdminNotifications(): List<com.example.data.model.AdminNotification> {
        val firestore = getDb() ?: return emptyList()
        val notifMap = mutableMapOf<String, com.example.data.model.AdminNotification>()

        fun parseDoc(doc: DocumentSnapshot) {
            val id = doc.id
            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
            val notif = com.example.data.model.AdminNotification(
                id = id,
                targetUid = doc.getString("targetUid") ?: "ALL",
                targetName = doc.getString("targetName") ?: "Tất cả",
                title = doc.getString("title") ?: "",
                message = doc.getString("message") ?: "",
                type = doc.getString("type") ?: "GENERAL",
                createdAt = createdAt,
                sentBy = doc.getString("sentBy") ?: "Admin",
                isPinned = doc.getBoolean("isPinned") ?: false
            )
            notifMap[id] = notif
        }

        // Query Path 1: Root collection admin_notifications
        try {
            val snap = kotlinx.coroutines.withTimeoutOrNull(6000L) {
                firestore.collection("admin_notifications")
                    .get().awaitTaskFirestore()
            }
            snap?.documents?.forEach { parseDoc(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi đọc tất cả admin_notifications: ${e.message}")
        }

        // Query Path 2: app_config/admin_notifications/items
        try {
            val snap = kotlinx.coroutines.withTimeoutOrNull(6000L) {
                firestore.collection("app_config").document("admin_notifications")
                    .collection("items")
                    .get().awaitTaskFirestore()
            }
            snap?.documents?.forEach { parseDoc(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi đọc app_config admin notifications: ${e.message}")
        }

        return notifMap.values.sortedWith(
            compareByDescending<com.example.data.model.AdminNotification> { it.isPinned }
                .thenByDescending { it.createdAt }
        )
    }

    suspend fun deleteAdminNotification(uid: String, notificationId: String): Boolean {
        val firestore = getDb() ?: return false
        var success = false

        // 1. Xóa trong root collection admin_notifications
        try {
            firestore.collection("admin_notifications").document(notificationId)
                .delete().awaitTaskFirestore()
            success = true
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi xóa admin_notifications: ${e.message}")
        }

        // 2. Xóa trong app_config/admin_notifications/items
        try {
            firestore.collection("app_config").document("admin_notifications")
                .collection("items").document(notificationId)
                .delete().awaitTaskFirestore()
            success = true
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi xóa app_config notification item: ${e.message}")
        }

        // 3. Ghi vào danh sách global_deleted_notifications để mọi máy user tự động lọc bỏ
        try {
            firestore.collection("app_config").document("global_deleted_notifications")
                .collection("items").document(notificationId)
                .set(mapOf("deletedAt" to System.currentTimeMillis(), "notificationId" to notificationId))
                .awaitTaskFirestore()
            success = true
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi ghi global_deleted_notifications: ${e.message}")
        }

        // 4. Nếu có targetUid cụ thể
        if (uid.isNotBlank() && uid != "ALL" && !isDemoUser(uid)) {
            try {
                firestore.collection("users").document(uid)
                    .collection("notifications").document(notificationId)
                    .delete().awaitTaskFirestore()
                success = true
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi xóa notification subcollection user: ${e.message}")
            }
        } else if (uid == "ALL" || uid.isBlank()) {
            // Xóa ngầm trong subcollection của các user
            try {
                val allUsers = getAllUserConfigs()
                allUsers.forEach { user ->
                    if (user.userId.isNotBlank() && !isDemoUser(user.userId)) {
                        try {
                            firestore.collection("users").document(user.userId)
                                .collection("notifications").document(notificationId).delete()
                        } catch (ex: Exception) {}
                    }
                }
            } catch (ex: Exception) {}
        }

        return success
    }

    suspend fun saveUserSalaryConfigToFirestore(config: com.example.data.model.UserConfig) {
        if (config.userId.startsWith("demo") || config.userId.contains("demo")) return
        val firestore = getDb() ?: return
        val map = mapOf(
            "userId" to config.userId,
            "luongCoBan" to config.luongCoBan,
            "luongDongBaoHiem" to config.luongDongBaoHiem,
            "tiLeDongBaoHiem" to config.tiLeDongBaoHiem,
            "ngayChotLuong" to config.ngayChotLuong,
            "doanPhiCongDoan" to config.doanPhiCongDoan,
            "heSoOtNgayThuong" to config.heSoOtNgayThuong,
            "heSoOtChuNhat" to config.heSoOtChuNhat,
            "heSoOtNgayLe" to config.heSoOtNgayLe,
            "tienChuyenCanGoc" to config.tienChuyenCanGoc,
            "soNgayPhepNam" to config.soNgayPhepNam,
            "phepNamConLai" to config.phepNamConLai,
            "lastAccumulatedMonth" to config.lastAccumulatedMonth,
            "pcKyThuat" to config.pcKyThuat,
            "pcTrachNhiem" to config.pcTrachNhiem,
            "pcChucVu" to config.pcChucVu,
            "pcHieuSuat" to config.pcHieuSuat,
            "pcSanPham" to config.pcSanPham,
            "pcComCa" to config.pcComCa,
            "pcComOt" to config.pcComOt,
            "pcNhaO" to config.pcNhaO,
            "pcDocHai" to config.pcDocHai,
            "pcDtDoanhThu" to config.pcDtDoanhThu,
            "pcXangXe" to config.pcXangXe,
            "pcThamNien" to config.pcThamNien,
            "pcKhac1" to config.pcKhac1,
            "pcKhac" to config.pcCaDem,
            "pcCaDem" to config.pcCaDem,
            "allowanceCalcTypes" to config.allowanceCalcTypes,
            "soGioNghiGiaiLao" to config.soGioNghiGiaiLao,
            "tinhKhauTruNghi" to config.tinhKhauTruNghi,
            "lichTrinh" to config.lichTrinh,
            "hoVaTen" to config.hoVaTen,
            "maNhanVien" to config.maNhanVien,
            "emailDangKy" to config.emailDangKy,
            "ngayVaoLam" to config.ngayVaoLam,
            "tienComMoiNgay" to config.tienComMoiNgay,
            "phuCap" to config.phuCap,
            "phuCapXangXe" to config.phuCapXangXe,
            "phuCapDienThoai" to config.phuCapDienThoai,
            "phuCapNhaO" to config.phuCapNhaO,
            "phuCapChuyenCan" to config.phuCapChuyenCan,
            "thuong" to config.thuong,
            "heSoOtDem" to config.heSoOtDem,
            "caDemStart" to config.caDemStart,
            "caDemEnd" to config.caDemEnd,
            "companyId" to config.companyId,
            "companyName" to config.companyName,
            "companyCode" to config.companyCode,
            "roleId" to config.roleId,
            "roleName" to config.roleName,
            "isAdmin" to config.isAdmin
        )
        firestore.collection("users").document(config.userId).collection("salary_config").document("settings")
            .set(map, SetOptions.merge())
            .awaitTaskFirestore()
        try {
            firestore.collection("users").document(config.userId).collection("config").document("salary_settings")
                .set(map, SetOptions.merge())
                .awaitTaskFirestore()
            firestore.collection("users_salary").document(config.userId)
                .set(map, SetOptions.merge())
                .awaitTaskFirestore()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving legacy user salary paths: ${e.message}")
        }
    }

    suspend fun fetchUserSalaryConfigFromFirestore(userId: String): com.example.data.model.UserConfig? {
        if (userId.startsWith("demo") || userId.contains("demo")) return null
        val firestore = getDb() ?: return null
        return try {
            val document = firestore.collection("users").document(userId).collection("salary_config").document("settings")
                .get()
                .awaitTaskFirestore()
            if (document != null && document.exists()) {
                document.toUserSalaryConfig(userId)
            } else {
                // Fallback to old root collection
                val oldDocument = firestore.collection("users_salary").document(userId)
                    .get()
                    .awaitTaskFirestore()
                if (oldDocument != null && oldDocument.exists()) {
                    oldDocument.toUserSalaryConfig(userId)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user salary config from Firestore: ${e.message}", e)
            null
        }
    }

    suspend fun deleteUserFully(userId: String) {
        if (userId.startsWith("demo") || userId.contains("demo")) return
        val firestore = getDb() ?: return
        try {
            // 1. Delete salary_config subcollection documents
            val salaryConfigs = firestore.collection("users").document(userId).collection("salary_config").get().awaitTaskFirestore()
            salaryConfigs?.documents?.forEach { doc ->
                doc.reference.delete().awaitTaskFirestore()
            }
            
            // 2. Delete attendance_logs subcollection documents
            val logs = firestore.collection("users").document(userId).collection("attendance_logs").get().awaitTaskFirestore()
            logs?.documents?.forEach { doc ->
                doc.reference.delete().awaitTaskFirestore()
            }
            
            // 3. Delete the main user document
            firestore.collection("users").document(userId).delete().awaitTaskFirestore()
            
            // 4. Delete from old root collection if exists
            firestore.collection("users_salary").document(userId).delete().awaitTaskFirestore()
            
            Log.d(TAG, "Successfully deleted user fully: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user $userId: ${e.message}")
        }
    }
}

fun DocumentSnapshot.toUserSalaryConfig(userId: String): com.example.data.model.UserConfig {
    val pcKyThuatVal = getDouble("pcKyThuat") ?: getDouble("phu_cap_ky_thuat") ?: 0.0
    val pcTrachNhiemVal = getDouble("pcTrachNhiem") ?: getDouble("phu_cap_trach_nhiem") ?: 0.0
    val pcChucVuVal = getDouble("pcChucVu") ?: getDouble("phu_cap_chuc_vu") ?: 0.0
    val pcHieuSuatVal = getDouble("pcHieuSuat") ?: getDouble("phu_cap_hieu_suat") ?: 0.0
    val pcSanPhamVal = getDouble("pcSanPham") ?: getDouble("phu_cap_san_pham") ?: 0.0
    val pcComCaVal = getDouble("pcComCa") ?: getDouble("phu_cap_com_ca") ?: 0.0
    val pcComOtVal = getDouble("pcComOt") ?: getDouble("phu_cap_com_ot") ?: 0.0
    val pcNhaOVal = getDouble("pcNhaO") ?: getDouble("phu_cap_nha_o") ?: 0.0
    val pcDocHaiVal = getDouble("pcDocHai") ?: getDouble("phu_cap_doc_hai") ?: 0.0
    val pcDtDoanhThuVal = getDouble("pcDtDoanhThu") ?: getDouble("phu_cap_dien_thoai") ?: 0.0
    val pcXangXeVal = getDouble("pcXangXe") ?: getDouble("phu_cap_xang_xe") ?: 0.0
    val pcThamNienVal = getDouble("pcThamNien") ?: getDouble("phu_cap_tham_nien") ?: 0.0
    val pcKhac1Val = getDouble("pcKhac1") ?: 0.0
    val pcCaDemVal = getDouble("pcCaDem") ?: getDouble("pcKhac") ?: getDouble("phu_cap_khac") ?: 0.0

    return com.example.data.model.UserConfig(
        userId = userId,
        luongCoBan = getDouble("luongCoBan") ?: getDouble("luong_co_ban") ?: 0.0,
        luongDongBaoHiem = getDouble("luongDongBaoHiem") ?: getDouble("luong_bao_hiem") ?: 0.0,
        tiLeDongBaoHiem = getDouble("tiLeDongBaoHiem") ?: getDouble("tile_bao_hiem") ?: 10.5,
        ngayChotLuong = getLong("ngayChotLuong")?.toInt() ?: getLong("ngay_chot_luong")?.toInt() ?: 1,
        doanPhiCongDoan = getDouble("doanPhiCongDoan") ?: getDouble("doan_phi_40k") ?: 0.0,
        heSoOtNgayThuong = getDouble("heSoOtNgayThuong") ?: getDouble("he_so_ot_normal") ?: 1.5,
        heSoOtChuNhat = getDouble("heSoOtChuNhat") ?: getDouble("he_so_ot_sunday") ?: 2.0,
        heSoOtNgayLe = getDouble("heSoOtNgayLe") ?: getDouble("he_so_ot_holiday") ?: 3.0,
        tienChuyenCanGoc = getDouble("tienChuyenCanGoc") ?: getDouble("tien_chuyen_can") ?: 0.0,
        soNgayPhepNam = getLong("soNgayPhepNam")?.toInt() ?: getLong("so_ngay_nghi_phep")?.toInt() ?: 0,
        phepNamConLai = getLong("phepNamConLai")?.toInt() ?: 0,
        lastAccumulatedMonth = getString("lastAccumulatedMonth") ?: getString("last_accumulated_month") ?: "",
        pcKyThuat = pcKyThuatVal,
        pcTrachNhiem = pcTrachNhiemVal,
        pcChucVu = pcChucVuVal,
        pcHieuSuat = pcHieuSuatVal,
        pcSanPham = pcSanPhamVal,
        pcComCa = pcComCaVal,
        pcComOt = pcComOtVal,
        pcNhaO = pcNhaOVal,
        pcDocHai = pcDocHaiVal,
        pcDtDoanhThu = pcDtDoanhThuVal,
        pcXangXe = pcXangXeVal,
        pcThamNien = pcThamNienVal,
        pcKhac1 = pcKhac1Val,
        pcCaDem = pcCaDemVal,
        allowanceCalcTypes = getString("allowanceCalcTypes") ?: "",
        soGioNghiGiaiLao = getDouble("soGioNghiGiaiLao") ?: 1.5,
        tinhKhauTruNghi = getBoolean("tinhKhauTruNghi") ?: false,
        lichTrinh = getString("lichTrinh") ?: "07:30 - 19:30",
        hoVaTen = getString("hoVaTen") ?: getString("fullName") ?: "User Demo",
        maNhanVien = getString("maNhanVien") ?: getString("maNhanVien") ?: "demo_${userId.takeLast(6)}",
        emailDangKy = getString("emailDangKy") ?: "",
        ngayVaoLam = getString("ngayVaoLam") ?: "",
        tienComMoiNgay = getDouble("tienComMoiNgay") ?: 50000.0,
        phuCap = getDouble("phuCap") ?: 1000000.0,
        phuCapXangXe = getDouble("phuCapXangXe") ?: 500000.0,
        phuCapDienThoai = getDouble("phuCapDienThoai") ?: 300000.0,
        phuCapNhaO = getDouble("phuCapNhaO") ?: 1000000.0,
        phuCapChuyenCan = getDouble("phuCapChuyenCan") ?: 500000.0,
        thuong = getDouble("thuong") ?: 0.0,
        heSoOtDem = getDouble("heSoOtDem") ?: getDouble("he_so_ot_dem") ?: 1.75,
        caDemStart = getString("caDemStart") ?: getString("thoi_gian_ca_dem") ?: "22:00",
        caDemEnd = getString("caDemEnd") ?: "06:00",
        companyId = getString("companyId") ?: "default_company",
        companyName = getString("companyName") ?: "Công ty Mặc Định",
        companyCode = getString("companyCode") ?: "DEFAULT",
        roleId = getString("roleId") ?: "",
        roleName = getString("roleName") ?: "",
        
        
        isAdmin = getBoolean("isAdmin") ?: false
    )
}

fun DocumentSnapshot.toCompanyConfig(): CompanyConfig {
    val id = getString("companyId") ?: this.id
    return CompanyConfig(
        companyId = id,
        companyName = getString("companyName") ?: "Công ty Mặc Định",
        companyCode = getString("companyCode") ?: "DEFAULT",
        
        
        description = getString("description") ?: "",
        address = getString("address") ?: "",
        luongCoBan = getDouble("luongCoBan") ?: 6000000.0,
        luongDongBaoHiem = getDouble("luongDongBaoHiem") ?: 5000000.0,
        tiLeDongBaoHiem = getDouble("tiLeDongBaoHiem") ?: 10.5,
        ngayChotLuong = getLong("ngayChotLuong")?.toInt() ?: 1,
        doanPhiCongDoan = getDouble("doanPhiCongDoan") ?: 40000.0,
        heSoOtNgayThuong = getDouble("heSoOtNgayThuong") ?: 1.5,
        heSoOtChuNhat = getDouble("heSoOtChuNhat") ?: 2.0,
        heSoOtNgayLe = getDouble("heSoOtNgayLe") ?: 3.0,
        heSoOtDem = getDouble("heSoOtDem") ?: 1.75,
        caDemStart = getString("caDemStart") ?: "22:00",
        caDemEnd = getString("caDemEnd") ?: "06:00",
        tienChuyenCanGoc = getDouble("tienChuyenCanGoc") ?: 500000.0,
        soNgayPhepNam = getLong("soNgayPhepNam")?.toInt() ?: 12,
        pcKyThuat = getDouble("pcKyThuat") ?: 0.0,
        pcTrachNhiem = getDouble("pcTrachNhiem") ?: 0.0,
        pcChucVu = getDouble("pcChucVu") ?: 0.0,
        pcHieuSuat = getDouble("pcHieuSuat") ?: 0.0,
        pcSanPham = getDouble("pcSanPham") ?: 0.0,
        pcComCa = getDouble("pcComCa") ?: 30000.0,
        pcComOt = getDouble("pcComOt") ?: 15000.0,
        pcNhaO = getDouble("pcNhaO") ?: 0.0,
        pcDocHai = getDouble("pcDocHai") ?: 0.0,
        pcDtDoanhThu = getDouble("pcDtDoanhThu") ?: 0.0,
        pcXangXe = getDouble("pcXangXe") ?: 0.0,
        pcThamNien = getDouble("pcThamNien") ?: 0.0,
        pcKhac1 = getDouble("pcKhac1") ?: 0.0,
        pcCaDem = getDouble("pcCaDem") ?: 0.0,
        allowanceCalcTypes = getString("allowanceCalcTypes") ?: "",
        soGioNghiGiaiLao = getDouble("soGioNghiGiaiLao") ?: 1.5,
        tinhKhauTruNghi = getBoolean("tinhKhauTruNghi") ?: false,
        lichTrinh = getString("lichTrinh") ?: "07:30 - 19:30",
        rolesData = getString("rolesData") ?: "",
        createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
        updatedAt = getLong("updatedAt") ?: System.currentTimeMillis()
    )
}

data class AppVersionControl(
    val latestVersionCode: Long,
    val downloadUrl: String,
    val isNewVersionAvailable: Boolean,
    val latestVersionName: String = ""
)

data class PublishedVersionResult(
    val versionCode: Long,
    val versionName: String,
    val downloadUrl: String,
    val isSuccess: Boolean,
    val errorMessage: String = ""
)

fun DocumentSnapshot.toUserConfig(uid: String): UserConfig {
    return UserConfig(
        uid = uid,
        fullName = getString("fullName") ?: getString("hoVaTen") ?: "Nhân viên mới",
        hourlyRate = getDouble("hourlyRate") ?: ( (getDouble("luongCoBan") ?: getDouble("luong_co_ban") ?: 6000000.0) / 26.0 / 8.0 ),
        currency = getString("currency") ?: "đ",
        dailyTargetHours = getDouble("dailyTargetHours") ?: 8.0,
        he_so_ot_dem = getDouble("he_so_ot_dem") ?: getDouble("heSoOtDem") ?: 1.75,
        thoi_gian_ca_dem = getString("thoi_gian_ca_dem") ?: getString("caDemStart") ?: "22:00",
        
        luong_co_ban = getDouble("luong_co_ban") ?: getDouble("luongCoBan") ?: 6000000.0,
        luong_bao_hiem = getDouble("luong_bao_hiem") ?: getDouble("luongDongBaoHiem") ?: 5000000.0,
        tile_bao_hiem = getDouble("tile_bao_hiem") ?: getDouble("tiLeDongBaoHiem") ?: 10.5,
        doan_phi_40k = getDouble("doan_phi_40k") ?: getDouble("doanPhiCongDoan") ?: 40000.0,
        ngay_chot_luong = getLong("ngay_chot_luong")?.toInt() ?: getLong("ngayChotLuong")?.toInt() ?: 25,
        
        he_so_ot_normal = getDouble("he_so_ot_normal") ?: getDouble("heSoOtNgayThuong") ?: 1.5,
        he_so_ot_sunday = getDouble("he_so_ot_sunday") ?: getDouble("heSoOtChuNhat") ?: 2.0,
        he_so_ot_holiday = getDouble("he_so_ot_holiday") ?: getDouble("heSoOtNgayLe") ?: 3.0,
        
        tien_chuyen_can = getDouble("tien_chuyen_can") ?: getDouble("tienChuyenCanGoc") ?: 500000.0,
        so_ngay_nghi_phep = getLong("so_ngay_nghi_phep")?.toInt() ?: getLong("soNgayPhepNam")?.toInt() ?: 12,
        
        phu_cap_ky_thuat = getDouble("phu_cap_ky_thuat") ?: getDouble("pcKyThuat") ?: 0.0,
        phu_cap_trach_nhiem = getDouble("phu_cap_trach_nhiem") ?: getDouble("pcTrachNhiem") ?: 0.0,
        phu_cap_chuc_vu = getDouble("phu_cap_chuc_vu") ?: getDouble("pcChucVu") ?: 0.0,
        phu_cap_hieu_suat = getDouble("phu_cap_hieu_suat") ?: getDouble("pcHieuSuat") ?: 0.0,
        phu_cap_san_pham = getDouble("phu_cap_san_pham") ?: getDouble("pcSanPham") ?: 0.0,
        phu_cap_com_ca = getDouble("phu_cap_com_ca") ?: getDouble("pcComCa") ?: 30000.0,
        phu_cap_com_ot = getDouble("phu_cap_com_ot") ?: getDouble("pcComOt") ?: 15000.0,
        phu_cap_tham_nien = getDouble("phu_cap_tham_nien") ?: getDouble("pcThamNien") ?: 0.0,
        phu_cap_nha_o = getDouble("phu_cap_nha_o") ?: getDouble("pcNhaO") ?: 0.0,
        phu_cap_doc_hai = getDouble("phu_cap_doc_hai") ?: getDouble("pcDocHai") ?: 0.0,
        phu_cap_dien_thoai = getDouble("phu_cap_dien_thoai") ?: getDouble("pcDtDoanhThu") ?: 0.0,
        phu_cap_xang_xe = getDouble("phu_cap_xang_xe") ?: getDouble("pcXangXe") ?: 0.0,
        phu_cap_khac = getDouble("phu_cap_khac") ?: getDouble("pcCaDem") ?: getDouble("pcKhac") ?: 0.0,
        
        phu_cap = getDouble("phu_cap") ?: 500000.0,
        thuong = getDouble("thuong") ?: 0.0,
        tien_khau_tru_nghi = getDouble("tien_khau_tru_nghi") ?: getBoolean("tinhKhauTruNghi")?.let { if (it) 1.0 else 0.0 } ?: 0.0,
        tong_tien_com = getDouble("tong_tien_com") ?: 600000.0,
        last_accumulated_month = getLong("last_accumulated_month")?.toInt() ?: getString("lastAccumulatedMonth")?.hashCode() ?: -1
    )
}

fun DocumentSnapshot.toAttendanceRecord(uid: String): AttendanceRecord {
    val idVal = getLong("id") ?: 0L
    val rawDate = getString("dateString") ?: ""
    return AttendanceRecord(
        id = idVal,
        uid = uid,
        dateString = com.example.data.SalaryCalculator.normalizeDateToDmy(rawDate),
        clockInTime = getLong("clockInTime") ?: 0L,
        clockOutTime = getLong("clockOutTime"),
        status = getString("status") ?: "Active",
        notes = getString("notes") ?: ""
    )
}
