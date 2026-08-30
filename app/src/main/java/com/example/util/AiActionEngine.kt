package com.example.util

import android.content.Context
import android.widget.Toast
import com.example.data.SalaryCalculator
import com.example.data.model.UserConfig
import com.example.viewmodel.TimeSnapViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * AiActionEngine: Hệ thống điều phối & thực thi hành động AI đa nhiệm toàn diện.
 * - Hỗ trợ phân tích & thực thi cùng lúc nhiều hành động (Multi-Action).
 * - Nhận diện ngày tháng tự nhiên (ngày 31, hôm qua, cuối tháng, từ 20 đến 25...).
 * - Nhận diện tiền tệ tự nhiên (50k, 1tr, 12 triệu, 10 củ...).
 * - Bộ phòng vệ Intent Fallback Engine: Đảm bảo khi AI trả lời "đã chấm công / đã đổi lương",
 *   dữ liệu THỰC SỰ ĐƯỢC GHI NHẬN VÀO ROOM DATABASE & FIREBASE ngay cả khi AI quên gắn tag [[ACTION:...]].
 */
object AiActionEngine {

    data class ExecutionResult(
        val actionsExecutedCount: Int,
        val summaryMessages: List<String>,
        val isSuccess: Boolean
    )

    fun executeActions(
        context: Context,
        userPrompt: String,
        aiResponseText: String,
        viewModel: TimeSnapViewModel,
        userConfig: UserConfig?,
        onNavigateTab: (String) -> Unit
    ): ExecutionResult {
        val executedMessages = mutableListOf<String>()
        val processedKeys = mutableSetOf<String>()

        // 1. EXTRACT EXPLICIT ACTION TAGS (e.g. [[ACTION:TYPE:PARAM]])
        val actionRegex = Regex("\\[\\[ACTION:([A-Z_]+)(?::([^\\]]+))?\\]\\]")
        val explicitMatches = actionRegex.findAll(aiResponseText).toList()

        for (match in explicitMatches) {
            val actionType = match.groupValues.getOrNull(1)?.trim() ?: ""
            val actionParam = match.groupValues.getOrNull(2)?.trim() ?: ""
            executeSingleAction(
                actionType = actionType,
                actionParam = actionParam,
                context = context,
                viewModel = viewModel,
                userConfig = userConfig,
                onNavigateTab = onNavigateTab,
                processedKeys = processedKeys,
                executedMessages = executedMessages
            )
        }

        // 2. RESILIENT FALLBACK INTENT ENGINE
        // If explicit actions did not cover the user prompt or AI response intent,
        // extract implicit intent from userPrompt & aiResponseText to prevent "fake completion" (nói suông).
        extractAndExecuteImplicitIntents(
            userPrompt = userPrompt,
            aiResponseText = aiResponseText,
            context = context,
            viewModel = viewModel,
            userConfig = userConfig,
            onNavigateTab = onNavigateTab,
            processedKeys = processedKeys,
            executedMessages = executedMessages
        )

        // 3. SHOW CONSOLIDATED USER FEEDBACK
        if (executedMessages.isNotEmpty()) {
            val summaryToast = if (executedMessages.size == 1) {
                "⚡ ${executedMessages[0]}"
            } else {
                "⚡ AI đã thực thi ${executedMessages.size} tác vụ:\n" + executedMessages.joinToString("\n") { "• $it" }
            }
            Toast.makeText(context, summaryToast, Toast.LENGTH_LONG).show()
        }

        return ExecutionResult(
            actionsExecutedCount = executedMessages.size,
            summaryMessages = executedMessages,
            isSuccess = executedMessages.isNotEmpty()
        )
    }

