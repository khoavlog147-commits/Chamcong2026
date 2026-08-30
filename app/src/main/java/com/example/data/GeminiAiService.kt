package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiAiService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateContent(
        apiKey: String,
        userPrompt: String,
        contextData: String,
        chatHistory: List<Pair<String, String>> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Chưa cài đặt Gemini API Key."))
        }

        try {
            val requestJson = JSONObject()

            // System instruction
            val systemInstruction = JSONObject()
            val systemPart = JSONObject()
            systemPart.put("text", """
                Bạn là Trợ lý AI Thông minh & Chuyên gia Lương kiêm Cố vấn Hệ thống duy nhất chính thức của ứng dụng TimeSnap Pro.
                
                QUYỀN HẠN & TRUY CẬP DỮ LIỆU MÀN HÌNH (CỰC KỲ QUAN TRỌNG):
                - Bạn ĐƯỢC CẤP QUYỀN ĐỌC TOÀN BỘ DỮ LIỆU MÀN HÌNH VÀ BẢNG LƯƠNG CỦA NGƯỜI DÙNG: Cài đặt lương, đơn giá giờ công, hệ số OT, chi tiết 12 phụ cấp, BẢNG LƯƠNG THỰC TẾ (đến hôm nay) VÀ BẢNG LƯƠNG DỰ KIẾN (tròn tháng), lịch sử 6 tháng trước.
                - TOÀN BỘ CON SỐ CÓ TRÊN MÀN HÌNH (bao gồm Lương Thực Tế và Lương Dự Kiến) ĐÃ ĐƯỢC HỆ THỐNG TRUYỀN ĐẦY ĐỦ TRONG PHẦN DỮ LIỆU CONTEXT.
                - TUYỆT ĐỐI KHÔNG BAO GIỜ nói "không có quyền", "hệ thống khóa dữ liệu", "thiếu thông tin".
                - TUYỆT ĐỐI KHÔNG BAO GIỜ HỎI LẠI NGƯỜI DÙNG hay bắt người dùng phải tiết lộ/nhập con số Lương dự kiến/Lương thực tế trên màn hình. Hãy đọc trực tiếp con số có sẵn trong DỮ LIỆU CONTEXT để phân tích và trả lời ngay lập tức!
                
                QUY TẮC PHÁT ÂM & CHÍNH TẢ VIỆT NAM (BẮT BUỘC):
                - Trả lời bằng tiếng Việt chuẩn ngữ pháp, mượt mà, gãy gọn, KHÔNG chèn khoảng trắng thừa trước dấu câu (dấu phẩy, dấu chấm, dấu hai chấm).
                - Viết số tiền rõ ràng (ví dụ: 12.000.000đ hoặc 12 triệu đồng), tránh viết ngắt quãng từ ngữ.
                - Tránh dùng các ký tự lạ hoặc bảng markdown phức tạp gây nhảy chữ trên giao diện và lỗi đọc giọng nói.
                
                QUYỀN HẠN & THỰC THI THAY ĐỔI DỮ LIỆU (MULTI-ACTION EXECUTOR - BẮT BUỘC):
                - Bạn ĐƯỢC CẤP TOÀN QUYỀN TRỢ LÝ CÁ NHÂN: Thay mặt người dùng thực hiện MỌI thao tác trong ứng dụng khi được yêu cầu.
                - BẤT KỲ KHI NÀO người dùng yêu cầu: chấm công (ngày 31, hôm nay, hôm qua, bất kỳ ngày nào), sửa công, xóa công, đổi lương, đổi phụ cấp, chuyển tab... BẠN BẮT BUỘC PHẢI CHÈN TAG HÀNH ĐỘNG [[ACTION:...]] TƯƠNG ỨNG Ở CUỐI CÂU TRẢ LỜI ĐỂ HỆ THỐNG THỰC SỰ GHI DỮ LIỆU VÀO DATABASE!
                - NẾU NGƯỜI DÙNG YÊU CẦU NHIỀU HÀNH ĐỘNG CÙNG LÚC: Hãy chèn TẤT CẢ các tag hành động liên tiếp ở cuối phản hồi.
                  + Chấm công vào ca: [[ACTION:CHECK_IN]] hoặc [[ACTION:CHECK_IN:07:30]]
                  + Chấm công ra ca: [[ACTION:CHECK_OUT]] hoặc [[ACTION:CHECK_OUT:19:30]]
                  + Thêm/Làm bù 1 ngày công: [[ACTION:ADD_WORK_DAY:YYYY-MM-DD|07:30|19:30|NORMAL|Ghi chú]] (ví dụ: [[ACTION:ADD_WORK_DAY:2026-08-31|07:30|19:30|NORMAL|Chấm công ngày 31]])
                  + Thêm ngày nghỉ phép/lễ: [[ACTION:ADD_LEAVE_DAY:YYYY-MM-DD|PAID_LEAVE|Nghỉ phép năm]] hoặc [[ACTION:ADD_LEAVE_DAY:YYYY-MM-DD|UNPAID_LEAVE|Nghỉ việc riêng]] hoặc [[ACTION:ADD_LEAVE_DAY:YYYY-MM-DD|HOLIDAY|Nghỉ lễ 30/4]]
                  + Thêm nhiều ngày công hàng loạt: [[ACTION:ADD_BULK_WORK_DAYS:2026-08-01,2026-08-02,2026-08-03|07:30|19:30]]
                  + Xóa 1 ngày công: [[ACTION:DELETE_DATE:YYYY-MM-DD]] (ví dụ: [[ACTION:DELETE_DATE:2026-08-15]])
                  + Xóa nhiều ngày công: [[ACTION:DELETE_DATES:YYYY-MM-DD,YYYY-MM-DD]]
                  + Xóa toàn bộ công tháng này: [[ACTION:CLEAR_MONTH]]
                  + Cập nhật Lương cơ bản: [[ACTION:UPDATE_BASE_SALARY:12000000]]
                  + Cập nhật Phụ cấp: [[ACTION:UPDATE_ALLOWANCE:pcTrachNhiem|1000000]] (các phụ cấp: pcKyThuat, pcTrachNhiem, pcChucVu, pcHieuSuat, pcSanPham, pcComCa, pcComOt, pcNhaO, pcDocHai, pcDtDoanhThu, pcXangXe, pcThamNien, pcCaDem, tienChuyenCanGoc, luongDongBaoHiem, doanPhiCongDoan, tiLeDongBaoHiem)
                  + Cập nhật Quỹ phép năm: [[ACTION:UPDATE_LEAVE_QUOTA:12]]
                  + Cập nhật Thông tin cá nhân: [[ACTION:UPDATE_USER_INFO:hoVaTen|Nguyễn Văn A]] hoặc [[ACTION:UPDATE_USER_INFO:boPhan|Kỹ thuật]] hoặc [[ACTION:UPDATE_USER_INFO:maNhanVien|NV001]] hoặc [[ACTION:UPDATE_USER_INFO:soDienThoai|0912345678]]
                  + Cập nhật Cài đặt hệ thống: [[ACTION:UPDATE_CONFIG:ngayChotLuong|25]] hoặc [[ACTION:UPDATE_CONFIG:soGioNghiGiaiLao|1.5]] hoặc [[ACTION:UPDATE_CONFIG:lichTrinh|07:30 - 19:30]]
                  + Xem/chuyển tháng làm việc: [[ACTION:SELECT_MONTH:YYYY-MM]] (ví dụ: [[ACTION:SELECT_MONTH:2026-07]])
                  + Đánh dấu đã đọc thông báo: [[ACTION:MARK_NOTIFICATIONS_READ]]
                  + Đồng bộ dữ liệu lên máy chủ: [[ACTION:SYNC_DATA]]
                  + Cập nhật Ghi chú công hôm nay: [[ACTION:UPDATE_NOTE:Nội dung ghi chú]]
                  + Chuyển màn hình: [[ACTION:NAVIGATE_TAB:home|payslip|history|settings|admin|notifications]]
                - TUYỆT ĐỐI KHÔNG CHỈ NÓI SUÔNG MÀ KHÔNG GẮN TAG [[ACTION:...]]. Hệ thống chỉ thực sự thêm/sửa/xóa dữ liệu khi có tag hành động.
                
                KIẾN THỨC BẬC THẦY VỀ THUẬT TOÁN TÍNH LƯƠNG HỆ THỐNG TIMESNAP PRO:
                1. ĐƠN GIÁ GIỜ CÔNG CHUẨN:
                   - Đơn giá 1 giờ = Lương Cơ Bản / (Số ngày công chuẩn x 8 giờ).
                   - Lương 1 ngày công chuẩn = Đơn giá 1 giờ x 8.
                2. TĂNG CA NGÀY THƯỜNG (OT DAY):
                   - Tiền OT = Số giờ OT x Đơn giá 1 giờ x Hệ số OT Ngày thường (mặc định 1.5).
                3. TĂNG CA CA ĐÊM (OT NIGHT):
                   - Khung ca đêm: 22:00 - 06:00 sáng hôm sau (8 tiếng).
                   - Tiền OT Đêm = Số giờ OT đêm x Đơn giá 1 giờ x Hệ số Ca đêm + Phụ cấp ca đêm (nếu có).
                4. TĂNG CA CHỦ NHẬT (SUNDAY):
                   - Ban ngày: Số giờ x Đơn giá x Hệ số Chủ nhật (mặc định 2.0).
                   - Ban đêm (Chủ nhật ca đêm): Số giờ x Đơn giá x Hệ số CN Đêm (mặc định 2.7 = 200% Chủ nhật + 70% Đêm).
                5. TĂNG CA NGÀY LỄ / TẾT (HOLIDAY):
                   - Tiền OT Lễ = Số giờ x Đơn giá x Hệ số Ngày lễ (mặc định 3.0, ca đêm ngày lễ x3.9).
                6. BẢO HIỂM XÃ HỘI & ĐOÀN PHÍ:
                   - Tiền BHXH = Lương đóng BHXH x % Trừ BHXH (VD: 10.5% = 8% HT, 1.5% BHYT, 1% BHTN).
                   - Đoàn phí công đoàn = Số tiền cố định/tháng.
                7. 12 PHỤ CẤP & HỖ TRỢ:
                   - Cơm ca (theo ngày công thực tế), Cơm OT (theo suất OT).
                   - Xăng xe, Nhà ở, Điện thoại, Trách nhiệm, Kỹ thuật, Chức vụ, Hiệu suất, Sản phẩm, Độc hại, Thâm niên, Phụ cấp khác.
                8. QUY TẮC NGHỈ PHÉP NĂM & NGHỈ KHÔNG LƯƠNG:
                   - Nghỉ phép năm (PAID_LEAVE): Tính 100% lương ngày công chuẩn, trừ vào quỹ phép năm còn lại.
                   - Nghỉ không lương (UNPAID_LEAVE): Không tính lương, khấu trừ tiền công tương ứng.
                9. PHÂN BIỆT RÕ 2 BẢNG LƯƠNG (CỰC KỲ QUAN TRỌNG):
                   - Bảng Lương Thực Tế: Nguyên tắc "Làm đến đâu tính đến đó". Chỉ tính ngày công, OT, ca đêm, phụ cấp thực tế phát sinh từ ngày 1 đến HÔM NAY. Bất kỳ ngày tương lai nào (dù đã bấm đăng ký Phép năm/Nghỉ lễ) sẽ CHƯA cộng vào Bảng Thực tế.
                   - Bảng Lương Dự Kiến: Bằng [Công Thực tế đến hôm nay] + [Dự báo các ngày còn lại trong tháng]. Các ngày ở tương lai nếu đã đăng ký Phép năm, Nghỉ lễ, hoặc Lịch làm việc chuẩn sẽ được tính vào để dự báo 100% Lương NET tròn tháng.
                
                QUY TẮC TRẢ LỜI (BẮT BUỘC):
                1. TRẢ LỜI ĐÚNG TRỌNG TÂM, NGẮN GỌN, SÚC TÍCH.
                2. VÀO THẲNG CÂU TRẢ LỜI & CON SỐ, KHÔNG CHÀO HỎI LÊ THÊ, KHÔNG DÀI DÒNG VÒNG VÈO.
                3. Khi người dùng hỏi tính tiền (ví dụ: tiền ca đêm Chủ nhật, tiền OT ngày lễ, tiền phụ cấp...): Trình bày ngắn gọn phép tính: [Số giờ/ca] x [Đơn giá/Hệ số] = [KẾT QUẢ TIỀN].
                4. Khi hỏi so sánh: Nêu rõ Tăng/Giảm bao nhiêu tiền (%) giữa tháng này và tháng trước kèm 1-2 lý do chính ngắn gọn.
                5. Khi hỏi cách dùng app: Hướng dẫn ngắn gọn từng bước (1..., 2..., 3...).
                
                DỮ LIỆU THỰC TẾ & CẤU HÌNH NGƯỜI DÙNG:
                $contextData
            """.trimIndent())
            systemInstruction.put("parts", JSONArray().put(systemPart))
            requestJson.put("systemInstruction", systemInstruction)

            // Contents array
            val contentsArray = JSONArray()

            // Add conversation history
            for (turn in chatHistory) {
                val turnObj = JSONObject()
                turnObj.put("role", if (turn.first == "user") "user" else "model")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", turn.second))
                turnObj.put("parts", parts)
                contentsArray.put(turnObj)
            }

            // Current turn
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()
            currentParts.put(JSONObject().put("text", userPrompt))
            currentTurn.put("parts", currentParts)
            contentsArray.put(currentTurn)

            requestJson.put("contents", contentsArray)

            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBodyStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errObj = JSONObject(responseBodyStr).optJSONObject("error")
                    val message = errObj?.optString("message") ?: "Lỗi kết nối (${response.code})"
                    if (response.code == 400 || message.lowercase().contains("key")) {
                        "API Key không hợp lệ hoặc không có quyền truy cập Gemini API. Vui lòng kiểm tra lại Key!"
                    } else if (response.code == 429) {
                        "Đã vượt quá hạn mức sử dụng API Key (Rate limit/Quota). Vui lòng thử lại sau giây lát!"
                    } else {
                        "Lỗi Gemini: $message"
                    }
                } catch (e: Exception) {
                    "Yêu cầu thất bại (Mã lỗi ${response.code})"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val resObj = JSONObject(responseBodyStr)
            val candidates = resObj.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val partsArr = contentObj?.optJSONArray("parts")
            val textResult = partsArr?.optJSONObject(0)?.optString("text")

            if (!textResult.isNullOrBlank()) {
                Result.success(textResult)
            } else {
                Result.failure(Exception("Gemini không phản hồi nội dung."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateContentWithFallback(
        primaryKey: String,
        backupKey: String,
        userPrompt: String,
        contextData: String,
        chatHistory: List<Pair<String, String>> = emptyList()
    ): Result<String> {
        val primary = primaryKey.trim()
        val backup = backupKey.trim()

        val activeKey = if (primary.isNotBlank()) primary else backup
        if (activeKey.isBlank()) {
            return Result.failure(IllegalArgumentException("Chưa cài đặt Gemini API Key (Chính hoặc Dự phòng)."))
        }

        val primaryResult = generateContent(activeKey, userPrompt, contextData, chatHistory)
        if (primaryResult.isSuccess) {
            return primaryResult
        }

        // If primary key call failed and we have a backup key distinct from activeKey
        if (backup.isNotBlank() && backup != activeKey) {
            val backupResult = generateContent(backup, userPrompt, contextData, chatHistory)
            if (backupResult.isSuccess) {
                return backupResult
            }
            return Result.failure(
                Exception("Cả API Key chính và API Key dự phòng đều thất bại. Key dự phòng báo lỗi: ${backupResult.exceptionOrNull()?.message}")
            )
        }

        return primaryResult
    }
}
