package com.example

import com.example.data.SalaryCalculator
import com.example.data.model.TimeEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class SalaryCalculatorTest {

    private fun getTimeMs(
        year: Int = 2026,
        month: Int = 9,
        day: Int = 15,
        hour: Int,
        minute: Int
    ): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    // 1. Ca1: vào 07:30 ra 19:30 đúng giờ tuyệt đối
    @Test
    fun testCase1_ca1_onTime() {
        val inTime = getTimeMs(hour = 7, minute = 30)
        val outTime = getTimeMs(hour = 19, minute = 30)
        val entry = TimeEntry(
            userId = "user1",
            date = "2026-09-15",
            shiftId = "ca1",
            checkInTime = inTime,
            checkOutTime = outTime
        )

        val result = SalaryCalculator.calculateSingleEntry(entry)

        assertEquals("workDay should be 1.0", 1.0, result.workDay, 0.001)
        assertEquals("otHours should be 4.0", 4.0, result.otHours, 0.001)
        assertEquals("lateMinutes should be 0", 0, result.lateMinutes)
        assertEquals("earlyLeaveMinutes should be 0", 0, result.earlyLeaveMinutes)
    }

    // 2. Ca1: vào trễ 15 phút, ra đúng giờ
    @Test
    fun testCase2_ca1_late15Minutes() {
        val inTime = getTimeMs(hour = 7, minute = 45) // Trễ 15 phút
        val outTime = getTimeMs(hour = 19, minute = 30) // Đúng giờ
        val entry = TimeEntry(
            userId = "user1",
            date = "2026-09-15",
            shiftId = "ca1",
            checkInTime = inTime,
            checkOutTime = outTime
        )

        val result = SalaryCalculator.calculateSingleEntry(entry)

        assertEquals("workDay should be 1.0", 1.0, result.workDay, 0.001)
        assertEquals("otHours should be 3.75", 3.75, result.otHours, 0.001)
        assertEquals("lateMinutes should be 15", 15, result.lateMinutes)
        assertEquals("earlyLeaveMinutes should be 0", 0, result.earlyLeaveMinutes)
    }

    // 3. Ca1: vào đúng giờ, ra sớm 30 phút
    @Test
    fun testCase3_ca1_earlyLeave30Minutes() {
        val inTime = getTimeMs(hour = 7, minute = 30) // Đúng giờ
        val outTime = getTimeMs(hour = 19, minute = 0)  // Ra sớm 30 phút
        val entry = TimeEntry(
            userId = "user1",
            date = "2026-09-15",
            shiftId = "ca1",
            checkInTime = inTime,
            checkOutTime = outTime
        )

        val result = SalaryCalculator.calculateSingleEntry(entry)

        assertEquals("workDay should be 1.0", 1.0, result.workDay, 0.001)
        assertEquals("otHours should be 3.5", 3.5, result.otHours, 0.001)
        assertEquals("lateMinutes should be 0", 0, result.lateMinutes)
        assertEquals("earlyLeaveMinutes should be 30", 30, result.earlyLeaveMinutes)
    }

    // 4. Ca1: vào trễ 210 phút, ra đúng giờ (case đi trễ nửa buổi)
    @Test
    fun testCase4_ca1_late210Minutes() {
        val inTime = getTimeMs(hour = 11, minute = 0) // Trễ 210 phút (3h30p)
        val outTime = getTimeMs(hour = 19, minute = 30) // Ra đúng giờ
        val entry = TimeEntry(
            userId = "user1",
            date = "2026-09-15",
            shiftId = "ca1",
            checkInTime = inTime,
            checkOutTime = outTime
        )

        val result = SalaryCalculator.calculateSingleEntry(entry)

        assertEquals("workDay should be 1.0", 1.0, result.workDay, 0.001)
        assertEquals("otHours should be 0.5", 0.5, result.otHours, 0.001)
        assertEquals("lateMinutes should be 210", 210, result.lateMinutes)
        assertEquals("earlyLeaveMinutes should be 0", 0, result.earlyLeaveMinutes)
    }

    // 5. Ca1: quên chấm ra (checkOutTime = null, isWorking = true)
    @Test
    fun testCase5_ca1_missingCheckOut() {
        val inTime = getTimeMs(hour = 7, minute = 30)
        val entry = TimeEntry(
            userId = "user1",
            date = "2026-09-15",
            shiftId = "ca1",
            checkInTime = inTime,
            checkOutTime = null,
            isWorking = true
        )

        val result = SalaryCalculator.calculateSingleEntry(entry)

        assertEquals("workDay should be 1.0", 1.0, result.workDay, 0.001)
        assertEquals("otHours should be 0.0", 0.0, result.otHours, 0.001)
        assertEquals("lateMinutes should be 0", 0, result.lateMinutes)
        assertEquals("earlyLeaveMinutes should be 0", 0, result.earlyLeaveMinutes)
    }

    // 6. Ca2: vào ra đúng giờ chuẩn (07:30 - 20:00)
    @Test
    fun testCase6_ca2_standardHours() {
        val inTime = getTimeMs(hour = 7, minute = 30)
        val outTime = getTimeMs(hour = 20, minute = 0)
        val entry = TimeEntry(
            userId = "user1",
            date = "2026-09-15",
            shiftId = "ca2",
            checkInTime = inTime,
            checkOutTime = outTime
        )

        val result = SalaryCalculator.calculateSingleEntry(entry)

        assertEquals("workDay should be 1.0", 1.0, result.workDay, 0.001)
        assertEquals("otHours should be 3.0", 3.0, result.otHours, 0.001)
        assertEquals("lateMinutes should be 0", 0, result.lateMinutes)
        assertEquals("earlyLeaveMinutes should be 0", 0, result.earlyLeaveMinutes)
    }

    // 7. Ca_dem: vào 19:30 ra 07:30 hôm sau (qua ngày)
    @Test
    fun testCase7_caDem_crossDay() {
        val inTime = getTimeMs(day = 15, hour = 19, minute = 30)
        val outTime = getTimeMs(day = 16, hour = 7, minute = 30) // Hôm sau
        val entry = TimeEntry(
            userId = "user1",
            date = "2026-09-15",
            shiftId = "ca_dem",
            checkInTime = inTime,
            checkOutTime = outTime
        )

        val result = SalaryCalculator.calculateSingleEntry(entry)

        assertEquals("workDay should be 1.0", 1.0, result.workDay, 0.001)
        assertEquals("otHours should be 4.0", 4.0, result.otHours, 0.001)
        assertEquals("lateMinutes should be 0", 0, result.lateMinutes)
        assertEquals("earlyLeaveMinutes should be 0", 0, result.earlyLeaveMinutes)
    }

    // 8. Case dayType = "PAID_LEAVE" -> workDay phải = 1.0, otHours = 0.0
    @Test
    fun testCase8_paidLeave() {
        val entry = TimeEntry(
            userId = "user1",
            date = "2026-09-15",
            dayType = "PAID_LEAVE"
        )

        val result = SalaryCalculator.calculateSingleEntry(entry)

        assertEquals("workDay should be 1.0", 1.0, result.workDay, 0.001)
        assertEquals("otHours should be 0.0", 0.0, result.otHours, 0.001)
        assertEquals("lateMinutes should be 0", 0, result.lateMinutes)
        assertEquals("earlyLeaveMinutes should be 0", 0, result.earlyLeaveMinutes)
    }

    // 9. Case dayType = "UNPAID_LEAVE" -> workDay phải = 0.0
    @Test
    fun testCase9_unpaidLeave() {
        val entry = TimeEntry(
            userId = "user1",
            date = "2026-09-15",
            dayType = "UNPAID_LEAVE"
        )

        val result = SalaryCalculator.calculateSingleEntry(entry)

        assertEquals("workDay should be 0.0", 0.0, result.workDay, 0.001)
        assertEquals("otHours should be 0.0", 0.0, result.otHours, 0.001)
        assertEquals("lateMinutes should be 0", 0, result.lateMinutes)
        assertEquals("earlyLeaveMinutes should be 0", 0, result.earlyLeaveMinutes)
    }
}