    private fun executeSingleAction(
        actionType: String,
        actionParam: String,
        context: Context,
        viewModel: TimeSnapViewModel,
        userConfig: UserConfig?,
        onNavigateTab: (String) -> Unit,
        processedKeys: MutableSet<String>,
        executedMessages: MutableList<String>
    ) {
        val key = "$actionType:$actionParam"
        if (processedKeys.contains(key)) return

        when (actionType) {
            "CHECK_IN" -> {
                val note = actionParam.ifBlank { "AI hỗ trợ chấm công vào ca" }
                viewModel.toggleCheckIn(note = note)
                processedKeys.add(key)
                executedMessages.add("Chấm công vào ca thành công")
            }
            "CHECK_OUT" -> {
                val note = actionParam.ifBlank { "AI hỗ trợ chấm công ra ca" }
                viewModel.toggleCheckIn(note = note)
                processedKeys.add(key)
                executedMessages.add("Chấm công ra ca thành công")
            }
            "ADD_WORK_DAY", "ADD_TIME_ENTRY" -> {
                // Format: DATE|CHECK_IN|CHECK_OUT|DAY_TYPE|NOTE
                if (actionParam.isNotBlank()) {
                    val parts = actionParam.split("|")
                    val rawDateStr = parts.getOrNull(0)?.trim() ?: ""
                    val checkInStr = parts.getOrNull(1)?.trim()?.ifBlank { "08:00" } ?: "08:00"
                    val checkOutStr = parts.getOrNull(2)?.trim()?.ifBlank { "17:00" } ?: "17:00"
                    val dayType = parts.getOrNull(3)?.trim()?.ifBlank { "NORMAL" } ?: "NORMAL"
                    val note = parts.getOrNull(4)?.trim() ?: "AI hỗ trợ ghi nhận công"

                    val normDateStr = resolveDateToDmy(rawDateStr)
                    if (normDateStr.isNotBlank()) {
                        val (inHour, inMin) = parseHourMinute(checkInStr, 8, 0)
                        val (outHour, outMin) = parseHourMinute(checkOutStr, 17, 0)

                        viewModel.addSingleEntry(
                            dateStr = normDateStr,
                            checkInHour = inHour,
                            checkInMin = inMin,
                            checkOutHour = outHour,
                            checkOutMin = outMin,
                            dayTypeOverride = dayType,
                            noteStr = note
                        )
                        processedKeys.add(key)
                        processedKeys.add("ADD_WORK_DATE:$normDateStr")
                        executedMessages.add("Ghi nhận công ngày $normDateStr ($checkInStr - $checkOutStr)")
                    }
                }
            }
            "ADD_LEAVE_DAY" -> {
                // Format: DATE|DAY_TYPE|NOTE
                if (actionParam.isNotBlank()) {
                    val parts = actionParam.split("|")
                    val rawDateStr = parts.getOrNull(0)?.trim() ?: ""
                    val dayType = parts.getOrNull(1)?.trim()?.ifBlank { "PAID_LEAVE" } ?: "PAID_LEAVE"
                    val note = parts.getOrNull(2)?.trim() ?: "Nghỉ phép (AI đăng ký)"

                    val normDateStr = resolveDateToDmy(rawDateStr)
                    if (normDateStr.isNotBlank()) {
                        viewModel.addSingleEntry(
                            dateStr = normDateStr,
                            checkInHour = 8,
                            checkInMin = 0,
                            checkOutHour = 17,
                            checkOutMin = 0,
                            dayTypeOverride = dayType,
                            noteStr = note
                        )
                        processedKeys.add(key)
                        processedKeys.add("ADD_WORK_DATE:$normDateStr")
                        val typeLabel = if (dayType == "PAID_LEAVE") "Nghỉ phép năm" else "Nghỉ không lương"
                        executedMessages.add("Đăng ký $typeLabel ngày $normDateStr")
                    }
                }
            }
            "ADD_BULK_WORK_DAYS" -> {
                // Format: DATE1,DATE2,DATE3|CHECK_IN|CHECK_OUT
                if (actionParam.isNotBlank()) {
                    val parts = actionParam.split("|")
                    val rawDatesListStr = parts.getOrNull(0)?.trim() ?: ""
                    val checkInStr = parts.getOrNull(1)?.trim()?.ifBlank { "08:00" } ?: "08:00"
                    val checkOutStr = parts.getOrNull(2)?.trim()?.ifBlank { "17:00" } ?: "17:00"

                    val dateList = rawDatesListStr.split(",").map { resolveDateToDmy(it.trim()) }.filter { it.isNotBlank() }
                    if (dateList.isNotEmpty()) {
                        val (inHour, inMin) = parseHourMinute(checkInStr, 8, 0)
                        val (outHour, outMin) = parseHourMinute(checkOutStr, 17, 0)

                        viewModel.addBulkEntries(
                            selectedDates = dateList,
                            checkInHour = inHour,
                            checkInMin = inMin,
                            checkOutHour = outHour,
                            checkOutMin = outMin,
                            skipSunday = false,
                            skipHoliday = false,
                            autoRecognizeOt = true
                        )
                        processedKeys.add(key)
                        dateList.forEach { processedKeys.add("ADD_WORK_DATE:$it") }
                        executedMessages.add("Thêm ${dateList.size} ngày công hàng loạt")
                    }
                }
            }
            "DELETE_DATE" -> {
                if (actionParam.isNotBlank()) {
                    val normDateStr = resolveDateToDmy(actionParam.trim())
                    viewModel.deleteEntryByDateString(normDateStr)
                    processedKeys.add(key)
                    processedKeys.add("DELETE_DATE:$normDateStr")
                    executedMessages.add("Xóa ngày công $normDateStr")
                }
            }
            "DELETE_DATES" -> {
                if (actionParam.isNotBlank()) {
                    val dateList = actionParam.split(",").map { resolveDateToDmy(it.trim()) }.filter { it.isNotBlank() }
                    for (d in dateList) {
                        viewModel.deleteEntryByDateString(d)
                        processedKeys.add("DELETE_DATE:$d")
                    }
                    processedKeys.add(key)
                    executedMessages.add("Xóa ${dateList.size} ngày công")
                }
            }
            "CLEAR_MONTH" -> {
                viewModel.clearAllEntriesInSelectedMonth()
                processedKeys.add(key)
                executedMessages.add("Xóa toàn bộ dữ liệu công tháng hiện tại")
            }
            "UPDATE_BASE_SALARY" -> {
                val amount = parseMoneyValue(actionParam)
                if (amount != null && userConfig != null) {
                    val updated = userConfig.copy(luongCoBan = amount)
                    viewModel.updateSalaryConfig(updated)
                    processedKeys.add(key)
                    val fmt = DecimalFormat("#,###")
                    executedMessages.add("Cập nhật Lương cơ bản: ${fmt.format(amount)}đ")
                }
            }
            "UPDATE_ALLOWANCE" -> {
                // Format: ALLOWANCE_NAME|AMOUNT
                val parts = actionParam.split("|")
                val name = parts.getOrNull(0)?.trim() ?: ""
                val amount = parseMoneyValue(parts.getOrNull(1) ?: "")
                if (name.isNotBlank() && amount != null && userConfig != null) {
                    val cur = userConfig
                    val updated = when (name.lowercase()) {
                        "pckythuat", "kythuat" -> cur.copy(pcKyThuat = amount)
                        "pctrachnhiem", "trachnhiem" -> cur.copy(pcTrachNhiem = amount)
                        "pcchucvu", "chucvu" -> cur.copy(pcChucVu = amount)
                        "pchieusuat", "hieusuat" -> cur.copy(pcHieuSuat = amount)
                        "pcsanpham", "sanpham" -> cur.copy(pcSanPham = amount)
                        "pccomca", "comca", "tiencom", "com" -> cur.copy(pcComCa = amount)
                        "pccomot", "comot" -> cur.copy(pcComOt = amount)
                        "pcnhao", "nhao", "tiennha" -> cur.copy(pcNhaO = amount)
                        "pcdochai", "dochai" -> cur.copy(pcDocHai = amount)
                        "pcdtdoanhthu", "doanhthu", "dienthoai" -> cur.copy(pcDtDoanhThu = amount)
                        "pcxangxe", "xangxe", "xang" -> cur.copy(pcXangXe = amount)
                        "pcthamnien", "thamnien" -> cur.copy(pcThamNien = amount)
                        "pccadem", "cadem" -> cur.copy(pcCaDem = amount)
                        "tienchuyencangoc", "chuyencan" -> cur.copy(tienChuyenCanGoc = amount)
                        "luongdongbaohiem", "baohiem", "lbh" -> cur.copy(luongDongBaoHiem = amount)
                        else -> cur
                    }
                    viewModel.updateSalaryConfig(updated)
                    processedKeys.add(key)
                    val fmt = DecimalFormat("#,###")
                    executedMessages.add("Cập nhật phụ cấp $name: ${fmt.format(amount)}đ")
                }
            }
            "UPDATE_LEAVE_QUOTA" -> {
                val quota = actionParam.trim().toIntOrNull()
                if (quota != null && userConfig != null) {
                    val updated = userConfig.copy(soNgayPhepNam = quota, phepNamConLai = quota)
                    viewModel.updateSalaryConfig(updated)
                    processedKeys.add(key)
                    executedMessages.add("Cập nhật Quỹ phép năm: $quota ngày")
                }
            }
            "UPDATE_USER_INFO" -> {
                val parts = actionParam.split("|")
                val uKey = parts.getOrNull(0)?.trim() ?: ""
                val uVal = parts.getOrNull(1)?.trim() ?: ""
                if (uKey.isNotBlank() && uVal.isNotBlank() && userConfig != null) {
                    val cur = userConfig
                    val updated = when (uKey.lowercase()) {
                        "hovaten", "ten", "name" -> cur.copy(hoVaTen = uVal)
                        "bophan", "phongban", "department" -> cur.copy(boPhan = uVal)
                        "manhanvien", "manv", "id" -> cur.copy(maNhanVien = uVal)
                        "sodienthoai", "sdt", "phone" -> cur.copy(soDienThoai = uVal)
                        "companyname", "congty" -> cur.copy(companyName = uVal)
                        "lichtrinh", "schedule" -> cur.copy(lichTrinh = uVal)
                        else -> cur
                    }
                    viewModel.updateSalaryConfig(updated)
                    processedKeys.add(key)
                    executedMessages.add("Cập nhật thông tin $uKey: $uVal")
                }
            }
            "UPDATE_CONFIG" -> {
                val parts = actionParam.split("|")
                val cKey = parts.getOrNull(0)?.trim() ?: ""
                val cVal = parts.getOrNull(1)?.trim() ?: ""
                if (cKey.isNotBlank() && cVal.isNotBlank() && userConfig != null) {
                    val cur = userConfig
                    val updated = when (cKey.lowercase()) {
                        "ngaychotluong" -> cur.copy(ngayChotLuong = cVal.toIntOrNull() ?: cur.ngayChotLuong)
                        "sogionghigiailao", "gionghi" -> cur.copy(soGioNghiGiaiLao = cVal.toDoubleOrNull() ?: cur.soGioNghiGiaiLao)
                        "tinhkhautrunghi" -> cur.copy(tinhKhauTruNghi = cVal.toBooleanStrictOrNull() ?: cur.tinhKhauTruNghi)
                        "hesootngaythuong" -> cur.copy(heSoOtNgayThuong = cVal.toDoubleOrNull() ?: cur.heSoOtNgayThuong)
                        "hesootchunhat" -> cur.copy(heSoOtChuNhat = cVal.toDoubleOrNull() ?: cur.heSoOtChuNhat)
                        "hesootngayle" -> cur.copy(heSoOtNgayLe = cVal.toDoubleOrNull() ?: cur.heSoOtNgayLe)
                        "hesootdem" -> cur.copy(heSoOtDem = cVal.toDoubleOrNull() ?: cur.heSoOtDem)
                        "cademstart" -> cur.copy(caDemStart = cVal)
                        "cademend" -> cur.copy(caDemEnd = cVal)
                        "lichtrinh" -> cur.copy(lichTrinh = cVal)
                        else -> cur
                    }
                    viewModel.updateSalaryConfig(updated)
                    processedKeys.add(key)
                    executedMessages.add("Cập nhật cài đặt $cKey: $cVal")
                }
            }
            "SELECT_MONTH" -> {
                if (actionParam.isNotBlank()) {
                    val rawMonth = actionParam.trim()
                    val normalizedMonth = if (rawMonth.contains("/")) {
                        val p = rawMonth.split("/")
                        if (p.size == 2) "${p[1]}-${p[0].padStart(2, '0')}" else rawMonth
                    } else rawMonth
                    viewModel.setSelectedMonth(normalizedMonth)
                    processedKeys.add(key)
                    executedMessages.add("Chuyển sang xem tháng $normalizedMonth")
                }
            }
            "MARK_NOTIFICATIONS_READ" -> {
                viewModel.markAllNotificationsAsRead()
                processedKeys.add(key)
                executedMessages.add("Đánh dấu đọc tất cả thông báo")
            }
            "SYNC_DATA" -> {
                viewModel.triggerSync()
                processedKeys.add(key)
                executedMessages.add("Đồng bộ dữ liệu lên máy chủ")
            }
            "UPDATE_NOTE" -> {
                if (actionParam.isNotBlank()) {
                    viewModel.updateActiveEntryNote(actionParam.trim())
                    processedKeys.add(key)
                    executedMessages.add("Cập nhật ghi chú: ${actionParam.trim()}")
                }
            }
            "NAVIGATE_TAB" -> {
                if (actionParam.isNotBlank()) {
                    val tabTarget = actionParam.trim().lowercase()
                    val mappedTab = when (tabTarget) {
                        "home", "trang chủ", "trangchu" -> "home"
                        "history", "lịch sử", "lichsu" -> "history"
                        "payslip", "phiếu lương", "phieuluong", "bảng lương", "bangluong" -> "payslip"
                        "settings", "cài đặt", "caidat" -> "settings"
                        "admin", "quản trị", "quantri" -> "admin"
                        "notifications", "thông báo", "thongbao" -> "notifications"
                        else -> tabTarget
                    }
                    onNavigateTab(mappedTab)
                    processedKeys.add(key)
                    executedMessages.add("Chuyển sang màn hình $mappedTab")
                }
            }
        }
    }

