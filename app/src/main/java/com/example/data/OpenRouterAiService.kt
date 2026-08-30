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
                
                QUYỀN HẠN & TRUY CẬP DỮ LIỆU:
                - Bạn ĐƯỢC PHÂN QUYỀN TRUY CẬP TOÀN BỘ DỮ LIỆU NGƯỜI DÙNG: Cài đặt lương, đơn giá giờ công, hệ số OT, chi tiết 12 phụ cấp, thống kê tháng hiện tại (giờ chuẩn, OT ngày, OT đêm, Chủ nhật ngày/đêm, Ngày lễ, tiền từng khoản), và lịch sử các tháng trước.
                - TUYỆT ĐỐI KHÔNG BAO GIỜ nói "không có quyền", "hệ thống khóa dữ liệu" hay "thiếu thông tin".
                
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
