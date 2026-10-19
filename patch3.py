import re

with open("app/src/main/java/com/example/notification/NotificationHelper.kt", "r") as f:
    content = f.read()

pattern = re.compile(
    r"    // Ước tính giờ ra ca dựa trên Mode \(Tần suất xuất hiện nhiều nhất\) và chu kỳ đổi ca\n"
    r"    suspend fun estimateHistoricalCheckoutTime\(context: Context, uid: String, activeEntry: TimeEntry\): Long \{.*?"
    r"        return cal\.timeInMillis\n"
    r"    \}",
    re.DOTALL
)

replacement = """    // Ước tính giờ ra ca dựa trên Mode (Tần suất xuất hiện nhiều nhất) và chu kỳ đổi ca
    suspend fun estimateHistoricalCheckoutTime(context: Context, uid: String, activeEntry: TimeEntry): Long {
        var targetHour = -1
        var targetMin = -1

        try {
            val db = com.example.data.db.AppDatabase.getInstance(context)
            val entries = db.timeEntryDao().getLastCompletedEntries(uid, 100)
            
            if (entries.isNotEmpty()) {
                val activeCi = activeEntry.checkInTime ?: System.currentTimeMillis()
                val activeCal = java.util.Calendar.getInstance().apply { timeInMillis = activeCi }
                val isNightShift = activeCal.get(java.util.Calendar.HOUR_OF_DAY) >= 15
                
                val targetEntries = entries.filter { entry ->
                    val ci = entry.checkInTime ?: return@filter false
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ci }
                    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                    if (isNightShift) hour >= 15 else hour < 15
                }.ifEmpty { entries }
                
                val frequencyMap = mutableMapOf<Int, Int>()
                for (entry in targetEntries) {
                    val co = entry.checkOutTime ?: continue
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = co }
                    val rawMins = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
                    val roundedMins = ((rawMins + 7) / 15) * 15 % 1440
                    frequencyMap[roundedMins] = (frequencyMap[roundedMins] ?: 0) + 1
                }

                val mostFrequentMins = frequencyMap.maxByOrNull { it.value }?.key
                if (mostFrequentMins != null) {
                    targetHour = mostFrequentMins / 60
                    targetMin = mostFrequentMins % 60
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (targetHour == -1) {
            targetHour = 17
            targetMin = 30
        }
        val fallbackTargetHour = targetHour
        val fallbackTargetMin = targetMin

        val ciMs = activeEntry.checkInTime ?: System.currentTimeMillis()
        var currentTargetMs = ciMs
        
        var cal = java.util.Calendar.getInstance()
        var found = false
        var loopCount = 0
        
        while (!found && loopCount < 14) {
            val customTime = getEffectiveCheckOutTime(context, currentTargetMs)
            
            if (customTime.isNotBlank() && customTime.contains(":") && customTime.length == 5) {
                val parts = customTime.split(":")
                targetHour = parts.getOrNull(0)?.toIntOrNull() ?: 17
                targetMin = parts.getOrNull(1)?.toIntOrNull() ?: 30
            } else {
                targetHour = fallbackTargetHour
                targetMin = fallbackTargetMin
            }
            
            cal = java.util.Calendar.getInstance().apply {
                timeInMillis = currentTargetMs
                set(java.util.Calendar.HOUR_OF_DAY, targetHour)
                set(java.util.Calendar.MINUTE, targetMin)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            
            // CheckOut có thể qua ngày hôm sau nếu là ca đêm. 
            // Ta đảm bảo Checkout > Checkin. Nếu cal < ciMs, cộng 1 ngày
            if (cal.timeInMillis < ciMs + 4 * 3600 * 1000L) { // Tối thiểu làm 4 tiếng
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            
            if (cal.timeInMillis > System.currentTimeMillis()) {
                found = true
                break
            }
            
            val nextDay = java.util.Calendar.getInstance().apply {
                timeInMillis = currentTargetMs
                add(java.util.Calendar.DAY_OF_YEAR, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            currentTargetMs = nextDay.timeInMillis
            loopCount++
        }
        
        return cal.timeInMillis
    }"""

new_content, count = pattern.subn(replacement, content)
if count > 0:
    with open("app/src/main/java/com/example/notification/NotificationHelper.kt", "w") as f:
        f.write(new_content)
    print("Replaced CheckOut!")
else:
    print("Pattern not found CheckOut.")