    /**
     * Resilient Intent Fallback Engine:
     * Quét ngữ nghĩa tự nhiên của User Prompt & AI Response để tìm các hành động chưa được thực thi.
     */
    private fun extractAndExecuteImplicitIntents(
        userPrompt: String,
        aiResponseText: String,
        context: Context,
        viewModel: TimeSnapViewModel,
        userConfig: UserConfig?,
        onNavigateTab: (String) -> Unit,
        processedKeys: MutableSet<String>,
        executedMessages: MutableList<String>
    ) {
        val p = userPrompt.lowercase()
        val r = aiResponseText.lowercase()

        // 1. INTENT: CHẤM CÔNG VÀO / RA CA
        if ((p.contains("chấm công vào") || p.contains("vào ca") || p.contains("bấm công vào")) && !processedKeys.contains("CHECK_IN:")) {
            viewModel.toggleCheckIn(note = "AI nhận diện vào ca")
            processedKeys.add("CHECK_IN:")
            executedMessages.add("Chấm công vào ca thành công")
        } else if ((p.contains("chấm công ra") || p.contains("ra ca") || p.contains("bấm công ra") || p.contains("tan ca")) && !processedKeys.contains("CHECK_OUT:")) {
            viewModel.toggleCheckIn(note = "AI nhận diện ra ca")
            processedKeys.add("CHECK_OUT:")
            executedMessages.add("Chấm công ra ca thành công")
        }

        // 2. INTENT: CHẤM CÔNG / THÊM NGÀY CÔNG CỤ THỂ (e.g. "chấm công ngày 31", "thêm công ngày 28, 29, 30, 31")
        if (p.contains("chấm công") || p.contains("thêm công") || p.contains("ghi nhận công") || p.contains("đi làm") ||
            r.contains("đã chấm công") || r.contains("đã ghi nhận công") || r.contains("đã thêm công")) {
            
            val extractedDates = extractDatesFromText(userPrompt)
            for (d in extractedDates) {
                val normDate = resolveDateToDmy(d)
                if (normDate.isNotBlank() && !processedKeys.contains("ADD_WORK_DATE:$normDate") && !processedKeys.contains("DELETE_DATE:$normDate")) {
                    val isLeave = p.contains("nghỉ phép") || p.contains("phép năm") || p.contains("nghỉ không lương")
                    val dayType = if (p.contains("nghỉ không lương")) "UNPAID_LEAVE" else if (isLeave) "PAID_LEAVE" else "NORMAL"
                    
                    viewModel.addSingleEntry(
                        dateStr = normDate,
                        checkInHour = 8,
                        checkInMin = 0,
                        checkOutHour = 17,
                        checkOutMin = 0,
                        dayTypeOverride = dayType,
                        noteStr = "AI tự động ghi nhận công"
                    )
                    processedKeys.add("ADD_WORK_DATE:$normDate")
                    val label = if (dayType == "PAID_LEAVE") "nghỉ phép" else if (dayType == "UNPAID_LEAVE") "nghỉ không lương" else "ngày công"
                    executedMessages.add("Ghi nhận $label ngày $normDate (08:00 - 17:00)")
                }
            }
        }

        // 3. INTENT: XÓA NGÀY CÔNG (e.g. "xóa ngày 31", "hủy công ngày 15")
        if (p.contains("xóa ngày") || p.contains("hủy ngày") || p.contains("bỏ công ngày") || p.contains("xóa công")) {
            val extractedDates = extractDatesFromText(userPrompt)
            for (d in extractedDates) {
                val normDate = resolveDateToDmy(d)
                if (normDate.isNotBlank() && !processedKeys.contains("DELETE_DATE:$normDate")) {
                    viewModel.deleteEntryByDateString(normDate)
                    processedKeys.add("DELETE_DATE:$normDate")
                    executedMessages.add("Xóa ngày công $normDate")
                }
            }
        }

        // 4. INTENT: ĐỔI LƯƠNG CƠ BẢN (e.g. "đổi lương cơ bản thành 12 triệu", "lương 15tr")
        if ((p.contains("lương cơ bản") || p.contains("lcb")) && (p.contains("thành") || p.contains("là") || p.contains("đổi") || p.contains("chỉnh"))) {
            val money = extractFirstMoneyValue(userPrompt)
            if (money != null && userConfig != null && !processedKeys.contains("UPDATE_BASE_SALARY:$money")) {
                val updated = userConfig.copy(luongCoBan = money)
                viewModel.updateSalaryConfig(updated)
                processedKeys.add("UPDATE_BASE_SALARY:$money")
                val fmt = DecimalFormat("#,###")
                executedMessages.add("Cập nhật Lương cơ bản: ${fmt.format(money)}đ")
            }
        }

        // 5. INTENT: ĐỔI PHỤ CẤP CƠM / TRÁCH NHIỆM / XĂNG XE...
        if (p.contains("phụ cấp") || p.contains("tiền cơm") || p.contains("cơm ca") || p.contains("xăng xe") || p.contains("tiền nhà")) {
            val money = extractFirstMoneyValue(userPrompt)
            if (money != null && userConfig != null) {
                var allowanceMatched = false
                val cur = userConfig
                val updated = when {
                    p.contains("cơm") -> {
                        allowanceMatched = true
                        cur.copy(pcComCa = money)
                    }
                    p.contains("xăng") -> {
                        allowanceMatched = true
                        cur.copy(pcXangXe = money)
                    }
                    p.contains("nhà") -> {
                        allowanceMatched = true
                        cur.copy(pcNhaO = money)
                    }
                    p.contains("trách nhiệm") -> {
                        allowanceMatched = true
                        cur.copy(pcTrachNhiem = money)
                    }
                    p.contains("kỹ thuật") -> {
                        allowanceMatched = true
                        cur.copy(pcKyThuat = money)
                    }
                    p.contains("chức vụ") -> {
                        allowanceMatched = true
                        cur.copy(pcChucVu = money)
                    }
                    p.contains("chuyên cần") -> {
                        allowanceMatched = true
                        cur.copy(tienChuyenCanGoc = money)
                    }
                    else -> cur
                }
                if (allowanceMatched && !processedKeys.contains("IMPLICIT_ALLOWANCE:$money")) {
                    viewModel.updateSalaryConfig(updated)
                    processedKeys.add("IMPLICIT_ALLOWANCE:$money")
                    val fmt = DecimalFormat("#,###")
                    executedMessages.add("Cập nhật phụ cấp: ${fmt.format(money)}đ")
                }
            }
        }

        // 6. INTENT: CHUYỂN MÀN HÌNH (e.g. "mở phiếu lương", "xem lịch sử", "vào cài đặt")
        if (p.contains("phiếu lương") || p.contains("bảng lương")) {
            if (!processedKeys.contains("NAVIGATE:payslip")) {
                onNavigateTab("payslip")
                processedKeys.add("NAVIGATE:payslip")
                executedMessages.add("Chuyển sang màn hình Phiếu lương")
            }
        } else if (p.contains("lịch sử") || p.contains("nhật ký công")) {
            if (!processedKeys.contains("NAVIGATE:history")) {
                onNavigateTab("history")
                processedKeys.add("NAVIGATE:history")
                executedMessages.add("Chuyển sang màn hình Lịch sử")
            }
        } else if (p.contains("cài đặt") || p.contains("thiết lập")) {
            if (!processedKeys.contains("NAVIGATE:settings")) {
                onNavigateTab("settings")
                processedKeys.add("NAVIGATE:settings")
                executedMessages.add("Chuyển sang màn hình Cài đặt")
            }
        } else if (p.contains("trang chủ") || p.contains("màn hình chính")) {
            if (!processedKeys.contains("NAVIGATE:home")) {
                onNavigateTab("home")
                processedKeys.add("NAVIGATE:home")
                executedMessages.add("Chuyển về Trang chủ")
            }
        }

        // 7. INTENT: ĐỒNG BỘ DỮ LIỆU
        if (p.contains("đồng bộ") || p.contains("sync")) {
            if (!processedKeys.contains("SYNC")) {
                viewModel.triggerSync()
                processedKeys.add("SYNC")
                executedMessages.add("Đồng bộ dữ liệu lên máy chủ")
            }
        }
    }

