import java.text.SimpleDateFormat
import java.util.*

fun main() {
    val selectedMonth = "2026-07"
    val parts = selectedMonth.split("-")
    if (parts.size == 2) {
        val yyyy = parts[0]
        val mm = parts[1]
        val maxDay = 31 // just for test
        for (day in 1..2) {
            val dateStr1 = String.format(Locale.US, "%02d/%s/%s", day, mm, yyyy)
            val dateStr2 = String.format(Locale.US, "%s-%s-%02d", yyyy, mm, day)
            println("$dateStr1 | $dateStr2")
        }
    }
}
