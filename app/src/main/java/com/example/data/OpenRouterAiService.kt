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

object OpenRouterAiService {
    private const val BASE_URL = "https://openrouter.ai/api/v1/chat/completions"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateContent(
        apiKey: String,
        model: String = "meta-llama/llama-3.3-70b-instruct:free",
        userPrompt: String,
        contextData: String,
        chatHistory: List<Pair<String, String>> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Chưa cài đặt OpenRouter API Key."))
        }

        try {
            val requestJson = JSONObject()
            val selectedModel = if (model.isBlank()) "meta-llama/llama-3.3-70b-instruct:free" else model
            requestJson.put("model", selectedModel)

            val messagesArray = JSONArray()

            // System instruction
            val systemMsg = JSONObject()
            systemMsg.put("role", "system")
            systemMsg.put("content", """
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
                  + Chấm công vào ca: [[ACTION:CHECK_IN]] hoặc [[ACTION:CHECK_IN:08:00]]
                  + Chấm công ra ca: [[ACTION:CHECK_OUT]] hoặc [[ACTION:CHECK_OUT:17:30]]
                  + Thêm/Làm bù 1 ngày công: [[ACTION:ADD_WORK_DAY:YYYY-MM-DD|08:00|17:00|NORMAL|Ghi chú]] (ví dụ: [[ACTION:ADD_WORK_DAY:2026-08-31|08:00|17:00|NORMAL|Chấm công ngày 31]])
                  + Thêm ngày nghỉ phép/lễ: [[ACTION:ADD_LEAVE_DAY:YYYY-MM-DD|PAID_LEAVE|Nghỉ phép năm]] hoặc [[ACTION:ADD_LEAVE_DAY:YYYY-MM-DD|UNPAID_LEAVE|Nghỉ việc riêng]] hoặc [[ACTION:ADD_LEAVE_DAY:YYYY-MM-DD|HOLIDAY|Nghỉ lễ 30/4]]
                  + Thêm nhiều ngày công hàng loạt: [[ACTION:ADD_BULK_WORK_DAYS:2026-08-01,2026-08-02,2026-08-03|08:00|17:00]]
                  + Xóa 1 ngày công: [[ACTION:DELETE_DATE:YYYY-MM-DD]] (ví dụ: [[ACTION:DELETE_DATE:2026-08-15]])
                  + Xóa nhiều ngày công: [[ACTION:DELETE_DATES:YYYY-MM-DD,YYYY-MM-DD]]
                  + Xóa toàn bộ công tháng này: [[ACTION:CLEAR_MONTH]]
                  + Cập nhật Lương cơ bản: [[ACTION:UPDATE_BASE_SALARY:12000000]]
                  + Cập nhật Phụ cấp: [[ACTION:UPDATE_ALLOWANCE:pcTrachNhiem|1000000]] (các phụ cấp: pcKyThuat, pcTrachNhiem, pcChucVu, pcHieuSuat, pcSanPham, pcComCa, pcComOt, pcNhaO, pcDocHai, pcDtDoanhThu, pcXangXe, pcThamNien, pcCaDem, tienChuyenCanGoc, luongDongBaoHiem, doanPhiCongDoan, tiLeDongBaoHiem)
                  + Cập nhật Quỹ phép năm: [[ACTION:UPDATE_LEAVE_QUOTA:12]]
                  + Cập nhật Thông tin cá nhân: [[ACTION:UPDATE_USER_INFO:hoVaTen|Nguyễn Văn A]] hoặc [[ACTION:UPDATE_USER_INFO:boPhan|Kỹ thuật]] hoặc [[ACTION:UPDATE_USER_INFO:maNhanVien|NV001]] hoặc [[ACTION:UPDATE_USER_INFO:soDienThoai|0912345678]]
                  + Cập nhật Cài đặt hệ thống: [[ACTION:UPDATE_CONFIG:ngayChotLuong|25]] hoặc [[ACTION:UPDATE_CONFIG:soGioNghiGiaiLao|1.5]] hoặc [[ACTION:UPDATE_CONFIG:lichTrinh|08:00 - 17:00]]
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
                   - Bảng Lương Thực Tế: Nguyên tắc "Làm đến đâu tính đến đó". Chỉ tính ngày công, OT, ca đêm, phụ cấp thực tế phát sinh từ ngày 1 đến HÔM NAY.
                   - Bảng Lương Dự Kiến: Bằng [Công Thực tế đến hôm nay] + [Dự báo các ngày còn lại trong tháng].
                
                QUY TẮC TRẢ LỜI (BẮT BUỘC):
                1. TRẢ LỜI ĐÚNG TRỌNG TÂM, NGẮN GỌN, SÚC TÍCH.
                2. VÀO THẲNG CÂU TRẢ LỜI & CON SỐ, KHÔNG CHÀO HỎI LÊ THÊ, KHÔNG DÀI DÒNG VÒNG VÈO.
                
                DỮ LIỆU THỰC TẾ & CẤU HÌNH NGƯỜI DÙNG:
                $contextData
            """.trimIndent())
            messagesArray.put(systemMsg)

            // Conversation history
            for (turn in chatHistory) {
                val turnObj = JSONObject()
                turnObj.put("role", if (turn.first == "user") "user" else "assistant")
                turnObj.put("content", turn.second)
                messagesArray.put(turnObj)
            }

            // Current prompt
            val userMsg = JSONObject()
            userMsg.put("role", "user")
            userMsg.put("content", userPrompt)
            messagesArray.put(userMsg)

            requestJson.put("messages", messagesArray)

            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("HTTP-Referer", "https://timesnap.app")
                .addHeader("X-Title", "TimeSnap Pro")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBodyStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errObj = JSONObject(responseBodyStr).optJSONObject("error")
                    val message = errObj?.optString("message") ?: "Lỗi kết nối OpenRouter (${response.code})"
                    if (response.code == 401 || message.lowercase().contains("api key")) {
                        "OpenRouter API Key không hợp lệ. Vui lòng kiểm tra lại Key!"
                    } else if (response.code == 429) {
                        "Đã vượt quá hạn mức sử dụng OpenRouter. Vui lòng thử lại sau giây lát!"
                    } else {
                        "Lỗi OpenRouter: $message"
                    }
                } catch (e: Exception) {
                    "Yêu cầu thất bại (Mã lỗi ${response.code})"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val resObj = JSONObject(responseBodyStr)
            val choices = resObj.optJSONArray("choices")
            val firstChoice = choices?.optJSONObject(0)
            val messageObj = firstChoice?.optJSONObject("message")
            val textResult = messageObj?.optString("content")

            if (!textResult.isNullOrBlank()) {
                Result.success(textResult)
            } else {
                Result.failure(Exception("OpenRouter AI không phản hồi nội dung."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