    /**
     * Resolves natural language date references to dd/MM/yyyy:
     * - "31" -> "31/08/2026" (current month/year)
     * - "31/08" -> "31/08/2026"
     * - "2026-08-31" -> "31/08/2026"
     * - "hôm nay" -> today in dd/MM/yyyy
     * - "hôm qua" -> yesterday in dd/MM/yyyy
     * - "hôm kia" -> 2 days ago in dd/MM/yyyy
     * - "cuối tháng" -> last day of current month in dd/MM/yyyy
     */
    fun resolveDateToDmy(input: String): String {
        val s = input.trim().lowercase()
        val cal = Calendar.getInstance()

        when {
            s == "hôm nay" || s == "today" -> {
                val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                val y = cal.get(Calendar.YEAR)
                return "$d/$m/$y"
            }
            s == "hôm qua" || s == "yesterday" -> {
                cal.add(Calendar.DAY_OF_MONTH, -1)
                val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                val y = cal.get(Calendar.YEAR)
                return "$d/$m/$y"
            }
            s == "hôm kia" -> {
                cal.add(Calendar.DAY_OF_MONTH, -2)
                val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                val y = cal.get(Calendar.YEAR)
                return "$d/$m/$y"
            }
            s == "cuối tháng" || s == "ngày cuối tháng" -> {
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                val y = cal.get(Calendar.YEAR)
                return "${maxDay.toString().padStart(2, '0')}/$m/$y"
            }
            s == "đầu tháng" || s == "ngày đầu tháng" -> {
                val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                val y = cal.get(Calendar.YEAR)
                return "01/$m/$y"
            }
        }

        // Just day number e.g. "31" or "ngày 31"
        val dayOnlyMatch = Regex("(?:ngày\\s+)?(\\d{1,2})$").matchEntire(s)
        if (dayOnlyMatch != null) {
            val dayInt = dayOnlyMatch.groupValues[1].toIntOrNull()
            if (dayInt != null && dayInt in 1..31) {
                val d = dayInt.toString().padStart(2, '0')
                val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                val y = cal.get(Calendar.YEAR)
                return "$d/$m/$y"
            }
        }

        // Standard formatting via SalaryCalculator
        return SalaryCalculator.normalizeDateToDmy(input)
    }

