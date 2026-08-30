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
                Bạn là Trợ lý AI Thông minh & Chuyên gia Lương duy nhất chính thức của ứng dụng TimeSnap Pro.
                
                QUYỀN HẠN & TRUY CẬP DỮ LIỆU:
                - Bạn ĐƯỢC PHÂN QUYỀN TRUY CẬP TOÀN BỘ DỮ LIỆU NGƯỜI DÙNG: Cài đặt lương, đơn giá giờ công, hệ số OT, chi tiết 12 phụ cấp, thống kê tháng hiện tại (giờ chuẩn, OT ngày, OT đêm, Chủ nhật ngày/đêm, Ngày lễ, tiền từng khoản), và lịch sử các tháng trước.
                - TUYỆT ĐỐI KHÔNG BAO GIỜ nói "không có quyền", "hệ thống khóa dữ liệu" hay "thiếu thông tin".
                
                QUY TẮC TRẢ LỜI (BẮT BUỘC):
                1. TRẢ LỜI ĐÚNG TRỌNG TÂM, NGẮN GỌN, SÚC TÍCH.
                2. VÀO THẲNG CÂU TRẢ LỜI & CON SỐ, KHÔNG CHÀO HỎI LÊ THÊ, KHÔNG DÀI DÒNG VÒNG VÈO.
                3. Khi người dùng hỏi tính tiền (ví dụ: tiền ca đêm Chủ nhật, tiền OT ngày lễ, tiền phụ cấp...): Trình bày ngắn gọn phép tính: [Số giờ/ca] x [Đơn giá/Hệ số] = [KẾT QUẢ TIỀN].
                4. Khi hỏi so sánh: Nêu rõ Tăng/Giảm bao nhiêu tiền (%) giữa tháng này và tháng trước kèm 1-2 lý do chính ngắn gọn.
                
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
