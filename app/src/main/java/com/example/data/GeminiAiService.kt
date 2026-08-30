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
}