    /**
     * Extracts date candidates from user natural language prompt.
     * Finds "ngày 31", "ngày 28, 29, 30", "31/08", "từ ngày 25 đến 28", "hôm nay", "hôm qua", "cuối tháng".
     */
    fun extractDatesFromText(text: String): List<String> {
        val results = mutableListOf<String>()
        val lower = text.lowercase()

        if (lower.contains("hôm nay")) results.add("hôm nay")
        if (lower.contains("hôm qua")) results.add("hôm qua")
        if (lower.contains("hôm kia")) results.add("hôm kia")
        if (lower.contains("cuối tháng")) results.add("cuối tháng")
        if (lower.contains("đầu tháng")) results.add("đầu tháng")

        // Range pattern: "từ ngày 25 đến ngày 28" or "từ 25 đến 28"
        val rangeMatch = Regex("(?:từ\\s+(?:ngày\\s+)?)(\\d{1,2})\\s+(?:đến|tới)\\s+(?:ngày\\s+)?(\\d{1,2})").find(lower)
        if (rangeMatch != null) {
            val startDay = rangeMatch.groupValues[1].toIntOrNull() ?: 1
            val endDay = rangeMatch.groupValues[2].toIntOrNull() ?: 1
            if (startDay in 1..31 && endDay in 1..31 && startDay <= endDay) {
                for (day in startDay..endDay) {
                    results.add(day.toString())
                }
                return results
            }
        }

        // Specific dates pattern: "ngày 28, 29, 30, 31" or "ngày 31"
        val datesMatch = Regex("(?:các\\s+)?ngày\\s+([0-9\\s,/-]+)").find(lower)
        if (datesMatch != null) {
            val rawNumbers = datesMatch.groupValues[1]
            val splitNumbers = rawNumbers.split(",", " và ", " & ").map { it.trim() }
            for (num in splitNumbers) {
                val dInt = num.toIntOrNull()
                if (dInt != null && dInt in 1..31) {
                    results.add(dInt.toString())
                } else if (num.contains("/") || num.contains("-")) {
                    results.add(num)
                }
            }
        }

        // Direct full date matches (e.g. 31/08/2026 or 2026-08-31 or 31/08)
        val fullDateMatches = Regex("\\b\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{4})?\\b").findAll(text)
        for (m in fullDateMatches) {
            if (!results.contains(m.value)) {
                results.add(m.value)
            }
        }

        // Standalone trailing day e.g. "chấm công 31"
        if (results.isEmpty()) {
            val singleNumber = Regex("\\b(\\d{1,2})\\b").findAll(text).mapNotNull { it.groupValues[1].toIntOrNull() }.filter { it in 1..31 }
            for (sn in singleNumber) {
                results.add(sn.toString())
            }
        }

        return results.distinct()
    }

    /**
     * Parses money values in natural Vietnamese:
     * - "50k" -> 50,000
     * - "500k" -> 500,000
     * - "1tr" / "1 triệu" / "1 củ" -> 1,000,000
     * - "1.5tr" / "1.5 triệu" / "1,5tr" -> 1,500,000
     * - "12.000.000" -> 12,000,000
     */
    fun parseMoneyValue(rawInput: String): Double? {
        val s = rawInput.trim().lowercase().replace("đ", "").replace("vnd", "").replace("vnđ", "").trim()
        if (s.isBlank()) return null

        // "12.000.000" or "12,000,000"
        if (Regex("^\\d{1,3}(?:[.,]\\d{3})+$").matches(s)) {
            val clean = s.replace(".", "").replace(",", "")
            return clean.toDoubleOrNull()
        }

        // "50k", "500k"
        val kMatch = Regex("^([0-9.]+)\\s*k$").matchEntire(s)
        if (kMatch != null) {
            val num = kMatch.groupValues[1].toDoubleOrNull() ?: return null
            return num * 1000.0
        }

        // "1.5tr", "12tr", "1.5 triệu", "12 triệu", "10 củ"
        val trMatch = Regex("^([0-9.,]+)\\s*(?:tr|triệu|trieu|củ|cu)$").matchEntire(s)
        if (trMatch != null) {
            val num = trMatch.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return null
            return num * 1000000.0
        }

        // Plain raw number
        val rawNum = s.replace(".", "").replace(",", "").toDoubleOrNull()
        return rawNum
    }

    private fun extractFirstMoneyValue(text: String): Double? {
        val lower = text.lowercase()
        // Try match "12 triệu", "12tr", "500k", "12.000.000"
        val matches = Regex("([0-9.,]+)\\s*(?:k|tr|triệu|trieu|củ|cu|đ|vnd|vnđ)?").findAll(lower)
        for (m in matches) {
            val candidate = m.value.trim()
            val parsed = parseMoneyValue(candidate)
            if (parsed != null && parsed >= 1000) {
                return parsed
            }
        }
        return null
    }

    private fun parseHourMinute(timeStr: String, defaultHour: Int, defaultMinute: Int): Pair<Int, Int> {
        val p = timeStr.trim().split(":")
        val h = p.getOrNull(0)?.toIntOrNull() ?: defaultHour
        val m = p.getOrNull(1)?.toIntOrNull() ?: defaultMinute
        return Pair(h, m)
    }
}
